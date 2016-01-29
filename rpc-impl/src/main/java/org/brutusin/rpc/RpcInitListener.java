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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.json.spi.JsonCodec;
import org.brutusin.rpc.http.RpcServlet;
import org.brutusin.rpc.websocket.WebsocketEndpoint;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@WebListener
public class RpcInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        JsonCodec.getInstance().registerStringFormat(MetaDataInputStream.class, "inputstream");
        RpcContextImpl.getInstance().init();
        initHttpRpcRuntime(sce);
        initWebsocketRpcRuntime(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        RpcContextImpl.getInstance().destroy();
    }

    private void initHttpRpcRuntime(ServletContextEvent sce) {
        RpcServlet servlet = new RpcServlet();
        ServletRegistration.Dynamic regInfo = sce.getServletContext().addServlet("rpc.http", servlet);
        regInfo.setLoadOnStartup(1);
        regInfo.addMapping(RpcConfig.getPath() + "/http");
    }

    private void initWebsocketRpcRuntime(ServletContextEvent sce) {
        ServerContainer sc = (ServerContainer) sce.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
        ServerEndpointConfig sec = ServerEndpointConfig.Builder.create(WebsocketEndpoint.class, RpcConfig.getPath() + "/wskt").build();
        try {
            sc.addEndpoint(sec);
        } catch (DeploymentException ex) {
            throw new RuntimeException(ex);
        }
    }
}
