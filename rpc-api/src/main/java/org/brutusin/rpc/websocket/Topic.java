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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.RpcComponent;
import org.springframework.core.ResolvableType;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <F> Filter class
 * @param <M> Message class
 */
public abstract class Topic<F, M> extends RpcComponent {

    private final Set<WritableSession> sessions = Collections.synchronizedSet(new HashSet<WritableSession>());

    /**
     * Returns the whole collection of active topic subscribers
     *
     * @return
     */
    public final Set<WritableSession> getSubscribers() {
        return sessions;
    }

    /**
     * Returns the subset of subscribers that satisfy the filtering criteria
     *
     * @param filter
     * @return
     */
    public abstract Set<WritableSession> getSubscribers(F filter);

    public final boolean fire(F filter, M message) {
        if (message == null) {
            return false;
        }
        JsonSchema messageSchema = JsonCodec.getInstance().getSchema(getMessageType());
        try {
            messageSchema.validate(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(message)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        MessageResponse mr = new MessageResponse();
        mr.setTopic(getId());
        mr.setMessage(message);
        Set<WritableSession> subscribers = getSubscribers(filter);
        if (subscribers == null || subscribers.isEmpty()) {
            return false;
        }
        synchronized (subscribers) {
            if (subscribers != null) {
                for (WritableSession session : subscribers) {
                    session.sendToPeer(mr);
                }
            }
        }
        return true;
    }

    public final void subscribe() throws InvalidSubscriptionException {
        WritableSession session = (WritableSession) WebsocketActionSupport.getInstance().getSession();
        if (sessions.contains(session)) {
            throw new InvalidSubscriptionException("Current session is already subscribed to this topic");
        }
        beforeSubscribe(session);
        sessions.add(session);
    }

    public final void unsubscribe() throws InvalidSubscriptionException {
        WritableSession session = (WritableSession) WebsocketActionSupport.getInstance().getSession();
        if (!sessions.contains(session)) {
            throw new InvalidSubscriptionException("Current session is not subscribed to this topic");
        }
        sessions.remove(session);
        afterUnsubscribe(session);
    }

    /**
     * These two methods are auxiliary methods that subclasses can implement to
     * be keep track by themselves of the actual subscribers of the topic, and
     * create data structures better suited for a efficient topic filtering than
     * iterating over the whole collection of subscribers.
     *
     * See {@link #afterUnsubscribe(org.brutusin.rpc.websocket.WritableSession)}
     *
     * @param session
     */
    protected void beforeSubscribe(WritableSession session) {
    }

    /**
     * See {@link #beforeSubscribe(org.brutusin.rpc.websocket.WritableSession)}
     *
     * @param session
     */
    protected void afterUnsubscribe(WritableSession session) {
    }

    public Type geFilterType() {
        return getType(ResolvableType.forClass(Topic.class, getClass()).getGenerics()[0]);
    }

    public Type getMessageType() {
        return getType(ResolvableType.forClass(Topic.class, getClass()).getGenerics()[1]);
    }

}
