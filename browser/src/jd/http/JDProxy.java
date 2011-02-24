package jd.http;

import java.net.Proxy;
import java.net.SocketAddress;

/*dummy class for backwards comp. to 09581*/
@Deprecated
public class JDProxy extends Proxy {

    public JDProxy(final Type type, final SocketAddress sa) {
        super(type, sa);
        // TODO Auto-generated constructor stub
    }

}
