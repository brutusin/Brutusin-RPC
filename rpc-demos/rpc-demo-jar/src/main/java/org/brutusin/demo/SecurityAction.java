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

import org.brutusin.rpc.Server;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.SafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SecurityAction extends SafeAction<Void, String> {

    @Override
    public Cacheable<String> execute(Void input) throws Exception {
        String name;
        if (HttpActionSupport.getInstance().getUserPrincipal() == null) {
            name = null;
        } else {
            name = HttpActionSupport.getInstance().getUserPrincipal().getName();
        }

        return Cacheable.never(name + " " + HttpActionSupport.getInstance().isUserInRole("USER"));
    }

    public static void main(String[] args) {
        Server.test(new SecurityAction());
    }

}
