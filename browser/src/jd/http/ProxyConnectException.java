package jd.http;

import java.net.ConnectException;

public class ProxyConnectException extends ConnectException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 189884014110822090L;

    public ProxyConnectException(final String message) {
        super(message);
    }

}
