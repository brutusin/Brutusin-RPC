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
package org.brutusin.rpc.actions;

import java.util.Map;
import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.RpcAction;
import org.brutusin.rpc.actions.SchemaActionInput.SchemaMode;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class SchemaActionHelper {

    public static <A extends RpcAction> JsonSchema execute(SchemaActionInput input, Map<String, A> services) throws Exception {
        A service = validateAndGetService(input, services);
        JsonSchema value;
        if (input.getMode() == SchemaMode.I) {
            value = service.getInputSchema();
        } else {
            value = service.getOutputSchema();
        }
        return value;
    }

    private static <A extends RpcAction> A validateAndGetService(SchemaActionInput input, Map<String, A> services) {
        if (input.getId() == null) {
            throw new IllegalArgumentException("Resource id is required");
        }
        if (input.getMode() == null) {
            throw new IllegalArgumentException("Schema mode is required");
        }
        A service = services.get(input.getId());
        if (service == null) {
            throw new IllegalArgumentException("Resource not found '" + input.getId() + "'");
        }
        return service;
    }
}
