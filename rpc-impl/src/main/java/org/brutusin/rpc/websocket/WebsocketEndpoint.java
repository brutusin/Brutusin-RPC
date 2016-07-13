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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.exception.ServiceNotFoundException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.RpcSpringContext;
import org.brutusin.rpc.exception.ErrorFactory;
import org.brutusin.rpc.exception.InvalidRequestException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.core.context.SecurityContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpoint extends Endpoint {

    private final Map<String, WebsocketContext> contextMap = Collections.synchronizedMap(new HashMap());
    private final Map<String, SessionImpl> wrapperMap = Collections.synchronizedMap(new HashMap());

    /**
     *
     * @param session
     * @param config
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        final WebsocketContext websocketContext = contextMap.get(session.getRequestParameterMap().get("requestId").get(0));
        if (!allowAccess(session, websocketContext)) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Authentication required"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return;
        }
        final SessionImpl sessionImpl = new SessionImpl(session, websocketContext);
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

    public Map<String, WebsocketContext> getContextMap() {
        return contextMap;
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        contextMap.remove(session.getRequestParameterMap().get("requestId").get(0));
        final SessionImpl sessionImpl = wrapperMap.remove(session.getId());
        if (sessionImpl != null) {
            try {
                WebsocketActionSupportImpl.setInstance(new WebsocketActionSupportImpl(sessionImpl));
                for (Topic topic : sessionImpl.getCtx().getSpringContext().getTopics().values()) {
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
    }

    @Override
    public void onError(Session session, Throwable thr) {
        if (!(thr instanceof IOException)) {
            thr.printStackTrace();
        }
    }

    protected boolean allowAccess(Session session, WebsocketContext websocketContext) {
        final RpcSpringContext rpcCtx = websocketContext.getSpringContext();
        if (rpcCtx.getParent() != null) {
            try {
                if (rpcCtx.getParent().getBean("springSecurityFilterChain") != null) { // Security active
                    final SecurityContext sc = (SecurityContext) websocketContext.getSecurityContext();
                    if (sc.getAuthentication() == null) {
                        return false;
                    } else {
                        return sc.getAuthentication().isAuthenticated();
                    }
                }
            } catch (NoSuchBeanDefinitionException ex) {
                return true;
            }
        }
        return true;
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
            result = execute(req, sessionImpl.getCtx().getSpringContext());
        } catch (Throwable th) {
            throwable = th;
        }
        if (req != null && req.getId() == null) {
            return null;
        }
        RpcResponse resp;
        if (result instanceof RpcResponse) {
            resp = (RpcResponse) result;
        } else {
            resp = new RpcResponse();
            if (req != null) {
                resp.setId(req.getId());
            }
            resp.setError(ErrorFactory.getError(throwable));
            resp.setResult(result);
        }
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
                input = JsonCodec.getInstance().load(request.getParams(), Miscellaneous.getClass(inputType));
            }
        }
        return service.execute(input);
    }
}
