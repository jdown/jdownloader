package jd.http.basic;

import java.io.IOException;

import jd.http.URLConnectionAdapter;

public class BasicHTTPException extends IOException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final URLConnectionAdapter connection;

    public BasicHTTPException(final URLConnectionAdapter connection, final IOException e) {
        super(e);
        this.connection = connection;

    }

    public URLConnectionAdapter getConnection() {
        return connection;
    }

}
