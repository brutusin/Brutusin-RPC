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
import java.security.Principal;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.RpcSpringContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <M>
 */
public class SessionImpl<M> implements WritableSession<M> {

    private static final Logger LOGGER = Logger.getLogger(SessionImpl.class.getName());
    private final int queueMaxSize = 10;
    private final Thread t;
    private final LinkedList<String> messageQueue = new LinkedList();
    private final javax.websocket.Session session;
    private final RpcSpringContext rpcCtx;

    public SessionImpl(javax.websocket.Session session, RpcSpringContext rpcCtx) {
        this.session = session;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (Thread.interrupted()) {
                            break;
                        }
                        String message = null;
                        synchronized (messageQueue) {
                            while (messageQueue.isEmpty()) {
                                messageQueue.wait();
                            }
                            message = messageQueue.pop();
                        }
                        try {
                            SessionImpl.this.session.getBasicRemote().sendText(message);
                        } catch (IOException ex) {
                            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                        }
                    }
                } catch (InterruptedException ie) {
                    // Interrupted by close()
                }
            }
        };
        this.rpcCtx = rpcCtx;
        t = rpcCtx.getThreadFactory().newThread(runnable);
        t.setDaemon(true);
    }

    public boolean isSecure() {
        return session.isSecure();
    }

    public String getId() {
        return session.getId();
    }

    public Principal getUserPrincipal() {
        return session.getUserPrincipal();
    }

    public Map<String, Object> getUserProperties() {
        return session.getUserProperties();
    }

    public void sendToPeer(M m) {
        send(JsonCodec.getInstance().transform(m));
    }

    public void sendToPeerRaw(String message) {
        send(message);
    }

    public RpcSpringContext getRpcCtx() {
        return rpcCtx;
    }

    private void send(String message) {
        synchronized (messageQueue) {
            if (messageQueue.size() == queueMaxSize) {
                throw new IllegalStateException("Exceeded maximum size message queue for a peer session: " + queueMaxSize);
            }
            messageQueue.add(message);
            messageQueue.notify();
        }
    }

    public void init() {
        this.t.start();
    }

    public void close() {
        this.t.interrupt();
    }

}
