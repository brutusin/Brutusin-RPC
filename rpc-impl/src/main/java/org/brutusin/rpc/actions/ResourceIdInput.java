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

import java.util.Collection;
import org.brutusin.json.DynamicSchemaProvider;
import org.brutusin.json.ParseException;
import org.brutusin.json.annotations.DependentProperty;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class ResourceIdInput implements DynamicSchemaProvider {

    @DependentProperty
    private String id;

    @Override
    public JsonSchema getDynamicSchema(String fieldName, JsonNode input) {
        if (fieldName.equals("$.id")) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"string\",\"required\":true,\"title\":\"Service id\",\"enum\":");
            sb.append(JsonCodec.getInstance().transform(getResourceIds()));
            sb.append("}");
            try {
                return JsonCodec.getInstance().parseSchema(sb.toString());
            } catch (ParseException ex) {
                throw new AssertionError();
            }
        } else {
            throw new IllegalArgumentException("Schema of field '" + fieldName + "' is not dynamic");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    protected abstract Collection<String> getResourceIds();
}
