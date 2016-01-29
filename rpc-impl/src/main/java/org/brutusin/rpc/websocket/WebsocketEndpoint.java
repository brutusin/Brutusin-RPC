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
import java.util.Map;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import org.brutusin.rpc.RpcContext;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.exception.ServiceNotFoundException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.RpcUtils;
import org.brutusin.rpc.exception.InvalidRequestException;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpoint extends Endpoint {

    private final Map<String, WebsocketAction> services = RpcContext.getInstance().getWebSocketServices();

    /**
     *
     * @param session
     * @param config
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        final SessionImpl sessionImpl = new SessionImpl(session);
        sessionImpl.init();
        session.getUserProperties().put(SessionImpl.class.getName(), sessionImpl);
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            public void onMessage(String message) {
                WebsocketActionContext.setInstance(new WebsocketActionContext() {
                    @Override
                    public org.brutusin.rpc.websocket.Session getSession() {
                        return sessionImpl;
                    }
                });
                try {
                    String response = process(message);
                    if (response != null) {
                        sessionImpl.sendToPeerRaw(response);
                    }
                } finally {
                    WebsocketActionContext.clear();
                }
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        final SessionImpl sessionImpl = (SessionImpl) session.getUserProperties().get(SessionImpl.class.getName());
        WebsocketActionContext.setInstance(new WebsocketActionContext() {
            @Override
            public org.brutusin.rpc.websocket.Session getSession() {
                return sessionImpl;
            }
        });
        try {
            for (Topic topic : RpcContext.getInstance().getTopics().values()) {
                try {
                    topic.unsubscribe();
                } catch (InvalidSubscriptionException ise) {
                }
            }
        } finally {
            WebsocketActionContext.clear();
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
    private String process(String message) {
        RpcRequest req = null;
        Object result = null;
        Throwable throwable = null;
        try {
            req = JsonCodec.getInstance().parse(message, RpcRequest.class);
            result = execute(req);
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
    private Object execute(RpcRequest request) throws Exception {
        if (!"2.0".equals(request.getJsonrpc())) {
            throw new InvalidRequestException("Only jsonrpc 2.0 supported");
        }
        String serviceId = request.getMethod();
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
