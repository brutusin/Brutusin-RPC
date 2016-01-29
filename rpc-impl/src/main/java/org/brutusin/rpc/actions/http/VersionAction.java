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
package org.brutusin.rpc.actions.http;

import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcAction;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;
import org.brutusin.rpc.RpcUtils;
import org.springframework.core.GenericTypeResolver;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Returns the current **version** of [`org.brutusin:rcp`](https://github.com/brutusin/rpc). *[See action source code at [github](https://github.com/brutusin/rpc/blob/master/src/main/java/org/brutusin/rpc/actions/http/VersionAction.java)]*")
public class VersionAction extends SafeAction<Void, String> {

    @Override
    public Cacheable<String> execute(Void input) throws Exception {
        return Cacheable.never(RpcUtils.getVersion());
    }
    
    public static void main(String[] args) {
        Class<?> resolveTypeArgument = GenericTypeResolver.resolveTypeArgument(VersionAction.class, RpcAction.class);
        System.out.println(resolveTypeArgument);
    }
}
