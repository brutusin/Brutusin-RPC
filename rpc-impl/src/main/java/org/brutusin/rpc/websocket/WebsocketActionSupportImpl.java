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

import java.util.Map;
import java.util.Set;
import org.brutusin.rpc.RpcSpringContext;
import org.brutusin.rpc.http.HttpAction;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketActionSupportImpl extends WebsocketActionSupport {

    private final RpcSpringContext rpcCtx;
    private final SessionImpl session;

    public static void setInstance(WebsocketActionSupport context) {
        CONTEXTS.set(context);
    }

    public static void clear() {
        CONTEXTS.remove();
    }

    public WebsocketActionSupportImpl(RpcSpringContext rpcCtx) {
        this.rpcCtx = rpcCtx;
        this.session = null;
    }

    public WebsocketActionSupportImpl(SessionImpl session) {
        this.rpcCtx = session.getCtx().getSpringContext();
        this.session = session;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public boolean isUserInRole(String role) {
        return session.isUserInRole(role);
    }
    
    @Override
    public Set<String> getUserRoles() {
        return session.getUserRoles();
    }

    @Override
    public ApplicationContext getSpringContext() {
        return rpcCtx;
    }

    @Override
    public Object getHttpSession() {
        return session.getCtx().getHttpSession();
    }
    
    @Override
    public Map<String, HttpAction> getHttpServices() {
        return rpcCtx.getHttpServices();
    }

    @Override
    public Map<String, WebsocketAction> getWebSocketServices() {
        return rpcCtx.getWebSocketServices();
    }

    @Override
    public Map<String, Topic> getTopics() {
        return rpcCtx.getTopics();
    }
}
