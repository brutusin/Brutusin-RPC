package org.brutusin.demo;

import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;


public class RuntimeExceptionAction extends SafeAction<Void, Void> {

    @Override
    public Cacheable<Void> execute(Void input) {
        throw new RuntimeException("An unexpected runtime exception");
    }
}
