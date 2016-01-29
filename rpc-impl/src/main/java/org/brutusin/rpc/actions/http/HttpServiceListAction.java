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

import java.util.Map;
import org.brutusin.rpc.RpcContext;
import org.brutusin.json.DynamicSchemaProvider;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcUtils;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.http.SafeAction;
import org.brutusin.rpc.http.StreamResult;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("This descriptor service returns the **list** of the deployed `HTTP` services. *[See action source code at [github](https://github.com/brutusin/rpc/blob/master/src/main/java/org/brutusin/rpc/actions/http/HttpServiceListAction.java)]*")
public class HttpServiceListAction extends SafeAction<Void, HttpServiceItem[]> {
    
    private HttpServiceItem[] serviceItems;
    
    @Override
    public void init() throws Exception {
        Map<String, HttpAction> services = RpcContext.getInstance().getHttpServices();
        this.serviceItems = new HttpServiceItem[services.size()];
        int i = 0;
        for (Map.Entry<String, HttpAction> entrySet : services.entrySet()) {
            String id = entrySet.getKey();
            HttpAction service = entrySet.getValue();
            HttpServiceItem si = new HttpServiceItem();
            si.setId(id);
            si.setSafe(service instanceof SafeAction);
            si.setIdempotent(service.isIdempotent());
            si.setDescription(RpcUtils.getDescription(service));
            Class<?> inputClass = RpcUtils.getClass(service.getInputType());
            si.setDynamicInputSchema(DynamicSchemaProvider.class.isAssignableFrom(inputClass));
            si.setBinaryResponse(StreamResult.class.isAssignableFrom(RpcUtils.getClass(service.getOutputType())));
            si.setUpload(JsonCodec.getInstance().getSchema(inputClass).toString().contains("\"format\":\"inputstream\""));
            this.serviceItems[i++] = si;
        }
    }
    
    @Override
    public Cacheable<HttpServiceItem[]> execute(Void input) throws Exception {
        return Cacheable.conditionally(this.serviceItems);
    }
}
