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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.appwork.utils.logging.Log;

public class Cookie {

    private static final String[] dateformats = new String[] { "EEE, dd-MMM-yy HH:mm:ss z", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z", "EEE MMM dd HH:mm:ss z yyyy", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEEE, dd-MMM-yy HH:mm:ss z" };

    private String path;
    private String host;
    private String value;
    private String key;
    private String domain;

    private long hostTime = -1;
    private long creationTime = System.currentTimeMillis();
    private long expireTime = -1;

    public Cookie() {
        host = "";
        key = "";
        value = "";
    }

    public Cookie(final String host, final String key, final String value) {
        this.host = host;
        this.key = key;
        this.value = value;
    }

    /* compares host and key */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        final Cookie other = (Cookie) obj;
        if (host == null) {
            if (other.host != null) { return false; }
        } else if (!host.equalsIgnoreCase(other.host)) { return false; }
        if (key == null) {
            if (other.key != null) { return false; }
        } else if (!key.equalsIgnoreCase(other.key)) { return false; }
        return true;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getDomain() {
        return domain;
    }

    public long getExpireDate() {
        return expireTime;
    }

    public String getHost() {
        return host;
    }

    public long getHostTime() {
        return hostTime;
    }

    public String getKey() {
        return key;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public boolean isExpired() {
        if (expireTime == -1) {
            // System.out.println("isexpired: no expireDate found! " + this.host
            // + " " + this.key);
            return false;
        }
        if (hostTime == -1) {
            Log.L.severe("Cookie: no HostTime found! ExpireStatus cannot be checked " + host + " " + key);
            return false;
        } else {
            final long check = System.currentTimeMillis() - creationTime + hostTime;
            // System.out.println(this.host + " " + this.key + " " +
            // this.creationTime + " " + this.hostTime + " " + this.expireTime +
            // " " + check);
            // if (check > this.expireTime) {
            // // System.out.println("Expired: " + this.host + " " + this.key);
            // return true;
            // } else
            // return false;
            return check > expireTime;
        }
    }

    public void setCreationTime(final long time) {
        creationTime = time;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public void setExpireDate(final long time) {
        expireTime = time;
    }

    public void setExpires(final String expires) {
        if (expires == null) {
            expireTime = -1;
            // System.out.println("setExpire: Cookie: no expireDate found! " +
            // this.host + " " + this.key);
            return;
        }
        Date expireDate = null;
        for (final String format : Cookie.dateformats) {
            try {
                final SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.UK);
                sdf.setLenient(false);
                expireDate = sdf.parse(expires);
                break;
            } catch (final Exception e2) {
            }
        }
        if (expireDate != null) {
            expireTime = expireDate.getTime();
            return;
        }
        expireTime = -1;
        Log.L.severe("Cookie: no Format for " + expires + " found!");
        return;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setHostTime(final long time) {
        hostTime = time;
    }

    public void setHostTime(final String date) {
        if (date == null) {
            hostTime = -1;
            // System.out.println("Cookie: no HostTime found! " + this.host +
            // " " + this.key);
            return;
        }
        Date responseDate = null;
        for (final String format : Cookie.dateformats) {
            try {
                final SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.UK);
                sdf.setLenient(false);
                responseDate = sdf.parse(date);
                break;
            } catch (final Exception e2) {
            }
        }
        if (responseDate != null) {
            hostTime = responseDate.getTime();
            return;
        }
        hostTime = -1;
        Log.L.severe("Cookie: no Format for " + date + " found!");
        return;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return key + "=" + value + " @" + host;
    }

    // /* compares host and key */
    // public boolean equals(final Cookie cookie2) {
    // if (cookie2 == this) return true;
    // if (!cookie2.getHost().equalsIgnoreCase(this.getHost())) return false;
    // if (!cookie2.getKey().equalsIgnoreCase(this.getKey())) return false;
    // return true;
    // }

    public void update(final Cookie cookie2) {
        setCreationTime(cookie2.creationTime);
        setExpireDate(cookie2.expireTime);
        setValue(cookie2.value);
        // this.setHostTime(cookie2.getCreationTime());
        this.setHostTime(cookie2.creationTime); // ???
    }

}
