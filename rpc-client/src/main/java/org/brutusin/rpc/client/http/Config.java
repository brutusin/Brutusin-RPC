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
package org.brutusin.rpc.client.http;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class Config {

    private final int pingSeconds;
    private final int maxCacheEntries;
    private final int maxCacheObjectSize;
    private final int connectTimeOutSeconds;
    private final int socketTimeOutSeconds;
    private final int maxConections;

    public Config(int pingSeconds, int maxCacheEntries, int maxCacheObjectSize, int connectTimeOutSeconds, int socketTimeOutSeconds, int maxConections) {
        this.pingSeconds = pingSeconds;
        this.maxCacheEntries = maxCacheEntries;
        this.maxCacheObjectSize = maxCacheObjectSize;
        this.connectTimeOutSeconds = connectTimeOutSeconds;
        this.socketTimeOutSeconds = socketTimeOutSeconds;
        this.maxConections = maxConections;
    }

    public int getMaxCacheEntries() {
        return maxCacheEntries;
    }

    public int getMaxCacheObjectSize() {
        return maxCacheObjectSize;
    }

    public int getPingSeconds() {
        return pingSeconds;
    }

    public int getConnectTimeOutSeconds() {
        return connectTimeOutSeconds;
    }

    public int getSocketTimeOutSeconds() {
        return socketTimeOutSeconds;
    }

    public int getMaxConections() {
        return maxConections;
    }
}
