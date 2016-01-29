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
package org.brutusin.rpc.http;

/**
 * 
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 * @param <T> 
 */
public final class Cacheable<T> {
    
    private final T value;
    private final CachingInfo cachingInfo;

    public static <T> Cacheable<T> never(T value) {
        return new Cacheable<T>(value, null);
    }
    
    public static <T> Cacheable<T> conditionally(T value) {
        return new Cacheable<T>(value, new CachingInfo());
    }

    public static <T> Cacheable<T> forMaxSeconds(T value, int s) {
        CachingInfo cachingInfo = new CachingInfo(s);
        return new Cacheable<T>(value, cachingInfo);
    }

    public static <T> Cacheable<T> forMaxMinutes(T value, int m) {
        CachingInfo cachingInfo = new CachingInfo(m * CachingInfo.MINUTE);
        return new Cacheable<T>(value, cachingInfo);
    }
    
    public static <T> Cacheable<T> forMaxHours(T value, int h) {
        CachingInfo cachingInfo = new CachingInfo(h * CachingInfo.HOUR);
        return new Cacheable<T>(value, cachingInfo);
    }
    
    public static <T> Cacheable<T> forMaxDays(T value, int d) {
        CachingInfo cachingInfo = new CachingInfo(d * CachingInfo.DAY);
        return new Cacheable<T>(value, cachingInfo);
    }
    
    public static <T> Cacheable<T> forForever(T value) {
        CachingInfo cachingInfo = new CachingInfo(CachingInfo.FOREVER);
        return new Cacheable<T>(value, cachingInfo);
    }

    public Cacheable(T value, CachingInfo cachingInfo) {
        this.value = value;
        this.cachingInfo = cachingInfo;
    }

    public T getValue() {
        return value;
    }

    public CachingInfo getCachingInfo() {
        return cachingInfo;
    }
}
