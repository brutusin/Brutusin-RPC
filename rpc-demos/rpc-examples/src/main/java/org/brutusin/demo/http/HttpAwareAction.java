package org.brutusin.demo.http;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.HttpActionSupport;
import org.brutusin.rpc.http.SafeAction;

public class HttpAwareAction extends SafeAction<Void, List<String>> {

    @Override
    public Cacheable<List<String>> execute(Void input) throws Exception {
        HttpServletRequest httpReq = HttpActionSupport.getInstance().getHttpServletRequest();
        List<String> ret = new ArrayList();
        Enumeration<String> headerNames = httpReq.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            ret.add(header + ":" + httpReq.getHeader(header));
        }
        return Cacheable.never(ret);
    }
}
