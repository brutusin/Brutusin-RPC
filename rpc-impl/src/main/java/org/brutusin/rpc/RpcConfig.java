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

import java.io.File;
import org.brutusin.rpc.spi.ServerRuntime;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class RpcConfig {

    private static final String PATH = EnvProperties.get("org.brutusin.rpc.http-path", "/rpc");
    private static final String SERVER_URI_ENCODING;
    private static final File UPLOAD_FOLDER = new File(EnvProperties.get("org.brutusin.rpc.upload.folder", new File(System.getProperty("java.io.tmpdir"), "brutusin-rcp-uploads").getAbsolutePath()));
    private static final Long MAX_FILE_SIZE = Long.valueOf(EnvProperties.get("org.brutusin.rpc.upload.max-file-size", String.valueOf(Long.MAX_VALUE)));
    private static final Long MAX_REQUEST_SIZE = Long.valueOf(EnvProperties.get("org.brutusin.rpc.upload.max-request-size", String.valueOf(Long.MAX_VALUE)));
    private static final String ACCESS_CONTROL_ORIGIN_HOST = EnvProperties.get("org.brutusin.rpc.cors-host", null);
    private static final Boolean INCLUDE_BUITIN_SERVICES = Boolean.valueOf(EnvProperties.get("org.brutusin.rpc.include-builtin-services", "true"));
    private static final Boolean INCLUDE_ENV_SERVICE = Boolean.valueOf(EnvProperties.get("org.brutusin.rpc.include-env-service", "true"));
    private static final String TEST_COMPONENT = EnvProperties.get("org.brutusin.rpc.test-class", null);

    static {
        String defUriEncoding;
        if (ServerRuntime.getInstance() == null) {
            // External web container
            defUriEncoding = "ISO-8859-1";
        } else {
            // Embedded server
            defUriEncoding = ServerRuntime.getInstance().getURIEncoding();
        }
        SERVER_URI_ENCODING = EnvProperties.get("org.brutusin.rpc.server-uri-encoding", defUriEncoding);
    }

    private RpcConfig() {
    }

    public static String getPath() {
        return PATH;
    }

    public static File getUploadFolder() {
        return UPLOAD_FOLDER;
    }

    public static Long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    public static Long getMaxRequestSize() {
        return MAX_REQUEST_SIZE;
    }

    public static String getAccessControlOriginHost() {
        return ACCESS_CONTROL_ORIGIN_HOST;
    }

    public static Boolean isIncludeEnvironmentViewerService() {
        return INCLUDE_ENV_SERVICE;
    }

    public static Boolean isIncludeBuiltinServices() {
        return INCLUDE_BUITIN_SERVICES;
    }

    public static String getServerUriEncoding() {
        return SERVER_URI_ENCODING;
    }

    public static String getTestComponentClass() {
        return TEST_COMPONENT;
    }
}
