package jd.updater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.zip.ZipException;

import jd.http.Browser;
import jd.http.HTTPProxy;
import jd.http.URLConnectionAdapter;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.appwork.utils.zip.ZipIOException;
import org.appwork.utils.zip.ZipIOReader;

public class UpdaterController {

    public static final String[]        UPDATE_MIRROR = new String[] { "http://update0.jdownloader.org/", "http://update1.jdownloader.org/", "http://update2.jdownloader.org/", "http://update3.jdownloader.org/", };

    private final UpdaterEventSender    eventSender;
    private final WebUpdaterOptions     options;
    private Browser                     br;

    private final BranchController      branchController;
    private ArrayList<Server>           repoList;

    private String                      hashList;

    private String                      optionsList;

    private ArrayList<FileUpdate>       classFiles;

    private ArrayList<FileUpdate>       nonClassFiles;

    private ArrayList<FileUpdate>       filteredNonClassFiles;

    private ArrayList<FileUpdate>       filteredClassFiles;

    private boolean                     isRestartRequired;

    private final ArrayList<FileUpdate> extractionList;

    public UpdaterController(final WebUpdaterOptions options) {
        this.options = options;
        eventSender = new UpdaterEventSender();
        initBrowser();
        branchController = new BranchController(this);
        extractionList = new ArrayList<FileUpdate>();
    }

    public boolean checkIfRestartIsRequired() {
        // we cannot reload jars at runtime
        if (filteredNonClassFiles.size() > 0) { return true; }

        if (filteredClassFiles.size() > 0) {
            // cannot check if plugins are loaded
            if (options.getPluginClassloader() == null) { return true; }
            for (final FileUpdate f : filteredClassFiles) {

                if (f.isClassLoaded()) { return true; }
            }
        }

        return false;
    }

