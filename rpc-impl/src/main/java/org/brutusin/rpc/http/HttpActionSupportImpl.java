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
package org.brutusin.rpc.http;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.brutusin.rpc.RpcSpringContext;
import org.brutusin.rpc.RpcUtils;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpActionSupportImpl extends HttpActionSupport {

    private final RpcSpringContext rpcCtx;
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public static void setInstance(HttpActionSupport context) {
        CONTEXTS.set(context);
    }

    public static void clear() {
        CONTEXTS.remove();
    }

    public HttpActionSupportImpl(RpcSpringContext rpcCtx) {
        this(rpcCtx, null, null);
    }

    public HttpActionSupportImpl(RpcSpringContext rpcCtx, HttpServletRequest req, HttpServletResponse resp) {
        this.rpcCtx = rpcCtx;
        this.req = req;
        this.resp = resp;
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return req;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return resp;
    }

    public Set<String> getUserRoles() {
        Object securityContext;
        if (ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder", HttpActionSupportImpl.class.getClassLoader())) {
            securityContext = SecurityContextHolder.getContext();
        } else {
            securityContext = null;
        }
        return RpcUtils.getUserRoles(securityContext);
    }
    
    @Override
    public final Principal getUserPrincipal() {
        if (req == null) {
            throw new IllegalStateException("Trying to get principal out of the context of a request");
        }
        return req.getUserPrincipal();
    }

    @Override
    public final boolean isUserInRole(String role) {
        if (req == null) {
            throw new IllegalStateException("Trying to get principal role out of the context of a request");
        }
        return req.isUserInRole(role);
    }

    @Override
    public ApplicationContext getSpringContext() {
        return rpcCtx;
    }

    @Override
    public HttpSession getHttpSession() {
        return req.getSession();
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
