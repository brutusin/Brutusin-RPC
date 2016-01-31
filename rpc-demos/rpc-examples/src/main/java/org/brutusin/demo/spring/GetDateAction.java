package org.brutusin.demo.spring;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.brutusin.rpc.http.Cacheable;
import org.brutusin.rpc.http.SafeAction;

public class GetDateAction extends SafeAction<Void, String> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat();

    @Override
    public Cacheable<String> execute(Void input) throws Exception {
        return Cacheable.never(dateFormat.format(new Date()));
    }

    public void setDatePattern(String pattern) {
        dateFormat = new SimpleDateFormat(pattern);
    }
}
