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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.catalina.Globals;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.EmptyResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.Constants;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.brutusin.rpc.actions.websocket.PublishAction;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.spi.ServerRuntime;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;

/**
 * Tomcat ServerRuntime service provider.
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class TomcatRuntime extends ServerRuntime {

    private static final Logger LOGGER = Logger.getLogger(TomcatRuntime.class.getName());

    private static File getRootFolder() {
        return new File("");
    }

    private static void addAutoOpen(StandardContext ctx, String... openUrls) {
        if (openUrls != null) {
            final String[] urls = openUrls;
            ctx.addApplicationLifecycleListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            for (int i = 0; i < urls.length; i++) {
                                Desktop.getDesktop().browse(new URI(urls[i]));
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(TomcatRuntime.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });
        }
    }

    private static StandardContext addTestApp(final Tomcat tomcat, File rootFolder) throws Exception {
        File docBase = new File(rootFolder.getAbsolutePath(), "src/main/webapp");
        if (!docBase.exists()) {
            docBase = new File(rootFolder.getAbsolutePath(), "src/main/resources/META-INF/resources");
        }
        if (!docBase.exists()) {
            docBase = Files.createTempDirectory("default-doc-base").toFile();
        }
        LOGGER.info("Setting application docbase as '" + docBase.getAbsolutePath() + "'");
        StandardContext ctx = (StandardContext) tomcat.addWebapp("", docBase.getAbsolutePath());
        ctx.setParentClassLoader(TomcatRuntime.class.getClassLoader());
        StandardJarScanner jarScanner = (StandardJarScanner) ctx.getJarScanner();
        if (System.getProperty(Constants.SKIP_JARS_PROPERTY) == null && System.getProperty(Constants.SCAN_JARS_PROPERTY) == null) {
            LOGGER.info("Disabling TLD scanning");
            StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) jarScanner.getJarScanFilter();
            jarScanFilter.setTldSkip("*");
        }
        WebResourceRoot resources = new StandardRoot(ctx);
        WebResourceSet resourceSet;
        File additionClassesFolder = new File(rootFolder.getAbsolutePath(), "target/classes");
        if (additionClassesFolder.exists()) {
            resourceSet = new DirResourceSet(resources, "/WEB-INF/classes", additionClassesFolder.getAbsolutePath(), "/");
            LOGGER.info("Loading application resources from '" + additionClassesFolder.getAbsolutePath() + "'");
        } else {
            resourceSet = new EmptyResourceSet(resources);
        }
        resources.addPreResources(resourceSet);
        ctx.setResources(resources);

        return ctx;
    }

    protected Tomcat createTomcat(int port) throws IOException {
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
        Tomcat tomcat = new Tomcat();
        Path tempPath = Files.createTempDirectory("brutusin-rcp-tests");
        tomcat.setBaseDir(tempPath.toString());
        tomcat.setPort(port);

        return tomcat;
    }

    @Override
    public String getURIEncoding() {
        if (Globals.STRICT_SERVLET_COMPLIANCE) {
            return "ISO-8859-1";
        } else {
            return "UTF-8";
        }
    }

    @Override
    public void exec(int port) {
        try {
            Tomcat tomcat = createTomcat(port);
            StandardContext stdCtx = addTestApp(tomcat, getRootFolder());
            addAutoOpen(stdCtx, "http://localhost:" + port);
            tomcat.start();
            tomcat.getServer().await();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void test(final int port, final RpcAction action) {
        try {
            RpcConfig.getInstance().setTestMode(true);
            Tomcat tomcat = createTomcat(port);
            final String id = action.getClass().getName();
            String url;
            if (action instanceof HttpAction) {
                url = "http://localhost:" + port + "/rpc/repo/?hash=http-services/" + id;
            } else {
                url = "http://localhost:" + port + "/rpc/repo/?hash=wskt-services/" + id;
            }
            StandardContext stdCtx = addTestApp(tomcat, getRootFolder());
            stdCtx.addApplicationLifecycleListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    RpcSpringContext rpcCtx = RpcUtils.getSpringContext(sce.getServletContext());
                    if (action instanceof HttpAction) {
                        rpcCtx.register(id, (HttpAction) action);
                    } else {
                        rpcCtx.register(id, (WebsocketAction) action);
                    }
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });
            addAutoOpen(stdCtx, url);
            tomcat.start();
            tomcat.getServer().await();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void test(final int port, final Topic topic) {
        try {
            RpcConfig.getInstance().setTestMode(true);
            Tomcat tomcat = createTomcat(port);
            final String topicId = topic.getClass().getName();
            final PublishAction publishAction = new PublishAction(topic);
            StandardContext stdCtx = addTestApp(tomcat, getRootFolder());
            stdCtx.addApplicationLifecycleListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    RpcSpringContext rpcCtx = RpcUtils.getSpringContext(sce.getServletContext());
                    rpcCtx.register("publish-service", publishAction);
                    rpcCtx.register(topicId, topic);
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });
            addAutoOpen(stdCtx, "http://localhost:" + port + "/rpc/test/topic.jsp?id=" + topicId);
            tomcat.start();
            tomcat.getServer().await();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
