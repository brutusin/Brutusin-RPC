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
import java.util.Map;
import org.brutusin.rpc.spi.ServerRuntime;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class RpcConfig {

    public static final String ENV_PROP_PATH = "org.brutusin.rpc.http-path";
    public static final String ENV_PROP_SERVER_URI_ENC = "org.brutusin.rpc.server-uri-encoding";
    public static final String ENV_PROP_UPLOAD_FOLDER = "org.brutusin.rpc.upload.folder";
    public static final String ENV_PROP_MAX_FILE_SIZE = "org.brutusin.rpc.upload.max-file-size";
    public static final String ENV_PROP_MAX_REQUEST_SIZE = "org.brutusin.rpc.upload.max-request-size";
    public static final String ENV_PROP_ACCESS_CONTROL_ORIGIN_HOST = "org.brutusin.rpc.cors-host";
    public static final String ENV_PROP_INCLUDE_BUITIN_SERVICES = "org.brutusin.rpc.include-builtin-services";
    public static final String ENV_PROP_INCLUDE_ENV_SERVICE = "org.brutusin.rpc.include-env-service";
    public static final String ENV_PROP_TEST_MODE = "org.brutusin.rpc.test-mode";

    public static final String SYSTEM_ENV_TO_PROP_PREFIX = "org.brutusin.rpc.*";

    private static final RpcConfig INSTANCE = new RpcConfig();

    private String path;
    private String serverUriEncoding;
    private File uploadFolder;
    private Long maxFileSize;
    private Long maxRequestSize;
    private String accessControOriginHost;
    private boolean includeBuiltinServices;
    private boolean includeEnvService;
    private boolean testMode;

    private RpcConfig() {
        trasformEnvToSystemProperties();
        this.path = getEnv(ENV_PROP_PATH, "/rpc");
        String defUriEncoding;
        if (ServerRuntime.getInstance() == null) {
            defUriEncoding = "ISO-8859-1"; // External web container ASCII imposed by specification
        } else {
            defUriEncoding = ServerRuntime.getInstance().getURIEncoding(); // External web container
        }
        this.serverUriEncoding = getEnv(ENV_PROP_SERVER_URI_ENC, defUriEncoding);
        this.uploadFolder = new File(getEnv(ENV_PROP_UPLOAD_FOLDER, new File(System.getProperty("java.io.tmpdir"), "brutusin-rcp-uploads").getAbsolutePath()));
        this.maxFileSize = Long.valueOf(getEnv(ENV_PROP_MAX_FILE_SIZE, String.valueOf(Long.MAX_VALUE)));
        this.maxRequestSize = Long.valueOf(getEnv(ENV_PROP_MAX_REQUEST_SIZE, String.valueOf(Long.MAX_VALUE)));
        this.accessControOriginHost = getEnv(ENV_PROP_ACCESS_CONTROL_ORIGIN_HOST, null);
        this.includeBuiltinServices = Boolean.valueOf(getEnv(ENV_PROP_INCLUDE_BUITIN_SERVICES, "true"));
        this.includeEnvService = Boolean.valueOf(getEnv(ENV_PROP_INCLUDE_ENV_SERVICE, "true"));
        this.testMode = Boolean.valueOf(getEnv(ENV_PROP_TEST_MODE, "false"));
    }

    public static RpcConfig getInstance() {
        return INSTANCE;
    }

    private static String getEnv(String prop, String defValue) {
        String value = System.getenv(prop);
        if (value == null) {
            value = defValue;
        }
        return value;
    }

    private static void trasformEnvToSystemProperties() {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entrySet : env.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();
            if (key.startsWith(SYSTEM_ENV_TO_PROP_PREFIX)) {
                System.setProperty(key.substring(SYSTEM_ENV_TO_PROP_PREFIX.length()), value);
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getServerUriEncoding() {
        return serverUriEncoding;
    }

    public File getUploadFolder() {
        return uploadFolder;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public Long getMaxRequestSize() {
        return maxRequestSize;
    }

    public String getAccessControOriginHost() {
        return accessControOriginHost;
    }

    public boolean isIncludeBuiltinServices() {
        return includeBuiltinServices;
    }

    public boolean isIncludeEnvService() {
        return includeEnvService;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setServerUriEncoding(String serverUriEncoding) {
        this.serverUriEncoding = serverUriEncoding;
    }

    public void setUploadFolder(File uploadFolder) {
        this.uploadFolder = uploadFolder;
    }

    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void setMaxRequestSize(Long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public void setAccessControOriginHost(String accessControOriginHost) {
        this.accessControOriginHost = accessControOriginHost;
    }

    public void setIncludeBuiltinServices(boolean includeBuiltinServices) {
        this.includeBuiltinServices = includeBuiltinServices;
    }

    public void setIncludeEnvService(boolean includeEnvService) {
        this.includeEnvService = includeEnvService;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }
}
