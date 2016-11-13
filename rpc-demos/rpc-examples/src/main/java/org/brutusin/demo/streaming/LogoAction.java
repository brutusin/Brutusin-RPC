/*
 * Copyright 2015 Ignacio del Valle Alles idelvall@brutusin.org.
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
package org.brutusin.demo.streaming;

import org.brutusin.commons.io.MetaDataInputStream;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;
import org.brutusin.rpc.http.StreamResult;
import org.brutusin.rpc.Server;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
@Description("Service that returns an image (binary payload) an uses a dynamic input schema.")
public class LogoAction extends SafeAction<LogoActionInput, StreamResult> {

    @Override
    public Cacheable<StreamResult> execute(LogoActionInput input) throws Exception {
        MetaDataInputStream is = new MetaDataInputStream(getClass().getClassLoader().getResourceAsStream("brutusin-logo.png"), input != null ? input.getAttachmentName() : null, "image/png", 18694L, 1450550428L);
        return Cacheable.conditionally(new StreamResult(is));
    }

    public static void main(String[] args) throws Exception {
        Server.test(new LogoAction());
    }
}
