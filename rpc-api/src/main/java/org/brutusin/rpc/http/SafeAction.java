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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Used to define business operations considered <i>safe</i> according to the
 * HTTP definition, meaning that the action "... defined semantics are
 * essentially read-only; i.e., the client does not request, and does not
 * expect, any state change on the origin server as a result of applying a safe
 * method to a target resource. Likewise, reasonable use of a safe method is not
 * expected to cause any harm, loss of property, or unusual burden on the origin
 * server."
 * <br/><br/><b>Threading issues</b>: Instances of this class will be accessed
 * by several threads concurrently, so implementing subclasses must be
 * thread-safe.
 * <br/><br/><b>Instance life-cycle</b>: For each request received the following
 * methods are (can be) executed:
 * <ol>
 * <li> {@link #getCachingInfo(java.lang.Object)}: That returns caching
 * information for this request.</li>
 * <li> {@link #execute(java.lang.Object)}: Depending on the client request
 * being conditional, and on the value returned by the previous method, this
 * method is or is not executed.
 * </ol>
 * <br/>
 * See section 4.2.1 of <a
 * href="http://www.rfc-editor.org/rfc/rfc7231.txt}">rfc7231</a> for more
 * details.
 *
 * @see UnsafeAction
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <I> Input POJO class.
 * @param <O> Output POJO class.
 */
public abstract class SafeAction<I, O> extends HttpAction<I, Cacheable<O>> {

    protected Long getLastModified(I input) {
        return null;
    }

    @Override
    public final boolean isIdempotent() {
        return true;
    }

    @Override
    public final boolean isSafe(){
        return true;
    }
    
    @Override
    public Type getOutputType() {
        // Don't show Cacheable in the output type
        Type type = super.getOutputType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getActualTypeArguments().length == 1) {
                return pt.getActualTypeArguments()[0];
            }
        }
        return Object.class;
    }
}
