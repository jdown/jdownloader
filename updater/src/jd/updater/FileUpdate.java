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

package jd.updater;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.utils.Hash;
import org.appwork.utils.IO;

public class FileUpdate {

    private final String            hash;
    private final ArrayList<Server> serverList = new ArrayList<Server>();

    private Server                  currentServer;

    private final String            relURL;

    private final UpdaterController updaterController;
    private UpdateOptions           options;

    public FileUpdate(final String serverString, final String hash, final UpdaterController updaterController) {
        this.updaterController = updaterController;
        this.hash = hash;
        relURL = serverString;
        // serverList.addAll(updaterController.getRepoList());

    }

    public boolean equals() {
        if (exists()) {
            final String localHash = getLocalHash();
            if (localHash != null) { return localHash.equalsIgnoreCase(hash); }
        }
        return false;
    }

    public boolean exists() {

        return getLocalFile().exists() || getUpdateFilepath().exists();

    }

    public File getLocalFile() {
        return updaterController.getOptions().getWorkingDir() != null ? new File(updaterController.getOptions().getWorkingDir(), getRelURL()) : updaterController.getResource(getRelURL());
    }

    private String getLocalHash() {
        return Hash.getMD5(getUpdateFilepath().exists() ? getUpdateFilepath() : getLocalFile());
    }

    public String getRelURL() {
        return relURL;
    }

    public String getRemoteHash() {
        return hash;
    }

    /**
     * Returns the local tmp file this is workingdir/update/
     * 
     * @return
     */
    public File getUpdateFilepath() {

        return new File((updaterController.getOptions().getWorkingDir() != null ? new File(updaterController.getOptions().getWorkingDir(), "update") : updaterController.getResource("update")) + "/" + updaterController.getBranch() + getRelURL());
    }

    /**
     * as long as there are valid servers, this method returns a valid url.
     * 
     * @return
     */
    private String getURL() {
        Server serv;

        serv = Server.selectServer(serverList);
        currentServer = serv;
        serverList.remove(serv);
        return mergeUrl(serv.getPath(), relURL);

    }

    public boolean hasServer() {
        return !serverList.isEmpty();
    }

    public boolean isClassLoaded() {
        if (updaterController.getOptions().getPluginClassloader() == null) { throw new RuntimeException("No PLuginclassloader available"); }

        java.lang.reflect.Method m;
        try {

            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });

            m.setAccessible(true);

