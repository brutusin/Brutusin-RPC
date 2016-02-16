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

import javax.servlet.http.HttpServletRequest;
import org.springframework.core.NamedThreadLocal;

/**
 * Instances of this class have their life-cycle managed by a ServletRequestListener
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class GlobalThreadLocal {

    private static final ThreadLocal<GlobalThreadLocal> requests = new NamedThreadLocal<GlobalThreadLocal>("GlobalThreadLocal");

    private final HttpServletRequest httpRequest;
    private final Object securityContext;

    public GlobalThreadLocal(HttpServletRequest httpRequest, Object securityContext) {
        this.httpRequest = httpRequest;
        this.securityContext = securityContext;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public Object getSecurityContext() {
        return securityContext;
    }
    
    public static void set(GlobalThreadLocal wc) {
        requests.set(wc);
    }

    public static GlobalThreadLocal get() {
        return requests.get();
    }

    public static void clear() {
        requests.remove();
    }
}
