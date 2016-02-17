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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.ServletSecurityElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.RpcServlet;
import org.brutusin.rpc.websocket.WebsocketContext;
import org.brutusin.rpc.websocket.WebsocketEndpoint;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.util.ClassUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcWebInitializer implements WebApplicationInitializer {

    private static final Logger LOGGER = Logger.getLogger(RpcWebInitializer.class.getName());
    public static final String SERVLET_NAME = "brutusin-rpc";

    public void onStartup(final ServletContext ctx) throws ServletException {
        final RpcSpringContext rpcCtx = new RpcSpringContext(!RpcConfig.getInstance().isTestMode());
        rpcCtx.refresh();
        ctx.setAttribute(SERVLET_NAME, rpcCtx);
        final Filter globalThreadLocalFilter = new Filter() {
            private final AtomicInteger counter = new AtomicInteger();

            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                if (httpRequest.getRequestURI() == null || !(httpRequest.getRequestURI().substring(httpRequest.getContextPath().length()).startsWith(RpcConfig.getInstance().getPath() + "/wskt"))) {
                    chain.doFilter(request, response);
                    return;
                }
                final Map<String, String[]> fakedParams = Collections.singletonMap("requestId", new String[]{String.valueOf(counter.getAndIncrement())});
                HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public Map<String, String[]> getParameterMap() {
                        return fakedParams;
                    }
                };
                /*
                 * current request is needed for getEndpointInstance(). In glassfish getEndpointInstance() is executed out this filter chain, 
                 * but inside whole request-response cycle (controlled by the overall listener that sets and removes GlobalThreadLocal)
                 */
                if (GlobalThreadLocal.get() == null) {
                    throw new AssertionError();
                }
                Object securityContext;
                if (ClassUtils.isPresent("org.springframework.security.core.context.SecurityContextHolder", RpcWebInitializer.class.getClassLoader())) {
                    securityContext = SecurityContextHolder.getContext();
                } else {
                    securityContext = null;
                }
                GlobalThreadLocal.set(new GlobalThreadLocal(wrappedRequest, securityContext)); // override current request with the one with faked params and security context
                chain.doFilter(wrappedRequest, response);
            }

            public void init(FilterConfig filterConfig) throws ServletException {
            }

            public void destroy() {
            }
        };
        JsonCodec.getInstance().registerStringFormat(MetaDataInputStream.class, "inputstream");
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
                    if (rootCtx.containsBean("springSecurityFilterChain")) {
                        LOGGER.info("Appending RpcWebsocketFilter to springSecurityFilterChain");
                        FilterChainProxy fcp = (FilterChainProxy) rootCtx.getBean("springSecurityFilterChain");
                        fcp.getFilterChains().get(0).getFilters().add(globalThreadLocalFilter); // add globalThreadLocalFilter after the security stack
                    }
                }
            }
        });
        Map<String, ? extends FilterRegistration> filterRegistrations = ctx.getFilterRegistrations();
        if (filterRegistrations != null && !filterRegistrations.keySet().contains("springSecurityFilterChain")) {
            LOGGER.info("Registering RpcWebsocketFilter in ServletContext");
            FilterRegistration.Dynamic dynamic = ctx.addFilter("RpcWebsocketFilter", globalThreadLocalFilter);
            dynamic.addMappingForUrlPatterns(null, false, RpcConfig.getInstance().getPath() + "/wskt");
        }
        initHttpRpcRuntime(ctx, rpcCtx);
    }

    private void initHttpRpcRuntime(ServletContext ctx, RpcSpringContext rpcCtx) {
        LOGGER.info("Starting HTTP RPC runtime");
        RpcServlet servlet = new RpcServlet(rpcCtx);
        ServletRegistration.Dynamic regInfo = ctx.addServlet(SERVLET_NAME, servlet);
        ServletSecurityElement sec = new ServletSecurityElement(new HttpConstraintElement());
        regInfo.setServletSecurity(sec);
        regInfo.setLoadOnStartup(1);
        regInfo.addMapping(RpcConfig.getInstance().getPath() + "/http");
    }

    private void initWebsocketRpcRuntime(final ServletContext ctx, final RpcSpringContext rpcCtx) {
        LOGGER.info("Starting Websocket RPC runtime");
        ServerContainer sc = (ServerContainer) ctx.getAttribute("javax.websocket.server.ServerContainer");
        if (sc == null) {
            LOGGER.severe("ServerContainer not found");
            return;
        }
        ServerEndpointConfig.Configurator cfg;
        cfg = new ServerEndpointConfig.Configurator() {

            @Override
            public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {

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
        };

        ServerEndpointConfig sec = ServerEndpointConfig.Builder.create(WebsocketEndpoint.class, RpcConfig.getInstance().getPath() + "/wskt").configurator(cfg).build();

        try {
            sc.addEndpoint(sec);
        } catch (DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }
}
