package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
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
        if (this.proxy == null || !this.proxy.getType().equals(HTTPProxy.TYPE.HTTP)) { throw new IOException("HTTPProxyHTTPConnection: invalid HTTP Proxy!"); }
        if (this.proxy.getPass() != null && this.proxy.getPass().length() > 0 || this.proxy.getUser() != null && this.proxy.getUser().length() > 0) {
            /* add proxy auth */
            final String user = this.proxy.getUser() == null ? "" : this.proxy.getUser();
            final String pass = this.proxy.getPass() == null ? "" : this.proxy.getPass();
            this.requestProperties.put("Proxy-Authorization", "Basic " + Encoding.Base64Encode(user + ":" + pass));
        }
        if (this.isConnected()) { return;/* oder fehler */
        }
        this.httpSocket = this.createSocket();
        this.httpSocket.setSoTimeout(this.readTimeout);
        this.httpResponseCode = -1;
        final InetAddress host = InetAddress.getByName(this.proxy.getHost());
        final long startTime = System.currentTimeMillis();
        try {
            this.httpSocket.connect(new InetSocketAddress(host, this.proxy.getPort()), this.connectTimeout);
        } catch (final ConnectException e) {
            this.proxy.setStatus(HTTPProxy.STATUS.OFFLINE);
            throw new ProxyConnectException(e.getMessage());
        }
        this.requestTime = System.currentTimeMillis() - startTime;
        this.httpPath = this.httpURL.toString();
        /* now send Request */
        final StringBuilder sb = new StringBuilder();
        sb.append(this.httpMethod.name()).append(' ').append(this.httpPath).append(" HTTP/1.1\r\n");
        for (final String key : this.requestProperties.keySet()) {
            if (this.requestProperties.get(key) == null) {
                continue;
            }
            sb.append(key).append(": ").append(this.requestProperties.get(key)).append("\r\n");
        }
        sb.append("\r\n");
        this.httpSocket.getOutputStream().write(sb.toString().getBytes("UTF-8"));
        this.httpSocket.getOutputStream().flush();
        if (this.httpMethod != METHOD.POST) {
            this.outputClosed = true;
            this.connectInputStream();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        this.connect();
        this.connectInputStream();
        if (this.getResponseCode() == 407) {
            this.proxy.setStatus(HTTPProxy.STATUS.INVALIDAUTH);
            throw new ProxyAuthException();
        }
        return super.getInputStream();
    }
}
