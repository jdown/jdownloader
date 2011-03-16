package jd.http;

import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import jd.http.requests.PostFormDataRequest;
import jd.http.requests.PostRequest;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyHTTPConnectionImpl;

public class URLConnectionAdapterHTTPProxyImpl extends HTTPProxyHTTPConnectionImpl implements URLConnectionAdapter {

    private Request request;

    public URLConnectionAdapterHTTPProxyImpl(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
    }

    @Override
    public long getLongContentLength() {
        return this.getContentLength();
    }

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void setRequest(final Request request) {
        this.request = request;
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
