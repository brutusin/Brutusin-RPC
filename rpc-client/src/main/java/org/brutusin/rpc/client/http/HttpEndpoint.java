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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.RpcErrorCode;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.client.ProgressCallback;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpEndpoint {

    public static final String JSON_CONTENT_TYPE = "application/json";

    private final URI endpoint;

    private enum HttpMethod {

        GET, POST, PUT
    }

    private final CloseableHttpClient http;
    private final Map<String, HttpMethod> serviceMap;

    public HttpEndpoint(URI endpoint, Config cfg) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint is required");
        }
        this.endpoint = endpoint;
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

        this.http = CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(new StandardHttpRequestRetryHandler())
                .setConnectionManager(cm)
                .build();

        CloseableHttpResponse servicesResp = doExec("rpc.http.services", null, null, HttpMethod.GET, null);
        try {
            JsonNode services = JsonCodec.getInstance().parse(Miscellaneous.toString(servicesResp.getEntity().getContent(), "UTF-8")).get("result");
            this.serviceMap = new HashMap<String, HttpMethod>();
            for (int i = 0; i < services.getSize(); i++) {
                JsonNode service = services.get(i);
                HttpMethod method;
                if (service.get("safe").asBoolean()) {
                    method = HttpMethod.GET;
                } else {
                    if (service.get("idempotent").asBoolean()) {
                        method = HttpMethod.PUT;
                    } else {
                        method = HttpMethod.POST;
                    }
                }
                serviceMap.put(service.get("id").asString(), method);
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Map<String, File> sortFiles(final Map<String, File> files) {
        if (files == null) {
            return null;
        }
        Comparator<String> comparator = new Comparator<String>() {
            public int compare(String s1, String s2) {
                if (s1.equals(s2)) {
                    return 0;
                }
                File f1 = files.get(s1);
                File f2 = files.get(s2);
                if (f1 == null) {
                    return -1;
                }
                if (f2 == null) {
                    return 1;
                }
                return Long.compare(f1.length(), f2.length());
            }
        };
        Map<String, File> ret = new TreeMap<String, File>(comparator);
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

    public void exec(final HttpCallback callback, final String serviceId, final JsonNode input, final Map<String, File> files, final ProgressCallback progressCallback) throws IOException {
        final HttpMethod method = serviceMap.get(serviceId);
        if (method == null) {
            throw new IllegalArgumentException("Invalid service id " + serviceId);
        }
        Thread t;
        t = new Thread() {
            @Override
            public void run() {
                try {
                    CloseableHttpResponse resp = doExec(serviceId, input, files, method, progressCallback);
                    if (resp.getEntity() == null) {
                        throw new RuntimeException(resp.getStatusLine().toString());
                    }
                    if (!resp.getEntity().getContentType().getValue().startsWith(JSON_CONTENT_TYPE)) {
                        MetaDataInputStream is = new MetaDataInputStream(resp.getEntity().getContent(), getAttachmentFileName(resp.getFirstHeader("Content-Disposition")), resp.getEntity().getContentType().getValue(), resp.getEntity().getContentLength(), null);
                        if (callback != null) {
                            callback.callBinary(is);
                        }
                    } else {
                        RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
                        rpcResponse.setResult(JsonCodec.getInstance().parse(Miscellaneous.toString(resp.getEntity().getContent(), "UTF-8")));
                        if (callback != null) {
                            callback.call(rpcResponse);
                        }
                    }
                } catch (ConnectException ex) {
                    RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
                    rpcResponse.setError(new RpcResponse.Error(RpcErrorCode.connectionError, ex));
                    if (callback != null) {
                        callback.call(rpcResponse);
                    }
                } catch (Throwable t) {
                    RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
                    rpcResponse.setError(new RpcResponse.Error(RpcErrorCode.internalError, t));
                    if (callback != null) {
                        callback.call(rpcResponse);
                    }
                }
            }
        };

        t.setDaemon(true);
        t.start();

    }

    private CloseableHttpResponse doExec(String serviceId, JsonNode input, Map<String, File> files, HttpMethod httpMethod, final ProgressCallback progressCallback) throws IOException {
        files = sortFiles(files);
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
            if (files == null) {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("jsonrpc", payload));
                entity = new UrlEncodedFormEntity(nvps);
            } else {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.STRICT);
                builder.addPart("jsonrpc", new StringBody(payload, ContentType.APPLICATION_JSON));
                for (Map.Entry<String, File> entrySet : files.entrySet()) {
                    String key = entrySet.getKey();
                    File value = entrySet.getValue();
                    builder.addPart(key, new FileBody(value));
                }
                entity = builder.build();
                if (progressCallback != null) {
                    entity = new ProgressHttpEntityWrapper(entity, progressCallback);
                }
            }
            reqBase.setEntity(entity);
        }
        return this.http.execute(req);
    }

    public void close() throws IOException {
        this.http.close();
    }

    public static void main(String[] args) throws Exception {
        final CountDownLatch counter = new CountDownLatch(1);
        HttpEndpoint endpoint = new HttpEndpoint(new URI("http://localhost:8080/rpc/http"), null);
        HttpCallback callback = new HttpCallback() {
            public void callBinary(MetaDataInputStream is) {
                System.out.println("binary");
                System.out.println(is.getName());
                counter.countDown();
            }

            public void call(RpcResponse<JsonNode> response) {
                System.out.println(response.getResult());
                counter.countDown();
            }
        };
//        HashMap<String, File> files = new HashMap<String, File>();
//        files.put("file1", new File("C:/temp/1.fastq"));
//        files.put("file2", new File("C:/temp/1.chr5.sam"));
//        files.put("file3", new File("C:/temp/4.pileup"));
//        endpoint.doExec(callback, "upload", JsonCodec.getInstance().parse("{\"inputStreams\":[\"file1\",\"file2\"]}"), files, HttpEndpoint.HttpMethod.PUT, new ProgressCallback() {
//            public void progress(float progress) {
//                System.out.println(progress);
//            }
//        });
        endpoint.exec(callback, "logo", JsonCodec.getInstance().parse("{\"attachment\":true,\"attachmentName\":\"aaa.jpg\"}"), null, null);
        counter.await();
    }

}
