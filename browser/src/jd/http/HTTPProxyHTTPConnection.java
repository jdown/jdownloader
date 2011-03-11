package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import jd.nutils.encoding.Encoding;

public class HTTPProxyHTTPConnection extends HTTPConnection {

    public HTTPProxyHTTPConnection(final URL url, final HTTPProxy p) {
        super(url, p);
    }

    /* TODO: proxy to ssl and ssl proxy */
    @Override
    public void connect() throws IOException {
        if (proxy == null || !proxy.getType().equals(HTTPProxy.TYPE.HTTP)) { throw new IOException("HTTPProxyHTTPConnection: invalid HTTP Proxy!"); }
        if (proxy.getPass() != null && proxy.getPass().length() > 0 || proxy.getUser() != null && proxy.getUser().length() > 0) {
            /* add proxy auth */
            final String user = proxy.getUser() == null ? "" : proxy.getUser();
            final String pass = proxy.getPass() == null ? "" : proxy.getPass();
            requestProperties.put("Proxy-Authorization", "Basic " + Encoding.Base64Encode(user + ":" + pass));
        }
        if (isConnected()) { return;/* oder fehler */
        }
        httpSocket = createSocket();
        httpSocket.setSoTimeout(readTimeout);
        httpResponseCode = -1;
        final InetAddress host = InetAddress.getByName(proxy.getHost());
        final long startTime = System.currentTimeMillis();
        try {
            httpSocket.connect(new InetSocketAddress(host, proxy.getPort()), connectTimeout);

        } catch (final IOException e) {
            proxy.setStatus(HTTPProxy.STATUS.OFFLINE);
            throw new ProxyConnectException(e.getMessage());
        }
        requestTime = System.currentTimeMillis() - startTime;
        httpPath = httpURL.toString();
        /* now send Request */
        final StringBuilder sb = new StringBuilder();
        sb.append(httpMethod.name()).append(' ').append(httpPath).append(" HTTP/1.1\r\n");
        for (final String key : requestProperties.keySet()) {
            if (requestProperties.get(key) == null) {
                continue;
            }
            sb.append(key).append(": ").append(requestProperties.get(key)).append("\r\n");
        }
        sb.append("\r\n");
        httpSocket.getOutputStream().write(sb.toString().getBytes("UTF-8"));
        httpSocket.getOutputStream().flush();
        if (httpMethod != RequestMethod.POST) {
            outputClosed = true;
            connectInputStream();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        connectInputStream();
        if (getResponseCode() == 407) {
            proxy.setStatus(HTTPProxy.STATUS.INVALIDAUTH);
            throw new ProxyAuthException();
        }
        return super.getInputStream();
    }
}
