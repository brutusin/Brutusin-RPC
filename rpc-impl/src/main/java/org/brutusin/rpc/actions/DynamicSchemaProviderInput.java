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

import org.brutusin.json.annotations.JsonProperty;
import org.brutusin.json.spi.JsonNode;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class DynamicSchemaProviderInput extends ResourceIdInput {

    @JsonProperty(required = true, title = "Field names")
    private String[] fieldNames;
    @JsonProperty(title = "Service input")
    private JsonNode input;

    public JsonNode getInput() {
        return input;
    }

    public void setInput(JsonNode input) {
        this.input = input;
    }

    public String[] getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }
}
