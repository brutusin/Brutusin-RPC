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

import org.brutusin.json.annotations.JsonProperty;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcResponse<E> {

    private final String jsonrpc = "2.0";
    private E result;
    private Error error;
    private Integer id;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public E getResult() {
        return result;
    }

    public void setResult(E result) {
        this.result = result;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public static class Error {

        @JsonProperty(required = true)
        private int code;
        @JsonProperty(required = true)
        private String message;
        @JsonProperty(required = true)
        private String meaning;
        private Object data;

        public Error() {
        }
        
        public Error(RpcErrorCode error) {
            this(error, null);
        }

        public Error(RpcErrorCode error, Object data) {
            this.code = error.getCode();
            this.meaning = error.getMeaning();
            this.message = error.getMessage();
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getMeaning() {
            return meaning;
        }

        public Object getData() {
            return data;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
