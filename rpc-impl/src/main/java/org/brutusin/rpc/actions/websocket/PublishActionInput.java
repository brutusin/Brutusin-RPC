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
package org.brutusin.rpc.actions.websocket;

import org.brutusin.json.annotations.JsonProperty;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class PublishActionInput<F, M> {

    @JsonProperty(title = "Filter", description = "Filter used by the topic to discriminate subscribers")
    private F filter;
    @JsonProperty(title = "Message", required = true, description = "Message to be published")
    private M message;

    public F getFilter() {
        return filter;
    }

    public void setFilter(F filter) {
        this.filter = filter;
    }

    public M getMessage() {
        return message;
    }

    public void setMessage(M message) {
        this.message = message;
    }
}
