package org.brutusin.demo.http;

import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.CachingInfo;
import org.brutusin.rpc.http.SafeAction;

public class ExpiringCachedAction extends SafeAction<Void, String> {

    private static final String MESSAGE = "This response is cacheable (shared caches) for one day, and can be stored in persistent memory";

    @Override
    public Cacheable<String> execute(Void input) throws Exception {
        return new Cacheable<String>(MESSAGE, new CachingInfo(CachingInfo.DAY, true, true));
    }
}
