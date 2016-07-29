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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.server.ServerContainer;
import org.brutusin.rpc.actions.http.DescriptionAction;
import org.brutusin.rpc.actions.http.EnvironmentPopertiesAction;
import org.brutusin.rpc.actions.http.HttpServiceListAction;
import org.brutusin.rpc.actions.http.LogoutAction;
import org.brutusin.rpc.actions.http.PingAction;
import org.brutusin.rpc.actions.http.UserDetailAction;
import org.brutusin.rpc.actions.http.VersionAction;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.http.HttpActionSupportImpl;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionSupportImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcSpringContext extends ClassPathXmlApplicationContext {

    private static final Logger LOGGER = Logger.getLogger(RpcSpringContext.class.getName());

    private final Set<RpcAction> frameworkActions = new HashSet<RpcAction>();
    private Map<String, HttpAction> httpServices;
    private Map<String, WebsocketAction> webSocketServices;
    private Map<String, Topic> webSocketTopics;
    private final ServerContainer sc;

    public RpcSpringContext(ServerContainer sc) {
        this(sc, true);
    }

    public RpcSpringContext(ServerContainer sc, boolean loadAppDescriptor) {
        this.sc = sc;
        setConfigLocations(getXmlNames(loadAppDescriptor));
        refresh();
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
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        beanFactory.registerSingleton(SpringNames.WSKT_CONTAINER, this.sc);
    }

    public ServerContainer getWebsocketContainer() {
        return sc;
    }

    @Override
    public void onRefresh() throws BeansException {
        cleanRpc();
        registerBuiltServices();
        this.httpServices = getBeansOfType(HttpAction.class);
        this.webSocketServices = getBeansOfType(WebsocketAction.class);
        this.webSocketTopics = getBeansOfType(Topic.class);
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

    public Map<String, Topic> getWebSocketTopics() {
        return webSocketTopics;
    }

    public void setWebSocketTopics(Map<String, Topic> webSocketTopics) {
        this.webSocketTopics = webSocketTopics;
    }

    private void registerBuiltServices() {
        if (RpcConfig.getInstance().isIncludeBuiltinServices()) {
            registerBuiltinAction(SpringNames.SRV_HTTP_VERSION, new VersionAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_DESC, new DescriptionAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_SERVICE_LIST, new HttpServiceListAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_SCHEMA, new org.brutusin.rpc.actions.http.SchemaAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_SCHEMA_PROVIDER, new org.brutusin.rpc.actions.http.DynamicSchemaProviderAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_LOGOUT, new LogoutAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_USER, new UserDetailAction());
            registerBuiltinAction(SpringNames.SRV_HTTP_PING, new PingAction());

            registerBuiltinAction(SpringNames.SRV_WSKT_VERSION, new org.brutusin.rpc.actions.websocket.VersionAction());
            registerBuiltinAction(SpringNames.SRV_WSKT_DESC, new org.brutusin.rpc.actions.websocket.DescriptionAction());
            registerBuiltinAction(SpringNames.SRV_WSKT_SERVICE_LIST, new org.brutusin.rpc.actions.websocket.ServiceListAction());
            registerBuiltinAction(SpringNames.SRV_WSKT_SCHEMA, new org.brutusin.rpc.actions.websocket.SchemaAction());
            registerBuiltinAction(SpringNames.SRV_WSKT_SCHEMA_PROVIDER, new org.brutusin.rpc.actions.websocket.DynamicSchemaProviderAction());
            registerBuiltinAction(SpringNames.SRV_WSKT_PING, new org.brutusin.rpc.actions.websocket.PingAction());

            registerBuiltinAction(SpringNames.TPC_LIST, new org.brutusin.rpc.actions.websocket.TopicListAction());
            registerBuiltinAction(SpringNames.TPC_SCHEMA, new org.brutusin.rpc.actions.websocket.TopicSchemaAction());
            registerBuiltinAction(SpringNames.TPC_SUBSCRIBE, new org.brutusin.rpc.actions.websocket.SubscribeAction());
            registerBuiltinAction(SpringNames.TPC_UNSUBSCRIBE, new org.brutusin.rpc.actions.websocket.UnsubscribeAction());
        }

        if (RpcConfig.getInstance().isIncludeEnvService()) {
            registerBuiltinAction(SpringNames.SRV_HTTP_ENV, new EnvironmentPopertiesAction());
        }
    }

    private void registerBuiltinAction(String name, RpcAction action) {
        getBeanFactory().registerSingleton(name, action);
        frameworkActions.add(action);
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

    private void initHttpActions() {
        Map<String, HttpAction> beans = getBeansOfType(HttpAction.class);
        for (Map.Entry<String, HttpAction> entry : beans.entrySet()) {
            String id = entry.getKey();
            HttpAction action = entry.getValue();
            init(action, id);
            if (action.isActive() && action.getDescription() == null) {
                LOGGER.warning("Action '" + id + "' is not documented. For maintainability reasons, document '" + action.getClass() + "' class with @Description");
            }
        }
    }

    private void initWsktActions() {
        Map<String, WebsocketAction> beans = getBeansOfType(WebsocketAction.class);
        for (Map.Entry<String, WebsocketAction> entry : beans.entrySet()) {
            String id = entry.getKey();
            WebsocketAction action = entry.getValue();
            init(action, id);
            if (action.isActive() && action.getDescription() == null) {
                LOGGER.warning("Action '" + id + "' is not documented. For maintainability reasons, document '" + action.getClass() + "' class with @Description");
            }
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
                if (topic.isActive() && topic.getDescription() == null) {
                    LOGGER.warning("Topic '" + id + "' is not documented. For maintainability reasons, document '" + topic.getClass() + "' class with @Description");
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public boolean isFrameworkAction(RpcAction action) {
        return frameworkActions.contains(action);
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
}
