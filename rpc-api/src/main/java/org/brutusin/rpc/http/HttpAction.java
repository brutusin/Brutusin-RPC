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
package org.brutusin.rpc.http;

import org.brutusin.rpc.RpcAction;

/**
 * Instances of this class define the business logic of the services. One
 * instance is created per service to serve all requests.
 * <br/><br/><b>Threading issues</b>: Instances of this class will be accessed
 * by several threads concurrently, so implementing subclasses must be
 * thread-safe.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <I> Input POJO class.
 * @param <O> Output POJO class
 */
public abstract class HttpAction<I, O> extends RpcAction<I, O> {

    /**
     * Action idempotence
     * (http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.1.2)
     *
     * @return
     */
    public abstract boolean isIdempotent();

}
