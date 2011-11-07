package jd.http;

import java.util.logging.Logger;

import org.appwork.utils.net.httpconnection.HTTPProxy;

public class BrowserSettingsThread extends Thread implements BrowserSettings {

    private HTTPProxy proxy;
    private boolean debug;
    private boolean verbose;
    protected Logger logger;

    public BrowserSettingsThread(final Runnable r) {
        super(r);
        this.copySettings();
    }

    public BrowserSettingsThread(final Runnable r, final String name) {
        super(r, name);
        this.copySettings();
    }

    public BrowserSettingsThread(final String name) {
        super(name);
        this.copySettings();
    }

    private void copySettings() {
        final Thread currentThread = Thread.currentThread();
        /**
         * use BrowserSettings from current thread if available
         */
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            this.proxy = settings.getCurrentProxy();
            this.debug = settings.isDebug();
            this.verbose = settings.isVerbose();
            this.logger = settings.getLogger();
        }
    }

    public HTTPProxy getCurrentProxy() {
        return this.proxy;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setCurrentProxy(final HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public void setDebug(final boolean b) {
        this.debug = b;
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public void setVerbose(final boolean b) {
        this.verbose = b;
    }

}
