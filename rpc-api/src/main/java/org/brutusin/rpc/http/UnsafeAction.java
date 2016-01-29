/*
 * Copyright 2015 Ignacio del Valle Alles idelvall@brutusin.org.
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
package org.brutusin.rpc.http;

/**
 * Used to define business operations considered not <i>safe</i> according to
 * the HTTP definition.
 * <br/><br/><b>Threading issues</b>: Instances of this class will be accessed
 * by several threads concurrently, so implementing subclasses must be
 * thread-safe.
 * <br/><br/>
 * See section 4.2.1 of
 * <a href="http://www.rfc-editor.org/rfc/rfc7231.txt}">rfc7231</a>
 * for more details.
 *
 * @see SafeAction
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <I> Input POJO class.
 * @param <O> Output POJO class
 */
public abstract class UnsafeAction<I, O> extends HttpAction<I, O> {

    public boolean isIdempotent() {
        return false;
    }
}
