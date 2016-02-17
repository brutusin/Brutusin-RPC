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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.brutusin.rpc.GlobalThreadLocal;
import org.brutusin.rpc.RpcConfig;
import org.brutusin.rpc.RpcWebInitializer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketFilter implements Filter {

    private boolean isDisabled = false;

    private final AtomicInteger counter = new AtomicInteger();

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (isDisabled || httpRequest.getRequestURI() == null || !(httpRequest.getRequestURI().substring(httpRequest.getContextPath().length()).startsWith(RpcConfig.getInstance().getPath() + "/wskt"))) {
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

    public void disable() {
        this.isDisabled = true;
    }
}
