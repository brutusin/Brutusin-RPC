/*
 * Copyright 2017 Ignacio del Valle Alles idelvall@brutusin.org.
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
package org.brutusin.rpc.client.topic;

import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.brutusin.commons.Bean;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.ComponentItem;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.client.RpcCallback;
import org.brutusin.rpc.client.wskt.TopicCallback;
import org.brutusin.rpc.client.wskt.WebsocketEndpoint;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WritableSession;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class DelegatingTopic extends Topic<Void, JsonNode> {

    private static final Logger LOGGER = Logger.getLogger(DelegatingTopic.class.getName());

    private final WebsocketEndpoint endpoint;
    private final String targetId;

    public DelegatingTopic(WebsocketEndpoint endpoint, final String targetId) {
        this.endpoint = endpoint;
        this.targetId = targetId;
        register();
    }

    private ComponentItem getComponentItem() {
        try {
            final Bean<ComponentItem> bean = new Bean<ComponentItem>();
            endpoint.exec(new RpcCallback() {
                public void call(RpcResponse<JsonNode> response) {
                    try {
                        if (response.getError() != null) {
                            LOGGER.severe(response.getError().getMessage().toString());
                            return;
                        }
                        JsonNode topics = response.getResult();
                        for (int i = 0; i < topics.getSize(); i++) {
                            JsonNode topic = topics.get(i);
                            if (topic.get("id").asString().equals(targetId)) {
                                bean.setValue(JsonCodec.getInstance().load(topic, ComponentItem.class));
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    } finally {
                        synchronized (bean) {
                            bean.notifyAll();
                        }
                    }
                }
            }, "rpc.topics", null);
            if (endpoint.isAvailable()) {
                synchronized (bean) {
                    if (bean.getValue() == null) {
                        bean.wait();
                    }
                }
            }
            return bean.getValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonSchema getMessageSchema() {
        try {
            final Bean<JsonSchema> bean = new Bean<JsonSchema>();
            endpoint.exec(new RpcCallback() {
                public void call(RpcResponse<JsonNode> response) {
                    try {
                        if (response.getError() != null) {
                            LOGGER.severe(response.getError().getMessage().toString());
                            return;
                        }
                        bean.setValue(JsonCodec.getInstance().parseSchema(response.getResult().toString()));
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    } finally {
                        synchronized (bean) {
                            bean.notifyAll();
                        }
                    }
                }
            }, "rpc.topics.schema", JsonCodec.getInstance().parse("{\"id\":\"" + targetId + "\"}"));
            if (endpoint.isAvailable()) {
                synchronized (bean) {
                    if (bean.getValue() == null) {
                        bean.wait();
                    }
                }
            }
            return bean.getValue();
        } catch (Exception ie) {
            throw new RuntimeException(ie);
        }
    }

    private void register() {
        endpoint.subscribe(targetId, new TopicCallback() {
            public void call(JsonNode message) {
                fire(null, message);
            }
        });
    }

    @Override
    public Set<WritableSession> getSubscribers(Void filter) {
        return getSubscribers();
    }

    @Override
    public boolean isActive() {
        ComponentItem ci = getComponentItem();
        if (ci == null) {
            return false;
        }
        return ci.isActive();
    }

    @Override
    public String getDescription() {
        ComponentItem ci = getComponentItem();
        if (ci == null) {
            return null;
        }
        return ci.getDescription();
    }

    @Override
    public URL getSourceCode() {
        ComponentItem ci = getComponentItem();
        if (ci == null) {
            return null;
        }
        return ci.getSourceCode();
    }
}
