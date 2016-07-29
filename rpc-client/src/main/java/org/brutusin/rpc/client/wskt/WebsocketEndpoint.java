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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.brutusin.commons.Trie;
import org.brutusin.json.ParseException;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.RpcRequest;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.client.RpcCallback;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpoint {

    private static final Logger LOGGER = Logger.getLogger(WebsocketEndpoint.class.getName());

    private final URI endpoint;

    private final WebSocketContainer webSocketContainer;
    private final AtomicInteger reqCounter = new AtomicInteger();

    private final Map<String, JsonNode> serviceMap = new HashMap();
    private final Map<Integer, RpcCallback> rpcCallbacks = new HashMap();
    private final Map<String, TopicCallback> topicCallbacks = new HashMap();

    private final LinkedList<RpcRequest> reconnectingQueue = new LinkedList();
    private final LinkedList<Trie<RpcCallback, String, JsonNode>> initialQueue = new LinkedList();
    private final MessageListener messageListener;

    private final Thread pingThread;

    private Websocket websocket;
    private boolean reconnecting;
    private AtomicInteger reconnectionCounter = new AtomicInteger();

    public WebsocketEndpoint(WebSocketContainer webSocketContainer, URI endpoint, Config cfg) {
        if (cfg == null) {
            cfg = new ConfigurationBuilder().build();
        }
        this.webSocketContainer = webSocketContainer;
        this.endpoint = endpoint;
        this.messageListener = new MessageListener() {
            @Override
            public void onMessage(String message) {
                try {
                    JsonNode response = JsonCodec.getInstance().parse(message);
                    if (response.get("jsonrpc") != null) {
                        RpcResponse<JsonNode> rpcResponse = new RpcResponse<JsonNode>();
                        if (response.get("error") != null) {
                            rpcResponse.setError(JsonCodec.getInstance().load(response.get("error"), RpcResponse.Error.class));
                        }
                        rpcResponse.setResult(response.get("result"));
                        Integer id = response.get("id").asInteger();
                        rpcResponse.setId(id);
                        RpcCallback callback = rpcCallbacks.remove(id);
                        callback.call(rpcResponse);
                    } else {
                        String topic = response.get("topic").asString();
                        TopicCallback callback = topicCallbacks.get(topic);
                        callback.call(response.get("message"));
                    }
                } catch (ParseException ex) {
                    Logger.getLogger(WebsocketEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        final int pingSeconds = cfg.getPingSeconds();
        doExec(new RpcCallback() {
            public void call(RpcResponse<JsonNode> response) {
                if (response.getError() != null) {
                    LOGGER.severe(response.toString());
                    return;
                }
                JsonNode services = response.getResult();
                for (int i = 0; i < services.getSize(); i++) {
                    JsonNode service = services.get(i);
                    serviceMap.put(service.get("id").asString(), service);
                }
                for (Trie<RpcCallback, String, JsonNode> req : initialQueue) {
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
                        Thread.sleep(1000 * pingSeconds);
                        doExec(new RpcCallback() {
                            public void call(RpcResponse<JsonNode> response) {
                                if (response.getError() != null) {
                                    LOGGER.severe(response.toString());
                                }
                            }
                        }, "rpc.wskt.ping", null, false);
                    } catch (InterruptedException ie) {
                        break;
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
        int counter = reconnectionCounter.incrementAndGet();
        if (counter > 1) {
            LOGGER.warning("Reconnecting websocket client to " + endpoint);
        }
        if (this.websocket != null) {
            try {
                this.websocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            this.websocket = null;
        }

        final ClientEndpointConfig cec = new ClientEndpointConfig() {

            public List<String> getPreferredSubprotocols() {
                return Collections.EMPTY_LIST;
            }

            public List<Extension> getExtensions() {
                return Collections.EMPTY_LIST;
            }

            public List<Class<? extends Encoder>> getEncoders() {
                return Collections.EMPTY_LIST;
            }

            public List<Class<? extends Decoder>> getDecoders() {
                return Collections.EMPTY_LIST;
            }

            public Map<String, Object> getUserProperties() {
                return new HashMap<String, Object>();
            }

            public ClientEndpointConfig.Configurator getConfigurator() {
                return new Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {

                        System.out.println(headers);
                    }

                    @Override
                    public void afterResponse(HandshakeResponse hr) {
                        super.afterResponse(hr);
                    }
                };
            }
        };
        new Thread() {
            @Override
            public void run() {
                try {
                    webSocketContainer.connectToServer(new Endpoint() {
                        @Override
                        public void onOpen(final Session session, EndpointConfig config) {
                            try {
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
                                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                                        @Override
                                        public void onMessage(String message) {
                                            if (message != null) {
                                                messageListener.onMessage(message);
                                            }
                                        }
                                    });
                                    List<RpcRequest> list = new LinkedList(reconnectingQueue);
                                    reconnectingQueue.clear();
                                    for (RpcRequest req : list) {
                                        sendRequest(req, true);
                                    }

                                    for (String topic : topicCallbacks.keySet()) {
                                        try {
                                            doExec(null, "rpc.topics.subscribe", JsonCodec.getInstance().parse("{\"id\":\"" + topic + "\"}"), true);
                                        } catch (ParseException ex) {
                                            throw new AssertionError();
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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
                } catch (Throwable th) {
                    LOGGER.log(Level.SEVERE, th.getMessage(), th);
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

    private synchronized void doExec(RpcCallback callback, String serviceId, JsonNode input, boolean enqueueIfNotAvailable) {
        Integer reqId = null;
        if (callback != null) {
            reqId = reqCounter.getAndIncrement();
            rpcCallbacks.put(reqId, callback);
        }
        RpcRequest request = new RpcRequest();
        request.setJsonrpc("2.0");
        request.setId(reqId);
        request.setParams(input);
        request.setMethod(serviceId);

        sendRequest(request, enqueueIfNotAvailable);
    }

    public synchronized void exec(RpcCallback callback, String serviceId, JsonNode input) {
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
            initialQueue.add(new Trie<RpcCallback, String, JsonNode>(callback, serviceId, input));
        }
    }

    public synchronized void subscribe(String topicId, TopicCallback callback) {
        topicCallbacks.put(topicId, callback);
        if (this.websocket != null) {
            try {
                exec(null, "rpc.topics.subscribe", JsonCodec.getInstance().parse("{\"id\":\"" + topicId + "\"}"));
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
}
