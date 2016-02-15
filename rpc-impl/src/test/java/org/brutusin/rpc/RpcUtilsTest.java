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

import org.brutusin.commons.Pair;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class RpcUtilsTest {

    public RpcUtilsTest() {
    }

    @Test
    public void testDoOriginMatch() {
        Pair<String, String>[] matches = new Pair[]{
            new Pair("*", "*"),
            new Pair("file://", "*"),
            new Pair("localhost", "http://localhost"),
            new Pair("localhost:8080", "http://localhost:8080"),
            new Pair("http://localhost", "http://localhost:80"),
            new Pair("https://localhost", "http://localhost:443")
        };

        Pair<String, String>[] nonMatches = new Pair[]{
            new Pair(null, null),
            new Pair("*", null),
            new Pair("file://", "http://localhost:8080"),
            new Pair("http://localhost", "http://localhost:8080"),
            new Pair("http://host", "http://localhost")
        };

        for (int i = 0; i < matches.length; i++) {
            Pair<String, String> pair = matches[i];
            System.out.println("asserting match: " + pair);
            assertTrue(RpcUtils.doOriginsMatch(pair.getElement1(), pair.getElement2()));
            assertTrue(RpcUtils.doOriginsMatch(pair.getElement2(), pair.getElement1()));

        }
        for (int i = 0; i < nonMatches.length; i++) {
            Pair<String, String> pair = nonMatches[i];
            System.out.println("asserting not match: " + pair);
            assertFalse(RpcUtils.doOriginsMatch(pair.getElement1(), pair.getElement2()));
            assertFalse(RpcUtils.doOriginsMatch(pair.getElement2(), pair.getElement1()));
        }
    }

}