    private void createFilteredList() {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.FILTER_LIST_START));
        try {
            filteredNonClassFiles = new ArrayList<FileUpdate>();

            for (final FileUpdate file : nonClassFiles) {

                if (file.isUpdateRequired()) {
                    filteredNonClassFiles.add(file);

                    continue;
                } else {
                    if (file.getLocalFile().getName().endsWith(".extract")) {
                        // zip file.
                        if (file.getLocalTmpFile().exists()) {
                            extractionList.add(file);
                        }
                    }
                }

            }

            filteredClassFiles = new ArrayList<FileUpdate>();
            for (final FileUpdate file : classFiles) {

                if (file.isUpdateRequired()) {
                    filteredClassFiles.add(file);
                    continue;
                }

            }
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.FILTER_LIST_END));
        }
    }

    private void doUpdate() throws UpdateException, InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_START));
        try {
            int i = 0;
            final int total = filteredClassFiles.size() + filteredNonClassFiles.size();
            for (final FileUpdate file : filteredNonClassFiles) {
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_PROGRESS, (i++ * 100 / total)));
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_FILE_START, file));

                file.update(repoList);

                if (file.getLocalTmpFile().getName().endsWith(".extract")) {
                    extractionList.add(file);
                    eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.ARCHIVE_ADDED, file));

                }
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_FILE_END, file));

            }

            for (final FileUpdate file : filteredClassFiles) {
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_PROGRESS, (i++ * 100 / total)));
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_FILE_START, file));

                file.update(repoList);
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_FILE_END, file));

            }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_PROGRESS, 100));
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DOWNLOAD_END));
        }

    }

    void errorWait() throws InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PENALTY_WAIT, 10000));

        Thread.sleep(10000);

    }

    private void extract() throws ZipException, ZipIOException, IOException {
        int i = 0;
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXTRACT_START));
        try {
            for (final FileUpdate file : extractionList) {

                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXTRACT_PROGRESS, (i++ * 100 / extractionList.size())));

                ZipIOReader zip;
                zip = new ZipIOReader(file.getLocalTmpFile());
                zip.setOverwrite(false);
                zip.extractTo(file.getLocalTmpFile().getParentFile());
                zip.close();
                final String hash = Hash.getMD5(file.getLocalTmpFile());
                // delete zip file and create hash file instead. if a hashfile
                // is found, this means that the zip has been extracted
                file.getLocalTmpFile().delete();
                final File hashFile = new File(file.getLocalTmpFile() + ".md5");
                hashFile.delete();
                IO.writeStringToFile(hashFile, hash);
            }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXTRACT_PROGRESS, 100));
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXTRACT_END));
        }
    }

    public Browser getBr() {
        return br;
    }

    public ArrayList<FileUpdate> getClassFiles() {
        return classFiles;
    }

    public UpdaterEventSender getEventSender() {
        return eventSender;
    }

    public ArrayList<FileUpdate> getExtractionList() {
        return extractionList;
    }

    public ArrayList<FileUpdate> getFilteredClassFiles() {
        return filteredClassFiles;
    }

    public ArrayList<FileUpdate> getFilteredNonClassFiles() {
        return filteredNonClassFiles;
    }

    public ArrayList<FileUpdate> getNonClassFiles() {
        return nonClassFiles;
    }

    public WebUpdaterOptions getOptions() {
        return options;
    }

    public ArrayList<Server> getRepoList() {
        return repoList;
    }

    private void initBrowser() {
        br = new Browser();
        if (Browser.getGlobalLogger() == null) {
            Browser.setGlobalLogger(Log.L);
        }
        final Storage downloadStorage = JSonStorage.getPlainStorage("DOWNLOAD");

        /* init default global Timeouts */
        Browser.setGlobalReadTimeout(downloadStorage.get(UpdaterConstants.PARAM_DOWNLOAD_READ_TIMEOUT, 100000));
        Browser.setGlobalConnectTimeout(downloadStorage.get(UpdaterConstants.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000));

        if (downloadStorage.get(UpdaterConstants.USE_PROXY, false)) {
            final String host = downloadStorage.get(UpdaterConstants.PROXY_HOST, "");
            final int port = downloadStorage.get(UpdaterConstants.PROXY_PORT, 8080);
            final String user = downloadStorage.get(UpdaterConstants.PROXY_USER, "");
            final String pass = downloadStorage.get(UpdaterConstants.PROXY_PASS, "");
            if ("".equals(host.trim())) {
                Log.L.warning("Proxy disabled. No host");
                downloadStorage.put(UpdaterConstants.USE_PROXY, false);
                return;
            }

            final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }
        if (downloadStorage.get(UpdaterConstants.USE_SOCKS, false)) {
            final String user = downloadStorage.get(UpdaterConstants.PROXY_USER_SOCKS, "");
            final String pass = downloadStorage.get(UpdaterConstants.PROXY_PASS_SOCKS, "");
            final String host = downloadStorage.get(UpdaterConstants.SOCKS_HOST, "");
            final int port = downloadStorage.get(UpdaterConstants.SOCKS_PORT, 1080);
            if ("".equals(host.trim())) {
                Log.L.warning("Socks Proxy disabled. No host");
                downloadStorage.put(UpdaterConstants.USE_SOCKS, false);
                return;
            }

            final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }

    }

    // private String getZipMD5(final int trycount) {
    //
    // return WebUpdater.UPDATE_MIRROR[trycount %
    // WebUpdater.UPDATE_MIRROR.length] + this.getBranch() + "_update.md5";
    // }
    //
    // private String getZipUrl(final int trycount) {
    // return WebUpdater.UPDATE_MIRROR[trycount %
    // WebUpdater.UPDATE_MIRROR.length] + this.getBranch() + "_update.zip";
    // }

    public boolean isRestartRequired() {
        return isRestartRequired;
    }

    private void loadFileList() throws UpdateException, InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.FILELIST_UPDATE_START));
        try {
            for (final RandomIterator<String> it = new RandomIterator<String>(UpdaterController.UPDATE_MIRROR); it.hasNext();) {
                it.next();
                try {
                    String path = it.curr() + branchController.getCurrentBranch() + "_update.md5";
                    final String serverHash = br.getPage(path + "?t=" + System.currentTimeMillis()).trim();
                    if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                        Log.L.warning("No update.md5 on " + path);
                        continue;
                    }
                    final File localZipFile = Application.getRessource(UpdaterConstants.UPDATE_ZIP_LOCAL_PATH);
                    final String localHash = localZipFile.exists() ? Hash.getMD5(localZipFile) : null;
                    if (!serverHash.equalsIgnoreCase(localHash)) {
                        path = it.curr() + branchController.getCurrentBranch() + "_update.zip";
                        final URLConnectionAdapter con = br.openGetConnection(path + "?t=" + System.currentTimeMillis());
                        if (con.getResponseCode() != 200) {
                            Log.L.warning("No update.zip on " + path);
                            con.disconnect();
                            continue;
                        }
                        Browser.download(localZipFile, con);
                    }

                    final ZipIOReader zip = new ZipIOReader(localZipFile);
                    // make sure that extract directory is empty
                    final File extractTo = Application.getRessource("tmp/update/" + System.currentTimeMillis());
                    if (extractTo.exists()) {
                        Files.deleteRecursiv(extractTo);
                    }
                    extractTo.mkdirs();
                    zip.extractTo(extractTo);
                    zip.close();
                    final File hashFile = new File(extractTo, "hashlist.lst");
                    if (!hashFile.exists()) { throw new UpdateException("Could not download hashlist"); }
                    final File optionsFile = new File(extractTo, "optionslist.lst");
                    hashList = IO.readFileToString(hashFile);
                    if (optionsFile.exists()) {
                        optionsList = IO.readFileToString(optionsFile);
                    }
                    Files.deleteRecursiv(extractTo);
                    return;
                } catch (final Exception e) {
                    e.printStackTrace();
                    errorWait();
                }
                try {
                    Thread.sleep(250);
                } catch (final InterruptedException e) {
                    continue;
                }
            }
            throw new UpdateException("Could not download hashlist");
        } finally {
            parse();
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.FILELIST_UPDATE_END));

        }
    }

    private void parse() {

        final String pattern = "[\r\n\\;]*([^=]+)\\=(.*?)\\;";
        classFiles = new ArrayList<FileUpdate>();
        nonClassFiles = new ArrayList<FileUpdate>();
        FileUpdate entry;
        final String[][] matches = new Regex(hashList, pattern).getMatches();
        final HashMap<String, FileUpdate> map = new HashMap<String, FileUpdate>();
        for (final String[] m : matches) {
            entry = new FileUpdate(m[0], m[1], this);
            if (entry.getRelURL().endsWith(".class")) {
                classFiles.add(entry);
            } else {
                nonClassFiles.add(entry);
            }
            map.put(entry.getRelURL(), entry);
        }
        // path=OS_MAC,ONLY_IF_EXISTS,..;
        if (optionsList != null) {
            final String[][] optionMatches = new Regex(optionsList, pattern).getMatches();
            for (final String[] m : optionMatches) {

                final FileUpdate file = map.get(m[0]);
                if (file != null) {
                    file.setUpdateOptions(UpdateOptions.create(m[1].split(";"), options));
                }
            }
        }
    }

    public void requestExit() {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXIT_REQUEST));
    }

    public void setNonClassFiles(final ArrayList<FileUpdate> nonClassFiles) {
        this.nonClassFiles = nonClassFiles;
    }

    public void setRestartRequired(final boolean isRestartRequired) {
        this.isRestartRequired = isRestartRequired;
    }

    public void start() throws InterruptedException, UpdateException, ZipException, ZipIOException, IOException {
        Log.L.finer("Update ask for delay");

        waitDelay();
        Log.L.finer("Fetch Branch:");
        String branch;
        JSonStorage.getPlainStorage("WEBUPDATE").put(UpdaterConstants.BRANCHINUSE, branch = branchController.getCurrentBranch());
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.BRANCH_UPDATED, branch));
        Log.L.finer("Update Repolist:");
        updateRepos();
        Log.L.finer("Update Filelist:");

        loadFileList();

        Log.L.finer("Comparing");

        createFilteredList();

        if (filteredNonClassFiles.size() > 0 || filteredClassFiles.size() > 0) {
            isRestartRequired = checkIfRestartIsRequired();
            Log.L.finer("Restart required: " + isRestartRequired);
            Log.L.finer("Update files now");

            doUpdate();

        }
        if (extractionList.size() > 0) {
            Log.L.finer("Extract ARchives");

            extract();

        }

    }

    private void updateRepos() throws InterruptedException, UpdateException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.REPO_UPDATE_START));
        try {
            for (final RandomIterator<String> it = new RandomIterator<String>(UpdaterController.UPDATE_MIRROR); it.hasNext();) {
                it.next();
                try {
                    final String path = it.curr() + branchController.getCurrentBranch() + "_server.list";
                    br.getPage(path + "?t=" + System.currentTimeMillis());
                    if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                        // branches should be available on ALL mirrors.
                        throw new UpdateException("Branch not found: " + branchController.getCurrentBranch() + " " + path + " use " + branchController.getLatestStableBranch() + " instead!");
                    }
                    if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                        errorWait();
                        continue;
                    }
                    int total = 0;
                    final ArrayList<Server> servers = new ArrayList<Server>();
                    Server serv;
                    // use auto balance based on requesttimes
                    boolean auto = false;
                    for (final String[] match : br.getRegex("(\\-?\\d+)\\:([^\r^\n]*)").getMatches()) {
                        servers.add(serv = new Server(Integer.parseInt(match[0]), match[1].trim()));
                        if (serv.getPercent() < 0) {
                            auto = true;
                        }
                        total += serv.getPercent();
                    }
                    if (auto) {
                        Log.L.info("Use Auto Load Balance based on requesttimes");
                    }
                    for (final Server s : servers) {
                        if (auto) {
                            s.setPercent(-1);
                        } else {
                            s.setPercent(s.getPercent() * 100 / total);
                        }

                    }
                    repoList = servers;
                    Log.L.fine("Serverlist: " + JSonStorage.toString(repoList));
                    return;
                } catch (final Exception e) {
                    e.printStackTrace();
                    errorWait();
                }
                try {
                    Thread.sleep(250);
                } catch (final InterruptedException e) {
                    continue;
                }
            }
            throw new UpdateException("Could not update repos");
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.REPO_UPDATE_END));

        }
    }

    private void waitDelay() {
        // by default wait 2 minutes before update
        long ticketTime = 120000l;
        try {

            // check tickettime
            ticketTime = Long.parseLong(br.getPage("http://update0.jdownloader.org/tickettime.txt").trim());
            if (ticketTime < 0) { throw new UpdateException("No Free Slots"); }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        if (ticketTime > 1000) {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.WAIT_FOR_SLOT_START, ticketTime));
            Log.L.info("Wait for slot: " + ticketTime + " ms");
            try {
                Thread.sleep(ticketTime);
            } catch (final InterruptedException e) {
                Log.exception(Level.WARNING, e);

            }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.WAIT_FOR_SLOT_END));

        } else {
            Log.L.info("No Slot waittime. Direct Start");
        }
    }
}
