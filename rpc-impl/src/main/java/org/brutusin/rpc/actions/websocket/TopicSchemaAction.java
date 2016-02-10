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

import java.util.Map;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Return the **schema** of the messages of a topic. *[See action source code at [github](https://github.com/brutusin/rpc-impl/blob/master/src/main/java/org/brutusin/rpc/actions/websocket/TopicSchemaAction.java)]*")
public class TopicSchemaAction extends WebsocketAction<TopicIdInput, JsonSchema> {

    @Override
    public JsonSchema execute(TopicIdInput input) throws Exception {
        Map<String, Topic> topics = WebsocketActionContext.getInstance().getTopics();
        Topic topic = topics.get(input.getId());
        if (topic == null) {
            throw new IllegalArgumentException("Topic not found");
        }
        return JsonCodec.getInstance().getSchema(topic.getMessageType());
    }
}
