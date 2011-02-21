//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import sun.net.www.MessageHeader;
import sun.net.www.http.ChunkedInputStream;

public class HTTPConnection implements URLConnectionAdapter {

    protected LinkedHashMap<String, String> requestProperties = null;
    protected long[] ranges;

    protected Request request;

    protected String customcharset = null;

    protected Socket httpSocket = null;
    protected URL httpURL = null;
    protected HTTPProxy proxy = null;
    protected String httpPath = null;

    protected METHOD httpMethod = METHOD.GET;
    protected LowerCaseHashMap<List<String>> headers = null;
    protected int httpResponseCode = -1;
    protected String httpResponseMessage = "";
    protected int readTimeout = 30000;
    protected int connectTimeout = 30000;
    protected long requestTime = -1;
    protected InputStream inputStream = null;
    protected boolean inputStreamConnected = false;
    protected String httpHeader = null;
    protected byte[] preReadBytes = null;
    protected boolean outputClosed = false;

    public HTTPConnection(final URL url) {
        this(url, null);
    }

    public HTTPConnection(final URL url, final HTTPProxy p) {
        this.httpURL = url;
        this.proxy = p;
        this.requestProperties = new LinkedHashMap<String, String>();
        this.headers = new LowerCaseHashMap<List<String>>();
    }

    public void connect() throws IOException {
        if (this.isConnected()) { return;/* oder fehler */
        }
        if (this.httpURL.getProtocol().startsWith("https")) {
            final SocketFactory socketFactory = SSLSocketFactory.getDefault();
            this.httpSocket = socketFactory.createSocket();
        } else {
            this.httpSocket = this.createSocket();
        }
        this.httpSocket.setSoTimeout(this.readTimeout);
        this.httpResponseCode = -1;
        /* host auflösen nur wenn kein proxy, ansonsten über proxy */
        InetAddress host = null;
        try {
            host = InetAddress.getByName(this.httpURL.getHost());
        } catch (final UnknownHostException e) {
            System.out.println("Unknown Host:" + this.httpURL);
            throw e;
        }
        int port = this.httpURL.getPort();
        if (port == -1) {
            port = this.httpURL.getDefaultPort();
        }
        final long startTime = System.currentTimeMillis();
        if (this.proxy != null && !this.proxy.getType().equals(HTTPProxy.TYPE.DIRECT)) {
            throw new RuntimeException("Invalid Direct Proxy");
        } else {
            if (this.proxy != null) {
                /* bind socket to given interface */
                try {
                    if (this.proxy.getLocalIP() == null) { throw new IOException("Invalid localIP"); }
                    this.httpSocket.bind(new InetSocketAddress(this.proxy.getLocalIP(), 0));
                } catch (final IOException e) {
                    this.proxy.setStatus(HTTPProxy.STATUS.OFFLINE);
                    throw new ProxyConnectException(e.getMessage());
                }
            }
            this.httpSocket.connect(new InetSocketAddress(host, port), this.connectTimeout);
        }
        this.requestTime = System.currentTimeMillis() - startTime;
        this.httpPath = new Regex(this.httpURL.toString(), "https?://.*?(/.+)").getMatch(0);
        if (this.httpPath == null) {
            this.httpPath = "/";
        }
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

    protected synchronized void connectInputStream() throws IOException {
        if (this.inputStreamConnected) { return; }
        this.inputStreamConnected = true;
        /* first read http header */
        ByteBuffer header = HTTPConnectionUtils.readheader(this.httpSocket.getInputStream(), true);
        byte[] bytes = new byte[header.limit()];
        header.get(bytes);
        this.httpHeader = new String(bytes, "ISO-8859-1").trim();
        /* parse response code/message */
        if (this.httpHeader.startsWith("HTTP")) {
            final String code = new Regex(this.httpHeader, "HTTP.*? (\\d+)").getMatch(0);
            if (code != null) {
                this.httpResponseCode = Integer.parseInt(code);
            }
            this.httpResponseMessage = new Regex(this.httpHeader, "HTTP.*? \\d+ (.+)").getMatch(0);
            if (this.httpResponseMessage == null) {
                this.httpResponseMessage = "";
            }
        } else {
            this.preReadBytes = bytes;
            this.httpHeader = "unknown HTTP response";
            this.httpResponseCode = 200;
            this.httpResponseMessage = "unknown HTTP response";
            this.inputStream = this.httpSocket.getInputStream();
            return;
        }
        /* read rest of http headers */
        header = HTTPConnectionUtils.readheader(this.httpSocket.getInputStream(), false);
        bytes = new byte[header.limit()];
        header.get(bytes);
        String temp = new String(bytes, "UTF-8");
        /* split header into single strings, use RN or N(buggy fucking non rfc) */
        String[] headerStrings = temp.split("(\r\n)|(\n)");
        temp = null;
        for (final String line : headerStrings) {
            String key = null;
            String value = null;
            int index = 0;
            if ((index = line.indexOf(": ")) > 0) {
                key = line.substring(0, index);
                value = line.substring(index + 2);
            } else if ((index = line.indexOf(":")) > 0) {
                /* buggy servers that don't have :space ARG */
                key = line.substring(0, index);
                value = line.substring(index + 1);
            } else {
                key = null;
                value = line;
            }
            List<String> list = this.headers.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                this.headers.put(key, list);
            }
            list.add(value);
        }
        headerStrings = null;
        final List<String> chunked = this.headers.get("Transfer-Encoding");
        if (chunked != null && chunked.size() > 0 && "chunked".equalsIgnoreCase(chunked.get(0))) {
            /* TODO: write own chunkedinputstream */
            this.inputStream = new ChunkedInputStream(this.httpSocket.getInputStream(), new MyHttpClient(), new MessageHeader());
        } else {
            this.inputStream = this.httpSocket.getInputStream();
        }
    }

