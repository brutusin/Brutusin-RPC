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

import java.util.logging.Logger;
import javax.servlet.FilterRegistration;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletSecurityElement;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.brutusin.commons.Bean;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.RpcServlet;
import org.brutusin.rpc.websocket.WebsocketEndpoint;
import org.brutusin.rpc.websocket.WebsocketEndpointConfigurator;
import org.brutusin.rpc.websocket.WebsocketFilter;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Order(1)
public class RpcWebInitializer implements WebApplicationInitializer {

    private static final Logger LOGGER = Logger.getLogger(RpcWebInitializer.class.getName());

    public void onStartup(final ServletContext ctx) throws ServletException {
        final RpcServlet rpcServlet = registerRpcServlet(ctx);
        final WebsocketFilter websocketFilter = new WebsocketFilter();
        FilterRegistration.Dynamic dynamic = ctx.addFilter(WebsocketFilter.class.getName(), websocketFilter);
        dynamic.addMappingForUrlPatterns(null, false, RpcConfig.getInstance().getPath() + "/wskt");
        JsonCodec.getInstance().registerStringFormat(MetaDataInputStream.class, "inputstream");
        final Bean<RpcSpringContext> rpcCtxBean = new Bean<RpcSpringContext>();
        ctx.addListener(new ServletRequestListener() {

            public void requestDestroyed(ServletRequestEvent sre) {
                GlobalThreadLocal.clear();
            }

            public void requestInitialized(ServletRequestEvent sre) {
                GlobalThreadLocal.set(new GlobalThreadLocal((HttpServletRequest) sre.getServletRequest(), null));
            }
        });
        ctx.addListener(new ServletContextListener() {

            public void contextInitialized(ServletContextEvent sce) {
                RpcSpringContext rpcCtx = createRpcSpringContext(ctx);
                rpcCtxBean.setValue(rpcCtx);
                rpcServlet.setRpcCtx(rpcCtx);
                initWebsocketRpcRuntime(ctx, rpcCtx);
                ctx.setAttribute(RpcSpringContext.class.getName(), rpcCtx);
            }

            public void contextDestroyed(ServletContextEvent sce) {
                LOGGER.info("Destroying RPC context");
                rpcCtxBean.getValue().destroy();
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
                    rpcCtxBean.getValue().setParent(rootCtx);
                    if (rootCtx.containsBean("springSecurityFilterChain")) {
                        LOGGER.info("Moving WebsocketFilter behind springSecurityFilterChain");
                        websocketFilter.disable();
                        FilterChainProxy fcp = (FilterChainProxy) rootCtx.getBean("springSecurityFilterChain");
                        fcp.getFilterChains().get(0).getFilters().add(new WebsocketFilter());
                    }
                }
            }
        });
    }

    private RpcSpringContext createRpcSpringContext(ServletContext ctx) {
        ServerContainer sc = (ServerContainer) ctx.getAttribute("javax.websocket.server.ServerContainer");
        if (sc == null) {
            throw new Error("ServerContainer not found");
        }
        RpcSpringContext rpcCtx = new RpcSpringContext(sc, !RpcConfig.getInstance().isTestMode());
        return rpcCtx;
    }

    private RpcServlet registerRpcServlet(ServletContext ctx) {
        LOGGER.info("Starting HTTP RPC runtime");
        RpcServlet servlet = new RpcServlet();
        ServletRegistration.Dynamic regInfo = ctx.addServlet(RpcServlet.class.getName(), servlet);
        ServletSecurityElement sec = new ServletSecurityElement(new HttpConstraintElement());
        regInfo.setServletSecurity(sec);
        regInfo.setLoadOnStartup(1);
        regInfo.addMapping(RpcConfig.getInstance().getPath() + "/http");
        return servlet;
    }

    private void initWebsocketRpcRuntime(final ServletContext ctx, final RpcSpringContext rpcCtx) {
        LOGGER.info("Starting Websocket RPC runtime");
        ServerEndpointConfig.Configurator cfg = new WebsocketEndpointConfigurator(ctx, rpcCtx);
        ServerEndpointConfig sec = ServerEndpointConfig.Builder.create(WebsocketEndpoint.class, RpcConfig.getInstance().getPath() + "/wskt").configurator(cfg).build();
        try {
            rpcCtx.getWebsocketContainer().addEndpoint(sec);
        } catch (DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }
}
