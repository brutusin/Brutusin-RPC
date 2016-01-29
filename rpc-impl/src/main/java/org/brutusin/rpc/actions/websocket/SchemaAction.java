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
package org.brutusin.rpc.actions.websocket;

import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcContext;
import org.brutusin.rpc.actions.SchemaActionHelper;
import org.brutusin.rpc.websocket.WebsocketAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Returns the input/output **JSON schema** of the deployed `websocket` services. *[See action source code at [github](https://github.com/brutusin/rpc/blob/master/src/main/java/org/brutusin/rpc/actions/websocket/SchemaAction.java)]*")
public class SchemaAction extends WebsocketAction<ServiceSchemaActionInput, JsonSchema> {

    @Override
    public JsonSchema execute(ServiceSchemaActionInput input) throws Exception {
        return SchemaActionHelper.execute(input,RpcContext.getInstance().getWebSocketServices());
    }

}
