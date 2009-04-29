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

package jd.plugins;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import jd.config.Property;
import jd.event.JDBroadcaster;
import jd.nutils.io.JDIO;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

class FilePackageBroadcaster extends JDBroadcaster<FilePackageListener, FilePackageEvent> {

    // @Override
    protected void fireEvent(FilePackageListener listener, FilePackageEvent event) {
        listener.onFilePackageEvent(event);
    }

}

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author JD-Team
 */
public class FilePackage extends Property implements Serializable, DownloadLinkListener, FilePackageListener {

    // Zählt die instanzierungen durch um eine ID zu erstellen
    private static int counter = 0;

    private static final long serialVersionUID = -8859842964299890820L;

    private static final long UPDATE_INTERVAL = 2000;

    private String comment;

    private String downloadDirectory;

    private Vector<DownloadLink> downloadLinks;
    private transient static FilePackage FP = null;

    public static FilePackage getDefaultFilePackage() {
        if (FP == null) {
            FP = new FilePackage();
            FP.setName(JDLocale.L("controller.packages.defaultname", "various"));
        }
        return FP;
    }

    // Eindeutige PaketID
    private String id;

    private boolean lastSort = false;

    private int linksFailed;

    private int linksFinished;

    private int linksInProgress;

    private String name = null;

    private String password;
    private String dlpassword;
    private boolean extractAfterDownload = true;

    private long totalBytesLoaded_v2;

    private long totalDownloadSpeed_v2;

    private long totalEstimatedPackageSize_v2;

    private long updateTime;

    private long updateTime1;

    private boolean isFinished;

    private transient FilePackageBroadcaster broadcaster = new FilePackageBroadcaster();

    private Integer links_Disabled;

    private String ListHoster = null;

