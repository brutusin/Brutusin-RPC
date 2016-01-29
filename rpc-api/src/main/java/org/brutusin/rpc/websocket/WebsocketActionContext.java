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
package org.brutusin.rpc.websocket;

import java.security.Principal;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class WebsocketActionContext {

    private static final ThreadLocal<WebsocketActionContext> CONTEXTS = new ThreadLocal();

    public static WebsocketActionContext getInstance() {
        return CONTEXTS.get();
    }

    static void setInstance(WebsocketActionContext context) {
        CONTEXTS.set(context);
    }

    static void clear() {
        CONTEXTS.remove();
    }

    public abstract Session getSession();

    public final Principal getUserPrincipal() {
        Session session = getWebsocketSession();
        if (session == null) {
            return null;
        }
        return session.getUserPrincipal();
    }

    private Session getWebsocketSession() {
        Object session = getSession();
        if (!(session instanceof Session)) {
            return null;
        }
        return (Session) session;
    }

}
