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

    public static final String ENV_PROP_PATH = "BRUTUSIN_RPC_PATH";
    public static final String ENV_PROP_SERVER_URI_ENC = "BRUTUSIN_RPC_URI_ENCODING";
    public static final String ENV_PROP_UPLOAD_FOLDER = "BRUTUSIN_RPC_UPLOAD_FOLDER";
    public static final String ENV_PROP_MAX_FILE_SIZE = "BRUTUSIN_RPC_UPLOAD_MAX_FILE_SIZE";
    public static final String ENV_PROP_MAX_REQUEST_SIZE = "BRUTUSIN_RPC_UPLOAD_MAX_REQUEST_SIZE";
    public static final String ENV_PROP_MAX_WSK_QUEUE_SIZE = "BRUTUSIN_RPC_WSKT_MAX_QUEUE";
    public static final String ENV_PROP_ACCESS_CONTROL_ORIGIN_HOST = "BRUTUSIN_RPC_CORS_HOST";
    public static final String ENV_PROP_INCLUDE_BUITIN_SERVICES = "BRUTUSIN_RPC_INCLUDE_BUILTIN_SERVICES";
    public static final String ENV_PROP_INCLUDE_ENV_SERVICE = "BRUTUSIN_RPC_INCLUDE_ENV_SERVICE";
    public static final String ENV_PROP_TEST_MODE = "BRUTUSIN_RPC_TEST_MODE";
    public static final String ENV_PROP_LINK_SOURCE_CODE = "BRUTUSIN_RPC_LINK_SOURCE";

    public static final String SYSTEM_ENV_TO_PROP_PREFIX = "BRUTUSIN_RPC_*";

    private static final RpcConfig INSTANCE = new RpcConfig();

    private String path;
    private String serverUriEncoding;
    private File uploadFolder;
    private Long maxFileSize;
    private Long maxRequestSize;
    private Integer maxWsktQueueSize;
    private String accessControlOriginHost;
    private boolean includeBuiltinServices;
    private boolean includeEnvService;
    private boolean testMode;
    private boolean linkSourceCode;

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
        this.maxWsktQueueSize = Integer.valueOf(getEnv(ENV_PROP_MAX_WSK_QUEUE_SIZE, "0"));
        this.accessControlOriginHost = getEnv(ENV_PROP_ACCESS_CONTROL_ORIGIN_HOST, null);
        this.includeBuiltinServices = Boolean.valueOf(getEnv(ENV_PROP_INCLUDE_BUITIN_SERVICES, "true"));
        this.includeEnvService = Boolean.valueOf(getEnv(ENV_PROP_INCLUDE_ENV_SERVICE, "true"));
        this.testMode = Boolean.valueOf(getEnv(ENV_PROP_TEST_MODE, "false"));
        this.linkSourceCode = Boolean.valueOf(getEnv(ENV_PROP_LINK_SOURCE_CODE, "false"));
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

    public String getAccessControlOriginHost() {
        return accessControlOriginHost;
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

    public Integer getMaxWsktQueueSize() {
        return maxWsktQueueSize;
    }

    public void setMaxWsktQueueSize(Integer maxWsktQueueSize) {
        this.maxWsktQueueSize = maxWsktQueueSize;
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

    public void setAccessControlOriginHost(String accessControOriginHost) {
        this.accessControlOriginHost = accessControOriginHost;
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

    public boolean isLinkSourceCode() {
        return linkSourceCode;
    }

    public void setLinkSourceCode(boolean linkSourceCode) {
        this.linkSourceCode = linkSourceCode;
    }
}
