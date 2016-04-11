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

import org.brutusin.rpc.http.HttpServiceItem;
import java.util.Map;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcSpringContext;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.SafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("This descriptor service returns the **list** of the deployed `HTTP` services.")
public class HttpServiceListAction extends SafeAction<Void, HttpServiceItem[]> {

    @Override
    public Cacheable<HttpServiceItem[]> execute(Void input) throws Exception {
        RpcSpringContext rpcSpringContext = (RpcSpringContext)HttpActionSupport.getInstance().getSpringContext();
        Map<String, HttpAction> services = HttpActionSupport.getInstance().getHttpServices();
        HttpServiceItem[] serviceItems = new HttpServiceItem[services.size()];
        int i = 0;
        for (Map.Entry<String, HttpAction> entrySet : services.entrySet()) {
            String id = entrySet.getKey();
            HttpAction service = entrySet.getValue();
            HttpServiceItem si = new HttpServiceItem();
            si.setId(id);
            si.setSafe(service.isSafe());
            si.setIdempotent(service.isIdempotent());
            si.setDescription(service.getDescription());
            si.setDynamicInputSchema(service.isDynamicInputSchema());
            si.setBinaryResponse(service.isBinaryResponse());
            si.setUpload(service.isUpload());
            si.setFramework(rpcSpringContext.isFrameworkAction(service));
            si.setSourceCode(service.getSourceCode());
            serviceItems[i++] = si;
        }
        return Cacheable.conditionally(serviceItems);
    }
}
