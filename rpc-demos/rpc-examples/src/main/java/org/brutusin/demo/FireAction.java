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
package org.brutusin.demo;

import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class FireAction extends WebsocketAction<TopicMessage, Void> {

    private TestTopic topic;

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(TestTopic topic) {
        this.topic = topic;
    }

    @Override
    public Void execute(TopicMessage msg) throws Exception {
        topic.fire(null, msg);
        return null;
    }
}
