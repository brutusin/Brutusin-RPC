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
public class ConfigurationBuilder {

    private int pingSeconds = 5;
    private int maxCacheEntries = 1000;
    private int maxCacheObjectSize = 8192;
    private int connectTimeOutSeconds = 10;
    private int socketTimeOutSeconds = 20;
    private int maxConections = 5;

    public int getPingSeconds() {
        return pingSeconds;
    }

    public ConfigurationBuilder setPingSeconds(int pingSeconds) {
        this.pingSeconds = pingSeconds;
        return this;
    }

    public int getMaxCacheEntries() {
        return maxCacheEntries;
    }

    public ConfigurationBuilder setMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
        return this;
    }

    public int getMaxCacheObjectSize() {
        return maxCacheObjectSize;
    }

    public int getConnectTimeOutSeconds() {
        return connectTimeOutSeconds;
    }

    public ConfigurationBuilder setConnectTimeOutSeconds(int connectTimeOutSeconds) {
        this.connectTimeOutSeconds = connectTimeOutSeconds;
        return this;
    }

    public int getSocketTimeOutSeconds() {
        return socketTimeOutSeconds;
    }

    public ConfigurationBuilder setSocketTimeOutSeconds(int socketTimeOutSeconds) {
        this.socketTimeOutSeconds = socketTimeOutSeconds;
        return this;
    }

    public int getMaxConections() {
        return maxConections;
    }

    public ConfigurationBuilder setMaxConections(int maxConections) {
        this.maxConections = maxConections;
        return this;
    }

    /**
     *
     * @param maxCacheObjectSize in bytes
     * @return
     */
    public ConfigurationBuilder setMaxCacheObjectSize(int maxCacheObjectSize) {
        this.maxCacheObjectSize = maxCacheObjectSize;
        return this;
    }

    public Config build() {
        return new Config(pingSeconds, maxCacheEntries, maxCacheObjectSize, connectTimeOutSeconds, socketTimeOutSeconds, maxConections);
    }
}
