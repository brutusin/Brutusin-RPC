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

    public final Set<WritableSession> getSubscribers() {
        return sessions;
    }

    public abstract Set<WritableSession> getSubscribers(F filter);

    public final void fire(F filter, M message) throws Exception {
        if (message == null) {
            return;
        }
        JsonSchema messageSchema = JsonCodec.getInstance().getSchema(getMessageType());
        messageSchema.validate(JsonCodec.getInstance().parse(JsonCodec.getInstance().transform(message)));
        MessageResponse mr = new MessageResponse();
        mr.setTopic(getId());
        mr.setMessage(message);
        Set<WritableSession> subscribers = getSubscribers(filter);
        synchronized (subscribers) {
            if (subscribers != null) {
                for (WritableSession session : subscribers) {
                    session.sendToPeer(mr);
                }
            }
        }
    }

    public final void subscribe() throws InvalidSubscriptionException {
        WritableSession session = (WritableSession) WebsocketActionContext.getInstance().getSession();
        if (sessions.contains(session)) {
            throw new InvalidSubscriptionException("Current session is already subscribed to this topic");
        }
        sessions.add(session);
        onSubscribe(session);
    }

    public final void unsubscribe() throws InvalidSubscriptionException {
        WritableSession session = (WritableSession) WebsocketActionContext.getInstance().getSession();
        if (!sessions.contains(session)) {
            throw new InvalidSubscriptionException("Current session is not subscribed to this topic");
        }
        sessions.remove(session);
        onUnsubscribe(session);
    }

    protected void onSubscribe(WritableSession session) {
    }

    protected void onUnsubscribe(WritableSession session) {
    }

    public Type geFilterType() {
        return getType(ResolvableType.forClass(Topic.class, getClass()).getGenerics()[0]);
    }

    public Type getMessageType() {
        return getType(ResolvableType.forClass(Topic.class, getClass()).getGenerics()[1]);
    }

}
