/*
 * Copyright 2017 Ignacio del Valle Alles idelvall@brutusin.org.
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
package org.brutusin.demo;

import org.brutusin.commons.utils.CryptoUtils;
import org.brutusin.json.annotations.JsonProperty;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.Server;
import org.brutusin.rpc.http.UnsafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ExampleJCSAction extends UnsafeAction<Void, ExampleJCSAction.ExampleJCSActionOutput> {

    @Override
    public ExampleJCSActionOutput execute(Void input) throws Exception {
        return new ExampleJCSActionOutput("This is a signed message");
    }

    public static class JCSResponse<E> {

        private final E value;
        @JsonProperty(title = "@context")
        private final String context;
        private final String qualifier;
        private final String signature;

        public JCSResponse(String context, String qualifier, E value) {
            this.value = value;
            this.context = context;
            this.qualifier = qualifier;
            this.signature = CryptoUtils.getHashMD5(JsonCodec.getInstance().transform(this));
        }

        public String getContext() {
            return context;
        }

        public String getQualifier() {
            return qualifier;
        }

        public E getValue() {
            return value;
        }

        public String getSignature() {
            return signature;
        }
    }

    public static class ExampleJCSActionOutput extends JCSResponse<String> {

        public ExampleJCSActionOutput(String value) {
            super("example.context", "example.qualifier", value);
        }
    }

    public static void main(String[] args) throws Exception {
        Server.test(new ExampleJCSAction());
    }
}
