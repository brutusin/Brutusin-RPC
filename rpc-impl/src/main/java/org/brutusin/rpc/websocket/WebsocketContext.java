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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import org.brutusin.rpc.RpcSpringContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketContext {

    private final ServletContext servletContext;
    private final RpcSpringContext springContext;
    private final HttpSession httpSession;
    private final Object securityContext;


    public WebsocketContext(ServletContext servletContext, RpcSpringContext springContext, HttpSession httpSession, Object securityContext) {
        this.servletContext = servletContext;
        this.springContext = springContext;
        this.httpSession = httpSession;
        this.securityContext = securityContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public RpcSpringContext getSpringContext() {
        return springContext;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public Object getSecurityContext() {
        return securityContext;
    }
}
