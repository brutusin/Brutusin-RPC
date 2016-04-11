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
package org.brutusin.rpc.client.http;

import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.http.CachingInfo;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpResponse {

    private CachingInfo cachingInfo;
    private RpcResponse<JsonNode> rpcResponse;
    private MetaDataInputStream inputStream;

    public boolean isIsBinary() {
        return inputStream != null;
    }

    public RpcResponse<JsonNode> getRpcResponse() {
        return rpcResponse;
    }

    public void setRpcResponse(RpcResponse<JsonNode> rpcResponse) {
        this.rpcResponse = rpcResponse;
    }

    public MetaDataInputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(MetaDataInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public CachingInfo getCachingInfo() {
        return cachingInfo;
    }

    public void setCachingInfo(CachingInfo cachingInfo) {
        this.cachingInfo = cachingInfo;
    }
    
    @Override
    public String toString() {
        if(isIsBinary()){
            return super.toString();
        } else {
            return rpcResponse.toString();
        }
    }
}
