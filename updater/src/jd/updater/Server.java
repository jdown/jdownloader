//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.updater;

import java.io.Serializable;
import java.util.ArrayList;

public class Server implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 701200615385983L;

    /**
     * selects depending on the percents a ranodm server
     * 
     * @param list
     * @return
     */
    public static Server selectServer(final ArrayList<Server> list) {

        final int rand = (int) (Math.random() * 100);
        int total = 0;
        Server ret = null;
        for (final Server s : list) {
            if (s.getPercent() <= 0) { return Server.selectServerByRequestTime(list); }
            ret = s;
            total += s.getPercent();
            if (rand <= total && rand > total - s.getPercent()) {
                break;
            }
        }

        return ret;

    }

    /**
     * returns the Server with the best average Requesttime
     * 
     * @param list
     * @return
     */
    private static Server selectServerByRequestTime(final ArrayList<Server> list) {

        Server ret = null;
        for (final Server s : list) {
            if (ret == null || s.getRequestTime() < ret.getRequestTime()) {
                ret = s;
            }

        }

        return ret;
    }

    private int    percent;
    private String path;

    private long   requestTime  = 0;

    private int    requestCount = 0;

    public Server(final int percent, final String host) {
        this.percent = percent;
        path = host;
    }

    public String getPath() {
        return path;
    }

    public int getPercent() {
        return percent;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void setPercent(final int percent) {
        this.percent = percent;
    }

    /**
     * adds the latest requesttime and creates an average from all requesttimes
     * added so far
     * 
     * @param l
     */
    public void setRequestTime(final long l) {
        requestTime = (requestTime * requestCount + l) / (requestCount + 1);
        requestCount++;

    }

    @Override
    public String toString() {
        return path + " (" + percent + "%)";
    }
}