            final Object test1 = m.invoke(updaterController.getOptions().getPluginClassloader(), getRelURL().replace("/", ".").substring(0, getRelURL().length() - 6));
            return test1 != null;
        } catch (final SecurityException e) {
            e.printStackTrace();
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;

    }

    public boolean isUpdateRequired() {

        if (getRelURL().endsWith(".extract")) {
            // check if workspace already has extracted version
            File extractFile = new File(getLocalFile() + ".md5");
            try {
                if (extractFile.exists()) {
                    final String hashOfOrgExtractFile = IO.readFileToString(extractFile);
                    if (hashOfOrgExtractFile.equals(hash)) {
                        // everything is p2date
                        return false;
                    }
                }
            } catch (final IOException e) {

            }
            // check if we already have correct extracted files in updatedir

            extractFile = new File(getUpdateFilepath() + ".md5");
            try {
                if (extractFile.exists()) {
                    final String hashOfOrgExtractFile = IO.readFileToString(extractFile);
                    // ea38f021b6f66ea47c76d88baa1ace83

                    if (hashOfOrgExtractFile.equals(hash)) {
                        // everything is p2date
                        return false;
                    }
                }
            } catch (final IOException e) {

            }
            // check if correct extract file is in updatedir

            final String localTmpHash = Hash.getMD5(getUpdateFilepath());
            if (getRemoteHash().equals(localTmpHash)) {
                // extract file is in updatedir. probably has not been extracted
                // yet.
                // delete md5 hash;
                new File(getUpdateFilepath() + ".md5").delete();
                return false;
            }
            return true;

        } else {
            if (!exists()) {
                return options == null ? true : options.isUpdateAllowed(this);
            } else if (!equals()) {
                //
                return options == null ? true : options.isUpdateAllowed(this);
                //
            }
            return false;
        }
    }

    public String mergeUrl(final String server, final String file) {
        final String ret = server.endsWith("/") || file.charAt(0) == '/' ? server + file : server + "/" + file;
        return ret.replaceAll("//", "/").replaceAll("http:/", "http://");
    }

    public boolean needsRestart() {
        final String hash = Hash.getMD5(getUpdateFilepath());
        return hash == null ? false : hash.equalsIgnoreCase(hash);
    }

    public void reset(final ArrayList<Server> availableServers) {
        serverList.clear();
        serverList.addAll(availableServers);
    }

    public void setUpdateOptions(final UpdateOptions options) {
        this.options = options;

    }

    @Override
    public String toString() {
        return getRelURL();
    }

    /**
     * verwendet alle server bis die datei gefunden wurde
     * 
     * @return
     * @throws UpdateException
     * @throws InterruptedException
     * @throws IOException
     */
    public void update(final ArrayList<Server> availableServers) throws UpdateException, InterruptedException {

        long startTime, endTime;

        // for (int retry = 0; retry < 3; retry++) {
        if (availableServers == null || availableServers.isEmpty()) { throw new UpdateException("Could not update file: " + this); }
        reset(availableServers);
        while (hasServer()) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            String url = getURL();

            final File tmpFile = new File(getUpdateFilepath().getAbsolutePath() + ".download");
            // delete tmp file
            tmpFile.delete();
            final File updatetmp = getUpdateFilepath();
            if (updatetmp.exists() && Hash.getMD5(updatetmp).equals(hash)) {
                // nothing
                return;
            } else {
                // remove local tmp file, since it does either not exist or
                // is invalid
                getUpdateFilepath().delete();
                if (getUpdateFilepath().exists()) { throw new UpdateException("Could not delete Updatefile: " + getUpdateFilepath()); }
                if (url.contains("?")) {
                    url += "&r=" + System.currentTimeMillis();
                } else {
                    url += "?r=" + System.currentTimeMillis();
                }

                startTime = System.currentTimeMillis();
                URLConnectionAdapter con = null;
                int response = -1;
                try {
                    // Open connection
                    System.out.println("Download " + url);
                    con = updaterController.getBr().openGetConnection(url);
                    endTime = System.currentTimeMillis();
                    response = con.getResponseCode();
                    currentServer.setRequestTime(endTime - startTime);
                } catch (final Exception e) {
                    // Failed connection.retry next server
                    currentServer.setRequestTime(100000l);
                    try {
                        con.disconnect();
                    } catch (final Exception e1) {
                    }
                    updaterController.errorWait(e);
                    continue;
                }
                // connection estabilished
                if (response != 200) {
                    // responscode has errors. Try next server
                    currentServer.setRequestTime(500000l);
                    try {
                        con.disconnect();
                    } catch (final Exception e) {
                    }
                    updaterController.errorWait(new Exception("Responsecode: " + response));
                    continue;

                }
                // connection is ok. download now to *.,tmp file
                try {
                    Browser.download(tmpFile, con);
                } catch (final Exception e) {
                    // DOwnload failed. try next server
                    currentServer.setRequestTime(100000l);
                    try {
                        con.disconnect();
                    } catch (final Exception e1) {
                    }
                    updaterController.errorWait(e);
                    continue;
                }
                // Download is ok. b
                try {
                    con.disconnect();
                } catch (final Exception e) {
                }
            }

            final String downloadedHash = Hash.getMD5(tmpFile);
            if (downloadedHash != null && downloadedHash.equalsIgnoreCase(hash)) {

                // move to update folder
                getUpdateFilepath().delete();

                getUpdateFilepath().getParentFile().mkdirs();
                if (!tmpFile.renameTo(getUpdateFilepath())) { throw new UpdateException("Could not rename " + tmpFile + " to " + getUpdateFilepath()); }
                return;
            } else {
                tmpFile.delete();
                updaterController.errorWait(new Exception("Hash Mismatch " + url));
                continue;
            }
        }
        throw new UpdateException("Update failed on all available servers");

    }

    // }
}
