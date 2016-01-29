package org.brutusin.demo;

import org.brutusin.rpc.http.UnsafeAction;

public class UnSafeAction extends UnsafeAction<Void, String> {

    @Override
    public String execute(Void input) throws Exception {
        return "You a have successfully executed an action that only allows POST requests (since it is defined as unsafe)!";
    }
}
