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

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.servlet.ServletContext;
import org.brutusin.commons.utils.Miscellaneous;
import org.brutusin.rpc.actions.http.VersionAction;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcUtils {

    private RpcUtils() {
    }

    public static String getVersion() {
        try {
            return Miscellaneous.toString(VersionAction.class.getClassLoader().getResourceAsStream("brutusin-rpc.version"), "UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String getAnnotatedDescription(Class clazz) {
        Description desc = (Description) clazz.getAnnotation(Description.class);
        if (desc == null) {
            return null;
        }
        return desc.value();
    }

    public static String getDescription(Object obj) {
        if (obj instanceof Descriptible) {
            Descriptible dObj = (Descriptible) obj;
            return dObj.getDescription();
        }
        return getAnnotatedDescription(obj.getClass());
    }


    /**
     *
     * @param type
     * @return
     */
    public static Class getClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return getClass(pt.getRawType());
        }
        return Object.class;
    }
    
    /**
     * 
     * @param sc
     * @return 
     */
    public static RpcSpringContext getSpringContext(ServletContext sc){
        return (RpcSpringContext)sc.getAttribute(RpcWebInitializer.SERVLET_NAME);
    }
    
}
