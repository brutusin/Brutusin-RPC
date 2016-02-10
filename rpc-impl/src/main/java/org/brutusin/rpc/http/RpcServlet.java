/*
 * Copyright 2015 brutusin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.rpc.http;

import org.brutusin.rpc.RpcResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.commons.utils.CryptoUtils;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.exception.MaxLengthExceededException;
import org.brutusin.rpc.RpcConfig;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.exception.InvalidHttpMethodException;
import org.brutusin.rpc.exception.InvalidRequestException;
import org.brutusin.rpc.exception.RpcErrorCode;
import org.brutusin.rpc.exception.ServiceNotFoundException;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.SpringContextImpl;
import org.brutusin.rpc.RpcUtils;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcServlet extends HttpServlet {

    public static final String JSON_CONTENT_TYPE = "application/json";

    private static final String REQ_ATT_MULTIPART_PARAMS = "multipartParams";
    private static final String REQ_ATT_MULTIPART_ITERATOR = "multipartIterator";
    private static final String REQ_ATT_MULTIPART_CURRENT_ITEM = "multipartCurrentItem";
    private static final String REQ_ATT_TEMPORARY_FOLDER = "tempFolder";

    public static final String PARAM_PAYLOAD = "jsonrpc";

    private int uploadCounter;

    private Map<String, HttpAction> services;

    @Override
    public void init(ServletConfig config) throws ServletException {
        SpringContextImpl rpcApplicationContext = (SpringContextImpl) WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        services = rpcApplicationContext.getHttpServices();
        try {
            if (RpcConfig.getUploadFolder().exists()) {
                Miscellaneous.cleanDirectory(RpcConfig.getUploadFolder());
            } else {
                Miscellaneous.createDirectory(RpcConfig.getUploadFolder());
            }
        } catch (Exception ex) {
            Logger.getLogger(RpcServlet.class.getName()).log(Level.SEVERE, null, ex);
            throw new ServletException(ex);
        }
    }

    /**
     *
     * @param request
     * @return
     */
    private static boolean isMultipartContent(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase();
        if (!method.equals("POST") && !method.equals("PUT")) {
            return false;
        }
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase(Locale.ENGLISH).startsWith("multipart");
    }

    /**
     *
     * @param req
     * @return
     * @throws IOException
     */
    private static Map<String, String[]> parseMultipartParameters(HttpServletRequest req) throws IOException {
        if (isMultipartContent(req)) {
            Map<String, String[]> multipartParameters = new HashMap();
            Map<String, List<String>> map = new HashMap();
            try {
                ServletFileUpload upload = new ServletFileUpload();
                FileItemIterator iter = upload.getItemIterator(req);
                req.setAttribute(REQ_ATT_MULTIPART_ITERATOR, iter);
                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    if (!item.isFormField()) {
                        req.setAttribute(REQ_ATT_MULTIPART_CURRENT_ITEM, item);
                        break;
                    }
                    List<String> list = map.get(item.getFieldName());
                    if (list == null) {
                        list = new ArrayList();
                        map.put(item.getFieldName(), list);
                    }
                    String encoding = req.getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }
                    list.add(Miscellaneous.toString(item.openStream(), encoding));
                }
            } catch (FileUploadException ex) {
                throw new RuntimeException(ex);
            }
            for (Map.Entry<String, List<String>> entrySet : map.entrySet()) {
                String key = entrySet.getKey();
                List<String> value = entrySet.getValue();
                multipartParameters.put(key, value.toArray(new String[value.size()]));
            }
            return multipartParameters;
        }
        return null;
    }

    /**
     *
     * @param req
     * @return
     * @throws IOException
     */
    private static Map<String, String[]> getParameterMap(HttpServletRequest req) throws IOException {
        if (!isMultipartContent(req)) {
            return req.getParameterMap();
        } else {
            Object params = req.getAttribute(REQ_ATT_MULTIPART_PARAMS);
            if (params == null) {
                params = parseMultipartParameters(req);
                req.setAttribute(REQ_ATT_MULTIPART_PARAMS, params);
            }
            return (Map<String, String[]>) params;
        }
    }

    /**
     *
     * @param paramName
     * @param req
     * @return
     * @throws IOException
     */
    private static String getParameter(String paramName, HttpServletRequest req) throws IOException {
        Map<String, String[]> map = getParameterMap(req);
        if (map == null) {
            return null;
        }
        String[] array = map.get(paramName);
        if (array == null) {
            return null;
        }
        if (isMultipartContent(req)) {
            return array[0];
        } else {
            return new String(array[0].getBytes(RpcConfig.getServerUriEncoding()), "UTF-8");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        addFixedHeaders(resp);
        super.doOptions(req, resp);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    /**
     *
     * @return @throws IOException
     */
    private File createTempUploadDirectory() throws IOException {
        synchronized (RpcConfig.getUploadFolder()) {
            File ret = new File(RpcConfig.getUploadFolder(), String.valueOf(uploadCounter++));
            Miscellaneous.createDirectory(ret);
            return ret;
        }
    }

    /**
     *
     * @param req
     * @throws IOException
     */
    private void deleteTempUploadDirectory(HttpServletRequest req) throws IOException {
        File tempDirectory = (File) req.getAttribute(REQ_ATT_TEMPORARY_FOLDER);
        if (tempDirectory != null) {
            Miscellaneous.deleteDirectory(tempDirectory);
        }
    }

    /**
     *
     * @param req
     * @param rpcRequest
     * @param service
     * @return
     * @throws Exception
     */
    private Map<String, InputStream> getStreams(HttpServletRequest req, RpcRequest rpcRequest, HttpAction service) throws Exception {
        if (!ServletFileUpload.isMultipartContent(req)) {
            return null;
        }
        int streamsNumber = getInputStreamsNumber(rpcRequest, service);
        boolean isResponseStreamed = StreamResult.class.isAssignableFrom(RpcUtils.getClass(service.getOutputType()));
        FileItemIterator iter = (FileItemIterator) req.getAttribute(REQ_ATT_MULTIPART_ITERATOR);
        int count = 0;
        final Map<String, InputStream> map = new HashMap();
        final File tempDirectory;
        if (streamsNumber > 1 || streamsNumber == 1 && isResponseStreamed) {
            tempDirectory = createTempUploadDirectory();
            req.setAttribute(REQ_ATT_TEMPORARY_FOLDER, tempDirectory);
        } else {
            tempDirectory = null;
        }
        FileItemStream item = (FileItemStream) req.getAttribute(REQ_ATT_MULTIPART_CURRENT_ITEM);
        long availableLength = RpcConfig.getMaxRequestSize();
        while (item != null) {
            count++;
            long maxLength = Math.min(availableLength, RpcConfig.getMaxFileSize());
            if (count < streamsNumber || isResponseStreamed) { // if response is streamed all inputstreams have to be readed first
                File file = new File(tempDirectory, item.getFieldName());
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    Miscellaneous.pipeSynchronously(new LimitedLengthInputStream(item.openStream(), maxLength), fos);
                } catch (MaxLengthExceededException ex) {
                    if (maxLength == RpcConfig.getMaxFileSize()) {
                        throw new MaxLengthExceededException("Upload part '" + item.getFieldName() + "' exceeds maximum length (" + RpcConfig.getMaxFileSize() + " bytes)", RpcConfig.getMaxFileSize());
                    } else {
                        throw new MaxLengthExceededException("Request exceeds maximum length (" + RpcConfig.getMaxRequestSize() + " bytes)", RpcConfig.getMaxRequestSize());
                    }
                }
                map.put(item.getFieldName(), new MetaDataInputStream(new FileInputStream(file), item.getName(), item.getContentType(), file.length(), null));
                availableLength -= file.length();
            } else if (count == streamsNumber) {
                map.put(item.getFieldName(), new MetaDataInputStream(new LimitedLengthInputStream(item.openStream(), maxLength), item.getName(), item.getContentType(), null, null));
                break;
            }
            req.setAttribute(REQ_ATT_MULTIPART_CURRENT_ITEM, item);
            if (iter.hasNext()) {
                item = iter.next();
            } else {
                item = null;
            }
        }
        if (count != streamsNumber) {
            throw new IllegalArgumentException("Invalid multipart request received. Number of uploaded files (" + count + ") does not match expected (" + streamsNumber + ")");
        }
        return map;
    }

    /**
     *
     * @param req
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private RpcRequest getRequest(HttpServletRequest req) throws IOException, ParseException {
        String payload = getParameter(PARAM_PAYLOAD, req);
        if (payload == null) {
            throw new InvalidRequestException("Parameter '" + PARAM_PAYLOAD + "' is required");
        }
        return JsonCodec.getInstance().parse(payload, RpcRequest.class);
    }

    /**
     *
     * @param req
     * @return
     */
    private String getETag(HttpServletRequest req) {
        if (req.getMethod().equals("POST")) {
            // 304 (Not Modified) cannot be returned to a POST request. So If-None-Match is ignored, despite of not being present in a HTTP 1.1 compliant POST request
            return null;
        } else {
            String reqETag = req.getHeader("If-None-Match");
            if (reqETag != null && reqETag.startsWith("W/\"")) {
                reqETag = reqETag.substring(3, reqETag.length() - 1);
            }
            return reqETag;
        }
    }

    /**
     *
     * @param resp
     * @throws IOException
     */
    private void addFixedHeaders(HttpServletResponse resp) throws IOException {
        resp.addHeader("X-Powered-By", "brutusin-rpc");
        if (RpcConfig.getAccessControlOriginHost() != null) {
            resp.addHeader("Access-Control-Allow-Origin", RpcConfig.getAccessControlOriginHost());
            resp.addHeader("Access-Control-Allow-Methods", "HEAD, GET, POST, PUT, OPTIONS");
            resp.addHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Type, Content-Length");
        }
    }

    /**
     *
     * @param request
     * @return
     */
    private Object execute(HttpServletRequest req, RpcRequest request) throws Exception {
        if (request == null || !"2.0".equals(request.getJsonrpc())) {
            throw new InvalidRequestException("Only JSON-RPC 2.0 supported");
        }
        String serviceId = request.getMethod();
        if (serviceId == null || !services.containsKey(serviceId)) {
            throw new ServiceNotFoundException();
        }
        HttpAction service = services.get(serviceId);
        if (service instanceof UnsafeAction && req.getMethod().equals("GET")) {
            throw new InvalidHttpMethodException("Action is unsafe. Only POST or PUT methods are allowed");
        }
        if (req.getMethod().equals("PUT") && !service.isIdempotent()) {
            throw new InvalidHttpMethodException("Action is not idempotent. Only POST method is allowed");
        }
        Object input;
        if (request.getParams() == null) {
            input = null;
        } else {
            Type inputType = service.getInputType();
            JsonSchema inputSchema = JsonCodec.getInstance().getSchema(inputType);
            inputSchema.validate(request.getParams());
            Map<String, InputStream> streams = getStreams(req, request, service);
            input = JsonCodec.getInstance().parse(request.getParams().toString(), RpcUtils.getClass(inputType), streams).getElement1();
        }
        return service.execute(input);
    }

    private int getInputStreamsNumber(RpcRequest rpcRequest, HttpAction service) throws ParseException {
        Class<?> inputClass = RpcUtils.getClass(service.getInputType());
        return JsonCodec.getInstance().parse(rpcRequest.getParams().toString(), inputClass, null).getElement2();
    }

    /**
     * Does the work
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void execute(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        CachingInfo cachingInfo = null;
        Object result = null;
        Throwable throwable = null;
        RpcRequest rpcRequest = null;
        try {
            HttpActionSupportImpl.setInstance(new HttpActionSupportImpl(req, resp));
            rpcRequest = getRequest(req);
            result = execute(req, rpcRequest);
            if (result != null && result instanceof Cacheable) {
                Cacheable cacheable = (Cacheable) result;
                cachingInfo = cacheable.getCachingInfo();
                result = cacheable.getValue();
            }
        } catch (Throwable th) {
            throwable = th;
        }
        String reqETag = getETag(req);
        addFixedHeaders(resp);
        resp.setCharacterEncoding("UTF-8");

        try {
            if (result != null && StreamResult.class.isAssignableFrom(result.getClass())) {
                serviceStream(reqETag, req, resp, (StreamResult) result, cachingInfo);
            } else {
                RpcResponse rpcResp = new RpcResponse();
                if (rpcRequest != null) {
                    rpcResp.setId(rpcRequest.getId());
                }
                rpcResp.setError(RpcResponse.Error.from(throwable));
                rpcResp.setResult(result);
                serviceJsonResponse(reqETag, req, resp, rpcResp, cachingInfo);
            }
        } finally {
            HttpActionSupportImpl.clear();
            deleteTempUploadDirectory(req);
        }
    }

    /**
     *
     * @param error
     * @param resp
     */
    private static void setStatusCode(RpcResponse.Error error, HttpServletResponse resp) {
        if (error.getCode() == RpcErrorCode.internalError.getCode()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else if (error.getCode() == RpcErrorCode.methodNotFound.getCode()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else if (error.getCode() == RpcErrorCode.securityError.getCode()) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (error.getCode() == RpcErrorCode.applicationError.getCode()) {
            // Application error is considered another successful outcome     
        } else if (error.getCode() == RpcErrorCode.invalidHttpMethodError.getCode()) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     *
     * @param reqEtag
     * @param req
     * @param resp
     * @param rpcResponse
     * @param cachingInfo
     * @throws IOException
     */
    private void serviceJsonResponse(String reqEtag, HttpServletRequest req, HttpServletResponse resp, RpcResponse rpcResponse, CachingInfo cachingInfo) throws IOException {
        if (rpcResponse.getError() != null) {
            setStatusCode(rpcResponse.getError(), resp);
        }
        String json = JsonCodec.getInstance().transform(rpcResponse);
        resp.setContentType(JSON_CONTENT_TYPE);

        String eTag = null;
        if (cachingInfo != null) {
            if (json == null) {
                eTag = CryptoUtils.getHashMD5("null");
            } else {
                eTag = CryptoUtils.getHashMD5(json);
            }
        }
        addCacheHeaders(req, resp, cachingInfo, eTag);
        if (reqEtag != null && reqEtag.equals(eTag)) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            resp.getWriter().print(json);
        }
    }

    /**
     *
     * @param reqEtag
     * @param req
     * @param resp
     * @param resultStream
     * @param cachingInfo
     * @throws IOException
     */
    private void serviceStream(String reqEtag, HttpServletRequest req, HttpServletResponse resp, StreamResult resultStream, CachingInfo cachingInfo) throws IOException {
        String eTag = null;
        if (cachingInfo != null && resultStream.getStream().getLastModified() != null) {
            eTag = CryptoUtils.getHashMD5(String.valueOf(resultStream.getStream().getLastModified()));
        }
        addCacheHeaders(req, resp, cachingInfo, eTag);

        MetaDataInputStream stream = null;

        if (resultStream != null && resultStream.getStream() != null) {
            stream = resultStream.getStream();
            if (stream.getLength() != null) {
                resp.setHeader("Content-Length", String.valueOf(stream.getLength()));
            }
            if (stream.getName() != null) {
                resp.setContentType("application/octet-stream");
                resp.setHeader("Content-Disposition", "attachment; filename=" + stream.getName());
            } else {
                if (stream.getContentType() != null) {
                    resp.setContentType(stream.getContentType());
                } else {
                    resp.setContentType("application/octet-stream");
                }
            }
        }
        if (reqEtag != null && reqEtag.equals(eTag)) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else if (stream != null) {
            Miscellaneous.pipeSynchronously(stream, resp.getOutputStream());
        }
    }

    /**
     *
     * @param req
     * @param resp
     * @param cachingInfo
     * @param etag
     * @throws IOException
     */
    private void addCacheHeaders(HttpServletRequest req, HttpServletResponse resp, CachingInfo cachingInfo, String etag) throws IOException {
        // max-age overrides expires. For legacy proxies (intermedy) cache control is ignored and no cache is performed, the desired behaviour for a private cache. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3
        resp.addDateHeader("Expires", 0);
        if (cachingInfo == null) {
            resp.addHeader("Cache-Control", "max-age=0, no-cache, no-store");
            resp.addHeader("Pragma", "no-cache");
        } else {
            StringBuilder cacheControl = new StringBuilder("max-age=").append(cachingInfo.getMaxAge());
            if (cachingInfo.isShared()) {
                cacheControl.append(", public");
            } else {
                cacheControl.append(", private");
            }
            if (!cachingInfo.isStore()) {
                cacheControl.append(", no-store");
            }
            cacheControl.append(", must-revalidate");
            resp.addHeader("Cache-Control", cacheControl.toString());
            if (etag != null) {
                resp.setHeader("ETag", "W/\"" + etag + "\"");
            }
            if (req.getMethod().equals("POST")) {
                addContentLocation(req, resp);
            }
        }
    }

    /**
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private static void addContentLocation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuffer requestURL = req.getRequestURL();
        Map<String, String[]> parameterMap = getParameterMap(req);
        boolean first = true;
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = entry.getKey();
            String[] value = entry.getValue();
            for (int i = 0; i < value.length; i++) {
                if (first) {
                    first = false;
                    requestURL.append("?");
                } else {
                    requestURL.append("&");
                }
                try {
                    requestURL.append(name).append("=").append(URLEncoder.encode(value[i], resp.getCharacterEncoding()));
                } catch (UnsupportedEncodingException ex) {
                    throw new AssertionError();
                }
            }
        }
        resp.addHeader("Content-Location", resp.encodeRedirectURL(requestURL.toString()));
    }
}
