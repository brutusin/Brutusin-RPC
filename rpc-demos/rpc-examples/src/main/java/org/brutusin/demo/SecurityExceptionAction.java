package org.brutusin.demo;

import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.SafeAction;

public class SecurityExceptionAction extends SafeAction<Void, Void> {

    @Override
    public Cacheable<Void> execute(Void input) throws Exception {
        String name;
        if (HttpActionSupport.getInstance().getUserPrincipal() == null) {
            name = null;
        } else {
            name = HttpActionSupport.getInstance().getUserPrincipal().getName();
        }
        throw new SecurityException("Forbidden access. User principal: '" + name + "'");
    }
}
