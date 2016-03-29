/*
 * Copyright 2016 DREAMgenics S.L..
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
package org.brutusin.rpc.client.wskt;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.brutusin.commons.Trie;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.client.RpcRequest;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpoint {

    private static final Logger LOGGER = Logger.getLogger(WebsocketEndpoint.class.getName());

    private final URI endpoint;

    private final AtomicInteger reqCounter = new AtomicInteger();

    private final Map<String, JsonNode> serviceMap = new HashMap();
    private final Map<Integer, Callback> rpcCallbacks = new HashMap();
    private final Map<String, Callback> topicCallbacks = new HashMap();

    private final LinkedList<RpcRequest> reconnectingQueue = new LinkedList();
    private final LinkedList<Trie<Callback, String, JsonNode>> initialQueue = new LinkedList();

    private final Thread pingThread;

    private Websocket websocket;
    private boolean reconnecting;

    public WebsocketEndpoint(URI endpoint, final Configuration cfg) {
        this.endpoint = endpoint;
        doExec(new Callback() {
            public void call(JsonNode response) {
                if (response.get("error") != null) {
                    LOGGER.severe(response.toString());
                    return;
                }
                JsonNode services = response.get("result");
                for (int i = 0; i < services.getSize(); i++) {
                    JsonNode service = services.get(i);
                    serviceMap.put(service.get("id").asString(), service);
                }
                for (Trie<Callback, String, JsonNode> req : initialQueue) {
                    exec(req.getElement1(), req.getElement2(), req.getElement3());
                }
                initialQueue.clear();
            }
        }, "rpc.wskt.services", null, true);
        this.pingThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(1000 * cfg.getPingSeconds());
                        doExec(new Callback() {
                            public void call(JsonNode response) {
                                if (response.get("error") != null) {
                                    LOGGER.severe(response.toString());
                                }
                            }
                        }, "rpc.wskt.ping", null, false);
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            }
        };
        pingThread.setDaemon(true);
        pingThread.start();

    }

    private synchronized void reconnect() {
        if (reconnecting) {
            return;
        }
        reconnecting = true;
        if (this.websocket != null) {
            try {
                this.websocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            this.websocket.setMessageListener(null);
            this.websocket = null;
        }

        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        new Thread() {
            @Override
            public void run() {
                try {
                    webSocketContainer.connectToServer(new Endpoint() {
                        @Override
                        public void onOpen(final Session session, EndpointConfig config) {
                            synchronized (WebsocketEndpoint.this) {
                                WebsocketEndpoint.this.websocket = new Websocket() {
                                    @Override
                                    public void send(String message) throws IOException {
                                        session.getBasicRemote().sendText(message);
                                    }

                                    @Override
                                    public void close() throws IOException {
                                        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, null));
                                    }
                                };
                                WebsocketEndpoint.this.websocket.setMessageListener(new MessageListener() {
                                    @Override
                                    public void onMessage(String message) {
                                        try {
                                            JsonNode response = JsonCodec.getInstance().parse(message);
                                            if (response.get("jsonrpc") != null) {
                                                Integer id = response.get("id").asInteger();
                                                Callback callback = rpcCallbacks.remove(id);
                                                callback.call(response);
                                            } else {
                                                String topic = response.get("topic").asString();
                                                Callback callback = topicCallbacks.get(topic);
                                                callback.call(response.get("message"));
                                            }
                                        } catch (ParseException ex) {
                                            Logger.getLogger(WebsocketEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                });
                                session.addMessageHandler(new MessageHandler.Whole<String>() {
                                    @Override
                                    public void onMessage(String message) {
                                        MessageListener messageListener = WebsocketEndpoint.this.websocket.getMessageListener();
                                        if (message != null) {
                                            messageListener.onMessage(message);
                                        }
                                    }
                                });
                                for (RpcRequest req : reconnectingQueue) {
                                    sendRequest(req, true);
                                }
                                reconnectingQueue.clear();
                                for (String topic : topicCallbacks.keySet()) {
                                    try {
                                        doExec(null, "rpc.topics.subscribe", JsonCodec.getInstance().parse("{\"id\":\"" + topic + "\"}"), true);
                                    } catch (ParseException ex) {
                                        throw new AssertionError();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onClose(Session session, CloseReason closeReason) {
                            synchronized (WebsocketEndpoint.this) {
                                WebsocketEndpoint.this.websocket = null;
                            }
                        }

                        @Override
                        public void onError(Session session, Throwable thr) {
                            Logger.getLogger(WebsocketEndpoint.class.getName()).log(Level.SEVERE, null, thr);
                        }

                    }, cec, endpoint);
                } catch (DeploymentException ex) {
                    LOGGER.log(Level.SEVERE, "Websocket deployment failed " + endpoint + ". " + ex.getMessage());
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                } finally {
                    synchronized (WebsocketEndpoint.this) {
                        reconnecting = false;
                    }
                }
            }
        }.start();
    }

    private synchronized void sendRequest(RpcRequest request, boolean enqueueIfNotAvailable) {
        if (this.websocket == null) {
            if (enqueueIfNotAvailable) {
                reconnectingQueue.add(request);
            } else if (request.getId() != null) {
                rpcCallbacks.remove(request.getId());
            }
            reconnect();
        } else {
            try {
                this.websocket.send(JsonCodec.getInstance().transform(request));
            } catch (IOException ex) {
                if (!enqueueIfNotAvailable && request.getId() != null) {
                    rpcCallbacks.remove(request.getId());
                }
                LOGGER.severe(ex.getMessage());
            }
        }
    }

    private synchronized void doExec(Callback callback, String serviceId, JsonNode input, boolean enqueueIfNotAvailable) {
        Integer reqId = null;
        if (callback != null) {
            reqId = reqCounter.getAndIncrement();
            rpcCallbacks.put(reqId, callback);
        }
        RpcRequest request = new RpcRequest();
        request.setId(reqId);
        request.setParams(input);
        request.setMethod(serviceId);

        sendRequest(request, enqueueIfNotAvailable);
    }

    public synchronized void exec(Callback callback, String serviceId, JsonNode input) {
        if (serviceId == null) {
            throw new IllegalArgumentException("execParam.service is required");
        }
        if (serviceMap.size() > 0) {
            JsonNode service = serviceMap.get(serviceId);
            if (service == null) {
                throw new IllegalArgumentException("Service not found: '" + serviceId + "'");
            }
            doExec(callback, serviceId, input, true);
        } else {
            initialQueue.add(new Trie<Callback, String, JsonNode>(callback, serviceId, input));
        }
    }

    public synchronized void subscribe(String topicId, Callback callback) {
        topicCallbacks.put(topicId, callback);
        if (this.websocket != null) {
            try {
                exec(callback, "rpc.topics.subscribe", JsonCodec.getInstance().parse("{\"id\":\"" + topicId + "\"}"));
            } catch (ParseException ex) {
                throw new AssertionError();
            }
        }
    }

    public synchronized void unsubscribe(String topicId) {
        if (!topicCallbacks.containsKey(topicId)) {
            throw new IllegalArgumentException("Not subscribed to topic " + topicId);
        }
        try {
            topicCallbacks.remove(topicId);
            exec(null, "rpc.topics.unsubscribe", JsonCodec.getInstance().parse("{\"id\":\"" + topicId + "\"}"));
        } catch (ParseException ex) {
            throw new AssertionError();
        }
    }

    public void close() throws IOException {
        this.pingThread.interrupt();
        if (this.websocket != null) {
            this.websocket.close();
        }
    }

    public static class Configuration {

        private final int pingSeconds;

        public Configuration(int pingSeconds) {
            this.pingSeconds = pingSeconds;
        }

        public int getPingSeconds() {
            return pingSeconds;
        }
    }
}
