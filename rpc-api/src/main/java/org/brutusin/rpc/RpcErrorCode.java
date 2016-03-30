/*
 * Copyright 2016 Ignacio del Valle Alles idelvall@brutusin.org.
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
package org.brutusin.rpc;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public enum RpcErrorCode {

    parseError(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON input"),
    invalidRequest(-32600, "Invalid request", "The JSON sent is not a valid JSON-RPC request object"),
    methodNotFound(-32601, "Method not found", "The method does not exist / is not available"),
    invalidParams(-32602, "Invalid params", "Invalid method parameter(s)"),
    internalError(-32603, "Internal error", "Internal JSON-RPC error"),
    securityError(-32000, "Security error", "Security error"),
    applicationError(-32001, "Application error", "Error contemplated by the application logic"),
    invalidHttpMethodError(-32002, "HTTP invalid method", "The HTTP method used in the request is not allowed by target resource"),
    connectionError(-32003, "Connection error", "Disconnected from the peer"); // For completeness. Used in client proxies

    private final int code;
    private final String message;
    private final String meaning;

    private RpcErrorCode(int code, String message, String meaning) {
        this.code = code;
        this.message = message;
        this.meaning = meaning;
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
}