    public synchronized JDBroadcaster<FilePackageListener, FilePackageEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new FilePackageBroadcaster();
        return broadcaster;
    }

    public static FilePackage getInstance() {
        return new FilePackage();
    }

    private FilePackage() {
        links_Disabled = new Integer(0);
        downloadDirectory = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
        counter++;
        id = System.currentTimeMillis() + "_" + counter;
        downloadLinks = new Vector<DownloadLink>();
        getBroadcaster().addListener(this);
    }

    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        /* nach dem deserialisieren sollen die transienten neu geholt werden */
        stream.defaultReadObject();
        links_Disabled = new Integer(0);
        getBroadcaster().addListener(this);
    }

    /**
     * Diese Methode speichert Paketinformationen ab (falls die Datei noch nicht
     * bereits besteht)
     */

    public void add(DownloadLink link) {
        synchronized (downloadLinks) {
            if (!downloadLinks.contains(link)) {
                downloadLinks.add(link);
                link.setFilePackage(this);
                if (!link.isEnabled()) synchronized (links_Disabled) {
                    links_Disabled++;
                }
                link.getBroadcaster().addListener(this);
                getBroadcaster().fireEvent(new FilePackageEvent(this, FilePackageEvent.DOWNLOADLINK_ADDED, link));
            }
        }
    }

    public void add(int index, DownloadLink link) {
        synchronized (downloadLinks) {
            if (downloadLinks.contains(link)) {
                downloadLinks.remove(link);
                if (index > downloadLinks.size() - 1) {
                    downloadLinks.add(link);
                } else if (index < 0) {
                    downloadLinks.add(0, link);
                } else
                    downloadLinks.add(index, link);
                getBroadcaster().fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_UPDATE));
            } else {
                if (index > downloadLinks.size() - 1) {
                    downloadLinks.add(link);
                } else if (index < 0) {
                    downloadLinks.add(0, link);
                } else
                    downloadLinks.add(index, link);
                if (!link.isEnabled()) synchronized (links_Disabled) {
                    links_Disabled++;
                }
                link.setFilePackage(this);
                link.getBroadcaster().addListener(this);
                getBroadcaster().fireEvent(new FilePackageEvent(this, FilePackageEvent.DOWNLOADLINK_ADDED, link));
            }
        }
    }

    public void addAll(Vector<DownloadLink> links) {
        synchronized (downloadLinks) {
            for (DownloadLink dl : links) {
                add(dl);
            }
        }
    }

    public boolean isExtractAfterDownload() {
        return extractAfterDownload;
    }

    public void setExtractAfterDownload(boolean extractAfterDownload) {
        this.extractAfterDownload = extractAfterDownload;
    }

    public void addAllAt(Vector<DownloadLink> links, int index) {
        synchronized (downloadLinks) {
            for (int i = 0; i < links.size(); i++) {
                add(index + i, links.get(i));
            }
        }
    }

    public boolean contains(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.contains(link);
        }
    }

    public DownloadLink get(int index) {
        synchronized (downloadLinks) {
            return downloadLinks.get(index);
        }
    }

    /**
     * @return Gibt den Kommentar ab den der user im Linkgrabber zu diesem Paket
     *         abgegeben hat
     */
    public String getComment() {
        updateData();
        return comment == null ? "" : comment;
    }

    /**
     * @return Gibt den Downloadpfad zurück den der user für dieses paket
     *         festgelegt hat
     */
    public String getDownloadDirectory() {
        return downloadDirectory == null ? JDUtilities.getConfiguration().getDefaultDownloadDirectory() : downloadDirectory;
    }

    /**
     * @return Gibt nur den namen des Downloadverzeichnisses zurück. ACHTUNG! es
     *         wird nur der Directory-NAME zurückgegeben, nicht der ganze Pfad
     */
    public String getDownloadDirectoryName() {
        if (!hasDownloadDirectory()) { return "."; }
        return new File(downloadDirectory).getName();
    }

    public Vector<DownloadLink> getDownloadLinks() {
        synchronized (downloadLinks) {
            return downloadLinks;
        }
    }

    /**
     * Gibt die vorraussichtlich verbleibende Downloadzeit für dieses paket
     * zurück
     * 
     * @return
     */
    public long getETA() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        if (totalDownloadSpeed_v2 / 1024 == 0) { return -1; }
        return (Math.max(totalBytesLoaded_v2, totalEstimatedPackageSize_v2) - totalBytesLoaded_v2) / (totalDownloadSpeed_v2);
    }

    public String getId() {
        return id;
    }

    /**
     * Gibt die Anzahl der fehlerhaften Links zurück
     * 
     * @return
     */
    public int getLinksFailed() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return linksFailed;
    }

    /**
     * Gibt die Anzahl der fertiggestellten Links zurück
     * 
     * @return
     */
    public int getLinksFinished() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return linksFinished;
    }

    /**
     * Gibt zurück wieviele Links gerade in Bearbeitung sind
     * 
     * @return
     */
    public int getLinksInProgress() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return linksInProgress;
    }

    public boolean isFinished() {
        if (System.currentTimeMillis() - updateTime1 > UPDATE_INTERVAL) {
            updateTime1 = System.currentTimeMillis();
            boolean value = true;
            if (linksFinished > 0) {
                synchronized (downloadLinks) {
                    for (DownloadLink lk : downloadLinks) {
                        if (!lk.getLinkStatus().hasStatus(LinkStatus.FINISHED) && lk.isEnabled()) {
                            value = false;
                            break;
                        }
                    }
                }
            } else {
                value = false;
            }
            isFinished = value;
        }
        return isFinished;
    }

    public String getName() {
        if (name == null) return "";
        return name;
    }

    /**
     * 
     * @return Gibt das Archivpasswort zurück das der User für dieses paket
     *         angegeben hat
     */
    public String getPassword() {
        updateData();
        return password == null ? "" : password;
    }

    public String getDLPassword() {
        updateData();
        return dlpassword == null ? "" : dlpassword;
    }

    public void updateData() {
        synchronized (downloadLinks) {
            String password = this.password;
            StringBuilder comment = new StringBuilder(this.comment == null ? "" : this.comment);

            String[] pws = JDUtilities.passwordStringToArray(password);
            Vector<String> pwList = new Vector<String>();
            for (String element : pws) {
                pwList.add(element);
            }

            Vector<String> dlpwList = new Vector<String>();

            for (DownloadLink element : downloadLinks) {
                pws = JDUtilities.passwordStringToArray(element.getSourcePluginPassword());

                String dlpw = element.getStringProperty("pass", null);
                if (dlpw != null && !dlpwList.contains(dlpw)) dlpwList.add(dlpw);
                for (String element2 : pws) {
                    if (pwList.indexOf(element2) < 0) {
                        pwList.add(element2);
                    }
                }

                String newComment = element.getSourcePluginComment();
                if (newComment != null && comment.indexOf(newComment) < 0) {
                    comment.append("|");
                    comment.append(newComment);
                }
            }

            String cmt = comment.toString();
            if (cmt.startsWith("|")) {
                cmt = cmt.substring(1);
            }
            this.comment = cmt;
            this.password = JDUtilities.passwordArrayToString(pwList.toArray(new String[pwList.size()]));
            this.dlpassword = JDUtilities.passwordArrayToString(dlpwList.toArray(new String[dlpwList.size()]));
        }
    }

    /**
     * Diese Werte werden durch itterieren durch die downloadListe ermittelt. Um
     * dies nicht zu oft machen zu müssen geschiet das intervalartig
     * 
     * @return
     */
    /**
     * Gibt den Fortschritt des pakets in prozent zurück
     */
    public double getPercent() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return 100.0 * totalBytesLoaded_v2 / Math.max(1, Math.max(totalBytesLoaded_v2, totalEstimatedPackageSize_v2));
    }

    /**
     * Gibt die Anzahl der Verbleibenden Links zurück. Wurden alle Links bereits
     * abgearbeitet gibt diese Methode 0 zurück Da die Methode alle Links
     * durchläuft sollte sie aus Performancegründen mit bedacht eingesetzt
     * werden
     */
    public int getRemainingLinks() {
        updateCollectives();
        return size() - linksFinished;

    }

    // Gibt die erste gefundene sfv datei im Paket zurück
    public DownloadLink getSFV() {
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (dl.getFileOutput().toLowerCase().endsWith(".sfv")) return dl;
            }
        }
        return null;
    }

    /**
     * Gibt die aktuelle Downloadgeschwinigkeit des Pakets zurück
     * 
     * @return
     */
    public long getTotalDownloadSpeed() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return totalDownloadSpeed_v2;
    }

    /**
     * Gibt die geschätzte Gesamtgröße des Pakets zurück
     * 
     * @return
     */
    public long getTotalEstimatedPackageSize() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return Math.max(totalBytesLoaded_v2, totalEstimatedPackageSize_v2);
    }

    /**
     * Gibt zurück wieviele Bytes ingesamt schon in diesem Paket geladen wurden
     * 
     * @return
     */
    public long getTotalKBLoaded() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return totalBytesLoaded_v2;
    }

    /**
     * 
     * @return True/false, je nach dem ob ein Kommentar gespeichert ist
     */
    public boolean hasComment() {
        return comment != null && comment.length() > 0;
    }

    /**
     * @return True/false, je nach dem ob ein Downloadirectory festgelegt wurde
     */
    public boolean hasDownloadDirectory() {
        return downloadDirectory != null && downloadDirectory.length() > 0;
    }

    /**
     * @return true/false, je nachdem ob ein Passwort festgelegt wurde
     *         (archivpasswort)
     */
    public boolean hasPassword() {
        return password != null && password.length() > 0;
    }

    public int indexOf(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.indexOf(link);
        }
    }

    public DownloadLink getLinkAfter(DownloadLink link) {
        synchronized (downloadLinks) {
            int index = indexOf(link);
            if (index == -1 || index + 1 > downloadLinks.size() - 1) return null;
            return get(index + 1);
        }
    }

    public DownloadLink getLinkBefore(DownloadLink link) {
        synchronized (downloadLinks) {
            int index = indexOf(link);
            if (index == -1 || index - 1 < 0) return null;
            return get(index - 1);
        }
    }

    public DownloadLink lastElement() {
        synchronized (downloadLinks) {
            return downloadLinks.lastElement();
        }
    }

    public void remove(DownloadLink link) {
        if (link == null) return;
        synchronized (downloadLinks) {
            boolean ret = downloadLinks.remove(link);
            if (ret) {
                if (!link.isEnabled()) synchronized (links_Disabled) {
                    links_Disabled--;
                }
                link.getBroadcaster().removeListener(this);
                link.setFilePackage(null);
                getBroadcaster().fireEvent(new FilePackageEvent(this, FilePackageEvent.DOWNLOADLINK_REMOVED, link));
                if (downloadLinks.size() == 0) getBroadcaster().fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_EMPTY));
            }
        }
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDownloadDirectory(String subFolder) {
        downloadDirectory = JDUtilities.removeEndingPoints(subFolder);
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(getDefaultFilePackage().name);
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int size() {
        synchronized (downloadLinks) {
            return downloadLinks.size();
        }
    }

    public void abortDownload() {
        synchronized (downloadLinks) {
            for (DownloadLink downloadLink : downloadLinks) {
                downloadLink.setAborted(true);
            }
        }
    }

    public Vector<DownloadLink> getLinksWithStatus(int status) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (dl.getLinkStatus().hasStatus(status)) {
                    ret.add(dl);
                }
            }
        }
        return ret;
    }

    public String getHoster() {
        if (ListHoster == null) {
            Set<String> hosterList = new HashSet<String>();
            synchronized (downloadLinks) {
                for (DownloadLink dl : downloadLinks) {
                    hosterList.add(dl.getHost());
                }
            }
            ListHoster = hosterList.toString();
        }
        return ListHoster;
    }

    public void sort(final int col) {
        lastSort = !lastSort;
        synchronized (downloadLinks) {

            Collections.sort(downloadLinks, new Comparator<DownloadLink>() {

                public int compare(DownloadLink a, DownloadLink b) {
                    if (a.getName().endsWith(".sfv")) { return -1; }
                    if (b.getName().endsWith(".sfv")) { return 1; }
                    DownloadLink aa = a;
                    DownloadLink bb = b;
                    if (lastSort) {
                        aa = b;
                        bb = a;
                    }
                    switch (col) {
                    case 1:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 2:
                        return aa.getHost().compareToIgnoreCase(bb.getHost());
                    case 3:
                        if (aa.isAvailabilityChecked() && bb.isAvailabilityChecked()) {
                            return (aa.isAvailable() && !bb.isAvailable()) ? 1 : -1;
                        } else
                            return -1;
                    case 4:
                        return aa.getPercent() > bb.getPercent() ? 1 : -1;
                    default:
                        return -1;
                    }
                }
            });
        }
        getBroadcaster().fireEvent(new FilePackageEvent(this, FilePackageEvent.FILEPACKAGE_UPDATE));
    }

    /**
     * Alles undokumentiert, da selbsterklärend
     */
    // @Override
    public String toString() {
        return this.getName() + " " + this.size();
    }

    public void updateCollectives() {
        synchronized (downloadLinks) {

            totalEstimatedPackageSize_v2 = 0;
            totalDownloadSpeed_v2 = 0;
            linksFinished = 0;
            linksInProgress = 0;
            linksFailed = 0;
            totalBytesLoaded_v2 = 0;
            long avg = 0;
            DownloadLink next;
            int i = 0;

            for (Iterator<DownloadLink> it = downloadLinks.iterator(); it.hasNext();) {
                next = it.next();

                if (next.getDownloadSize() > 0) {

                    if (next.isEnabled()) {
                        totalEstimatedPackageSize_v2 += next.getDownloadSize();
                    }

                    avg = (i * avg + next.getDownloadSize()) / (i + 1);
                    // logger.info(i+"+ "+next.getDownloadMax()/1024+" kb
                    // avg:"+avg+" = +"+totalEstimatedPackageSize);
                    i++;
                } else {
                    if (it.hasNext()) {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize_v2 += avg;
                        }

                        // logger.info(i+"+avg "+avg+" kb
                        // =+"+totalEstimatedPackageSize);

                    } else {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize_v2 += avg / 2;
                            // logger.info(i+"+avg "+(avg/2)+" kb
                            // =+"+totalEstimatedPackageSize);
                        }
                    }
                }

                totalDownloadSpeed_v2 += Math.max(0, next.getDownloadSpeed());
                if (next.isEnabled()) {
                    totalBytesLoaded_v2 += next.getDownloadCurrent();
                }
                linksInProgress += next.getLinkStatus().isPluginActive() ? 1 : 0;
                linksFinished += next.getLinkStatus().hasStatus(LinkStatus.FINISHED) ? 1 : 0;
                if (next.getLinkStatus().isFailed()) {
                    linksFailed++;
                }
            }
        }
        updateTime = System.currentTimeMillis();
    }

    public int getLinksDisabled() {
        synchronized (links_Disabled) {
            return links_Disabled;
        }
    }

    public boolean isEnabled() {
        if (downloadLinks.size() <= getLinksDisabled()) return false;
        return true;
    }

    public void update_linksDisabled() {
        synchronized (links_Disabled) {
            links_Disabled = 0;
            synchronized (downloadLinks) {
                for (DownloadLink dl : downloadLinks) {
                    if (!dl.isEnabled()) links_Disabled++;
                }
            }
        }
    }

    public void handle_DownloadLinkEvent(DownloadLinkEvent event) {
        synchronized (links_Disabled) {
            switch (event.getID()) {
            case DownloadLinkEvent.DISABLED:
                links_Disabled++;
                break;
            case DownloadLinkEvent.ENABLED:
                links_Disabled--;
                break;
            }
        }
    }

    public void onFilePackageEvent(FilePackageEvent event) {
        synchronized (downloadLinks) {
            switch (event.getID()) {
            case FilePackageEvent.DOWNLOADLINK_ADDED:
            case FilePackageEvent.DOWNLOADLINK_REMOVED:
                Set<String> hosterList = new HashSet<String>();
                synchronized (downloadLinks) {
                    for (DownloadLink dl : downloadLinks) {
                        hosterList.add(dl.getHost());
                    }
                }
                ListHoster = hosterList.toString();
                break;
            }
        }

    }
}
