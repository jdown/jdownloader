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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import jd.http.requests.FormData;
import jd.http.requests.GetRequest;
import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;
import jd.http.requests.RequestVariable;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;

import org.appwork.utils.Regex;

public class Browser {

    private static final HashMap<String, Cookies> COOKIES         = new HashMap<String, Cookies>();
    private static HTTPProxy                      GLOBAL_PROXY    = null;
    private static Logger                         LOGGER          = null;

    // added proxy map to find proxy passwords.

    private static HashMap<String, Integer>       REQUEST_INTERVAL_LIMIT_MAP;

    private static HashMap<String, Long>          REQUESTTIME_MAP;

    private static int                            TIMEOUT_CONNECT = 30000;

    private static int                            TIMEOUT_READ    = 30000;

    private static boolean                        VERBOSE         = false;

    /**
     * Returns a corrected url, where multiple / and ../. are removed
     * 
     * @param url
     * @return
     */
    public static String correctURL(String url) {
        if (url == null) { return url; }
        /* check if we need to correct url */
        int begin = url.indexOf("://");
        if (begin > 0 && url.indexOf("/", begin + 3) < 0) {
            /* check for missing first / in url */
            url = url + "/";
        }
        if (begin > 0 && (!url.substring(begin + 3).contains("//") || !url.contains("./"))) { return url; }
        String ret = url;
        String end = null;
        String tmp = null;
        boolean endisslash = false;
        if (url.startsWith("http://")) {
            begin = 8;
        } else if (url.startsWith("https://")) {
            begin = 9;
        } else {
            begin = 0;
        }
        final int first = url.indexOf("/", begin);
        if (first < 0) { return ret; }
        ret = url.substring(0, first);
        final int endp = url.indexOf("?", first);
        if (endp > 0) {
            end = url.substring(endp);
            tmp = url.substring(first, endp);
        } else {
            tmp = url.substring(first);
        }
        /* is the end of url a / */
        endisslash = tmp.endsWith("/");

        /* filter multiple / */
        tmp = tmp.replaceAll("/+", "/");

        /* filter .. and . */
        final String parts[] = tmp.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase(".")) {
                parts[i] = "";
            } else if (parts[i].equalsIgnoreCase("..")) {
                if (i > 0) {
                    int j = i - 1;
                    while (true && j > 0) {
                        if (parts[j].length() > 0) {
                            parts[j] = "";
                            break;
                        }
                        j--;
                    }
                }
                parts[i] = "";
            }
        }
        tmp = "";
        for (final String part : parts) {
            if (part.length() > 0) {
                tmp = tmp + "/" + part;
            }
        }
        if (endisslash) {
            tmp = tmp + "/";
        }
        return ret + tmp + (end != null ? end : "");
    }

    /**
     * Downloads url to file.
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(final File file, final String url) throws IOException {
        new Browser().getDownload(file, url);
    }

    /**
     * Lädt über eine URLConnection eine Datei herunter. Zieldatei ist file.
     * 
     * @param file
     * @param con
     * @return Erfolg true/false
     * @throws IOException
     */
    public static void download(final File file, final URLConnectionAdapter con) throws IOException {
        if (file.isFile()) {
            if (!file.delete()) {
                System.out.println("Konnte Datei nicht löschen " + file);
                throw new IOException("Could not overwrite file: " + file);
            }
        }

        final File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
        file.createNewFile();

        final BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
        BufferedInputStream input;
        if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            input = new BufferedInputStream(new GZIPInputStream(con.getInputStream()));
        } else {
            input = new BufferedInputStream(con.getInputStream());
        }

        final byte[] b = new byte[1024];
        int len;
        while ((len = input.read(b)) != -1) {
            output.write(b, 0, len);
        }
        output.close();
        input.close();
    }

    public static int getGlobalConnectTimeout() {
        return Browser.TIMEOUT_CONNECT;
    }

    public static int getGlobalReadTimeout() {
        return Browser.TIMEOUT_READ;
    }

    public static String getHost(final String url) {
        if (url == null) { return null; }
        /* direct ip */
        String ret = new Regex(url, "(.*?://)?(\\d+\\.\\d+\\.\\d+\\.\\d+)(/|$|:)").getMatch(1);
        if (ret != null) { return ret; }
        /* normal url */
        ret = new Regex(url, ".*?([^.:/]+\\.[^.:/]+)(/|$|:)").getMatch(0);
        if (ret != null) { return ret.toLowerCase(); }
        return url;
    }

    /**
     * Returns the host for url. input: http://srv2.bluehost.to/dsdsf ->out
     * bluehost.to
     * 
     * @param url
     * @return
     * @throws MalformedURLException
     */

    public static String getHost(final URL url) {
        return Browser.getHost(url.getHost());
    }

    public static void init() {
        CookieHandler.setDefault(null);
        XTrustProvider.install();
        // Now you are telling the JRE to ignore the hostname
        final HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(final String arg0, final SSLSession arg1) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

    }

    /**
     * Sets the global connect timeout
     * 
     * @param valueMS
     */
    public static void setGlobalConnectTimeout(final int valueMS) {
        Browser.TIMEOUT_CONNECT = valueMS;
    }

    public static void setGlobalLogger(final Logger logger) {
        Browser.LOGGER = logger;
    }

    public static void setGlobalProxy(final HTTPProxy p) {
        Browser.GLOBAL_PROXY = p;
    }

    /**
     * Sets the global readtimeout in ms
     * 
     * @param valueMS
     */
    public static void setGlobalReadTimeout(final int valueMS) {
        Browser.TIMEOUT_READ = valueMS;
    }

    public static void setGlobalVerbose(final boolean b) {
        Browser.VERBOSE = b;
    }

    public static synchronized void setRequestIntervalLimitGlobal(final String host, final int i) {
        final String domain = Browser.getHost(host);
        if (domain == null) { return; }
        if (Browser.REQUEST_INTERVAL_LIMIT_MAP == null) {
            Browser.REQUEST_INTERVAL_LIMIT_MAP = new HashMap<String, Integer>();
            Browser.REQUESTTIME_MAP = new HashMap<String, Long>();
        }
        Browser.REQUEST_INTERVAL_LIMIT_MAP.put(domain, i);
    }

    private static synchronized void waitForPageAccess(final Browser browser, final Request request) throws InterruptedException {
        try {
            final String host = Browser.getHost(request.getUrl().getHost());
            Integer localLimit = null;
            Integer globalLimit = null;
            Long localLastRequest = null;
            Long globalLastRequest = null;

            if (browser.requestIntervalLimitMap != null) {
                localLimit = browser.requestIntervalLimitMap.get(host);
                localLastRequest = browser.requestTimeMap.get(host);
            }
            if (Browser.REQUEST_INTERVAL_LIMIT_MAP != null) {
                globalLimit = Browser.REQUEST_INTERVAL_LIMIT_MAP.get(host);
                globalLastRequest = Browser.REQUESTTIME_MAP.get(host);
            }

            if (localLimit == null && globalLimit == null) { return; }
            if (localLastRequest == null && globalLastRequest == null) { return; }
            if (localLimit != null && localLastRequest == null) { return; }
            if (globalLimit != null && globalLastRequest == null) { return; }

            if (globalLimit == null) {
                globalLimit = 0;
            }
            if (localLimit == null) {
                localLimit = 0;
            }
            if (localLastRequest == null) {
                localLastRequest = System.currentTimeMillis();
            }
            if (globalLastRequest == null) {
                globalLastRequest = System.currentTimeMillis();
            }
            final long dif = Math.max(localLimit - (System.currentTimeMillis() - localLastRequest), globalLimit - (System.currentTimeMillis() - globalLastRequest));

            if (dif > 0) {
                // System.out.println("Sleep " + dif + " before connect to " +
                // request.getUrl().getHost());
                Thread.sleep(dif);
                // waitForPageAccess(request);
            }
        } finally {
            final String host = Browser.getHost(request.getUrl().getHost());
            if (browser.requestTimeMap != null) {
                browser.requestTimeMap.put(host, System.currentTimeMillis());
            }
            if (Browser.REQUESTTIME_MAP != null) {
                Browser.REQUESTTIME_MAP.put(host, System.currentTimeMillis());
            }
        }
    }

    private String                   acceptLanguage      = "de, en-gb;q=0.9, en;q=0.8";
    /*
     * -1 means use default Timeouts
     * 
     * 0 means infinite (DO NOT USE if not needed)
     */
    private int                      connectTimeout      = -1;

    private HashMap<String, Cookies> cookies             = new HashMap<String, Cookies>();

    private boolean                  cookiesExclusive    = true;
    private URL                      currentURL          = null;

    private String                   customCharset       = null;

    private boolean                  debug               = false;
    private boolean                  doRedirects         = false;
    private RequestHeader            headers;
    private int                      limit               = 1 * 1024 * 1024;
    private Logger                   logger              = null;
    private HTTPProxy                proxy;
    private int                      readTimeout         = -1;
    private int                      redirectLoopCounter = 0;
    private Request                  request;
    private HashMap<String, Integer> requestIntervalLimitMap;
    private HashMap<String, Long>    requestTimeMap;

    private boolean                  verbose             = false;

    public Browser() {
        final Thread currentThread = Thread.currentThread();
        /**
         * use BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            proxy = settings.getCurrentProxy();
            debug = settings.isDebug();
            verbose = settings.isVerbose();
            logger = settings.getLogger();
        }
    }

    /**
     * Assures that the browser does not download any binary files in textmode
     * 
     * @param request
     * @throws BrowserException
     */
    private void checkContentLengthLimit(final Request request) throws BrowserException {
        if (request == null || request.getHttpConnection() == null || request.getHttpConnection().getHeaderField("Content-Length") == null) {
            return;
        } else if (Long.parseLong(request.getHttpConnection().getHeaderField("Content-Length")) > limit) {
            if (getLogger() != null) {
                getLogger().severe(request.printHeaders());
            }
            throw new BrowserException("Content-length too big", request.getHttpConnection());
        }
    }

    /**
     * Clears all cookies for the given url. URL has to be a valid url if
     * url==null,all cookies were cleared
     * 
     * @param url
     */
    public void clearCookies(final String url) {
        if (url == null) {
            cookies.clear();
        }
        final String host = Browser.getHost(url);
        final Iterator<String> it = getCookies().keySet().iterator();
        String check = null;
        while (it.hasNext()) {
            check = it.next();
            if (check.contains(host)) {
                cookies.get(check).clear();
                break;
            }
        }
    }

    public Browser cloneBrowser() {
        final Browser br = new Browser();
        br.requestIntervalLimitMap = requestIntervalLimitMap;
        br.requestTimeMap = requestTimeMap;
        br.acceptLanguage = acceptLanguage;
        br.connectTimeout = connectTimeout;
        br.currentURL = currentURL;
        br.doRedirects = doRedirects;
        br.setCustomCharset(customCharset);
        br.getHeaders().putAll(getHeaders());
        br.limit = limit;
        br.readTimeout = readTimeout;
        br.request = request;
        br.cookies = cookies;
        br.cookiesExclusive = cookiesExclusive;
        br.debug = debug;
        br.verbose = verbose;
        return br;
    }

    /**
     * Connects a request. and sets the requests as the browsers latest request
     * 
     * @param request
     * @throws IOException
     */
    public void connect(final Request request) throws IOException {
        // sets request BEVOR connection. this enhables to find the request in
        // the protocol handlers
        this.request = request;
        try {
            Browser.waitForPageAccess(this, request);
        } catch (final InterruptedException e) {
            throw new IOException("requestIntervalTime Exception");
        }
        request.connect();
    }

    public boolean containsHTML(final String regex) {
        return new Regex(this, regex).matches();
    }

    /**
     * Creates a new Request object based on a form
     * 
     * @param form
     * @return
     * @throws Exception
     */
    public Request createFormRequest(final Form form) throws Exception {
        String base = null;
        String action = null;
        if (request != null) {
            base = request.getUrl().toString();
            action = form.getAction(base);
        }

        try {
            // find base in source
            final String sourceBase = this.getRegex("<base.*?href=\"(.+?)\"").getMatch(0).trim();
            // check if valid url
            new URL(sourceBase);
            base = sourceBase;
            if (form.getAction(null) != null) {
                action = form.getAction(base);
            }

        } catch (final Throwable e) {

        }

        // action = action;
        switch (form.getMethod()) {

        case GET:
            final String varString = form.getPropertyString();
            if (varString != null && !varString.matches("[\\s]*")) {
                if (action.matches(".*\\?.+")) {
                    action += "&";
                } else if (action.matches("[^\\?]*")) {
                    action += "?";
                }
                action += varString;
            }
            return this.createGetRequest(action);

        case POST:
            if (form.getEncoding() == null || !form.getEncoding().toLowerCase().endsWith("form-data")) {
                return this.createPostRequest(action, form.getRequestVariables(), form.getEncoding());
            } else {
                final PostFormDataRequest request = (PostFormDataRequest) createPostFormDataRequest(action);
                if (form.getEncoding() != null) {
                    request.setEncodeType(form.getEncoding());
                }
                final int size = form.getInputFields().size();
                for (int i = 0; i < size; i++) {
                    final InputField entry = form.getInputFields().get(i);

                    if (entry.getValue() == null) {
                        // continue;
                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("image")) {
                        request.addFormData(new FormData(entry.getKey() + ".x", entry.getProperty("x", (int) (Math.random() * 100) + "")));
                        request.addFormData(new FormData(entry.getKey() + ".y", entry.getProperty("y", (int) (Math.random() * 100) + "")));
                    } else if (entry.getType() != null && entry.getType().equalsIgnoreCase("file")) {
                        request.addFormData(new FormData(entry.getKey(), entry.getFileToPost().getName(), entry.getFileToPost()));
                    } else if (entry.getKey() != null && entry.getValue() != null) {
                        request.addFormData(new FormData(entry.getKey(), entry.getValue()));
                    }
                }
                return request;
            }
        }
        return null;

    }

    /**
     * Creates a new GET request.
     * 
     * @param string
     *            a string including an url
     * 
     * @return the created GET request
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public Request createGetRequest(final String string) throws IOException {
        return createGetRequest(string, null);
    }

    /**
     * Creates a new GET request.
     * 
     * @param string
     *            a string including an url
     * @param oldRequest
     *            the old request for forwarding cookies to the new request. Can
     *            be null, to ignore old cookies.
     * 
     * @return the created GET request
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public Request createGetRequest(String string, final Request oldRequest) throws IOException {
        string = getURL(string);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(string);
        }

        final GetRequest request = new GetRequest(string);
        request.setCustomCharset(customCharset);
        if (selectProxy() != null) {
            request.setProxy(selectProxy());
        }

        // if old request is set, use it's cookies for the new request
        if (oldRequest != null) {
            request.setCookies(oldRequest.getCookies());
        }

        // doAuth(request);
        /* set Timeouts */
        request.setConnectTimeout(getConnectTimeout());
        request.setReadTimeout(getReadTimeout());

        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        forwardCookies(request);
        if (sendref) {
            request.getHeaders().put("Referer", currentURL.toString());
        }
        if (headers != null) {
            mergeHeaders(request);
        }

        // if (this.doRedirects && request.getLocation() != null) {
        // this.openGetConnection(null);
        // } else {
        //
        // currentURL = new URL(string);
        // }
        // return this.request.getHttpConnection();
        return request;
    }

    /**
     * Creates a new GET request including cookies from old request.
     * 
     * @param oldRequest
     *            the old request
     * 
     * @return the created GET request
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public Request createGetRequestRedirectedRequest(final Request oldRequest) throws IOException {
        return createGetRequest(oldRequest.getLocation(), oldRequest);
    }

    public Request createPostFormDataRequest(String url) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }

        final PostFormDataRequest request = new PostFormDataRequest(url);
        request.setCustomCharset(customCharset);
        if (selectProxy() != null) {
            request.setProxy(selectProxy());
        }

        request.getHeaders().put("Accept-Language", acceptLanguage);

        /* set Timeouts */
        request.setConnectTimeout(getConnectTimeout());
        request.setReadTimeout(getReadTimeout());
        forwardCookies(request);
        if (sendref) {
            request.getHeaders().put("Referer", currentURL.toString());
        }

        if (headers != null) {
            mergeHeaders(request);
        }
        return request;
    }

    /**
     * Creates a new postrequest based an an requestVariable Arraylist
     */
    private Request createPostRequest(String url, final ArrayList<RequestVariable> post, final String encoding) throws IOException {
        url = getURL(url);
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }

        final PostRequest request = new PostRequest(url);
        request.setCustomCharset(customCharset);
        if (selectProxy() != null) {
            request.setProxy(selectProxy());
        }
        // doAuth(request);
        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        /* set Timeouts */
        request.setConnectTimeout(getConnectTimeout());
        request.setReadTimeout(getReadTimeout());
        forwardCookies(request);
        if (sendref) {
            request.getHeaders().put("Referer", currentURL.toString());
        }
        if (post != null) {
            request.addAll(post);
        }
        /* check browser/call for content type encoding, or set to default */
        String brContentType = null;
        if (headers != null) {
            brContentType = headers.remove("Content-Type");
        }
        if (brContentType == null) {
            brContentType = encoding;
        }
        if (brContentType == null) {
            brContentType = "application/x-www-form-urlencoded";
        }
        request.setContentType(brContentType);
        if (headers != null) {
            mergeHeaders(request);
        }
        return request;
    }

    /**
     * Creates a new POstrequest based on a variable hashmap
     */
    public Request createPostRequest(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return this.createPostRequest(url, PostRequest.variableMaptoArray(post), null);
    }

    /**
     * Creates a postrequest based on a querystring
     */
    public Request createPostRequest(final String url, final String post) throws MalformedURLException, IOException {
        return createPostRequest(url, Request.parseQuery(post));
    }

    public Request createPostRequestfromRedirectedRequest(final Request oldrequest, final String postdata) throws IOException {
        final String url = getURL(oldrequest.getLocation());
        boolean sendref = true;
        if (currentURL == null) {
            sendref = false;
            currentURL = new URL(url);
        }
        final HashMap<String, String> post = Request.parseQuery(postdata);

        final PostRequest request = new PostRequest(url);
        request.setCustomCharset(customCharset);
        if (selectProxy() != null) {
            request.setProxy(selectProxy());
        }
        request.setCookies(oldrequest.getCookies());
        // doAuth(request);
        request.getHeaders().put("Accept-Language", acceptLanguage);
        // request.setFollowRedirects(doRedirects);
        /* set Timeouts */
        request.setConnectTimeout(getConnectTimeout());
        request.setReadTimeout(getReadTimeout());
        forwardCookies(request);
        if (sendref) {
            request.getHeaders().put("Referer", currentURL.toString());
        }
        if (post != null) {
            request.addAll(post);
        }
        if (headers != null) {
            mergeHeaders(request);
        }
        return request;
    }

    public Request createRequest(final Form form) throws Exception {
        return createFormRequest(form);
    }

    public Request createRequest(final String downloadURL) throws Exception {
        return createGetRequest(downloadURL);
    }

    /**
     * Downloads the contents behind con to file. if(con ==null), the latest
     * request is downloaded. Usefull for redirects
     * 
     * @param file
     * @param con
     * @throws IOException
     */
    public void downloadConnection(final File file, URLConnectionAdapter con) throws IOException {
        if (con == null) {
            con = request.getHttpConnection();
        }
        Browser.download(file, con);
    }

    public String followConnection() throws IOException {
        if (request.getHtmlCode() != null) {
            if (getLogger() != null) {
                getLogger().warning("Request has already been read");
            }
            return null;
        }
        try {
            checkContentLengthLimit(request);
            request.read();
        } catch (final BrowserException e) {
            throw e;
        } catch (final IOException e) {
            throw new BrowserException(e.getMessage(), request.getHttpConnection(), e).closeConnection();
        }
        if (isVerbose()) {
            if (getLogger() != null) {
                getLogger().finest("\r\n" + request + "\r\n");
            }
        }
        return request.getHtmlCode();
    }

    /**
     * Zeigt debuginformationen auch im Hauptprogramm an
     * 
     * @param b
     */
    public void forceDebug(final boolean b) {
        debug = b;
    }

    public void forwardCookies(final Request request) {
        if (request == null) { return; }
        final String host = Browser.getHost(request.getUrl());
        final Cookies cookies = getCookies().get(host);
        if (cookies == null) { return; }

        for (final Cookie cookie : cookies.getCookies()) {
            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }
            request.getCookies().add(cookie);
        }
    }

    public void forwardCookies(final URLConnectionAdapter con) {
        if (con == null) { return; }
        final String host = Browser.getHost(con.getURL().toString());
        final Cookies cookies = getCookies().get(host);
        final String cs = Request.getCookieString(cookies);
        if (cs != null && cs.trim().length() > 0) {
            con.setRequestProperty("Cookie", cs);
        }
    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    private String getBase(final String string) {
        if (string == null) { return ""; }

        final String base = getRegex("<base\\s*href=\"(.*?)\"").getMatch(0);
        if (base != null) { return base; }

        final URL url = request.getHttpConnection().getURL();
        final int port = url.getPort();
        final String host = url.getHost();

        String path = url.getPath();
        int id;
        if ((id = path.lastIndexOf('/')) >= 0) {
            path = path.substring(0, id);
        }
        return port != 80 && port > 0 ? "http://" + host + ":" + port + path + "/" : "http://" + host + path + "/";
    }

    public String getBaseURL() {
        if (request == null) { return null; }

        final String base = request.getUrl().toString();
        // if (base.matches("http://.*/.*")) {
        // return base.substring(0, base.lastIndexOf("/")) + "/";
        // } else {
        // return base + "/";
        // }
        return base.matches("http://.*/.*") ? base.substring(0, base.lastIndexOf("/")) + "/" : base + "/";
    }

    /**
     * returns current ConnectTimeout
     * 
     * @return
     */
    public int getConnectTimeout() {
        return connectTimeout < 0 ? Browser.TIMEOUT_CONNECT : connectTimeout;
    }

    public String getCookie(final String url, final String key) {
        final String host = Browser.getHost(url);
        final Cookies cookies = getCookies(host);
        final Cookie cookie = cookies.get(key);

        return cookie != null ? cookie.getValue() : null;
    }

    private HashMap<String, Cookies> getCookies() {
        return cookiesExclusive ? cookies : Browser.COOKIES;
    }

    public Cookies getCookies(final String url) {
        final String host = Browser.getHost(url);
        Cookies cookies2 = getCookies().get(host);
        if (cookies2 == null) {
            getCookies().put(host, cookies2 = new Cookies());
        }
        return cookies2;
    }

    public void getDownload(final File file, final String urlString) throws IOException {
        final URLConnectionAdapter con = openGetConnection(URLDecoder.decode(urlString, "UTF-8"));
        Browser.download(file, con);
    }

    public Form getForm(final int i) {
        final Form[] forms = getForms();
        return forms.length <= i ? null : forms[i];
    }

    /**
     * Returns the first form that has an input filed with name key
     * 
     * @param key
     * @return
     */
    public Form getFormbyKey(final String key) {
        for (final Form f : getForms()) {
            if (f.hasInputFieldByName(key)) { return f; }
        }
        return null;
    }

    public Form getFormbyProperty(final String property, final String name) {
        for (final Form form : getForms()) {
            if (form.getStringProperty(property) != null && form.getStringProperty(property).equalsIgnoreCase(name)) { return form; }
        }
        return null;
    }

    /**
     * Returns the first form with an Submitvalue of name
     * 
     * @param name
     * @return
     */
    public Form getFormBySubmitvalue(final String name) {
        for (final Form form : getForms()) {
            try {
                form.setPreferredSubmit(name);
                return form;
            } catch (final IllegalArgumentException e) {
            }
        }
        return null;
    }

    public Form[] getForms() {
        return Form.getForms(this);
    }

    public Form[] getForms(final String downloadURL) throws IOException {
        this.getPage(downloadURL);
        return this.getForms();
    }

    public RequestHeader getHeaders() {
        if (headers == null) {
            headers = new RequestHeader();
        }
        return headers;
    }

    public String getHost() {
        return request == null ? null : request.getUrl().getHost();
    }

    public URLConnectionAdapter getHttpConnection() {
        if (request == null) { return null; }
        return request.getHttpConnection();
    }

    public Logger getLogger() {
        if (logger != null) { return logger; }
        return Browser.LOGGER;
    }

    public String getMatch(final String string) {
        return getRegex(string).getMatch(0);
    }

    public String getPage(final String string) throws IOException {
        openRequestConnection(this.createGetRequest(string));
        return loadConnection(null).getHtmlCode();
    }

    public String getPage(final URL url) throws IOException {
        return getPage(url + "");
    }

    public HTTPProxy getProxy() {
        return proxy;
    }

    /**
     * returns current ReadTimeout
     * 
     * @return
     */
    public int getReadTimeout() {
        return readTimeout < 0 ? Browser.TIMEOUT_READ : readTimeout;
    }

    /**
     * If automatic redirectfollowing is disabled, you can get the redirect url
     * if there is any.
     * 
     * @return
     */
    public String getRedirectLocation() {
        if (request == null) { return null; }
        return request.getLocation();
    }

    public Regex getRegex(final Pattern compile) {
        return new Regex(this, compile);
    }

    public Regex getRegex(final String string) {
        return new Regex(this, string);
    }

    /**
     * Gets the latest request
     * 
     * @return
     */
    public Request getRequest() {
        return request;
    }

    public String getURL() {
        return request == null ? null : request.getUrl().toString();
    }

    /**
     * TRies to get a fuill url out of string
     * 
     * @throws BrowserException
     */
    public String getURL(String string) throws BrowserException {
        if (string == null) {
            string = getRedirectLocation();
        }
        if (string == null) { throw new BrowserException("Null URL"); }
        try {
            new URL(string);
        } catch (final Exception e) {
            if (request == null || request.getHttpConnection() == null) { return string; }
            final String base = getBase(string);
            if (string.startsWith("/") || string.startsWith("\\")) {
                try {
                    final URL bUrl = new URL(base);
                    if (bUrl.getPort() != 80 && bUrl.getPort() > 0) {
                        string = "http://" + new URL(base).getHost() + ":" + bUrl.getPort() + string;
                    } else {
                        string = "http://" + new URL(base).getHost() + string;
                    }
                } catch (final MalformedURLException e1) {
                    e1.printStackTrace();
                }
            } else {
                string = base + string;
            }
        }
        return Browser.correctURL(Encoding.urlEncode_light(string));
    }

    public boolean isCookiesExclusive() {
        return cookiesExclusive;
    }

    public boolean isDebug() {
        return debug || isVerbose();
    }

    public boolean isFollowingRedirects() {
        return doRedirects;
    }

    public boolean isVerbose() {
        return Browser.VERBOSE || verbose;
    }

    /**
     * Reads the content behind a con and returns them. Note: if con==null, the
     * current request is read. This is usefull for redirects. Note #2: if a
     * connection is loaded, data is not stored in the browser instance.
     * 
     * @param con
     * @return
     * @throws IOException
     */
    public Request loadConnection(URLConnectionAdapter con) throws IOException {

        Request requ;
        if (con == null) {
            requ = request;
        } else {
            requ = new Request(con) {

                @Override
                public long postRequest(final URLConnectionAdapter httpConnection) throws IOException {
                    return 0;
                }

                @Override
                public void preRequest(final URLConnectionAdapter httpConnection) throws IOException {
                    // TODO Auto-generated method stub

                }

            };
        }
        try {
            checkContentLengthLimit(requ);
            con = requ.getHttpConnection();
            requ.read();
        } catch (final BrowserException e) {
            throw e;
        } catch (final IOException e) {
            throw new BrowserException(e.getMessage(), con, e).closeConnection();
        }
        if (isVerbose()) {
            if (getLogger() != null) {
                getLogger().finest("\r\n" + requ + "\r\n");
            }
        }
        return requ;
    }

    private void mergeHeaders(final Request request) {
        if (headers.isDominant()) {
            request.getHeaders().clear();
        }
        final int size = headers.size();
        String value;
        for (int i = 0; i < size; i++) {
            value = headers.getValue(i);
            if (value == null) {
                request.getHeaders().remove(headers.getKey(i));
            } else {
                request.getHeaders().put(headers.getKey(i), value);
            }
        }
    }

    /**
     * Opens a new connection based on a Form
     * 
     * @param form
     * @return
     * @throws Exception
     */
    public URLConnectionAdapter openFormConnection(final Form form) throws Exception {
        return openRequestConnection(createFormRequest(form));
    }

    public URLConnectionAdapter openFormConnection(final int i) throws Exception {
        return openFormConnection(getForm(i));
    }

    /**
     * Opens a new get connection
     * 
     * @param string
     * @return
     * @throws IOException
     */
    public URLConnectionAdapter openGetConnection(final String string) throws IOException {
        return openRequestConnection(this.createGetRequest(string));

    }

    /**
     * Opens a Post COnnection based on a variable hashmap
     */
    public URLConnectionAdapter openPostConnection(final String url, final LinkedHashMap<String, String> post) throws IOException {
        return openRequestConnection(this.createPostRequest(url, post));
    }

    /**
     * OPens a new POst connection based on a query string
     */
    public URLConnectionAdapter openPostConnection(final String url, final String post) throws IOException {
        return openPostConnection(url, Request.parseQuery(post));
    }

    /**
     * Opens a connection based on the requets object
     */
    public URLConnectionAdapter openRequestConnection(final Request request) throws IOException {
        connect(request);
        if (isDebug()) {
            if (getLogger() != null) {
                getLogger().finest("\r\n" + request.printHeaders());
            }
        }
        updateCookies(request);
        this.request = request;
        if (doRedirects && request.getLocation() != null) {
            if (request.getLocation().toLowerCase().startsWith("ftp://")) { throw new BrowserException("Cannot redirect to FTP"); }
            final String org = request.getUrl().toExternalForm();
            final String red = request.getLocation();
            if (org.equalsIgnoreCase(red) && redirectLoopCounter >= 20) {
                if (getLogger() != null) {
                    getLogger().severe("20 Redirects!!!");
                }
            } else if (!org.equalsIgnoreCase(red) || redirectLoopCounter < 20) {
                if (org.equalsIgnoreCase(red)) {
                    redirectLoopCounter++;
                } else {
                    redirectLoopCounter = 0;
                }
                /* prevent buggy redirect loops */
                /* source==dest */
                try {
                    /* close old connection, because we follow redirect */
                    request.httpConnection.disconnect();
                } catch (final Exception e) {
                }
                openGetConnection(null);
            }
        } else {
            currentURL = request.getUrl();
        }
        return this.request.getHttpConnection();
    }

    /**
     * loads a new page (post)
     */
    public String postPage(final String url, final LinkedHashMap<String, String> post) throws IOException {
        openPostConnection(url, post);
        return loadConnection(null).getHtmlCode();
    }

    /**
     * loads a new page (POST)
     */
    public String postPage(final String url, final String post) throws IOException {
        return postPage(url, Request.parseQuery(post));
    }

    /**
     * loads a new page (post) the postdata is given by the poststring. it wiull
     * be send as it is
     */
    public String postPageRaw(final String url, final String post) throws IOException {
        final PostRequest request = (PostRequest) this.createPostRequest(url, new ArrayList<RequestVariable>(), null);
        request.setCustomCharset(customCharset);
        if (post != null) {
            request.setPostDataString(post);
        }
        openRequestConnection(request);
        return loadConnection(null).getHtmlCode();
    }

    /**
     * resets browser to default values
     */
    public void reset() {
        currentURL = null;
        request = null;
        setCustomCharset(null);
        doRedirects = false;
        cookies = new HashMap<String, Cookies>();
        connectTimeout = -1;
        readTimeout = -1;
        headers = null;
        limit = 1 * 1024 * 1024;
        proxy = null;
        debug = false;
        requestIntervalLimitMap = null;
        requestTimeMap = null;
        cookiesExclusive = true;
        verbose = false;
        acceptLanguage = "de, en-gb;q=0.9, en;q=0.8";
    }

    private HTTPProxy selectProxy() {
        if (proxy != null) {
            if (proxy == HTTPProxy.NONE) { return null; }
            return proxy;
        }
        return Browser.GLOBAL_PROXY;
    }

    public void setAcceptLanguage(final String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    /* TODO: setauth needs to be done */
    public void setAuth(final String domain, final String user, final String pass) {
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setCookie(final String url, final String key, final String value) {
        final String host = Browser.getHost(url);
        Cookies cookies;
        if (!getCookies().containsKey(host) || (cookies = getCookies().get(host)) == null) {
            cookies = new Cookies();
            getCookies().put(host, cookies);
        }
        cookies.add(new Cookie(host, key, value));
    }

    public void setCookiesExclusive(final boolean b) {
        if (cookiesExclusive == b) { return; }
        cookiesExclusive = b;
        if (b) {
            cookies.clear();
            for (final Entry<String, Cookies> next : Browser.COOKIES.entrySet()) {
                Cookies tmp;
                cookies.put(next.getKey(), tmp = new Cookies());
                tmp.add(next.getValue());
            }
        } else {
            cookies.clear();
        }
    }

    /* sets current URL, if null we dont send referer! */
    public void setCurrentURL(final String string) throws MalformedURLException {
        if (string == null || string.length() == 0) {
            currentURL = null;
        } else {
            currentURL = new URL(string);
        }
    }

    public void setCustomCharset(final String charset) {
        customCharset = charset;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public void setFollowRedirects(final boolean b) {
        doRedirects = b;
    }

    public void setHeader(final String field, final String value) {
        getHeaders().put(field, value);
    }

    public void setHeaders(final RequestHeader h) {
        headers = h;
    }

    public void setLoadLimit(final int i) {
        limit = i;
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public void setProxy(final HTTPProxy proxy) {
        if (debug) {
            if (Browser.LOGGER != null) {
                Browser.LOGGER.info("Use local proxy: " + proxy);
            }
        }
        this.proxy = proxy;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setRequest(final Request request) {
        if (request == null) { return; }
        updateCookies(request);
        this.request = request;
        currentURL = request.getUrl();
    }

    public void setRequestIntervalLimit(final String host, final int i) {
        final String domain = Browser.getHost(host);
        if (domain == null) { return; }
        if (requestIntervalLimitMap == null) {
            requestTimeMap = new HashMap<String, Long>();
            requestIntervalLimitMap = new HashMap<String, Integer>();
        }
        requestIntervalLimitMap.put(domain, i);

    }

    public void setVerbose(final boolean b) {
        verbose = b;
    }

    public String submitForm(final Form form) throws Exception {
        this.openFormConnection(form);
        checkContentLengthLimit(request);
        return request.read().getHtmlCode();
    }

    public String submitForm(final String formname) throws Exception {
        return this.submitForm(getFormBySubmitvalue(formname));
    }

    @Override
    public String toString() {
        if (request == null) { return "Browser. no request yet"; }
        return request.toString();
    }

    public void updateCookies(final Request request) {
        if (request == null) { return; }
        final String host = Browser.getHost(request.getUrl());
        Cookies cookies = getCookies().get(host);
        if (cookies == null) {
            cookies = new Cookies();
            getCookies().put(host, cookies);
        }
        cookies.add(request.getCookies());
    }
}
