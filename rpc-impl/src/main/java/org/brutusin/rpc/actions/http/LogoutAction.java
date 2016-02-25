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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcActionSupport;
import org.brutusin.rpc.http.UnsafeAction;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Logs out actual user")
public class LogoutAction extends UnsafeAction<Void, Void> {

    @Override
    public boolean isIdempotent() {
        return true;
    }

    @Override
    public Void execute(Void input) throws Exception {
        HttpServletRequest request = (HttpServletRequest) RpcActionSupport.forHttp().getHttpServletRequest();
        request.logout();
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return null;
    }
}
