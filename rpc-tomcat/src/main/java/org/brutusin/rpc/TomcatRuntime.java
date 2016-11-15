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
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogManager;
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

    private static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.manager", TomcatLogManager.class.getName());
        LOGGER = Logger.getLogger(TomcatRuntime.class.getName());
    }

    private static File getRootFolder() {
        return new File("");
    }

    private static void addAutoOpen(StandardContext ctx, final String... urls) {
        if (urls != null) {
            ctx.addApplicationLifecycleListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    openBrowser(urls);
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });
        }
    }

    private static void openBrowser(String... urls) {
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

    private static StandardContext addTestApp(final Tomcat tomcat, File rootFolder) throws Exception {
        File docBase = new File(rootFolder.getAbsolutePath(), "src/main/webapp");
        if (!docBase.exists()) {
            docBase = new File(rootFolder.getAbsolutePath(), "src/main/resources/META-INF/resources");
        }
        if (!docBase.exists()) {
            Path docBasePath = Files.createTempDirectory("default-doc-base");
            docBase = docBasePath.toFile();
            InputStream webStream = TomcatRuntime.class.getClassLoader().getResourceAsStream("META-INF/resources/WEB-INF/web.xml");
            if (webStream != null) {
                Path webInf = Files.createDirectory(FileSystems.getDefault().getPath(docBase.getAbsolutePath(), "WEB-INF"));
                Files.copy(webStream, FileSystems.getDefault().getPath(webInf.toString(), "web.xml"));
            }
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

    protected Tomcat createTomcat(Integer port) throws IOException {
        port = getPort(port);
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
    public void exec(Integer port) {
        try {
            port = getPort(port);
            Tomcat tomcat = createTomcat(port);
            StandardContext stdCtx = addTestApp(tomcat, getRootFolder());
            addAutoOpen(stdCtx, "http://localhost:" + port);
            start(tomcat);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void test(Integer port, final RpcAction action) {
        port = getPort(port);
        try {
            RpcConfig.getInstance().setTestMode(true);
            Tomcat tomcat = createTomcat(port);
            final String id = action.getClass().getName();
            final String url;
            if (action instanceof HttpAction) {
                url = "http://localhost:" + port + "/rpc/repo/?hash=http-services/" + id;
            } else {
                url = "http://localhost:" + port + "/rpc/repo/?hash=wskt-services/" + id;
            }
            StandardContext stdCtx = addTestApp(tomcat, getRootFolder());
            stdCtx.addApplicationLifecycleListener(new ServletContextListener() {
                @Override
                public void contextInitialized(final ServletContextEvent sce) {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            RpcSpringContext rpcCtx;
                            while ((rpcCtx = RpcUtils.getSpringContext(sce.getServletContext())) == null) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TomcatRuntime.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            if (action instanceof HttpAction) {
                                rpcCtx.register(id, (HttpAction) action);
                            } else {
                                rpcCtx.register(id, (WebsocketAction) action);
                            }
                            openBrowser(url);
                        }
                    };
                    t.start();
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });

            start(tomcat);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void start(final Tomcat tomcat) throws Exception {
        tomcat.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    LOGGER.info("Stopping Tomcat instance...");
                    Thread.sleep(1000);
                    tomcat.stop();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } finally {
                    TomcatLogManager.resetFinally();
                }
            }
        });
        tomcat.getServer().await();
    }

    private static Integer getPort(Integer port) {
        if (port != null) {
            return port;
        }
        return RpcConfig.getInstance().getPort();
    }

    @Override
    public void test(Integer port, final Topic topic) {
        port = getPort(port);
        try {
            RpcConfig.getInstance().setTestMode(true);
            Tomcat tomcat = createTomcat(port);
            final String topicId = topic.getClass().getName();
            final PublishAction publishAction = new PublishAction(topic);
            final String url = "http://localhost:" + port + "/rpc/test/topic.jsp?id=" + topicId;
            StandardContext stdCtx = addTestApp(tomcat, getRootFolder());

            stdCtx.addApplicationLifecycleListener(new ServletContextListener() {
                @Override
                public void contextInitialized(final ServletContextEvent sce) {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            RpcSpringContext rpcCtx;
                            while ((rpcCtx = RpcUtils.getSpringContext(sce.getServletContext())) == null) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TomcatRuntime.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            rpcCtx.register("publish-service", publishAction);
                            rpcCtx.register(topicId, topic);
                            openBrowser(url);
                        }
                    };
                    t.start();
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });
            start(tomcat);
        } catch (Exception ex) {
            throw new RuntimeException(ex);

        }
    }

    public static class TomcatLogManager extends LogManager {

        static TomcatLogManager instance;

        public TomcatLogManager() {
            instance = this;
        }

        @Override
        public void reset() { /* don't reset yet. */ }

        private void realReset() {
            super.reset();
        }

        public static void resetFinally() {
            instance.realReset();
        }
    }
}
