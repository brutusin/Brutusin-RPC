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
import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcUtils;
import org.brutusin.rpc.actions.ResourceItem;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionSupport;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("This descriptor service returns the **list** of the availabe **topics**. *[See action source code at [github](https://github.com/brutusin/rpc-impl/blob/master/src/main/java/org/brutusin/rpc/actions/websocket/TopicListAction.java)]*")
public class TopicListAction extends WebsocketAction<Void, ResourceItem[]> {

    @Override
    public ResourceItem[] execute(Void input) throws Exception {
         Map<String, Topic> topics = WebsocketActionSupport.getInstance().getTopics();
        ResourceItem[] topicItems = new ResourceItem[topics.size()];
        int i = 0;
        for (Map.Entry<String, Topic> entrySet : topics.entrySet()) {
            String id = entrySet.getKey();
            Topic topic = entrySet.getValue();
            ResourceItem ti = new ResourceItem();
            ti.setId(id);
            ti.setDescription(RpcUtils.getDescription(topic));
            topicItems[i++] = ti;
        }
        return topicItems;
    }
}
