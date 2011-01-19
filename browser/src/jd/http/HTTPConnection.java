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

    protected LinkedHashMap<String, String>  requestProperties    = null;
    protected long[]                         ranges;

    protected Request                        request;

    protected String                         customcharset        = null;

    protected Socket                         httpSocket           = null;
    protected URL                            httpURL              = null;
    protected HTTPProxy                      proxy                = null;
    protected String                         httpPath             = null;

    protected METHOD                         httpMethod           = METHOD.GET;
    protected LowerCaseHashMap<List<String>> headers              = null;
    protected int                            httpResponseCode     = -1;
    protected String                         httpResponseMessage  = "";
    protected int                            readTimeout          = 30000;
    protected int                            connectTimeout       = 30000;
    protected long                           requestTime          = -1;
    protected InputStream                    inputStream          = null;
    protected boolean                        inputStreamConnected = false;
    protected String                         httpHeader           = null;
    protected byte[]                         preReadBytes         = null;
    protected boolean                        outputClosed         = false;

    public HTTPConnection(final URL url) {
        this(url, null);
    }

    public HTTPConnection(final URL url, final HTTPProxy p) {
        httpURL = url;
        proxy = p;
        requestProperties = new LinkedHashMap<String, String>();
        headers = new LowerCaseHashMap<List<String>>();
    }

    public void connect() throws IOException {
        if (isConnected()) { return;/* oder fehler */
        }
        if (httpURL.getProtocol().startsWith("https")) {
            final SocketFactory socketFactory = SSLSocketFactory.getDefault();
            httpSocket = socketFactory.createSocket();
        } else {
            httpSocket = createSocket();
        }
        httpSocket.setSoTimeout(readTimeout);
        httpResponseCode = -1;
        /* host auflösen nur wenn kein proxy, ansonsten über proxy */
        InetAddress host = null;
        try {
            host = InetAddress.getByName(httpURL.getHost());
        } catch (final UnknownHostException e) {
            System.out.println("Unknown Host:" + httpURL);
            throw e;
        }
        int port = httpURL.getPort();
        if (port == -1) {
            port = httpURL.getDefaultPort();
        }
        final long startTime = System.currentTimeMillis();
        if (proxy != null && !proxy.getType().equals(HTTPProxy.TYPE.DIRECT)) {
            throw new RuntimeException("Invalid Direct Proxy");
        } else {
            httpSocket.connect(new InetSocketAddress(host, port), connectTimeout);
        }
        requestTime = System.currentTimeMillis() - startTime;
        httpPath = new Regex(httpURL.toString(), "https?://.*?(/.+)").getMatch(0);
        if (httpPath == null) {
            httpPath = "/";
        }
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
        if (httpMethod != METHOD.POST) {
            outputClosed = true;
            connectInputStream();
        }
    }

    protected synchronized void connectInputStream() throws IOException {
        if (inputStreamConnected) { return; }
        inputStreamConnected = true;
        /* first read http header */
        ByteBuffer header = HTTPConnectionUtils.readheader(httpSocket.getInputStream(), true);
        byte[] bytes = new byte[header.limit()];
        header.get(bytes);
        httpHeader = new String(bytes, "ISO-8859-1").trim();
        /* parse response code/message */
        if (httpHeader.startsWith("HTTP")) {
            final String code = new Regex(httpHeader, "HTTP.*? (\\d+)").getMatch(0);
            if (code != null) {
                httpResponseCode = Integer.parseInt(code);
            }
            httpResponseMessage = new Regex(httpHeader, "HTTP.*? \\d+ (.+)").getMatch(0);
            if (httpResponseMessage == null) {
                httpResponseMessage = "";
            }
        } else {
            preReadBytes = bytes;
            httpHeader = "unknown HTTP response";
            httpResponseCode = 200;
            httpResponseMessage = "unknown HTTP response";
            inputStream = httpSocket.getInputStream();
            return;
        }
        /* read rest of http headers */
        header = HTTPConnectionUtils.readheader(httpSocket.getInputStream(), false);
        bytes = new byte[header.limit()];
        header.get(bytes);
        String temp = new String(bytes, "UTF-8");
        /* split header into single strings, use RN or N(buggy fucking non rfc) */
        String[] headerStrings = temp.split("(\r\n)|(\n)");
        temp = null;
        for (final String line : headerStrings) {
            String key = null;
            String value = null;
            if (line.indexOf(": ") > 0) {
                key = line.substring(0, line.indexOf(": "));
                value = line.substring(line.indexOf(": ") + 2);
            } else {
                key = null;
                value = line;
            }
            List<String> list = headers.get(key);
            if (list == null) {
                list = new ArrayList<String>();
                headers.put(key, list);
            }
            list.add(value);
        }
        headerStrings = null;
        final List<String> chunked = headers.get("Transfer-Encoding");
        if (chunked != null && chunked.size() > 0 && "chunked".equalsIgnoreCase(chunked.get(0))) {
            /* TODO: write own chunkedinputstream */
            inputStream = new ChunkedInputStream(httpSocket.getInputStream(), new MyHttpClient(), new MessageHeader());
        } else {
            inputStream = httpSocket.getInputStream();
        }
    }

    public Socket createSocket() throws IOException {
        return new Socket();
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                httpSocket.close();
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String getCharset() {
        int i;
        if (customcharset != null) { return customcharset; }
        return getContentType() != null && (i = getContentType().toLowerCase().indexOf("charset=")) > 0 ? getContentType().substring(i + 8).trim() : null;
    }

    public int getContentLength() {
        return (int) getLongContentLength();
    }

    public String getContentType() {
        final String type = getHeaderField("Content-Type");
        if (type == null) { return "unknown"; }
        return type;
    }

    public String getHeaderField(final String string) {
        final List<String> ret = headers.get(string);
        if (ret == null || ret.size() == 0) { return null; }
        return ret.get(0);
    }

    public Map<String, List<String>> getHeaderFields() {
        return headers;
    }

    public InputStream getInputStream() throws IOException {
        connect();
        connectInputStream();
        if (getResponseCode() >= 200 && getResponseCode() <= 400 || getResponseCode() == 404) {
            return inputStream;
        } else {
            throw new IOException(getResponseCode() + " " + getResponseMessage());
        }
    }

    public long getLongContentLength() {
        final String length = getHeaderField("Content-Length");
        if (length == null) { return -1; }
        return Long.parseLong(length);
    }

    public OutputStream getOutputStream() throws IOException {
        connect();
        if (outputClosed) { throw new IOException("OutputStream no longer available"); }
        return httpSocket.getOutputStream();
    }

    public long[] getRange() {
        String range;
        if (ranges != null) { return ranges; }
        if ((range = getHeaderField("Content-Range")) == null) { return null; }
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
        return request;
    }

    public METHOD getRequestMethod() {
        return httpMethod;
    }

    public Map<String, String> getRequestProperties() {
        return requestProperties;
    }

    public String getRequestProperty(final String string) {
        return requestProperties.get(string);
    }

    public long getRequestTime() {
        return requestTime;
    }

    public int getResponseCode() {
        return httpResponseCode;
    }

    public String getResponseMessage() {
        return httpResponseMessage;
    }

    public URL getURL() {
        return httpURL;
    }

    public boolean isConnected() {
        if (httpSocket != null && httpSocket.isConnected()) { return true; }
        return false;
    }

    public boolean isContentDisposition() {
        return getHeaderField("Content-Disposition") != null;
    }

    public boolean isOK() {
        if (getResponseCode() > -2 && getResponseCode() < 400) { return true; }
        return false;
    }

    public void postDataSend() throws IOException {
        if (!isConnected()) { return; }
        /* disable outputStream now */
        outputClosed = true;
        connectInputStream();
    }

    public byte[] preReadBytes() {
        final byte[] ret = preReadBytes;
        preReadBytes = null;
        return ret;
    }

    public void setCharset(final String Charset) {
        customcharset = Charset;
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
        httpMethod = method;
    }

    public void setRequestProperty(final String key, final String value) {
        requestProperties.put(key, value);
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();
        sb.append("-->").append(getURL()).append("\r\n");

        sb.append("----------------Request------------------\r\n");

        sb.append(httpMethod.toString()).append(' ').append(getURL().getPath()).append((getURL().getQuery() != null ? "?" + getURL().getQuery() : "")).append(" HTTP/1.1\r\n");

        for (final String key : getRequestProperties().keySet()) {
            final String v = getRequestProperties().get(key);
            if (v == null) {
                continue;
            }
            sb.append(key);
            sb.append(new char[] { ':', ' ' });
            sb.append(v);
            sb.append(new char[] { '\r', '\n' });
        }
        sb.append(new char[] { '\r', '\n' });

        if (getRequest() != null) {
            if (getRequest() instanceof PostRequest) {
                if (((PostRequest) getRequest()).getPostDataString() != null) {
                    sb.append(((PostRequest) getRequest()).getPostDataString());
                }

            } else if (getRequest() instanceof PostFormDataRequest) {
                if (((PostFormDataRequest) getRequest()).getPostDataString() != null) {
                    sb.append(((PostFormDataRequest) getRequest()).getPostDataString());
                }
                sb.append(new char[] { '\r', '\n' });
            }

        }

        sb.append("----------------Response------------------\r\n");
        sb.append(httpHeader).append("\r\n");
        for (final Entry<String, List<String>> next : getHeaderFields().entrySet()) {
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
