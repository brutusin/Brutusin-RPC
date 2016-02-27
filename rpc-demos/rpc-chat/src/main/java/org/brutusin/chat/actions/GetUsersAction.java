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
package org.brutusin.chat.actions;

import org.brutusin.chat.User;
import org.brutusin.chat.topics.MessageTopic;
import java.util.Set;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WritableSession;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class GetUsersAction extends WebsocketAction<Void, User[]> {

    private MessageTopic topic;

    public MessageTopic getTopic() {
        return topic;
    }

    public void setTopic(MessageTopic topic) {
        this.topic = topic;
    }

    @Override
    public User[] execute(Void v) throws Exception {
        Set<WritableSession> subscribers = topic.getSubscribers();
        User[] ret = new User[subscribers.size()];
        synchronized (subscribers) {
            int i = 0;
            for (WritableSession session : subscribers) {
                ret[i++] = (User) session.getUserProperties().get("user");
            }
        }
        return ret;
    }
}
