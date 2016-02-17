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
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.brutusin.rpc.GlobalThreadLocal;
import org.brutusin.rpc.RpcConfig;
import org.brutusin.rpc.RpcSpringContext;
import org.brutusin.rpc.RpcUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

    private final ServletContext ctx;
    private final RpcSpringContext rpcCtx;

    public WebsocketEndpointConfigurator(ServletContext ctx, RpcSpringContext rpcCtx) {
        this.ctx = ctx;
        this.rpcCtx = rpcCtx;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        T ret = super.getEndpointInstance(endpointClass); //To change body of generated methods, choose Tools | Templates.
        if (!(ret instanceof WebsocketEndpoint)) {
            throw new AssertionError();
        }
        WebsocketEndpoint endpoint = (WebsocketEndpoint) ret;

        GlobalThreadLocal gtl = GlobalThreadLocal.get();
        HttpServletRequest req = gtl.getHttpRequest();
        WebsocketContext wskCtx = new WebsocketContext(ctx, rpcCtx, req.getSession(), gtl.getSecurityContext());
        endpoint.getContextMap().put(req.getParameterMap().get("requestId")[0], wskCtx);
        return ret;
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        if (originHeaderValue == null) {
            return true;
        }
        String accessControlOriginHost = RpcConfig.getInstance().getAccessControlOriginHost();
        // Host vs Origin comparation
        if (accessControlOriginHost == null) {
            HttpServletRequest req = GlobalThreadLocal.get().getHttpRequest();
            String hostHeaderValue = req.getHeader("Host");
            if (hostHeaderValue == null) {
                return false;
            }
            return RpcUtils.doOriginsMatch(originHeaderValue, hostHeaderValue);
            // White list comparation
        } else {
            return RpcUtils.doOriginsMatch(originHeaderValue, accessControlOriginHost);
        }
    }
}
