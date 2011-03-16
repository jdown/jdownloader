package jd.http;

import java.util.logging.Logger;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public interface BrowserSettings {

    public HTTPProxy getCurrentProxy();

    public Logger getLogger();

    public boolean isDebug();

    public boolean isVerbose();

    public void setCurrentProxy(HTTPProxy proxy);

    public void setDebug(boolean b);

    public void setLogger(Logger logger);

    public void setVerbose(boolean b);

}
