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

import org.brutusin.json.spi.JsonSchema;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.actions.SchemaActionHelper;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.SafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Returns the input/output **JSON schema** of the deployed `HTTP` services.")
public class SchemaAction extends SafeAction<SchemaActionInput, JsonSchema> {

    @Override
    public Cacheable<JsonSchema> execute(SchemaActionInput input) throws Exception {
        return Cacheable.conditionally(SchemaActionHelper.execute(input, HttpActionSupport.getInstance().getHttpServices()));
    }
}
