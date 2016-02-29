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
package org.brutusin.rpc_chat.topics;

import org.brutusin.rpc_chat.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.brutusin.rpc.RpcActionSupport;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WritableSession;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class MessageTopic extends Topic<Integer, Message> {

    private final Map<Integer, WritableSession> sessionMap = Collections.synchronizedMap(new HashMap());

    @Override
    protected void beforeSubscribe(WritableSession session) {
        User user = getUser();
        session.getUserProperties().put("user", user);
        sessionMap.put(user.getId(), session);
        Message message = new Message();
        message.setFrom(user.getId());
        message.setTime(System.currentTimeMillis());
        message.setLogged(true);
        fire(null, message);
    }

    @Override
    protected void afterUnsubscribe(WritableSession session) {
        User user = getUser();
        sessionMap.remove(getUser().getId());
        Message message = new Message();
        message.setFrom(user.getId());
        message.setTime(System.currentTimeMillis());
        message.setLogged(false);
        fire(null, message);
    }

    @Override
    public Set<WritableSession> getSubscribers(Integer filter) {
        if (filter == null) {
            return super.getSubscribers();
        }
        WritableSession toSession = sessionMap.get(filter);
        if (toSession == null) {
            return null;
        }
        HashSet<WritableSession> ret = new HashSet<WritableSession>();
        ret.add(toSession);
        ret.add(sessionMap.get(getUser().getId()));
        return ret;
    }

    private User getUser() {
        return User.from(RpcActionSupport.getInstance().getHttpSession());
    }
}
