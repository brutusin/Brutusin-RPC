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
package org.brutusin.rpc.client.wskt;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import javax.websocket.ContainerProvider;
import org.brutusin.json.spi.JsonNode;
import org.brutusin.rpc.RpcResponse;
import org.brutusin.rpc.client.RpcCallback;
import org.junit.Test;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class WebsocketEndpointTest {

    @Test
    public void test() throws Exception {
        final CountDownLatch counter = new CountDownLatch(1);
        final WebsocketEndpoint ws = new WebsocketEndpoint(ContainerProvider.getWebSocketContainer(), new URI("ws://localhost:8080/rpc/wskt"), null);
        ws.exec(new RpcCallback() {
            public void call(RpcResponse<JsonNode> response) {
                counter.countDown();
                System.out.println(response.getResult());
            }
        }, "rpc.wskt.version", null);
        counter.await();
        ws.close();
    }

}
