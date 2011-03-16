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

package jd.http.requests;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;

import org.appwork.utils.net.CountingOutputStream;
import org.appwork.utils.net.NullOutputStream;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;

public class PostRequest extends Request {
    public static ArrayList<RequestVariable> variableMaptoArray(final LinkedHashMap<String, String> post) {
        if (post == null) { return null; }
        final ArrayList<RequestVariable> ret = new ArrayList<RequestVariable>();
        for (final Entry<String, String> entry : post.entrySet()) {
            ret.add(new RequestVariable(entry.getKey(), entry.getValue()));
        }
        return ret;
    }

    private final ArrayList<RequestVariable> postData;
    private String postDataString = null;

    private String contentType = null;

    public PostRequest(final Form form) throws MalformedURLException {
        super(form.getAction(null));
        this.postData = new ArrayList<RequestVariable>();
    }

    public PostRequest(final String url) throws MalformedURLException {
        super(Browser.correctURL(url));
        this.postData = new ArrayList<RequestVariable>();
        /* forward basicauth from url to headers */
        final String basicAuth = Browser.getBasicAuthfromURL(url);
        if (basicAuth != null) {
            this.getHeaders().put("Authorization", "Basic " + basicAuth);
        }
    }

    public void addAll(final ArrayList<RequestVariable> post) {
        this.postData.addAll(post);
    }

    public void addAll(final HashMap<String, String> post) {
        for (final Entry<String, String> entry : post.entrySet()) {
            this.postData.add(new RequestVariable(entry));
        }
    }

    public void addVariable(final String key, final String value) {
        this.postData.add(new RequestVariable(key, value));
    }

    public String getPostDataString() {
        if (this.postData.isEmpty()) { return this.postDataString; }
        final StringBuilder buffer = new StringBuilder();
        for (final RequestVariable rv : this.postData) {
            if (rv.getKey() != null) {
                buffer.append("&");
                buffer.append(rv.getKey());
                buffer.append("=");
                if (rv.getValue() != null) {
                    buffer.append(rv.getValue());
                } else {
                    buffer.append("");
                }
            }
        }
        if (buffer.length() == 0) { return ""; }
        return buffer.substring(1);
    }

    /**
     * send the postData of the Request. in case httpConnection is null, it
     * outputs the data to a NullOutputStream
     */
    public long postRequest(final URLConnectionAdapter httpConnection) throws IOException {
        CountingOutputStream output = null;
        if (httpConnection != null && httpConnection.getOutputStream() != null) {
            output = new CountingOutputStream(httpConnection.getOutputStream());
        } else {
            output = new CountingOutputStream(new NullOutputStream());
        }
        String parameter = this.postDataString != null ? this.postDataString : this.getPostDataString();
        try {
            if (parameter != null) {
                if (this.postDataString == null) {
                    parameter = parameter.trim();
                }
                final OutputStreamWriter wr = new OutputStreamWriter(output);
                if (parameter != null) {
                    wr.write(parameter);
                }
                wr.flush();
                output.flush();
            }
        } finally {
            if (httpConnection != null) {
                httpConnection.postDataSend();
            }
        }
        return output.bytesWritten();
    }

    public void preRequest(final URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setRequestMethod(RequestMethod.POST);
        if (this.contentType != null) {
            httpConnection.setRequestProperty("Content-Type", this.contentType);
        }
        final String parameter = this.postDataString != null ? this.postDataString : this.getPostDataString();
        if (parameter != null) {
            httpConnection.setRequestProperty("Content-Length", this.postRequest(null) + "");
        } else {
            httpConnection.setRequestProperty("Content-Length", "0");
        }
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public void setPostDataString(final String post) {
        this.postDataString = post;
    }
}