    public Socket createSocket() throws IOException {
        return new Socket();
    }

    public void disconnect() {
        if (this.isConnected()) {
            try {
                this.httpSocket.close();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String getCharset() {
        int i;
        if (this.customcharset != null) { return this.customcharset; }
        return this.getContentType() != null && (i = this.getContentType().toLowerCase().indexOf("charset=")) > 0 ? this.getContentType().substring(i + 8).trim() : null;
    }

    public int getContentLength() {
        return (int) this.getLongContentLength();
    }

    public String getContentType() {
        final String type = this.getHeaderField("Content-Type");
        if (type == null) { return "unknown"; }
        return type;
    }

    public String getHeaderField(final String string) {
        final List<String> ret = this.headers.get(string);
        if (ret == null || ret.size() == 0) { return null; }
        return ret.get(0);
    }

    public Map<String, List<String>> getHeaderFields() {
        return this.headers;
    }

    public List<String> getHeaderFields(final String string) {
        final List<String> ret = this.headers.get(string);
        if (ret == null || ret.size() == 0) { return null; }
        return ret;
    }

    public InputStream getInputStream() throws IOException {
        this.connect();
        this.connectInputStream();
        final int code = this.getResponseCode();
        if (code >= 200 && code <= 400 || code == 404 || code == 403) {
            return this.inputStream;
        } else {
            throw new IOException(this.getResponseCode() + " " + this.getResponseMessage());
        }
    }

    public long getLongContentLength() {
        final String length = this.getHeaderField("Content-Length");
        if (length == null) { return -1; }
        return Long.parseLong(length);
    }

    public OutputStream getOutputStream() throws IOException {
        this.connect();
        if (this.outputClosed) { throw new IOException("OutputStream no longer available"); }
        return this.httpSocket.getOutputStream();
    }

    public long[] getRange() {
        String range;
        if (this.ranges != null) { return this.ranges; }
        if ((range = this.getHeaderField("Content-Range")) == null) { return null; }
        // bytes 174239-735270911/735270912
        final String[] ranges = new Regex(range, ".*?(\\d+).*?-.*?(\\d+).*?/.*?(\\d+)").getRow(0);
        if (ranges == null) {
            System.err.print(this + "");
            return null;
        }
        this.ranges = new long[] { Long.parseLong(ranges[0]), Long.parseLong(ranges[1]), Long.parseLong(ranges[2]) };
        return this.ranges;
    }

    public Request getRequest() {
        return this.request;
    }

    public METHOD getRequestMethod() {
        return this.httpMethod;
    }

    public Map<String, String> getRequestProperties() {
        return this.requestProperties;
    }

    public String getRequestProperty(final String string) {
        return this.requestProperties.get(string);
    }

    public long getRequestTime() {
        return this.requestTime;
    }

    public int getResponseCode() {
        return this.httpResponseCode;
    }

    public String getResponseMessage() {
        return this.httpResponseMessage;
    }

    public URL getURL() {
        return this.httpURL;
    }

    public boolean isConnected() {
        if (this.httpSocket != null && this.httpSocket.isConnected()) { return true; }
        return false;
    }

    public boolean isContentDisposition() {
        return this.getHeaderField("Content-Disposition") != null;
    }

    public boolean isOK() {
        if (this.getResponseCode() > -2 && this.getResponseCode() < 400) { return true; }
        return false;
    }

    public void postDataSend() throws IOException {
        if (!this.isConnected()) { return; }
        /* disable outputStream now */
        this.outputClosed = true;
        this.connectInputStream();
    }

    public byte[] preReadBytes() {
        final byte[] ret = this.preReadBytes;
        this.preReadBytes = null;
        return ret;
    }

    public void setCharset(final String Charset) {
        this.customcharset = Charset;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setRequest(final Request request) {
        this.request = request;

    }

    public void setRequestMethod(final METHOD method) {
        this.httpMethod = method;
    }

    public void setRequestProperty(final String key, final String value) {
        this.requestProperties.put(key, value);
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();
        sb.append("-->").append(this.getURL()).append("\r\n");

        sb.append("----------------Request------------------\r\n");

        sb.append(this.httpMethod.toString()).append(' ').append(this.getURL().getPath()).append((this.getURL().getQuery() != null ? "?" + this.getURL().getQuery() : "")).append(" HTTP/1.1\r\n");

        for (final String key : this.getRequestProperties().keySet()) {
            final String v = this.getRequestProperties().get(key);
            if (v == null) {
                continue;
            }
            sb.append(key);
            sb.append(new char[] { ':', ' ' });
            sb.append(v);
            sb.append(new char[] { '\r', '\n' });
        }
        sb.append(new char[] { '\r', '\n' });

        if (this.getRequest() != null) {
            if (this.getRequest() instanceof PostRequest) {
                if (((PostRequest) this.getRequest()).getPostDataString() != null) {
                    sb.append(((PostRequest) this.getRequest()).getPostDataString());
                }

            } else if (this.getRequest() instanceof PostFormDataRequest) {
                if (((PostFormDataRequest) this.getRequest()).getPostDataString() != null) {
                    sb.append(((PostFormDataRequest) this.getRequest()).getPostDataString());
                }
                sb.append(new char[] { '\r', '\n' });
            }

        }

        sb.append("----------------Response------------------\r\n");
        sb.append(this.httpHeader).append("\r\n");
        for (final Entry<String, List<String>> next : this.getHeaderFields().entrySet()) {
            // Achtung cookie reihenfolge ist wichtig!!!
            for (int i = next.getValue().size() - 1; i >= 0; i--) {
                if (next.getKey() == null) {
                    sb.append(next.getValue().get(i));
                    sb.append(new char[] { '\r', '\n' });
                } else {
                    sb.append(next.getKey());
                    sb.append(new char[] { ':', ' ' });
                    sb.append(next.getValue().get(i));
                    sb.append(new char[] { '\r', '\n' });
                }
            }
        }
        sb.append(new char[] { '\r', '\n' });

        return sb.toString();

    }
}
