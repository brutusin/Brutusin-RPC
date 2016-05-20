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

import java.io.IOException;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.rpc.spi.ServerRuntime;
import org.brutusin.rpc.websocket.Topic;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class Server {

    private static final String LOGO = loadLogo();

    private Server() {
    }
    
    private static String loadLogo() {
        try {
            return Miscellaneous.toString(Server.class.getClassLoader().getResourceAsStream("brutusin-rpc-logo.txt"), "UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void exec() {
        System.out.println(LOGO);
        ServerRuntime.getInstance().exec();
    }

    public static void exec(Integer port) {
        System.out.println(LOGO);
        ServerRuntime.getInstance().exec(port);
    }

    public static void test(RpcAction action) {
        System.out.println(LOGO);
        ServerRuntime.getInstance().test(action);
    }

    public static void test(Integer port, RpcAction action) {
        System.out.println(LOGO);
        ServerRuntime.getInstance().test(port, action);
    }

    public static void test(Topic topic) {
        System.out.println(LOGO);
        ServerRuntime.getInstance().test(topic);
    }

    public static void test(Integer port, Topic topic) {
        System.out.println(LOGO);
        ServerRuntime.getInstance().test(port, topic);
    }
}
