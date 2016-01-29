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

import org.brutusin.rpc.exception.MaxLengthExceededException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class LimitedLengthInputStream extends InputStream {

    private final InputStream is;
    private final long maxBytes;
    private long byteCounter;

    public LimitedLengthInputStream(InputStream is) {
        this(is, 0);
    }

    public LimitedLengthInputStream(InputStream is, long maxBytes) {
        this.is = is;
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        int b = is.read();
        if (b >= 0) {
            byteCounter++;
            if (byteCounter > maxBytes) {
                throw new MaxLengthExceededException(maxBytes);
            }
        }
        return b;
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }

    @Override
    public int hashCode() {
        return is.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LimitedLengthInputStream other = (LimitedLengthInputStream) obj;
        if (this.is != other.is && (this.is == null || !this.is.equals(other.is))) {
            return false;
        }
        return true;
    }
}
