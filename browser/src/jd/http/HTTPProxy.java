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

import java.net.InetAddress;

import jd.parser.Regex;

import org.appwork.utils.logging.Log;

public class HTTPProxy {

    public static enum STATUS {
        OK, OFFLINE, INVALIDAUTH
    }

    public static enum TYPE {
        NONE, DIRECT, SOCKS5, HTTP
    }

    public static final HTTPProxy NONE = new HTTPProxy(TYPE.NONE);

    private static String[] getInfo(final String host, final String port) {
        final String[] info = new String[2];
        if (host == null) { return info; }
        final String tmphost = host.replaceFirst("http://", "").replaceFirst("https://", "");
        String tmpport = new Regex(host, ".*?:(\\d+)").getMatch(0);
        if (tmpport != null) {
            info[1] = "" + tmpport;
        } else {
            if (port != null) {
                tmpport = new Regex(port, "(\\d+)").getMatch(0);
            }
            if (tmpport != null) {
                info[1] = "" + tmpport;
            } else {
                Log.L.severe("No proxyport defined, using default 8080");
                info[1] = "8080";
            }
        }
        info[0] = new Regex(tmphost, "(.*?)(:|/|$)").getMatch(0);
        return info;
    }

    private InetAddress localIP = null;

    private String user = null;

    private String pass = null;

    private int port = 80;
    private String host = null;
    private TYPE type = TYPE.DIRECT;
    private STATUS status = STATUS.OK;

    private HTTPProxy(final TYPE type) {
        this.type = type;
    }

    public HTTPProxy(final TYPE type, final String host, final int port) {
        this.port = port;
        this.type = type;
        this.host = HTTPProxy.getInfo(host, "" + port)[0];
    }

    public String getHost() {
        return this.host;
    }

    /**
     * @return the localIP
     */
    public InetAddress getLocalIP() {
        return this.localIP;
    }

    public String getPass() {
        return this.pass;
    }

    public int getPort() {
        return this.port;
    }

    /**
     * @return the status
     */
    public STATUS getStatus() {
        return this.status;
    }

    public TYPE getType() {
        return this.type;
    }

    public String getUser() {
        return this.user;
    }

    /**
     * this proxy is DIRECT = using a local bound IP
     * 
     * @return
     */
    public boolean isDirect() {
        return this.type == TYPE.DIRECT;
    }

    public boolean isLocal() {
        return this.isDirect() || this.isNone();
    }

    /**
     * this proxy is NONE = uses default gateway
     * 
     * @return
     */
    public boolean isNone() {
        return this.type == TYPE.NONE;
    }

    /**
     * this proxy is REMOTE = using http,socks proxy
     * 
     * @return
     */
    public boolean isRemote() {
        return !this.isDirect() && !this.isNone();
    }

    public boolean sameProxy(final HTTPProxy proxy) {
        if (proxy == null) { return false; }
        if (this == proxy) { return true; }
        if (!proxy.getType().equals(this.type)) { return false; }
        if (!proxy.getHost().equalsIgnoreCase(this.host)) { return false; }
        if (!proxy.getPass().equalsIgnoreCase(this.pass)) { return false; }
        if (!proxy.getUser().equalsIgnoreCase(this.user)) { return false; }
        if (proxy.getPort() != this.port) { return false; }
        return true;
    }

    /**
     * @param localIP
     *            the localIP to set
     */
    public void setLocalIP(final InetAddress localIP) {
        this.localIP = localIP;
    }

    public void setPass(final String pass) {
        this.pass = pass;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final STATUS status) {
        this.status = status;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        if (this.type == TYPE.NONE) { return "NONE"; }
        if (this.type == TYPE.DIRECT) { return "DIRECT"; }
        if (this.type == TYPE.HTTP) { return "HTTP:" + this.host; }
        if (this.type == TYPE.SOCKS5) { return "SOCKS5:" + this.host; }
        return "UNKNOWN";
    }
}
