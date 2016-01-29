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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class EnvProperties {

    private static final Map<String, String> PROPS = new TreeMap();

    private EnvProperties() {
    }

    public static String get(String propName, String defValue) {
        String value = PROPS.get(propName);
        if (value != null) {
            return value;
        }
        value = System.getenv(propName);
        if (value == null) {
            value = defValue;
        }
        PROPS.put(propName, value);
        return value;
    }

    public static Map<String, String> getProperties() {
        return Collections.unmodifiableMap(PROPS);
    }
}
