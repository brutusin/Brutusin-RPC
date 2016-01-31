package org.brutusin.demo.http;

import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;


public class ConditionalCachedAction extends SafeAction<Void, String> {

    private static final String MESSAGE = "This response is conditionally cacheable";

    @Override
    public Cacheable<String> execute(Void input) throws Exception {
        return Cacheable.conditionally(MESSAGE);
    }
}
