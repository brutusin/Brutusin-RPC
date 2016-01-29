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
package org.brutusin.demo.streaming;

import org.brutusin.json.DynamicSchemaProvider;
import org.brutusin.json.ParseException;
import org.brutusin.json.annotations.DependentProperty;
import org.brutusin.json.annotations.JsonProperty;
import org.brutusin.json.spi.Expression;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.json.spi.JsonSchema;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class LogoActionInput implements DynamicSchemaProvider {

    @JsonProperty(title = "Attachment", description = "Return logo as an attachment")
    private boolean attachment;
    @DependentProperty(dependsOn = "attachment")
    private String attachmentName;

    public boolean isAttachment() {
        return attachment;
    }

    public void setAttachment(boolean attachment) {
        this.attachment = attachment;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public JsonSchema getDynamicSchema(String fieldName, JsonNode data) {
        if (fieldName == null) {
            return JsonCodec.getInstance().getSchema(getClass());
        } else if (fieldName.equals("$.attachment")) {
            Expression exp = JsonCodec.getInstance().compile(fieldName);
            return exp.projectSchema(JsonCodec.getInstance().getSchema(getClass()));
        } else if (fieldName.equals("$.attachmentName")) {
            try {
                JsonNode attNode = data.get("attachment");
                if (attNode != null && attNode.asBoolean()) {
                    return JsonCodec.getInstance().parseSchema("{\"type\": \"string\",\"title\": \"File name\",\"required\": true,\"description\": \"Name for the attached file\"}");
                } else {
                    return JsonCodec.getInstance().parseSchema("{\"type\":null}");
                }
            } catch (ParseException pe) {
                throw new AssertionError(pe);
            }
        }
        throw new IllegalArgumentException("Invalid field name '" + fieldName + "'");
    }
}
