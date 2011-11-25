package org.jdownloader.update;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.update.exchange.ServerError;
import org.appwork.update.exchange.ServerResponses;
import org.appwork.update.updateclient.UpdateHttpClient;
import org.appwork.update.updateclient.UpdateHttpClientOptions;
import org.appwork.update.updateclient.http.ClientUpdateRequiredException;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.update.updateclient.http.UpdateServerException;
import org.appwork.update.updateclient.translation.T;
import org.appwork.utils.net.DownloadProgress;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.appwork.utils.net.BasicHTTP.BasicHTTPException;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;

public class UpdaterHttpClientImpl implements UpdateHttpClient, GenericConfigEventListener<Object> {
    private final BasicHTTP               client;

    private final UpdateHttpClientOptions options;

    public UpdaterHttpClientImpl() {
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        this.client = new BasicHTTP();
        this.options = JsonConfig.create(UpdateHttpClientOptions.class);
        this.client.setConnectTimeout(this.options.getConnectTimeout());
        this.client.setReadTimeout(this.options.getReadTimeout());
        this.updateProxy(false);
        this.options.getStorageHandler().getEventSender().addListener(this, true);
    }

    @Override
    public void download(final File file, final String url, final DownloadProgress progress) throws HTTPIOException, ClientUpdateRequiredException, InterruptedException, UpdateServerException {
        if (this.isInterrupted()) { throw new InterruptedException(); }
        try {
            this.client.download(new URL(url), progress, file);
            this.handleClientErrors();
        } catch (final BasicHTTPException e) {
            throw new HTTPIOException(this.client.getConnection().getResponseCode(), e.getMessage());
        } catch (final IOException e) {
            throw new HTTPIOException(e);
        }

    }

    @Override
    public byte[] get(final String url) throws HTTPIOException, ClientUpdateRequiredException, InterruptedException, UpdateServerException {
        if (this.isInterrupted()) { throw new InterruptedException(); }
        try {
            final byte[] ret = this.client.download(new URL(url), null, 0);

            this.handleClientErrors();
            return ret;
        } catch (final BasicHTTPException e) {
            throw new HTTPIOException(this.client.getConnection().getResponseCode(), e.getMessage());
        } catch (final IOException e) {
            throw new HTTPIOException(e);
        }

    }

    @Override
    public UpdateHttpClientOptions getOptions() {
        return this.options;
    }

    private void handleClientErrors() throws ClientUpdateRequiredException, UpdateServerException, HTTPIOException {
        if (this.client.getResponseHeader(ServerResponses.CLIENT_UPDATE_HEADER) != null) {

        throw new ClientUpdateRequiredException(this.client.getResponseHeader(ServerResponses.CLIENT_UPDATE_HEADER), this.client.getResponseHeader(ServerResponses.CLIENT_UPDATE_HASH));

        }
        if (this.client.getResponseHeader(ServerResponses.ERROR_HEADER) != null) { throw new UpdateServerException(ServerError.valueOf(this.client.getResponseHeader(ServerResponses.ERROR_HEADER))); }
        if (this.client.getConnection().getResponseCode() == ResponseCode.REDIRECT_FOUND.getCode()) {
            throw new HTTPIOException(ResponseCode.REDIRECT_FOUND.getCode(), T._.redirect_error(this.client.getResponseHeader("Location")));
        } else if (this.client.getConnection().getResponseCode() != ResponseCode.SUCCESS_OK.getCode()) {
            final int code = this.client.getConnection().getResponseCode();
            throw new HTTPIOException(code, T._.unexpected_http_error(code));
        }
    }

 

    public boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public void onConfigValidatorError(final KeyHandler<Object> keyHandler, final Object invalidValue, final ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(final KeyHandler<Object> keyHandler, final Object newValue) {
        this.updateProxy(true);
    }

    @Override
    public byte[] post(final String url, final String data) throws HTTPIOException, ClientUpdateRequiredException, InterruptedException, UpdateServerException {
        if (this.isInterrupted()) { throw new InterruptedException(); }
        try {
            final byte[] ret = this.client.postPage(new URL(url), data).getBytes();
            this.handleClientErrors();
            return ret;
        } catch (final BasicHTTPException e) {
            throw new HTTPIOException(this.client.getConnection().getResponseCode(), e.getMessage());
        } catch (final IOException e) {
            throw new HTTPIOException(e);

        }
    }

    @Override
    public void putHeader(final String key, final String value) {
        this.client.getRequestHeader().put(key, value);

    }

    private void updateProxy(final boolean preferConfigProxy) {
        final HTTPProxyStorable proxyStorable = this.options.getProxy();
        final List<HTTPProxy> proxies = HTTPProxy.getFromSystemProperties();
        if (proxyStorable != null && (preferConfigProxy || proxies.size() == 0)) {
            final HTTPProxy proxy = HTTPProxy.getHTTPProxy(proxyStorable);
            this.client.setProxy(proxy);
        } else if (proxies.size() > 0) {
            this.client.setProxy(proxies.get(0));
        }
    }

}
