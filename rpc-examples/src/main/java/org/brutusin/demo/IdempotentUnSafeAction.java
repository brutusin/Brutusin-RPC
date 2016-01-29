package org.brutusin.demo;

import org.brutusin.rpc.http.UnsafeAction;


public class IdempotentUnSafeAction extends UnsafeAction<String, String> {

    @Override
    public String execute(String s) throws Exception {
        return "Received '"+s+"' in an idempotent unsafe action (only allows POST and PUT requests)";
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }
}
