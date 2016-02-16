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
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpRequestHolder {

    private static final ThreadLocal<HttpServletRequest> requests = new NamedThreadLocal<HttpServletRequest>("Http requests");

    public static void set(HttpServletRequest wc) {
        requests.set(wc);
    }

    public static HttpServletRequest get() {
        return requests.get();
    }

    public static void clear() {
        requests.remove();
    }
}
