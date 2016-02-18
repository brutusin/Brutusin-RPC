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
package org.brutusin.rpc.actions.http;

import org.brutusin.rpc.Description;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.SafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Returns security information about the logged user")
public class UserDetailAction extends SafeAction<Void, UserDetail> {
    
    @Override
    public Cacheable<UserDetail> execute(Void input) throws Exception {
        UserDetail ud = new UserDetail();
        if (HttpActionSupport.getInstance().getUserPrincipal() != null) {
            ud.setPrincipal(HttpActionSupport.getInstance().getUserPrincipal().getName());
        }
        ud.setRoles(HttpActionSupport.getInstance().getUserRoles());
        return Cacheable.never(ud);
    }
}
