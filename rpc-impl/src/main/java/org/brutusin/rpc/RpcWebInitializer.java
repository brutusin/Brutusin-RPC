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
package org.brutusin.rpc;

import java.util.List;
import java.util.Map;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.RpcServlet;
import org.brutusin.rpc.websocket.WebsocketEndpoint;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcWebInitializer implements WebApplicationInitializer {

    public static final String SERVLET_NAME = "brutusin-rpc";

    public void onStartup(final ServletContext ctx) throws ServletException {
        final RpcSpringContext rpcCtx = new RpcSpringContext(!RpcConfig.getInstance().isTestMode());
        rpcCtx.refresh();
        ctx.setAttribute(SERVLET_NAME, rpcCtx);
        JsonCodec.getInstance().registerStringFormat(MetaDataInputStream.class, "inputstream");
        ctx.addListener(new RequestContextListener());
        ctx.addListener(new ServletContextListener() {
            public void contextInitialized(ServletContextEvent sce) {
                initHttpRpcRuntime(ctx, rpcCtx);
                initWebsocketRpcRuntime(ctx, rpcCtx);
            }

            public void contextDestroyed(ServletContextEvent sce) {
                rpcCtx.destroy();
            }
        });
        ctx.addListener(new ServletContextAttributeListener() {
            public void attributeAdded(ServletContextAttributeEvent event) {
                if (event.getName().equals(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)) {
                    updateRootContext();
                }
            }

            public void attributeRemoved(ServletContextAttributeEvent event) {
                if (event.getName().equals(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)) {
                    updateRootContext();
                }
            }

            public void attributeReplaced(ServletContextAttributeEvent event) {
                if (event.getName().equals(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)) {
                    updateRootContext();
                }
            }

            private void updateRootContext() {
                WebApplicationContext rootCtx = WebApplicationContextUtils.getWebApplicationContext(ctx);
                if (rootCtx != null) {
                    rpcCtx.setParent(rootCtx);
                }
            }
        });
    }

    private void initHttpRpcRuntime(ServletContext ctx, RpcSpringContext rpcCtx) {
        RpcServlet servlet = new RpcServlet(rpcCtx);
        ServletRegistration.Dynamic regInfo = ctx.addServlet(SERVLET_NAME, servlet);
        ServletSecurityElement sec = new ServletSecurityElement(new HttpConstraintElement());
        regInfo.setServletSecurity(sec);
        regInfo.setLoadOnStartup(1);
        regInfo.addMapping(RpcConfig.getInstance().getPath() + "/http");
    }

    private void initWebsocketRpcRuntime(final ServletContext ctx, final RpcSpringContext rpcCtx) {
        ServerContainer sc = (ServerContainer) ctx.getAttribute("javax.websocket.server.ServerContainer");
        ServerEndpointConfig.Configurator cfg;
        cfg = new ServerEndpointConfig.Configurator() {
            @Override
            public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
                Map<String, List<String>> headers = request.getHeaders();
                config.getUserProperties().put(WebsocketEndpoint.SERVLET_CONTEXT_KEY, ctx);
                config.getUserProperties().put(WebsocketEndpoint.RPC_SPRING_CTX, rpcCtx);
                if (request.getHttpSession() != null) {
                    config.getUserProperties().put(WebsocketEndpoint.HTTP_SESSION_KEY, request.getHttpSession());
                }
            }

            @Override
            public boolean checkOrigin(String originHeaderValue) {
                if(originHeaderValue==null){
                    return true;
                }
                String accessControlOriginHost = RpcConfig.getInstance().getAccessControlOriginHost();
                // Same origin verification
                if (accessControlOriginHost == null) {
                    HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                    String hostHeaderValue = req.getHeader("Host");
                    if (hostHeaderValue == null) {
                        return false;
                    }
                    return RpcUtils.doOriginsMatch(originHeaderValue, hostHeaderValue);
                } else {
                    return RpcUtils.doOriginsMatch(originHeaderValue,accessControlOriginHost);
                }
            }
        };
        
        ServerEndpointConfig sec = ServerEndpointConfig.Builder.create(WebsocketEndpoint.class, RpcConfig.getInstance().getPath() + "/wskt").configurator(cfg).build();
        try {
            sc.addEndpoint(sec);
        } catch (DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }
}
