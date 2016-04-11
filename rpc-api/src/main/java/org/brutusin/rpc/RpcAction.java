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
package org.brutusin.rpc;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.DynamicSchemaProvider;
import org.brutusin.json.spi.Expression;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;
import org.springframework.core.ResolvableType;

/**
 * Instances of this class define the business logic of the services. One
 * instance is created per service to serve all requests.
 * <br/><br/><b>Threading issues</b>: Instances of this class will be accessed
 * by several threads concurrently, so implementing subclasses must be
 * thread-safe.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <I> Input POJO class.
 * @param <O> Output POJO class
 */
public abstract class RpcAction<I, O> extends RpcComponent {

    /**
     * Business logic method.
     *
     * @param input
     * @return
     * @throws Exception
     */
    public abstract O execute(I input) throws Exception;

    public boolean isDynamicInputSchema() {
        return DynamicSchemaProvider.class.isAssignableFrom(Miscellaneous.getClass(getInputType()));
    }

    public Type getInputType() {
        return getType(ResolvableType.forClass(RpcAction.class, getClass()).getGenerics()[0]);
    }

    public Type getOutputType() {
        return getType(ResolvableType.forClass(RpcAction.class, getClass()).getGenerics()[1]);
    }

    public JsonSchema getInputSchema() {
        return JsonCodec.getInstance().getSchema(getInputType());
    }

    public Map<String, JsonSchema> getDynamicInputSchemas(String[] fieldNames, JsonNode data) {
        if (fieldNames == null || fieldNames.length == 0) {
            throw new IllegalArgumentException("Field names are required");
        }
        Map<String, JsonSchema> ret = new LinkedHashMap<String, JsonSchema>();

        DynamicSchemaProvider schemaProvider;
        Class<?> inputClass = Miscellaneous.getClass(getInputType());
        if (DynamicSchemaProvider.class.isAssignableFrom(inputClass)) {
            try {
                schemaProvider = (DynamicSchemaProvider) inputClass.newInstance();
            } catch (Exception ex) {
                throw new Error(ex);
            }
            for (String fieldName : fieldNames) {
                if (!ret.containsKey(fieldName)) {
                    ret.put(fieldName, schemaProvider.getDynamicSchema(fieldName, data));
                }
            }
        } else {
            for (String fieldName : fieldNames) {
                if (!ret.containsKey(fieldName)) {
                    Expression exp = JsonCodec.getInstance().compile(fieldName);
                    ret.put(fieldName, exp.projectSchema(JsonCodec.getInstance().getSchema(inputClass)));
                }
            }
        }
        return ret;
    }

    public JsonSchema getOutputSchema() {
        return JsonCodec.getInstance().getSchema(getOutputType());
    }
}
