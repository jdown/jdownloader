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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import jd.http.URLConnectionAdapter.METHOD;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;

public abstract class Request {
    // public static int MAX_REDIRECTS = 30;

    public static String getCookieString(final Cookies cookies) {
        if (cookies == null) { return null; }

        final StringBuilder buffer = new StringBuilder();
        boolean first = true;
        final LinkedList<Cookie> cookies2 = new LinkedList<Cookie>(cookies.getCookies());
        for (final Cookie cookie : cookies2) {
            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                buffer.append("; ");
            }
            buffer.append(cookie.getKey());
            buffer.append("=");
            buffer.append(cookie.getValue());
        }
        return buffer.toString();
    }

    /**
     * Gibt eine Hashmap mit allen key:value pairs im query zurück
     * 
     * @param query
     *            kann ein reines query ein (&key=value) oder eine url mit query
     * @return
     * @throws MalformedURLException
     */

    public static LinkedHashMap<String, String> parseQuery(String query) throws MalformedURLException {
        if (query == null) { return null; }
        final LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
        if (query.toLowerCase().trim().startsWith("http")) {
            query = new URL(query).getQuery();
        }

        if (query == null) { return ret; }
        final String[][] split = new Regex(query.trim(), "&?(.*?)=(.*?)($|&(?=.*?=.+))").getMatches();
        if (split != null) {
            final int splitLength = split.length;
            for (int i = 0; i < splitLength; i++) {
                ret.put(split[i][0], split[i][1]);
            }
        }
        return ret;
    }

    public static byte[] read(final URLConnectionAdapter con) throws IOException {
        BufferedInputStream is = null;
        if (con.getInputStream() != null) {
            if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
                is = new BufferedInputStream(new GZIPInputStream(con.getInputStream()));
            } else if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("deflate") && Application.getJavaVersion() >= 16000000) {
                is = new BufferedInputStream(new java.util.zip.DeflaterInputStream(con.getInputStream()));
            } else {
                is = new BufferedInputStream(con.getInputStream());
            }
        }
        if (is == null) {
            // TODO: check if we have t close con here
            return null;
        }
        ByteArrayOutputStream tmpOut;
        final long contentLength = con.getContentLength();
        if (contentLength != -1) {
            final int length = contentLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) contentLength;
            tmpOut = new ByteArrayOutputStream(length);
        } else {
            tmpOut = new ByteArrayOutputStream(16384);
        }
        final byte[] preRead = con.preReadBytes();
        if (preRead != null) {
            tmpOut.write(preRead);
        }
        /* added "Corrupt GZIP trailer" for CamWinsCom */
        try {
            final byte[] b = new byte[1024];
            int len;
            while ((len = is.read(b)) != -1) {
                tmpOut.write(b, 0, len);
            }
        } catch (final EOFException e) {
            Log.L.log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
        } catch (final IOException e) {
            if (e.toString().contains("end of ZLIB") || e.toString().contains("Premature") || e.toString().contains("Corrupt GZIP trailer")) {
                Log.L.log(java.util.logging.Level.SEVERE, "Try workaround for ", e);
            } else {
                throw e;
            }
        } finally {
            try {
                is.close();
            } catch (final Exception e) {
            }
            try {
                tmpOut.close();
            } catch (final Exception e) {
            }
            try {
                /* disconnect connection */
                con.disconnect();
            } catch (final Exception e) {
            }
        }
        return tmpOut.toByteArray();
    }

    /*
     * default timeouts, because 0 is infinite and BAD, if we need 0 then we
     * have to set it manually
     */
    private int connectTimeout = 30000;

    private int readTimeout = 60000;
    private Cookies cookies = null;
    private RequestHeader headers;

    private String htmlCode;
    protected URLConnectionAdapter httpConnection;

    private long readTime = -1;
    private boolean requested = false;
    private HTTPProxy proxy;
    private URL orgURL;
    private String customCharset = null;

    private byte[] byteArray;

    private BufferedImage image;

    public Request(final String url) throws MalformedURLException {
        this.orgURL = new URL(Browser.correctURL(url));
        this.initDefaultHeader();
    }

    public Request(final URLConnectionAdapter con) {
        this.httpConnection = con;
        this.collectCookiesFromConnection();
    }

    public Request cloneRequest() {
        return null;
    }

    private void collectCookiesFromConnection() {
        final List<String> cookieHeaders = this.httpConnection.getHeaderFields("Set-Cookie");
        final String date = this.httpConnection.getHeaderField("Date");
        if (cookieHeaders == null) { return; }
        if (this.cookies == null) {
            this.cookies = new Cookies();
        }

        final String host = Browser.getHost(this.httpConnection.getURL());

        for (int i = 0; i < cookieHeaders.size(); i++) {
            final String header = cookieHeaders.get(i);
            this.cookies.add(Cookies.parseCookies(header, host, date));
        }
    }

    /**
     * DO NEVER call this method directly... use browser.connect
     */
    protected Request connect() throws IOException {
        this.requested = true;
        this.openConnection();
        this.postRequest(this.httpConnection);
        this.httpConnection.connect();
        try {
            this.collectCookiesFromConnection();
        } catch (final NullPointerException e) {
            throw new IOException("Malformed url?", e);
        }
        return this;
    }

    public boolean containsHTML(final String html) throws CharacterCodingException {
        return this.getHtmlCode() == null ? false : this.getHtmlCode().contains(html);
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public long getContentLength() {
        return this.httpConnection == null ? -1 : this.httpConnection.getLongContentLength();
    }

    public Cookies getCookies() {
        if (this.cookies == null) {
            this.cookies = new Cookies();
        }
        return this.cookies;
    }

    public String getCookieString() {
        return Request.getCookieString(this.cookies);
    }

    public RequestHeader getHeaders() {
        return this.headers;
    }

    // public static boolean isExpired(String cookie) {
    // if (cookie == null) return false;
    //
    // try {
    // return (new Date().compareTo()) > 0;
    // } catch (Exception e) {
    // return false;
    // }
    // }

    public String getHtmlCode() throws CharacterCodingException {
        final String ct = this.httpConnection.getContentType();
        /* check for image content type */
        if (ct != null && Pattern.compile("images?/\\w*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(ct).matches()) { throw new IllegalStateException("Content-Type: " + ct); }
        if (this.htmlCode == null && this.byteArray != null) {
            /* use custom charset or charset from httpconnection */
            final String useCS = this.customCharset == null ? this.httpConnection.getCharset() : this.customCharset;
            try {
                try {
                    try {
                        if (useCS != null) {
                            /* try to use wanted charset */
                            this.htmlCode = new String(this.byteArray, useCS.toUpperCase());
                            this.byteArray = null;
                            return this.htmlCode;
                        }
                    } catch (final Exception e) {
                    }
                    this.htmlCode = new String(this.byteArray, "ISO-8859-1");
                    this.byteArray = null;
                    return this.htmlCode;
                } catch (final Exception e) {
                    Log.getLogger().severe("could neither charset: " + useCS + " nor default charset");
                    /* fallback to default charset in error case */
                    this.htmlCode = new String(this.byteArray);
                    this.byteArray = null;
                    return this.htmlCode;
                }
            } catch (final Exception e) {
                /* in case of error we do not reset byteArray */
            }
        }
        return this.htmlCode;
    }

    public URLConnectionAdapter getHttpConnection() {
        return this.httpConnection;
    }

    public String getLocation() {
        if (this.httpConnection == null) { return null; }

        String red = this.httpConnection.getHeaderField("Location");
        final String encoding = this.httpConnection.getHeaderField("Content-Type");
        if (red == null || red.length() == 0) { return null; }
        if (encoding != null && encoding.contains("UTF-8")) {
            red = Encoding.UTF8Decode(red, "ISO-8859-1");
        }
        try {
            new URL(red);
        } catch (final Exception e) {
            String path = this.getHttpConnection().getURL().getFile();
            if (!path.endsWith("/")) {
                final int lastSlash = path.lastIndexOf("/");
                if (lastSlash > 0) {
                    path = path.substring(0, path.lastIndexOf("/"));
                } else {
                    path = "";
                }
            }
            final int port = this.getHttpConnection().getURL().getPort();
            final int defaultport = this.getHttpConnection().getURL().getDefaultPort();
            String proto = "http://";
            if (this.getHttpConnection().getURL().toString().startsWith("https")) {
                proto = "https://";
            }
            String addPort = "";
            if (defaultport > 0 && port > 0 && defaultport != port) {
                addPort = ":" + port;
            }
            red = proto + this.getHttpConnection().getURL().getHost() + addPort + (red.charAt(0) == '/' ? red : path + "/" + red);
        }
        return Browser.correctURL(Encoding.urlEncode_light(red));

    }

    public HTTPProxy getProxy() {
        return this.proxy;
    }

    public long getReadTime() {
        return this.readTime;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public long getRequestTime() {
        return this.httpConnection == null ? -1 : this.httpConnection.getRequestTime();
    }

    /**
     * Returns the loaded bytes
     * 
     * @return
     */
    public byte[] getResponseBytes() {
        if (this.byteArray != null) { return this.byteArray.clone(); }
        return null;
    }

    public String getResponseHeader(final String key) {
        return this.httpConnection == null ? null : this.httpConnection.getHeaderField(key);
    }

    public Map<String, List<String>> getResponseHeaders() {
        return this.httpConnection == null ? null : this.httpConnection.getHeaderFields();
    }

    /**
     * tries to generate an image out of the loaded bytes
     * 
     * @return
     */
    public Image getResponseImage() {
        final String ct = this.httpConnection.getContentType();
        /* check for image content */
        if (ct != null && !Pattern.compile("images?/\\w*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(ct).matches()) { throw new IllegalStateException("Content-Type: " + ct); }
        // TODO..this is just quick and dirty.. may result in memory leaks
        if (this.image == null && this.byteArray != null) {
            final InputStream fake = new ByteArrayInputStream(this.byteArray);
            try {
                this.image = ImageIO.read(fake);
                // BasicWindow.showImage(image);
                // its an immage;
                this.byteArray = null;
            } catch (final Exception e) {
                Log.exception(e);
            }
        }
        return this.image;
    }

    /**
     * Will replace #getHtmlCode() with next release
     */
    public String getResponseText() throws CharacterCodingException {
        return this.getHtmlCode();
    }

    public URL getUrl() {
        return this.orgURL;
    }

    private boolean hasCookies() {
        return this.cookies != null && !this.cookies.isEmpty();
    }

    private void initDefaultHeader() {
        this.headers = new RequestHeader();
        this.headers.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10");
        this.headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        this.headers.put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");

        if (Application.getJavaVersion() >= 1.6) {
            /* deflate only java >=1.6 */
            this.headers.put("Accept-Encoding", "gzip,deflate");
        } else {
            this.headers.put("Accept-Encoding", "gzip");
        }
        this.headers.put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

        this.headers.put("Cache-Control", "no-cache");
        this.headers.put("Pragma", "no-cache");
        this.headers.put("Connection", "close");
    }

    public boolean isRequested() {
        return this.requested;
    }

    public String load() throws IOException {
        this.requestConnection();
        return this.getHtmlCode();
    }

    private void openConnection() throws IOException {

        this.httpConnection = HTTPConnectionFactory.createHTTPConnection(this.orgURL, this.proxy);
        this.httpConnection.setRequest(this);
        this.httpConnection.setReadTimeout(this.readTimeout);
        this.httpConnection.setConnectTimeout(this.connectTimeout);

        if (this.headers != null) {
            final int headersSize = this.headers.size();
            for (int i = 0; i < headersSize; i++) {
                this.httpConnection.setRequestProperty(this.headers.getKey(i), this.headers.getValue(i));
            }
        }
        this.preRequest(this.httpConnection);
        if (this.hasCookies()) {
            this.httpConnection.setRequestProperty("Cookie", this.getCookieString());
        }
    }

    public abstract long postRequest(URLConnectionAdapter httpConnection) throws IOException;

    abstract public void preRequest(URLConnectionAdapter httpConnection) throws IOException;

    public String printHeaders() {
        return this.httpConnection.toString();
    }

    public Request read() throws IOException {
        final long tima = System.currentTimeMillis();
        this.httpConnection.setCharset(this.customCharset);
        this.byteArray = Request.read(this.httpConnection);
        this.readTime = System.currentTimeMillis() - tima;
        return this;
    }

    private void requestConnection() throws IOException {
        this.connect();
        this.read();
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    // public void setProxy(String ip, String port) throws
    // NumberFormatException, MalformedURLException {
    // proxyip = ip;
    // proxyport = port;
    // if (ip == null || port == null) return;
    // url = new URL("http", proxyip, Integer.parseInt(proxyport),
    // url.toString());
    //
    // }

    public void setCookies(final Cookies cookies) {
        this.cookies = cookies;
    }

    public void setCustomCharset(final String charset) {
        this.customCharset = charset;
    }

    public void setHtmlCode(final String htmlCode) {
        // set bytebuffer to null... user works with htmlcode
        this.byteArray = null;
        this.htmlCode = htmlCode;
    }

    public void setProxy(final HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Request toHeadRequest() throws MalformedURLException {
        final Request ret = new Request(this.getUrl() + "") {
            // @Override
            @Override
            public long postRequest(final URLConnectionAdapter httpConnection) throws IOException {
                return 0;
            }

            // @Override
            @Override
            public void preRequest(final URLConnectionAdapter httpConnection) throws IOException {
                httpConnection.setRequestMethod(METHOD.HEAD);
            }
        };
        ret.connectTimeout = this.connectTimeout;
        ret.cookies = new Cookies(this.getCookies());
        ret.headers = this.getHeaders().clone();
        ret.setProxy(this.proxy);
        ret.readTime = this.readTimeout;
        ret.httpConnection = this.httpConnection;
        return ret;
    }

    // @Override
    @Override
    public String toString() {
        if (!this.requested) { return "Request not sent yet"; }
        try {
            this.getHtmlCode();
            if (this.htmlCode == null || this.htmlCode.length() == 0) {
                if (this.getLocation() != null) { return "Not HTML Code. Redirect to: " + this.getLocation(); }
                return "No htmlCode read";
            }
        } catch (final Exception e) {
            return "NOTEXT: " + e.getMessage();
        }
        return this.htmlCode;
    }

}
