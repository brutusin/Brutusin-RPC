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
package org.brutusin.rpc.spi;

import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.rpc.RpcAction;
import static org.brutusin.rpc.Server.DEFAULT_PORT;
import org.brutusin.rpc.websocket.Topic;

/**
 * This SPI defines the functionality that pluggable server embedded runtimes
 * have to provide.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class ServerRuntime {

    private static transient ServerRuntime instance;

    /**
     *
     * @return
     */
    public static ServerRuntime getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ServerRuntime.class) {
            if (instance == null) {
                instance = Miscellaneous.getInstance(ServerRuntime.class, false);
            }
            return instance;
        }
    }

    public final void exec() {
        exec(DEFAULT_PORT);
    }

    public final void test(RpcAction action) {
        test(DEFAULT_PORT, action);
    }

    public final void test(Topic topic) {
        test(DEFAULT_PORT, topic);
    }

    public abstract String getURIEncoding();

    public abstract void test(int port, Topic topic);

    public abstract void exec(int port);

    public abstract void test(int port, RpcAction action);
}
