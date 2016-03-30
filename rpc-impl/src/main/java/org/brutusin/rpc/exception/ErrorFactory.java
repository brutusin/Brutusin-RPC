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
package org.brutusin.rpc.exception;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.json.ParseException;
import org.brutusin.json.ValidationException;
import org.brutusin.rpc.RpcErrorCode;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.http.RpcServlet;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class ErrorFactory {

    public static RpcResponse.Error getError(Throwable th) {
        if (th == null) {
            return null;
        }
        if (th instanceof InvalidHttpMethodException) {
            return new RpcResponse.Error(RpcErrorCode.invalidHttpMethodError, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof InvalidRequestException) {
            return new RpcResponse.Error(RpcErrorCode.invalidRequest, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof ServiceNotFoundException) {
            return new RpcResponse.Error(RpcErrorCode.methodNotFound, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof MaxLengthExceededException) {
            return new RpcResponse.Error(RpcErrorCode.invalidParams, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof IllegalArgumentException) {
            return new RpcResponse.Error(RpcErrorCode.invalidParams, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof ParseException) {
            return new RpcResponse.Error(RpcErrorCode.parseError, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof SecurityException) {
            Logger.getLogger(RpcServlet.class.getName()).log(Level.WARNING, null, th);
            return new RpcResponse.Error(RpcErrorCode.securityError, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof ValidationException) {
            ValidationException ve = (ValidationException) th;
            return new RpcResponse.Error(RpcErrorCode.invalidParams, ve.getMessages());
        }
        if (th instanceof RuntimeException) {
            Logger.getLogger(RpcServlet.class.getName()).log(Level.SEVERE, null, th);
            return new RpcResponse.Error(RpcErrorCode.internalError, Miscellaneous.getRootCauseMessage(th));
        }
        if (th instanceof Exception) {
            return new RpcResponse.Error(RpcErrorCode.applicationError, Miscellaneous.getRootCauseMessage(th));
        }
        Logger.getLogger(RpcServlet.class.getName()).log(Level.SEVERE, null, th);
        return new RpcResponse.Error(RpcErrorCode.internalError, Miscellaneous.getRootCauseMessage(th));
    }

}
