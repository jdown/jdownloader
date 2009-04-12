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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.controlling.DownloadController;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.http.Browser;
import jd.nutils.JDImage;
import jd.parser.Regex;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {

    public PluginForHost(PluginWrapper wrapper) {
        super(wrapper);

    }

    /**
     * 
     * @param captchaAddress
     * @param downloadLink
     * @return
     * @throws IOException
     * @throws PluginException
     * @throws InterruptedException
     */
    public String getCaptchaCode(String captchaAddress, DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        File captchaFile = this.getLocalCaptchaFile(this);
        try {
            Browser.download(captchaFile, br.openGetConnection(captchaAddress));
        } catch (Exception e) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
        return captchaCode;

    }

    private static final String AGB_CHECKED = "AGB_CHECKED";
    private static final String CONFIGNAME = "pluginsForHost";
    private static int currentConnections = 0;

 

    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
    private static HashMap<Class<? extends PluginForHost>, Long> HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();

    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    protected DownloadInterface dl = null;
    private int maxConnections = 50;

    public static final String PROPERTY_PREMIUM = "PREMIUM";

    private static HashMap<Class<? extends PluginForHost>, Long> LAST_CONNECTION_TIME = new HashMap<Class<? extends PluginForHost>, Long>();
    private static HashMap<Class<? extends PluginForHost>, Long> LAST_STARTED_TIME = new HashMap<Class<? extends PluginForHost>, Long>();
    private Long WAIT_BETWEEN_STARTS = 0L;

    private boolean enablePremium = false;

    private boolean AccountwithoutUsername = false;

    private String premiumurl = null;

    public boolean checkLinks(DownloadLink[] urls) {
        return false;
    }

    @Override
    public void clean() {
        dl = null;
        super.clean();
    }

    protected int waitForFreeConnection(DownloadLink downloadLink) throws InterruptedException {
        int free;
        while ((free = this.getMaxConnections() - getCurrentConnections()) <= 0) {
            Thread.sleep(1000);
            downloadLink.getLinkStatus().setStatusText(JDLocale.LF("download.system.waitForconnection", "Cur. %s/%s connections...waiting", getCurrentConnections() + "", this.getMaxConnections() + ""));
            downloadLink.requestGuiUpdate();
        }
        return free;

    }

    protected void setBrowserExclusive() {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 1) {
            SimpleGUI.showConfigDialog(SimpleGUI.CURRENTGUI, config);
            return;
        }

        ArrayList<Account> accounts = getPremiumAccounts();
        if (e.getID() >= 200) {
            int accountID = e.getID() - 200;
            Account account = accounts.get(accountID);
            JDUtilities.getGUI().showAccountInformation(this, account);
        } else if (e.getID() >= 100) {
            int accountID = e.getID() - 100;
            Account account = accounts.get(accountID);

            account.setEnabled(!account.isEnabled());
            getPluginConfig().save();
        }

    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        if (account.getProperty(AccountInfo.PARAM_INSTANCE) != null) {
            AccountInfo ai = (AccountInfo) account.getProperty(AccountInfo.PARAM_INSTANCE);
            if ((System.currentTimeMillis() - ai.getCreateTime()) < 5 * 60 * 1000) { return ai; }
        }
        AccountInfo ret = fetchAccountInfo(account);
        if (ret == null) return null;
        account.setProperty(AccountInfo.PARAM_INSTANCE, ret);

        return ret;
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getAccountwithoutUsername() {
        return AccountwithoutUsername;
    }

    public void setAccountwithoutUsername(boolean b) {
        AccountwithoutUsername = b;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menuList = new ArrayList<MenuItem>();
        if (!this.enablePremium) return null;
        MenuItem account;
        MenuItem m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.menu.configs", "Configuration"), 1);
        m.setActionListener(this);
        MenuItem premium = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.menu.accounts", "Accounts"), 0);
        menuList.add(m);
        ArrayList<Account> accounts = getPremiumAccounts();

        int i = 1;
        int c = 0;
        for (Account a : accounts) {
            if (a == null) continue;
            try {
                c++;
                if (getAccountwithoutUsername()) {
                    if (a.getPass() == null || a.getPass().trim().length() == 0) continue;
                    account = new MenuItem(MenuItem.CONTAINER, i++ + ". " + "Account " + (i - 1), 0);
                } else {

                    if (a.getUser() == null || a.getUser().trim().length() == 0) continue;
                    account = new MenuItem(MenuItem.CONTAINER, i++ + ". " + a.getUser(), 0);
                    m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.menu.enable_premium", "Aktivieren"), 100 + c - 1);
                    m.setSelected(a.isEnabled());
                    m.setActionListener(this);
                    account.addMenuItem(m);

                    m = new MenuItem(JDLocale.L("plugins.menu.premiumInfo", "Accountinformationen abrufen"), 200 + c - 1);
                    m.setActionListener(this);
                    account.addMenuItem(m);
                    premium.addMenuItem(account);

                }

            } catch (Exception e) {
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
            }
        }
        if (premium.getSize() != 0) {
            menuList.add(premium);
        } else {
            menuList.add(new MenuItem(JDLocale.L("plugins.menu.noAccountsYet", "No accounts yet"), -1));
        }

        return menuList;

    }

    public abstract String getAGBLink();

    protected void enablePremium(String url) {
        this.enablePremium(JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getIntegerProperty(SimpleGuiConstants.PARAM_NUM_PREMIUM_CONFIG_FIELDS, 5), url);
    }

    protected void enablePremium() {
        this.enablePremium(JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME).getIntegerProperty(SimpleGuiConstants.PARAM_NUM_PREMIUM_CONFIG_FIELDS, 5), null);
    }

    protected void enablePremium(int size, String url) {
        if (size <= 0) return;
        this.premiumurl = url;

        enablePremium = true;
        ConfigEntry cfg;

        ConfigContainer premiumConfig = new ConfigContainer(this, JDLocale.L("plugins.hoster.premiumtab", "Premium Einstellungen"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, premiumConfig));

        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PREMIUMPANEL, getPluginConfig(), PROPERTY_PREMIUM, size));
        cfg.setActionListener(this);
        cfg.setDefaultValue(new ArrayList<Account>());

    }

    public int getChunksPerFile() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2);
    }

    public synchronized int getCurrentConnections() {
        return currentConnections;
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data, FilePackage fp) {

        Vector<DownloadLink> links = null;
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        if (hits != null && hits.length > 0) {
            links = new Vector<DownloadLink>();
            for (String file : hits) {
                while (file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }

                try {
                    // Zwecks Multidownload braucht jeder Link seine eigene
                    // Plugininstanz
                    PluginForHost plg = (PluginForHost) wrapper.getNewPluginInstance();
                    DownloadLink link = new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    links.add(link);
                    if (fp != null) {
                        link.setFilePackage(fp);
                    }

                } catch (IllegalArgumentException e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                } catch (SecurityException e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                }
            }
        }
        return links;
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     * @throws IOException
     */
    public abstract boolean getFileInformation(DownloadLink parameter) throws Exception;

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param parameter
     * @return
     */
    public String getFileInformationString(DownloadLink parameter) {
        return "";
    }

    public synchronized int getFreeConnections() {
        return Math.max(1, this.getMaxConnections() - currentConnections);
    }

    /**
     * Wird nicht gebraucht muss aber implementiert werden.
     */

    @Override
    public String getLinkName() {

        return null;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxRetries() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_RETRIES, 3);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    public boolean ignoreHosterWaittime(DownloadLink link) {
        if (!this.enablePremium || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) { return false; }

        Account currentAccount = null;

        ArrayList<Account> accounts = getPremiumAccounts();

        synchronized (accounts) {
            for (int i = 0; i < accounts.size(); i++) {
                if (!accounts.get(i).isTempDisabled() && accounts.get(i).isEnabled()) {

                    currentAccount = accounts.get(i);
                    break;
                }
            }
        }
        if (currentAccount != null) return true;
        return false;
    }

    public int getMaxSimultanDownloadNum(DownloadLink link) {
        return ignoreHosterWaittime(link) ? getMaxSimultanPremiumDownloadNum() : getMaxSimultanFreeDownloadNum();
    }

    public long getRemainingHosterWaittime() {
        if (!HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (HOSTER_WAIT_UNTIL_TIMES.get(this.getClass()) - System.currentTimeMillis()));
    }

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(this.getClass())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(this.getClass(), time);
    }

    public synchronized long getLastConnectionTime() {
        if (!LAST_CONNECTION_TIME.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (LAST_CONNECTION_TIME.get(this.getClass())));
    }

    public synchronized void putLastConnectionTime(long time) {
        LAST_CONNECTION_TIME.put(this.getClass(), time);
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        link.getLinkStatus().addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.getLinkStatus().setErrorMessage("Plugin has no handlePremium Method!");
    }

    public boolean canResume(DownloadLink link) {
        /*
         * TODO: hier sollte resumefähigkeit geprüft werden und nicht ob der
         * hoster premium accoutns hat
         */
        return ignoreHosterWaittime(link) ? true : false;
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    public void handle(DownloadLink downloadLink) throws Exception {
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        if (!isAGBChecked()) {
            logger.severe("AGB not signed : " + getPluginID());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
            downloadLink.getLinkStatus().setErrorMessage(JDLocale.L("plugins.hoster.error.agb", "TOC not signed"));
            return;
        }

        if (downloadLink.isDupeCheckallowed()) {
            /* check ob Datei existiert oder bereits geladen wird */
            System.out.println("PreDupeChecked: checking...");
            if (DownloadInterface.preDownloadCheckFailed(downloadLink)) {
                System.out.println("PreDupeChecked: mirror already loading!");
                return;
            }
            System.out.println("PreDupeChecked: no mirror found!");
        }

        Long t = 0l;

        if (HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass())) {
            t = HOSTER_WAIT_UNTIL_TIMES.get(this.getClass());
        }

        if (!enablePremium || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {

            if (t > 0) {
                this.resetHosterWaitTime();
                DownloadController.getDownloadController().fireRefresh();
            }
            try {
                handleFree(downloadLink);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
            }
            return;
        }
        Account account = null;
        ArrayList<Account> disabled = new ArrayList<Account>();

        ArrayList<Account> accounts = getPremiumAccounts();

        synchronized (accounts) {
            for (int i = 0; i < accounts.size(); i++) {
                Account next = accounts.get(i);

                if (!next.isTempDisabled() && next.isEnabled() && next.getPass() != null && next.getPass().trim().length() > 0) {
                    account = next;

                    break;
                } else if (next.isTempDisabled() && next.isEnabled()) {

                    disabled.add(next);

                }
            }
        }
        if (account != null) {
            long before = downloadLink.getDownloadCurrent();
            try {
                handlePremium(downloadLink, account);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
            }

            long traffic = downloadLink.getDownloadCurrent() - before;
            if (account.getProperty(AccountInfo.PARAM_INSTANCE) != null) {
                AccountInfo ai = (AccountInfo) account.getProperty(AccountInfo.PARAM_INSTANCE);
                ai.setTrafficLeft(ai.getTrafficLeft() - traffic);
            }

            synchronized (accounts) {
                if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_PREMIUM)) {
                    if (downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                        logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                        account.setTempDisabled(true);
                        account.setStatus(downloadLink.getLinkStatus().getErrorMessage());
                        getPluginConfig().save();
                    } else if (downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_ID_PREMIUM_DISABLE) {

                        account.setEnabled(false);
                        account.setStatus(downloadLink.getLinkStatus().getLongErrorMessage());

                        getPluginConfig().save();
                        logger.severe("Premium Account " + account.getUser() + ": expired:" + downloadLink.getLinkStatus().getLongErrorMessage());
                    } else {

                        account.setEnabled(false);
                        account.setStatus(downloadLink.getLinkStatus().getLongErrorMessage());
                        getPluginConfig().save();
                        logger.severe("Premium Account " + account.getUser() + ":" + downloadLink.getLinkStatus().getLongErrorMessage());
                    }

                } else {
                    account.setStatus(JDLocale.L("plugins.hoster.premium.status_ok", "Account is ok"));
                    getPluginConfig().save();
                }
            }

        } else {
            if (t > 0) {
                this.resetHosterWaitTime();
                DownloadController.getDownloadController().fireRefresh();
            }
            try {
                handleFree(downloadLink);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
            }
            synchronized (accounts) {
                if (disabled.size() > 0) {
                    int randId = (int) (Math.random() * disabled.size());
                    disabled.get(randId).setTempDisabled(false);
                    getPluginConfig().save();
                }
            }
        }

        return;
    }

    public boolean isAGBChecked() {
        if (!getPluginConfig().hasProperty(AGB_CHECKED)) {
            getPluginConfig().setProperty(AGB_CHECKED, JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_" + getPluginID(), false) || JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGB_CHECKED_" + getHost(), false));
            getPluginConfig().save();
        }
        return getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public void reset_downloadlink(DownloadLink link) {
    }

    public void resetHosterWaitTime() {
        HOSTER_WAIT_TIMES.put(this.getClass(), 0l);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), 0l);
    }

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor. Sollte nicht überschrieben werden
     */
    public final void resetPlugin() {
        reset();
        ArrayList<Account> accounts = getPremiumAccounts();

        for (Account account : accounts)
            account.setTempDisabled(false);
    }

    public void resetPluginGlobals() {
        br = new Browser();
    }

    public void setAGBChecked(boolean value) {
        getPluginConfig().setProperty(AGB_CHECKED, value);
        getPluginConfig().save();
    }

    public synchronized void setCurrentConnections(int CurrentConnections) {
        currentConnections = CurrentConnections;
    }

    public void setHosterWaittime(long milliSeconds) {
        HOSTER_WAIT_TIMES.put(this.getClass(), milliSeconds);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), System.currentTimeMillis() + milliSeconds);
    }

    public int getTimegapBetweenConnections() {
        return 750;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    public boolean waitForNextStartAllowed(DownloadLink downloadLink) throws InterruptedException {
        long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                this.sleep(time, downloadLink);
            } catch (PluginException e) {
                downloadLink.getLinkStatus().setStatusText(null);
                throw new InterruptedException();
            }
            downloadLink.getLinkStatus().setStatusText(null);
            return true;
        } else {
            downloadLink.getLinkStatus().setStatusText(null);
            return false;
        }
    }

    public boolean waitForNextConnectionAllowed() throws InterruptedException {
        long time = Math.max(0, getTimegapBetweenConnections() - (System.currentTimeMillis() - getLastConnectionTime()));
        if (time > 0) {
            Thread.sleep(time);
            return true;
        } else {
            return false;
        }
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void sleep(long i, DownloadLink downloadLink) throws PluginException {
        try {
            while (i > 0 && downloadLink.getDownloadLinkController() != null && !downloadLink.getDownloadLinkController().isAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(String.format(JDLocale.L("gui.downloadlink.status.wait", "wait %s min"), JDUtilities.formatSeconds(i / 1000)));
                downloadLink.requestGuiUpdate();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    public void sleep(long i, DownloadLink downloadLink, String message) throws PluginException {
        try {
            while (i > 0 && downloadLink.getDownloadLinkController() != null && !downloadLink.getDownloadLinkController().isAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(message + String.format(JDLocale.L("gui.downloadlink.status.wait", "wait %s min"), JDUtilities.formatSeconds(i / 1000)));
                downloadLink.requestGuiUpdate();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    /**
     * wird vom controlling (watchdog) beim stoppen aufgerufen. Damit werdend ie
     * hostercontrollvariablen zurückgesetzt.
     */
    public static void resetStatics() {
        HOSTER_WAIT_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();
        HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class<? extends PluginForHost>, Long>();

    }

    public Browser getBrowser() {
        return br;
    }

    public void setDownloadInterface(DownloadInterface dl2) {
        this.dl = dl2;

    }

    /**
     * Gibt die Url zurück, unter welcher ein PremiumAccount gekauft werden kann
     * 
     * @return
     */
    public String getBuyPremiumUrl() {
        return this.premiumurl;
    }

    public boolean isPremiumEnabled() {
        return enablePremium;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Account> getPremiumAccounts() {
        return (ArrayList<Account>) getPluginConfig().getProperty(PROPERTY_PREMIUM, new ArrayList<Account>());
    }

    public void setPremiumAccounts(ArrayList<Account> accounts) {
        ArrayList<Account> newaccounts = new ArrayList<Account>();
        for (Account toadd : accounts) {
            if (toadd.getUser().length() != 0) {
                newaccounts.add(toadd);
            }
        }
        getPluginConfig().setProperty(PROPERTY_PREMIUM, newaccounts);
        getPluginConfig().save();
    }

    /**
     * Diese funktion wird vom Controller aufegrufen wenn ein DownloadLink
     * frisch in die Linkliste eingefügt worden ist. Hier können
     * Initialisierungsfunktionen für das PLugin stehen.
     * 
     * @throws Exception
     */
    public void prepareLink(DownloadLink downloadLink) throws Exception {
    }

    /**
     * returns hosterspecific infos. for example the downloadserver
     * 
     * @return
     */
    public String getSessionInfo() {
        return "";
    }

    public final boolean hasHosterIcon() {
        File res = JDUtilities.getResourceFile("jd/img/hostericons/" + getHost() + ".png");
        return (res != null && res.exists());
    }

    public final ImageIcon getHosterIcon() {
        Image image = JDImage.getImage("hostericons/" + getHost());
        if (image != null) return new ImageIcon(image);
        return null;
    }

}
