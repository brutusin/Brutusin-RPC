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
import org.brutusin.rpc.actions.http.LogoutAction;
import org.brutusin.rpc.actions.http.UserDetailAction;
import org.brutusin.rpc.actions.http.VersionAction;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.http.HttpActionSupportImpl;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionSupportImpl;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcSpringContext extends ClassPathXmlApplicationContext {

    private static final Logger LOGGER = Logger.getLogger(RpcSpringContext.class.getName());

    private Map<String, HttpAction> httpServices;
    private Map<String, WebsocketAction> webSocketServices;
    private Map<String, Topic> webSocketTopics;
    private ThreadFactory threadFactory;

    public RpcSpringContext() {
        this(true);
    }

    public RpcSpringContext(boolean loadAppDescriptor) {
        super(getXmlNames(loadAppDescriptor), false);
        setClassLoader(Thread.currentThread().getContextClassLoader());
    }

    private static String[] getXmlNames(boolean loadAppDescriptor) {
        if (loadAppDescriptor) {
            return new String[]{SpringNames.CFG_CORE_FILE, SpringNames.CFG_FILE};
        } else {
            return new String[]{SpringNames.CFG_CORE_FILE};
        }
    }

    @Override
    public void onRefresh() throws BeansException {
        cleanRpc();
        threadFactory = new ThreadFactory();
        registerBuiltServices();
        this.httpServices = loadComponents(HttpAction.class);
        this.webSocketServices = loadComponents(WebsocketAction.class);
        this.webSocketTopics = loadComponents(Topic.class);
        initHttpActions();
        initWsktActions();
        initTopics();
    }

    @Override
    public void destroy() {
        super.destroy();
        cleanRpc();
    }

    private void cleanRpc() {
        if (threadFactory != null) {
            threadFactory.destroy();
        }
        if (httpServices != null) {
            destroy(httpServices.values());
            httpServices.clear();
        }
        if (webSocketServices != null) {
            destroy(webSocketServices.values());
            webSocketServices.clear();
        }
        if (webSocketTopics != null) {
            destroy(webSocketTopics.values());
            webSocketTopics.clear();
        }
    }

    private void registerBuiltServices() {
        if (RpcConfig.getInstance().isIncludeBuiltinServices()) {
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_VERSION, new VersionAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_SERVICE_LIST, new HttpServiceListAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_SCHEMA, new org.brutusin.rpc.actions.http.SchemaAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_SCHEMA_PROVIDER, new org.brutusin.rpc.actions.http.DynamicSchemaProviderAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_LOGOUT, new LogoutAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_USER, new UserDetailAction());

            getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_VERSION, new org.brutusin.rpc.actions.websocket.VersionAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_SERVICE_LIST, new org.brutusin.rpc.actions.websocket.ServiceListAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_SCHEMA, new org.brutusin.rpc.actions.websocket.SchemaAction());
            getBeanFactory().registerSingleton(SpringNames.SRV_WSKT_SCHEMA_PROVIDER, new org.brutusin.rpc.actions.websocket.DynamicSchemaProviderAction());

            getBeanFactory().registerSingleton(SpringNames.TPC_LIST, new org.brutusin.rpc.actions.websocket.TopicListAction());
            getBeanFactory().registerSingleton(SpringNames.TPC_SCHEMA, new org.brutusin.rpc.actions.websocket.TopicSchemaAction());
            getBeanFactory().registerSingleton(SpringNames.TPC_SUBSCRIBE, new org.brutusin.rpc.actions.websocket.SubscribeAction());
            getBeanFactory().registerSingleton(SpringNames.TPC_UNSUBSCRIBE, new org.brutusin.rpc.actions.websocket.UnsubscribeAction());
        }

        if (RpcConfig.getInstance().isIncludeEnvService()) {
            getBeanFactory().registerSingleton(SpringNames.SRV_HTTP_ENV, new EnvironmentPopertiesAction());
        }
    }

    private static <E extends RpcComponent> void destroy(Collection<E> components) {
        for (RpcComponent component : components) {
            try {
                component.destroy();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    private <E extends RpcComponent> Map<String, E> loadComponents(Class<E> clazz) {
        Map<String, E> beans = getBeansOfType(clazz);
        for (Map.Entry<String, E> entry : beans.entrySet()) {
            String id = entry.getKey();
            E component = entry.getValue();
            if (RpcUtils.getDescription(component) == null) {
                LOGGER.warning("Component '" + id + "' is not documented. For maintainability reasons, document '" + component.getClass() + "' class with @Description");
            }
        }
        return beans;
    }

    private void initHttpActions() {
        Map<String, HttpAction> beans = getBeansOfType(HttpAction.class);
        for (Map.Entry<String, HttpAction> entry : beans.entrySet()) {
            String id = entry.getKey();
            HttpAction action = entry.getValue();
            init(action, id);
        }
    }

    private void initWsktActions() {
        Map<String, WebsocketAction> beans = getBeansOfType(WebsocketAction.class);
        for (Map.Entry<String, WebsocketAction> entry : beans.entrySet()) {
            String id = entry.getKey();
            WebsocketAction action = entry.getValue();
            init(action, id);
        }
    }

    private void init(HttpAction action, String id) {
        try {
            HttpActionSupportImpl.setInstance(new HttpActionSupportImpl(this));
            action.init(id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            HttpActionSupportImpl.clear();
        }
    }

    private void init(WebsocketAction action, String id) {
        try {
            WebsocketActionSupportImpl.setInstance(new WebsocketActionSupportImpl(this));
            action.init(id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            WebsocketActionSupportImpl.clear();
        }
    }

    private void initTopics() {
        Map<String, Topic> beans = getBeansOfType(Topic.class);
        for (Map.Entry<String, Topic> entry : beans.entrySet()) {
            String id = entry.getKey();
            Topic topic = entry.getValue();
            try {
                topic.init(id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Map<String, HttpAction> getHttpServices() {
        return httpServices;
    }

    public Map<String, WebsocketAction> getWebSocketServices() {
        return webSocketServices;
    }

    public Map<String, Topic> getTopics() {
        return webSocketTopics;
    }

    public java.util.concurrent.ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void register(String id, HttpAction action) {
        try {
            if (httpServices.get(id) != null) {
                throw new IllegalArgumentException("Service with id='" + id + "' is already registered");
            }
            init(action, id);
            getBeanFactory().registerSingleton(id, action);
            httpServices.put(id, action);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void register(String id, WebsocketAction action) {
        try {
            if (webSocketServices.get(id) != null) {
                throw new IllegalArgumentException("Service with id='" + id + "' is already registered");
            }
            init(action, id);
            getBeanFactory().registerSingleton(id, action);
            webSocketServices.put(id, action);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void register(String id, Topic topic) {
        try {
            if (webSocketTopics.get(id) != null) {
                throw new IllegalArgumentException("Topic with id='" + id + "' is already registered");
            }
            topic.init(id);
            getBeanFactory().registerSingleton(id, topic);
            webSocketTopics.put(id, topic);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
