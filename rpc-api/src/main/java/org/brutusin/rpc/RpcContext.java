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
package org.brutusin.rpc;

import java.util.Map;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.spi.ServerRuntime;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.Topic;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class RpcContext {

    private static transient RpcContext instance;

    /**
     *
     * @return
     */
    public static RpcContext getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (RpcContext.class) {
            if (instance == null) {
                instance = Miscellaneous.getInstance(RpcContext.class);
            }
            return instance;
        }
    }

    public abstract ClassPathXmlApplicationContext getApplicationContext();

    public abstract void register(String id, RpcAction action);

    public abstract void register(String id, Topic topic);

    public abstract Map<String, HttpAction> getHttpServices();

    public abstract Map<String, WebsocketAction> getWebSocketServices();

    public abstract Map<String, Topic> getTopics();
}
