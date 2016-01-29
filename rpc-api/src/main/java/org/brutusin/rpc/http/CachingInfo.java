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
package org.brutusin.rpc.http;

/**
 * 
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public final class CachingInfo {

    public static final int MINUTE = 60;
    public static final int HOUR = 60 * MINUTE;
    public static final int DAY = 24 * HOUR;
    public static final int WEEK = 7 * DAY;
    public static final int MONTH = 30 * DAY;
    public static final int FOREVER = Integer.MAX_VALUE;

    private int maxAge;
    private boolean store;
    private boolean shared;

    public CachingInfo() {
        this(0);
    }

    public CachingInfo(int maxAge) {
        this(maxAge, true, false);
    }

    public CachingInfo(int maxAge, boolean store, boolean shared) {
        this.maxAge = maxAge;
        this.store = store;
        this.shared = shared;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isStore() {
        return store;
    }

    public void setStore(boolean store) {
        this.store = store;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
