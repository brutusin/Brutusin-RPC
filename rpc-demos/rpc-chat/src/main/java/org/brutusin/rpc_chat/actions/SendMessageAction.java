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
package org.brutusin.rpc_chat.actions;

import org.brutusin.rpc_chat.User;
import org.brutusin.rpc_chat.topics.Message;
import org.brutusin.rpc_chat.topics.MessageTopic;
import javax.servlet.http.HttpSession;
import org.brutusin.json.annotations.JsonProperty;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionSupport;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SendMessageAction extends WebsocketAction<SendMessageAction.SendMessageInput, Boolean> {

    private MessageTopic topic;

    public MessageTopic getTopic() {
        return topic;
    }

    public void setTopic(MessageTopic topic) {
        this.topic = topic;
    }

    @Override
    public Boolean execute(SendMessageInput input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("Input is required");
        }
        Integer from = User.from((HttpSession) WebsocketActionSupport.getInstance().getHttpSession()).getId();
        Message message = new Message();
        message.setFrom(from);
        message.setTo(input.getTo());
        message.setMessage(input.getMessage());
        message.setTime(System.currentTimeMillis());
        return topic.fire(input.getTo(), message);
    }

    public static class SendMessageInput {
        private Integer to;
        @JsonProperty(required = true)
        private String message;

        public Integer getTo() {
            return to;
        }

        public void setTo(Integer to) {
            this.to = to;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
