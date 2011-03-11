//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface URLConnectionAdapter {

    public static enum RequestMethod {
        GET, POST, HEAD
    }

    void connect() throws IOException;

    void disconnect();

    public String getCharset();

    int getContentLength();

    String getContentType();

    String getHeaderField(String string);

    /* WARNING: this returns a Case-Sensitive map */
    Map<String, List<String>> getHeaderFields();

    List<String> getHeaderFields(String string);

    InputStream getInputStream() throws IOException;

    long getLongContentLength();

    OutputStream getOutputStream() throws IOException;

    long[] getRange();

    public Request getRequest();

    RequestMethod getRequestMethod();

    Map<String, String> getRequestProperties();

    String getRequestProperty(String string);

    public long getRequestTime();

    int getResponseCode();

    String getResponseMessage();

    URL getURL();

    boolean isConnected();

    boolean isContentDisposition();

    boolean isOK();

    /* needs to get called after postData is send */
    public void postDataSend() throws IOException;

    /**
     * returns bytearray that belongs to content, WARNING will get null after
     * first call
     * 
     * @return
     */
    public byte[] preReadBytes();

    public void setCharset(String charset);

    void setConnectTimeout(int connectTimeout);

    void setReadTimeout(int readTimeout);

    void setRequest(Request request);

    void setRequestMethod(RequestMethod method);

    void setRequestProperty(String key, String string);
}
