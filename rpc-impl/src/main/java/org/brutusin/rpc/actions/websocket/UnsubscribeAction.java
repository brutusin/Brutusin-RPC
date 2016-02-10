/*
 * Copyright 2015 brutusin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.rpc.actions.websocket;

import org.brutusin.rpc.Description;
import org.brutusin.rpc.websocket.InvalidSubscriptionException;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("**Unsubscribes** current websocket session from the specified topic. *[See action source code at [github](https://github.com/brutusin/rpc-impl/blob/master/src/main/java/org/brutusin/rpc/actions/websocket/UnsubscribeAction.java)]*")
public class UnsubscribeAction extends WebsocketAction<TopicIdInput, Void> {

    @Override
    public Void execute(TopicIdInput input) throws InvalidSubscriptionException {
        if (input.getId() == null) {
            throw new IllegalArgumentException("Topic id is required");
        }
        Object obj = WebsocketActionContext.getInstance().getSpringContext().getBean(input.getId());
        if (obj == null || !(obj instanceof Topic)) {
            throw new IllegalArgumentException("Invalid topic id");
        }
        Topic topic = (Topic) obj;
        topic.unsubscribe();
        return null;
    }
}
