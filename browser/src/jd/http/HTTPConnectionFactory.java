package jd.http;

import java.net.URL;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class HTTPConnectionFactory {

    public static URLConnectionAdapter createHTTPConnection(final URL url, final HTTPProxy proxy) {
        if (proxy == null) { return new URLConnectionAdapterDirectImpl(url); }
        if (proxy.getType().equals(HTTPProxy.TYPE.NONE)) { return new URLConnectionAdapterDirectImpl(url); }
        if (proxy.getType().equals(HTTPProxy.TYPE.DIRECT)) { return new URLConnectionAdapterDirectImpl(url, proxy); }
        if (proxy.getType().equals(HTTPProxy.TYPE.SOCKS5)) { return new URLConnectionAdapterSocks5Impl(url, proxy); }
        if (proxy.getType().equals(HTTPProxy.TYPE.HTTP)) { return new URLConnectionAdapterHTTPProxyImpl(url, proxy); }
        throw new RuntimeException("unsupported proxy type: " + proxy.getType().name());
    }
}
