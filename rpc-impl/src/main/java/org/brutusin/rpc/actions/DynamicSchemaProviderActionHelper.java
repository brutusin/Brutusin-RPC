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
package org.brutusin.rpc.actions;

import java.util.Map;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.RpcAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class DynamicSchemaProviderActionHelper {

    public static <A extends RpcAction> Map<String, JsonSchema> execute(DynamicSchemaProviderInput input, Map<String, A> services) throws Exception {
        if (input.getId() == null) {
            throw new IllegalArgumentException("Service id is required");
        }
        A service = services.get(input.getId());
        if (service == null) {
            throw new IllegalArgumentException("Invalid service id '" + input.getId() + "'");
        }
        return service.getDynamicInputSchemas(input.getFieldNames(), input.getInput());
    }
}
