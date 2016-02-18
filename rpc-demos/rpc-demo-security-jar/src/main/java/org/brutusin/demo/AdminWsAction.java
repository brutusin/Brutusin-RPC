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

import org.brutusin.rpc.Description;
import org.brutusin.rpc.Server;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.brutusin.rpc.websocket.WebsocketActionSupport;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Hello word action that only accepts users with `ADMIN` role")
public class AdminWsAction extends WebsocketAction<Void, String> {

    @Override
    public String execute(Void input) throws Exception {
        if(!WebsocketActionSupport.getInstance().isUserInRole("ADMIN")){
            throw new SecurityException("Only admin users are allowed");
        }
        return "Hello " + WebsocketActionSupport.getInstance().getUserPrincipal().getName() +"!";
    }

    public static void main(String[] args) {
        Server.test(new AdminWsAction());
    }

}
