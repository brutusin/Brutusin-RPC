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

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.brutusin.rpc.actions.http.EnvironmentPopertiesAction;
import org.brutusin.rpc.actions.http.HttpServiceListAction;
import org.brutusin.rpc.actions.http.VersionAction;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcContextImpl extends RpcContext {

    static boolean testMode = false;

    private final ClassPathXmlApplicationContext applicationContext;
    private final Map<String, HttpAction> httpServices;
    private final Map<String, WebsocketAction> webSocketServices;
    private final Map<String, Topic> webSocketTopics;
    private final ThreadFactory threadFactory = new ThreadFactory();

    public RpcContextImpl() {
        if (testMode) {
            this.applicationContext = new ClassPathXmlApplicationContext(SpringNames.CFG_CORE_FILE);
        } else {
            this.applicationContext = new ClassPathXmlApplicationContext(SpringNames.CFG_CORE_FILE, SpringNames.CFG_FILE);
        }

        this.applicationContext.setClassLoader(Thread.currentThread().getContextClassLoader());
        registerBuiltServices(applicationContext);
        this.httpServices = loadComponents(this.applicationContext, HttpAction.class);
        this.webSocketServices = loadComponents(this.applicationContext, WebsocketAction.class);
        this.webSocketTopics = loadComponents(this.applicationContext, Topic.class);
    }

    @Override
    public ClassPathXmlApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void init() {
        initComponents(applicationContext, RpcComponent.class);
    }

    public void destroy() {
        applicationContext.destroy();
        threadFactory.destroy();
        destroy(httpServices.values());
        destroy(webSocketServices.values());
        destroy(webSocketTopics.values());
    }

    public static RpcContextImpl getInstance() {
        return (RpcContextImpl) RpcContext.getInstance();
    }

    @Override
    public void register(String id, RpcAction action) {
        try {
            if (action instanceof HttpAction) {
                if (httpServices.get(id) != null) {
                    throw new IllegalArgumentException("Service with id='" + id + "' is already registered");
                }
                action.init(id);
                applicationContext.getBeanFactory().registerSingleton(id, action);
                httpServices.put(id, (HttpAction) action);
            } else {
                if (webSocketServices.get(id) != null) {
                    throw new IllegalArgumentException("Service with id='" + id + "' is already registered");
                }
                action.init(id);
                applicationContext.getBeanFactory().registerSingleton(id, action);
                webSocketServices.put(id, (WebsocketAction) action);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void register(String id, Topic topic) {
        try {
            if (webSocketTopics.get(id) != null) {
                throw new IllegalArgumentException("Topic with id='" + id + "' is already registered");
            }
            topic.init(id);
            applicationContext.getBeanFactory().registerSingleton(id, topic);
            webSocketTopics.put(id, topic);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Map<String, HttpAction> getHttpServices() {
        return httpServices;
    }

    @Override
    public Map<String, WebsocketAction> getWebSocketServices() {
        return webSocketServices;
    }

    @Override
    public Map<String, Topic> getTopics() {
        return webSocketTopics;
    }

    public java.util.concurrent.ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    private void registerBuiltServices(ClassPathXmlApplicationContext ctx) {
        if (RpcConfig.isIncludeBuiltinServices()) {
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_VERSION, new VersionAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_SERVICE_LIST, new HttpServiceListAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_SCHEMA, new org.brutusin.rpc.actions.http.SchemaAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_SCHEMA_PROVIDER, new org.brutusin.rpc.actions.http.DynamicSchemaProviderAction());

            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_VERSION, new org.brutusin.rpc.actions.websocket.VersionAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_SERVICE_LIST, new org.brutusin.rpc.actions.websocket.ServiceListAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_SCHEMA, new org.brutusin.rpc.actions.websocket.SchemaAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_SCHEMA_PROVIDER, new org.brutusin.rpc.actions.websocket.DynamicSchemaProviderAction());

            ctx.getBeanFactory().registerSingleton(SpringNames.TPC_LIST, new org.brutusin.rpc.actions.websocket.TopicListAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.TPC_SCHEMA, new org.brutusin.rpc.actions.websocket.TopicSchemaAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.TPC_SUBSCRIBE, new org.brutusin.rpc.actions.websocket.SubscribeAction());
            ctx.getBeanFactory().registerSingleton(SpringNames.TPC_UNSUBSCRIBE, new org.brutusin.rpc.actions.websocket.UnsubscribeAction());
        }

        if (RpcConfig.isIncludeEnvironmentViewerService()) {
            ctx.getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_ENV, new EnvironmentPopertiesAction());
        }
    }

    public static <E extends RpcComponent> void destroy(Collection<E> components) {
        for (RpcComponent component : components) {
            try {
                component.destroy();
            } catch (Exception ex) {
                Logger.getLogger(RpcContextImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static <E extends RpcComponent> Map<String, E> loadComponents(ClassPathXmlApplicationContext ctx, Class<E> clazz) {
        Map<String, E> beans = ctx.getBeansOfType(clazz);
        for (Map.Entry<String, E> entry : beans.entrySet()) {
            String id = entry.getKey();
            E component = entry.getValue();
            if (RpcUtils.getDescription(component) == null) {
                Logger.getLogger(RpcContextImpl.class.getName()).warning("Component '" + id + "' is not documented. For maintainability reasons, document '" + component.getClass() + "' class with @Description");
            }
        }
        return beans;
    }

    public static <E extends RpcComponent> Map<String, E> initComponents(ClassPathXmlApplicationContext ctx, Class<E> clazz) {
        Map<String, E> beans = ctx.getBeansOfType(clazz);
        for (Map.Entry<String, E> entry : beans.entrySet()) {
            String id = entry.getKey();
            E component = entry.getValue();
            try {
                component.init(id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return beans;
    }

    private static class ThreadFactory implements java.util.concurrent.ThreadFactory {

        private final ThreadGroup tg = new ThreadGroup("brutusin-rpc-thread-group");

        public Thread newThread(final Runnable r) {
            Thread t = new Thread(tg, tg.getName()) {
                @Override
                public void run() {
                    r.run();
                }

                @Override
                public void interrupt() {
                    super.interrupt(); //To change body of generated methods, choose Tools | Templates.
                }
            };
            return t;
        }

        public void destroy() {
            if (!this.tg.isDestroyed()) {
                this.tg.destroy();
            }
        }
    }
}
