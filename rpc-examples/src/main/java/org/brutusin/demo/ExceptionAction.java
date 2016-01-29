package org.brutusin.demo;

import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;


public class ExceptionAction extends SafeAction<Void, Void> {

    @Override
    public Cacheable<Void> execute(Void input) throws Exception {
        throw new Exception("This action always throws exceptions");
    }
}
