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

import java.security.Principal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.RpcConfig;
import org.brutusin.rpc.RpcUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <M>
 */
public final class SessionImpl<M> implements WritableSession<M> {

    private static final Logger LOGGER = Logger.getLogger(SessionImpl.class.getName());
    private final int queueMaxSize;
    private final Thread t;
    private final LinkedList<String> messageQueue = new LinkedList();
    private final javax.websocket.Session session;
    private final WebsocketContext ctx;
    private final Set<String> roles;

    public SessionImpl(javax.websocket.Session session, WebsocketContext ctx) {
        this.session = session;
        this.queueMaxSize = RpcConfig.getInstance().getMaxWsktQueueSize();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
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
                        SessionImpl.this.session.getBasicRemote().sendText(message);
                    } catch (Throwable th) {
                        LOGGER.log(Level.WARNING, th.getMessage(), th);
                    }
                }
            }
        };
        t = ctx.getSpringContext().getThreadFactory().newThread(runnable);
        t.setDaemon(true);
        this.ctx = ctx;
        this.roles = RpcUtils.getUserRoles(ctx.getSecurityContext());
    }

    @Override
    public boolean isUserInRole(String role) {
        return roles.contains(role);
    }

    public Set<String> getUserRoles() {
        return roles;
    }

    @Override
    public boolean isSecure() {
        return session.isSecure();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public Principal getUserPrincipal() {
        return session.getUserPrincipal();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return session.getUserProperties();
    }

    @Override
    public void sendToPeer(M m) {
        send(JsonCodec.getInstance().transform(m));
    }

    public void sendToPeerRaw(String message) {
        send(message);
    }

    public WebsocketContext getCtx() {
        return ctx;
    }

    private void send(String message) {
        synchronized (messageQueue) {
            if (queueMaxSize > 0 && messageQueue.size() == queueMaxSize) {
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
