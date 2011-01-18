package jd.http;

import java.io.IOException;

public class BrowserException extends IOException {

    private static final long    serialVersionUID = 1509988898224037320L;
    private URLConnectionAdapter connection;
    private Exception            e                = null;

    public BrowserException(final String string) {
        super(string);
    }

    public BrowserException(final String string, final Exception e) {
        this(string);
        this.e = e;
    }

    public BrowserException(final String message, final URLConnectionAdapter con) {
        this(message);
        connection = con;
    }

    public BrowserException(final String message, final URLConnectionAdapter con, final Exception e) {
        this(message, con);
        this.e = e;
    }

    public BrowserException closeConnection() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
        return this;
    }

    /**
     * Returns the connection adapter that caused the browserexception
     * 
     * @return
     */
    public URLConnectionAdapter getConnection() {
        return connection;
    }

    public Exception getException() {
        return e;
    }

}