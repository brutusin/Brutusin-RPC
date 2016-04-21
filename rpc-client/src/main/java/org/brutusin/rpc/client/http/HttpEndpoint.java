/*
 * Copyright 2016 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.rpc.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.RpcErrorCode;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.client.ProgressCallback;
import org.brutusin.rpc.http.CachingInfo;
import org.brutusin.rpc.http.HttpServiceItem;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpEndpoint {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final Logger LOGGER = Logger.getLogger(HttpEndpoint.class.getName());

    private final ThreadLocal<HttpClientContext> contexts = new ThreadLocal<HttpClientContext>();
    private volatile boolean loaded;

    private final URI endpoint;

    private enum HttpMethod {

        GET, POST, PUT
    }

    private final CloseableHttpClient httpClient;
    private final HttpClientContextFactory clientContextFactory;
    private Map<String, HttpServiceItem> services;

    private Thread pingThread;

    public HttpEndpoint(URI endpoint) {
        this(endpoint, (Config) null, null);
    }

    public HttpEndpoint(URI endpoint, HttpClientContextFactory clientContextFactory) {
        this(endpoint, (Config) null, clientContextFactory);
    }

    public HttpEndpoint(URI endpoint, Config cfg, HttpClientContextFactory clientContextFactory) {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required");
        }
        if (cfg == null) {
            cfg = new ConfigurationBuilder().build();
        }
        CacheConfig cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(cfg.getMaxCacheEntries())
                .setMaxObjectSize(cfg.getMaxCacheObjectSize())
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000 * cfg.getConnectTimeOutSeconds())
                .setSocketTimeout(1000 * cfg.getSocketTimeOutSeconds())
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(cfg.getMaxConections());

        this.endpoint = endpoint;
        this.httpClient = CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(new StandardHttpRequestRetryHandler())
                .setConnectionManager(cm)
                .build();
        this.clientContextFactory = clientContextFactory;
        initPingThread(cfg.getPingSeconds());
    }

    public HttpEndpoint(URI endpoint, CloseableHttpClient httpClient) throws IOException {
        this(endpoint, httpClient, null);
    }

    public HttpEndpoint(URI endpoint, CloseableHttpClient httpClient, HttpClientContextFactory clientContextFactory) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required");
        }
        if (httpClient == null) {
            throw new IllegalArgumentException("HttpClient is required");
        }
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.clientContextFactory = clientContextFactory;
        initPingThread(new ConfigurationBuilder().build().getPingSeconds());
    }

    private void initPingThread(final int pingSeconds) {
        this.pingThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(1000 * pingSeconds);
                        try {
                            CloseableHttpResponse resp = doExec("rpc.http.ping", null, HttpMethod.GET, null);
                            resp.close();
                        } catch (ConnectException ex) {
                            LOGGER.log(Level.SEVERE, ex.getMessage() + " (" + HttpEndpoint.this.endpoint + ")");
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, ex.getMessage() + " (" + HttpEndpoint.this.endpoint + ")", ex);
                        }
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        };
        this.pingThread.setDaemon(true);
        this.pingThread.start();
    }

    private void loadServices() throws IOException {
        CloseableHttpResponse servicesResp = doExec("rpc.http.services", null, HttpMethod.GET, null);
        if (servicesResp.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Server returned status code " + servicesResp.getStatusLine().getStatusCode());
        }
        if (!servicesResp.getEntity().getContentType().getValue().contains(JSON_CONTENT_TYPE)) {
            throw new RuntimeException("Server returned response of type " + servicesResp.getEntity().getContentType().getValue() + ":\n" + Miscellaneous.toString(servicesResp.getEntity().getContent(), "UTF-8"));
        }
        HashMap serviceMap = new HashMap<String, HttpServiceItem>();
        try {
            JsonNode node = JsonCodec.getInstance().parse(Miscellaneous.toString(servicesResp.getEntity().getContent(), "UTF-8")).get("result");
            for (int i = 0; i < node.getSize(); i++) {
                JsonNode service = node.get(i);
                HttpServiceItem si = JsonCodec.getInstance().load(service, HttpServiceItem.class);
                serviceMap.put(si.getId(), si);
            }
            this.services = Collections.unmodifiableMap(serviceMap);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Map<String, HttpServiceItem> getServices() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    try {
                        loadServices();
                    } catch (IOException iOException) {
                        throw new RuntimeException(iOException);
                    }
                    loaded = true;
                }
            }
        }
        return services;
    }

    private static Map<String, InputStream> sortFiles(final Map<String, InputStream> files) {
        if (files == null) {
            return null;
        }

        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String s1, String s2) {
                if (s1.equals(s2)) {
                    return 0;
                }
                InputStream f1 = files.get(s1);
                InputStream f2 = files.get(s2);
                if (f1 == null || !(f1 instanceof MetaDataInputStream) || ((MetaDataInputStream) f1).getLength() == null) {
                    return -1;
                }
                if (f2 == null || !(f2 instanceof MetaDataInputStream) || ((MetaDataInputStream) f2).getLength() == null) {
                    return 1;
                }
                return Long.compare(((MetaDataInputStream) f1).getLength(), ((MetaDataInputStream) f2).getLength());
            }
        };
        Map<String, InputStream> ret = new TreeMap<String, InputStream>(comparator);
        ret.putAll(files);
        return ret;
    }

    private static String getAttachmentFileName(Header contentDiposition) {
        if (contentDiposition == null) {
            return null;
        }
        String value = contentDiposition.getValue();
        Pattern pat = Pattern.compile(".*filename[^;=\n]*=((['\"]).*?\\2|[^;\n]*)");
        Matcher matcher = pat.matcher(value);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private static HttpMethod getMethod(HttpServiceItem si) {
        if (si == null) {
            return null;
        }
        if (si.isSafe()) {
            return HttpMethod.GET;
        } else {
            if (si.isIdempotent()) {
                return HttpMethod.PUT;
            } else {
                return HttpMethod.POST;
            }
        }
    }

    /**
     *
     * @param serviceId
     * @param input supports inputstreams
     * @param progressCallback
     * @return
     * @throws IOException
     */
    public final HttpResponse exec(final String serviceId, final JsonNode input, final ProgressCallback progressCallback) throws IOException {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    loadServices();
                    loaded = true;
                }
            }
        }
        final HttpMethod method = getMethod(services.get(serviceId));
        if (method == null) {
            throw new IllegalArgumentException("Invalid service id " + serviceId);
        }
        HttpResponse ret = new HttpResponse();
        try {
            CloseableHttpResponse resp = doExec(serviceId, input, method, progressCallback);
            if (resp.getEntity() == null) {
                throw new RuntimeException(resp.getStatusLine().toString());
            }
            Header cacheControl = resp.getFirstHeader("Cache-Control");
            if (cacheControl != null) {
                HeaderElement[] elements = cacheControl.getElements();
                if (elements != null) {
                    for (HeaderElement element : elements) {
                        if (element.getName().equals("no-cache")) {
                            break;
                        }
                        if (ret.getCachingInfo() == null) {
                            ret.setCachingInfo(new CachingInfo(0, true, false));
                        }
                        if (element.getName().equals("max-age")) {
                            ret.getCachingInfo().setMaxAge(Integer.valueOf(element.getValue()));
                        }
                        if (element.getName().equals("public")) {
                            ret.getCachingInfo().setShared(true);
                        }
                        if (element.getName().equals("private")) {
                            ret.getCachingInfo().setShared(false);
                        }
                        if (element.getName().equals("no-store")) {
                            ret.getCachingInfo().setStore(false);
                        }
                    }
                }
            }
            if (!resp.getEntity().getContentType().getValue().startsWith(JSON_CONTENT_TYPE)) {
                MetaDataInputStream is = new MetaDataInputStream(resp.getEntity().getContent(), getAttachmentFileName(resp.getFirstHeader("Content-Disposition")), resp.getEntity().getContentType().getValue(), resp.getEntity().getContentLength(), null);
                ret.setInputStream(is);
            } else {
                JsonNode responseNode = JsonCodec.getInstance().parse(Miscellaneous.toString(resp.getEntity().getContent(), "UTF-8"));
                RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
                if (responseNode.get("error") != null) {
                    rpcResponse.setError(JsonCodec.getInstance().load(responseNode.get("error"), RpcResponse.Error.class));
                }
                rpcResponse.setResult(responseNode.get("result"));
                ret.setRpcResponse(rpcResponse);
            }
        } catch (ConnectException ex) {
            RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
            rpcResponse.setError(new RpcResponse.Error(RpcErrorCode.connectionError, ex.getMessage()));
            ret.setRpcResponse(rpcResponse);
        } catch (Throwable t) {
            RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
            rpcResponse.setError(new RpcResponse.Error(RpcErrorCode.internalError, t.getMessage()));
            ret.setRpcResponse(rpcResponse);
        }

        return ret;
    }

    private CloseableHttpResponse doExec(String serviceId, JsonNode input, HttpMethod httpMethod, final ProgressCallback progressCallback) throws IOException {
        RpcRequest request = new RpcRequest();
        request.setJsonrpc("2.0");
        request.setMethod(serviceId);
        request.setParams(input);
        final String payload = JsonCodec.getInstance().transform(request);
        final HttpUriRequest req;
        if (httpMethod == HttpMethod.GET) {
            String urlparam = URLEncoder.encode(payload, "UTF-8");
            req = new HttpGet(this.endpoint + "?jsonrpc=" + urlparam);
        } else {
            HttpEntityEnclosingRequestBase reqBase;
            if (httpMethod == HttpMethod.POST) {
                reqBase = new HttpPost(this.endpoint);
            } else if (httpMethod == HttpMethod.PUT) {
                reqBase = new HttpPut(this.endpoint);
            } else {
                throw new AssertionError();
            }
            req = reqBase;
            HttpEntity entity;
            Map<String, InputStream> files = JsonCodec.getInstance().getStreams(input);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.STRICT);
            builder.addPart("jsonrpc", new StringBody(payload, ContentType.APPLICATION_JSON));
            if (files != null && !files.isEmpty()) {
                files = sortFiles(files);
                for (Map.Entry<String, InputStream> entrySet : files.entrySet()) {
                    String key = entrySet.getKey();
                    InputStream is = entrySet.getValue();
                    if (is instanceof MetaDataInputStream) {
                        MetaDataInputStream mis = (MetaDataInputStream) is;
                        builder.addPart(key, new InputStreamBody(mis, mis.getName()));
                    } else {
                        builder.addPart(key, new InputStreamBody(is, key));
                    }
                }
            }
            entity = builder.build();
            if (progressCallback != null) {
                entity = new ProgressHttpEntityWrapper(entity, progressCallback);
            }
            reqBase.setEntity(entity);
        }
        HttpClientContext context = contexts.get();
        if (this.clientContextFactory != null && context == null) {
            context = clientContextFactory.create();
            contexts.set(context);
        }
        return this.httpClient.execute(req, context);
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public void close() throws IOException {
        this.pingThread.interrupt();
        this.httpClient.close();
    }

    public static void main(String[] args) throws Exception {

        HttpClientContextFactory ctxFact = new HttpClientContextFactory() {
            public HttpClientContext create() {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        new AuthScope("localhost", 8080, AuthScope.ANY_REALM, "basic"),
                        new UsernamePasswordCredentials("user", "password"));
                HttpClientContext context = HttpClientContext.create();
                context.setCredentialsProvider(credsProvider);
                return context;
            }
        };

        HttpEndpoint endpoint = new HttpEndpoint(new URI("http://localhost:8080/rpc/http"), ctxFact);

        HttpResponse resp = endpoint.exec("rpc.http.version", null, null);
        if (resp.isIsBinary()) {
            System.out.println("binary");
            System.out.println(resp.getInputStream().getName());
        } else {
            System.out.println(resp.getRpcResponse().getResult());
        }
    }
}
