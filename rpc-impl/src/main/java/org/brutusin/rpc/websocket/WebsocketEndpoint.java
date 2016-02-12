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
package org.brutusin.rpc.websocket;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.exception.ServiceNotFoundException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.RpcSpringContext;
import org.brutusin.rpc.RpcUtils;
import org.brutusin.rpc.exception.InvalidRequestException;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpoint extends Endpoint {

    public static String SERVLET_CONTEXT_KEY = "SERVLET_CONTEXT_KEY";
    public static String RPC_SPRING_CTX = "RPC_SPRING_CTX";
    public static String SESSION_IMPL_KEY = "SESSION_IMPL_KEY";

    private final Map<String, SessionImpl> wrapperMap = Collections.synchronizedMap(new HashMap());

    /**
     *
     * @param session
     * @param config
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {

        final RpcSpringContext rpcCtx = (RpcSpringContext) session.getUserProperties().get(RPC_SPRING_CTX);
        final SessionImpl sessionImpl = new SessionImpl(session, rpcCtx);
        sessionImpl.init();
        wrapperMap.put(session.getId(), sessionImpl);
        
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            public void onMessage(String message) {
                WebsocketActionSupportImpl.setInstance(new WebsocketActionSupportImpl(sessionImpl));
                try {
                    String response = process(message, sessionImpl);
                    if (response != null) {
                        sessionImpl.sendToPeerRaw(response);
                    }
                } finally {
                    WebsocketActionSupportImpl.clear();
                }
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        final SessionImpl sessionImpl = wrapperMap.remove(session.getId());
        if (sessionImpl == null) {
            throw new AssertionError();
        }
        try {
            WebsocketActionSupportImpl.setInstance(new WebsocketActionSupportImpl(sessionImpl));
            for (Topic topic : sessionImpl.getRpcCtx().getTopics().values()) {
                try {
                    topic.unsubscribe();
                } catch (InvalidSubscriptionException ise) {
                    // Ignored already unsubscribed
                }
            }
        } finally {
            WebsocketActionSupportImpl.clear();
            sessionImpl.close();
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        thr.printStackTrace();
    }

    /**
     *
     * @param message
     * @return
     */
    private String process(String message, SessionImpl sessionImpl) {
        RpcRequest req = null;
        Object result = null;
        Throwable throwable = null;
        try {
            req = JsonCodec.getInstance().parse(message, RpcRequest.class);
            result = execute(req, sessionImpl.getRpcCtx());
        } catch (Throwable th) {
            throwable = th;
        }
        if (req != null && req.getId() == null) {
            return null;
        }
        RpcResponse resp = new RpcResponse();
        if (req != null) {
            resp.setId(req.getId());
        }
        resp.setError(RpcResponse.Error.from(throwable));
        resp.setResult(result);
        return JsonCodec.getInstance().transform(resp);
    }

    /**
     *
     * @param request
     * @return
     */
    private Object execute(RpcRequest request, RpcSpringContext rpcCtx) throws Exception {
        if (!"2.0".equals(request.getJsonrpc())) {
            throw new InvalidRequestException("Only jsonrpc 2.0 supported");
        }
        String serviceId = request.getMethod();
        Map<String, WebsocketAction> services = rpcCtx.getWebSocketServices();
        if (serviceId == null || !services.containsKey(serviceId)) {
            throw new ServiceNotFoundException();
        }
        WebsocketAction service = services.get(serviceId);
        Object input;
        if (request.getParams() == null) {
            input = null;
        } else {
            Type inputType = service.getInputType();
            JsonSchema inputSchema = JsonCodec.getInstance().getSchema(inputType);
            inputSchema.validate(request.getParams());
            if (inputType.equals(Object.class)) {
                input = request.getParams();
            } else {
                input = JsonCodec.getInstance().load(request.getParams(), RpcUtils.getClass(inputType));
            }
        }
        return service.execute(input);
    }
}
