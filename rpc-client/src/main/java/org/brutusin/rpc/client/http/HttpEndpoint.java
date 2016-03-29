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
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.client.RpcRequest;
import org.brutusin.rpc.client.wskt.Callback;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpEndpoint {

    private final URI endpoint;

    private enum HttpMethod {

        GET, POST, PUT
    }

    private final CloseableHttpClient http;

    public HttpEndpoint(URI endpoint, Config cfg) {
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
    }

    private void doExec(Callback callback, String serviceId, JsonNode input, Map<String, File> files, HttpMethod httpMethod) throws IOException {
        RpcRequest request = new RpcRequest();
        request.setMethod(serviceId);
        request.setParams(input);
        final String payload = JsonCodec.getInstance().transform(request);
        HttpUriRequest req;
        if (httpMethod == HttpMethod.GET) {
            String urlparam = URLEncoder.encode(payload, "UTF-8");
            req = new HttpGet(this.endpoint + "?jsonrpc=" + urlparam);
        } else if (httpMethod == HttpMethod.POST) {
            HttpPost post = new HttpPost(this.endpoint);
            req = post;
            HttpEntity entity;
            if (files == null) {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("jsonrpc", payload));
                entity = new UrlEncodedFormEntity(nvps);
            } else {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.STRICT);
                builder.addPart("jsonrpc",  new StringBody(payload, ContentType.APPLICATION_JSON));
                for (Map.Entry<String, File> entrySet : files.entrySet()) {
                    String key = entrySet.getKey();
                    File value = entrySet.getValue();
                    builder.addPart(key, new FileBody(value));
                }
                entity = builder.build();
            }
            post.setEntity(entity);

        } else if (httpMethod == HttpMethod.PUT) {
            HttpPut put = new HttpPut(this.endpoint);
            req = put;
            HttpEntity entity;
            if (files == null) {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("jsonrpc", payload));
                entity = new UrlEncodedFormEntity(nvps);
            } else {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.STRICT);
                builder.addPart("jsonrpc",  new StringBody(payload, ContentType.APPLICATION_JSON));
                for (Map.Entry<String, File> entrySet : files.entrySet()) {
                    String key = entrySet.getKey();
                    File value = entrySet.getValue();
                    builder.addPart(key, new FileBody(value));
                }
                entity = builder.build();
            }
            put.setEntity(entity);
        } else {
            throw new AssertionError();
        }
        CloseableHttpResponse resp = this.http.execute(req);
        System.out.println(resp.getStatusLine().getStatusCode());
        System.out.println(resp.getEntity().getContentType());
        System.out.println(Arrays.toString(resp.getEntity().getContentType().getElements()));
        System.out.println(Miscellaneous.toString(resp.getEntity().getContent(), "UTF-8"));
    }

    public void close() throws IOException {
        this.http.close();
    }

    public static void main(String[] args) throws Exception {
        HttpEndpoint endpoint = new HttpEndpoint(new URI("http://localhost:8080/rpc/http"), null);
        HashMap<String,File> files = new HashMap<String, File>();
        files.put("file1", new File("C:/temp/1.fastq"));
        files.put("file2", new File("C:/temp/4.pileup"));
        endpoint.doExec(null, "upload", JsonCodec.getInstance().parse("{\"inputStreams\":[\"file1\",\"file2\"]}"), files, HttpEndpoint.HttpMethod.PUT);
        
        
        
    }
    
}
