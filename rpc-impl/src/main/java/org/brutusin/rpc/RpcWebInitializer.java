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

import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.RpcServlet;
import org.brutusin.rpc.websocket.WebsocketEndpoint;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcWebInitializer implements WebApplicationInitializer {

    public void onStartup(ServletContext ctx) throws ServletException {
        final SpringContextImpl appCtx = new SpringContextImpl(ctx);
        ctx.addListener(new ServletContextListener() {
            public void contextInitialized(ServletContextEvent sce) {
            }

            public void contextDestroyed(ServletContextEvent sce) {
                appCtx.destroy();
            }
        });
        JsonCodec.getInstance().registerStringFormat(MetaDataInputStream.class, "inputstream");
        appCtx.refresh();
        ctx.addListener(new ContextLoaderListener(appCtx));
        initHttpRpcRuntime(ctx);
        initWebsocketRpcRuntime(ctx);
    }

    private void initHttpRpcRuntime(ServletContext ctx) {
        RpcServlet servlet = new RpcServlet();
        ServletRegistration.Dynamic regInfo = ctx.addServlet("rpc.http", servlet);
        ServletSecurityElement sec = new ServletSecurityElement(new HttpConstraintElement());
        regInfo.setServletSecurity(sec);
        regInfo.setLoadOnStartup(1);
        regInfo.addMapping(RpcConfig.getPath() + "/http");
    }

    private void initWebsocketRpcRuntime(final ServletContext ctx) {
        ServerContainer sc = (ServerContainer) ctx.getAttribute("javax.websocket.server.ServerContainer");
        ServerEndpointConfig.Configurator cfg = new ServerEndpointConfig.Configurator() {
            @Override
            public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
                HttpSession httpSession = (HttpSession) request.getHttpSession();
                if (httpSession != null) {
                    config.getUserProperties().put("httpSession", httpSession);
                }
                config.getUserProperties().put("servletContext", ctx);
                
            }
        };
        ServerEndpointConfig sec = ServerEndpointConfig.Builder.create(WebsocketEndpoint.class, RpcConfig.getPath() + "/wskt").configurator(cfg).build();
        try {
            sc.addEndpoint(sec);
        } catch (DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }
}
