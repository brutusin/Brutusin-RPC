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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.brutusin.rpc.Description;
import org.brutusin.rpc.RpcConfig;
import org.brutusin.rpc.actions.http.EnvironmentPopertiesAction.Output;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Returns the [`org.brutusin:rcp`](https://github.com/brutusin/rpc) environment variables used in the current deployment.")
public class EnvironmentPopertiesAction extends SafeAction<Void, Output> {

    @Override
    public Cacheable<Output> execute(Void input) throws Exception {
        return Cacheable.never(new Output());
    }

    public static class Output {
        private final RpcConfig config = RpcConfig.getInstance();
        private final Map<String, String> envProperties =  new TreeMap<String, String>();
        private final Map<String, String> systemProperties =  new TreeMap<String, String>();

        public Output(){
            envProperties.putAll(System.getenv());
            Set<Map.Entry<Object, Object>> entrySet = System.getProperties().entrySet();
            for (Map.Entry<Object, Object> entry : entrySet) {
                systemProperties.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
                
        public RpcConfig getConfig() {
            return config;
        }

        public Map<String, String> getSystemProperties() {
            return systemProperties;
        }

        public Map<String, String> getEnvProperties() {
            return envProperties;
        }
    }
}
