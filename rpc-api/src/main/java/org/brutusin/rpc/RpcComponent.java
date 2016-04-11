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

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Enumeration;
import org.brutusin.commons.utils.Miscellaneous;
import org.springframework.core.ResolvableType;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public abstract class RpcComponent {

    private String id;

    /**
     * @param id Spring registration id
     * @throws Exception
     */
    public final void init(String id) throws Exception {
        this.id = id;
        init();
    }

    public final String getId() {
        return id;
    }

    /**
     * @param id Spring registration id
     * @throws Exception
     */
    protected void init() throws Exception {
    }

    /**
     * Called on undeploy
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
    }

    public String getDescription() {
        Description desc = (Description) getClass().getAnnotation(Description.class);
        if (desc == null) {
            return null;
        }
        return desc.value();
    }

    /**
     *
     * @param rt
     * @return
     */
    protected static Type getType(final ResolvableType rt) {
        if (!rt.hasGenerics()) {
            return rt.resolve();
        } else {
            return new ParameterizedType() {
                public Type[] getActualTypeArguments() {
                    Type[] ret = new Type[rt.getGenerics().length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = getType(rt.getGenerics()[i]);
                    }
                    return ret;
                }

                public Type getRawType() {
                    return rt.resolve();
                }

                public Type getOwnerType() {
                    return null;
                }
            };
        }
    }

  
    public URL getSourceCode() {
        try {
            URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/source-repo.txt");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (url.getFile().contains(jarUrl.getFile())) {
                    String baseUrl = Miscellaneous.toString(url.openStream(), "UTF-8");
                    Class clazz = getClass().getDeclaringClass();
                    if (clazz == null) {
                        clazz = getClass();
                    }
                    StringBuilder sb = new StringBuilder(baseUrl);
                    if (!baseUrl.endsWith("/")) {
                        sb.append("/");
                    }
                    return new URL(sb.append(clazz.getName().replace('.', '/')).append(".java").toString());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }
}
