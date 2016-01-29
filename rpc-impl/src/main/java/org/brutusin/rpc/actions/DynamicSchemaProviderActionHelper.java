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

import java.util.HashMap;
import java.util.Map;
import org.brutusin.json.spi.Expression;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.json.DynamicSchemaProvider;
import org.brutusin.rpc.RpcAction;
import org.brutusin.rpc.RpcUtils;

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
        DynamicSchemaProvider schemaProvider;
        Class<?> inputClass = RpcUtils.getClass(service.getInputType());
        if (DynamicSchemaProvider.class.isAssignableFrom(inputClass)) {
            schemaProvider = (DynamicSchemaProvider) inputClass.newInstance();
        } else {
            schemaProvider = null;
        }
        return getVariableSchemas(schemaProvider, inputClass, input);
    }

    private static Map<String, JsonSchema> getVariableSchemas(DynamicSchemaProvider schemaProvider, Class inputClass, DynamicSchemaProviderInput input) {
        Map<String, JsonSchema> ret = new HashMap<String, JsonSchema>();
        for (String fieldName : input.getFieldNames()) {
            if (!ret.containsKey(fieldName)) {
                if (schemaProvider == null) {
                    Expression exp = JsonCodec.getInstance().compile(fieldName);
                    JsonSchema projectedSchema = exp.projectSchema(JsonCodec.getInstance().getSchema(inputClass));
                    ret.put(fieldName, projectedSchema);
                } else {
                    if (fieldName.equals("$")) {
                        ret.put(fieldName, JsonCodec.getInstance().getSchema(inputClass));
                    } else {
                        ret.put(fieldName, schemaProvider.getDynamicSchema(fieldName, input.getInput()));
                    }
                }
            }
        }
        return ret;
    }
}
