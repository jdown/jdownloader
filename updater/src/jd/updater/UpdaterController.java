package jd.updater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.zip.ZipException;

import jd.http.Browser;
import jd.http.HTTPProxy;
import jd.http.URLConnectionAdapter;

import org.appwork.controlling.State;
import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
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
import org.codehaus.jackson.type.TypeReference;

public class UpdaterController implements StateMachineInterface {

    private static final String         BACKUP_EXTENSION       = ".backup";

    public static final String[]        UPDATE_MIRROR          = new String[] { "http://update0.jdownloader.org/", "http://update1.jdownloader.org/", "http://update2.jdownloader.org/", /*
                                                                                                                                                                                          * "http://update3.jdownloader.org/"
                                                                                                                                                                                          * ,
                                                                                                                                                                                          */};

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

    private String                      branch;

    private ArrayList<File>             filesToInstall;

    private final StateMachine          stateMachine;
    final static public State           INIT                   = new UpdaterState(0, "INIT");
    final static public State           DONE                   = new UpdaterState(100, "DONE");

    public static final State           SLOT_WAITING           = new UpdaterState(2, "SLOT_WAITING");

    public static final State           DOWNLOADING_BRANCHLIST = new UpdaterState(4, "DOWNLOADING_BRANCHLIST");

    public static final State           DOWNLOADING_REPOLIST   = new UpdaterState(7, "DOWNLOADING_REPOLIST");

    public static final State           DOWNLOADING_HASHLIST   = new UpdaterState(10, "DOWNLOADING_HASHLIST");

    public static final State           FILTERING              = new UpdaterState(15, "FILTERING");

    public static final State           DOWNLOADING            = new UpdaterState(20, "DOWNLOADING");

    public static final State           EXTRACTING             = new UpdaterState(70, "EXTRACTING");

    public static final State           WAITING_FOR_UNLOCK     = new UpdaterState(80, "WAITING_FOR_UNLOCK");

    public static final State           INSTALLING             = new UpdaterState(85, "INSTALLING");

    public static final State           REVERTING              = new UpdaterState(-1, "REVERTING");
    static {
        State.link(UpdaterController.INIT, UpdaterController.SLOT_WAITING, UpdaterController.DOWNLOADING_BRANCHLIST, UpdaterController.DOWNLOADING_REPOLIST, UpdaterController.DOWNLOADING_HASHLIST, UpdaterController.FILTERING, UpdaterController.DOWNLOADING, UpdaterController.EXTRACTING, UpdaterController.WAITING_FOR_UNLOCK, UpdaterController.INSTALLING, UpdaterController.DONE);
        State.link(UpdaterController.INSTALLING, UpdaterController.REVERTING, UpdaterController.DONE);

    }

    public UpdaterController(final WebUpdaterOptions options) {
        this.options = options;
        eventSender = new UpdaterEventSender();
        initBrowser();
        branchController = new BranchController(this);
        stateMachine = new StateMachine(this, UpdaterController.INIT, UpdaterController.DONE);
        extractionList = new ArrayList<FileUpdate>();
    }

    /**
     * checks wether JDownloader.jar is locked by a windows process
     * 
     * @return
     */
    private boolean canWriteToJDownloaderJar() {
        final File jdMain = getResource("JDownloader.jar");
        if (!jdMain.exists()) { return true; }
        if (!jdMain.canWrite()) { return false; }
        final File renameTest = new File(jdMain.getAbsoluteFile() + ".test");
        renameTest.delete();
        final boolean suc = jdMain.renameTo(renameTest);
        if (!suc) {
            //
            return false;
            //
        }
        renameTest.renameTo(jdMain);
        return true;
    }

    public void checkHashLists() throws InterruptedException, UpdateException, IOException, ZipIOException {

        updateRepos();

        String hash = null;
        for (final String mirror : UpdaterController.UPDATE_MIRROR) {
            String path = mirror + branchController.getCurrentBranch() + "_update.md5";
            final String serverHash = br.getPage(path + "?t=" + System.currentTimeMillis()).trim();
            if (hash == null) {
                hash = serverHash;
            } else {
                if (!hash.equals(serverHash)) { throw new UpdateException("different .zip.md5 detected on: " + path); }
            }
            if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                Log.L.warning("No update.md5 on " + path);
                throw new UpdateException("No update.md5 on " + path);
            }
            final File localZipFile = getResource(UpdaterConstants.UPDATE_ZIP_LOCAL_PATH);
            final String localHash = localZipFile.exists() ? Hash.getMD5(localZipFile) : null;
            if (!serverHash.equalsIgnoreCase(localHash)) {
                path = mirror + branchController.getCurrentBranch() + "_update.zip";
                final URLConnectionAdapter con = br.openGetConnection(path + "?t=" + System.currentTimeMillis());
                if (con.getResponseCode() != 200) {
                    Log.L.warning("No update.zip on " + path);
                    con.disconnect();
                    throw new UpdateException("No update.zip on " + path);

                }
                Browser.download(localZipFile, con);

                if (!serverHash.equalsIgnoreCase(Hash.getMD5(localZipFile))) { throw new UpdateException("Hash mimatch in " + path);

                }
            }

        }
        this.loadFileList();
        br = new Browser();
        final File dummy = getResource("dummyfile.tmp");
        dummy.deleteOnExit();
        for (final FileUpdate f : classFiles) {
            hash = f.getRemoteHash();
            for (final Server serv : repoList) {
                final String url = f.mergeUrl(serv.getPath(), f.getRelURL()) + "?" + System.currentTimeMillis();
                final URLConnectionAdapter con = br.openGetConnection(url);
                dummy.delete();
                Browser.download(dummy, con);

                final String newHash = Hash.getMD5(dummy);
                if (!newHash.equals(hash)) {
                    throw new UpdateException("Hash mismatch in: " + url);
                } else {
                    System.out.println(f.getRelURL() + " - " + serv + " - OK");
                }
            }

        }
        for (final FileUpdate f : nonClassFiles) {
            hash = f.getRemoteHash();
            for (final Server serv : repoList) {
                final String url = f.mergeUrl(serv.getPath(), f.getRelURL()) + "?" + System.currentTimeMillis();
                final URLConnectionAdapter con = br.openGetConnection(url);
                dummy.delete();
                Browser.download(dummy, con);

                final String newHash = Hash.getMD5(dummy);
                if (!newHash.equals(hash)) {
                    throw new UpdateException("Hash mismatch in: " + url);
                } else {
                    System.out.println(f.getRelURL() + " - " + serv + " - OK");
                }
            }

        }
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

    /**
     * Removes all backup files and returns a list of all new installed files
     * 
     * @param backups
     * @return
     */
    private ArrayList<File> cleanUp(final ArrayList<File> backups) {
        final ArrayList<File> ret = new ArrayList<File>();
        for (final File f : backups) {
            if (f.getName().endsWith(UpdaterController.BACKUP_EXTENSION) && !f.delete()) {
                Log.L.warning("Could not clean up backup file " + f);
                ret.add(new File(f.getParentFile(), f.getName().substring(0, f.getName().length() - UpdaterController.BACKUP_EXTENSION.length())));
            } else {
                ret.add(f);
            }

        }
        return ret;

    }

    /**
     * removes all files in update dir which are not in updatelist
     * 
     * @throws InterruptedException
     */
    private void cleanUpUpatedir() throws InterruptedException {
        // create map
        final HashMap<File, FileUpdate> map = new HashMap<File, FileUpdate>();
        for (final FileUpdate f : classFiles) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            map.put(f.getUpdateFilepath(), f);
        }
        for (final FileUpdate f : nonClassFiles) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            map.put(f.getUpdateFilepath(), f);
        }
        final File root = getResource("update/" + getBranch());
        final ArrayList<File> files = Files.getFiles(false, true, root);
        for (final File f : files) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            if (!map.containsKey(f)) {
                // this file is not in updatelist;
                f.delete();

            }
        }

    }

    private void createFilteredList() throws InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_FILTERING));
        try {
            filteredNonClassFiles = new ArrayList<FileUpdate>();

            for (final FileUpdate file : nonClassFiles) {
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                if (file.isUpdateRequired()) {
                    filteredNonClassFiles.add(file);

                    continue;
                } else {
                    if (file.getLocalFile().getName().endsWith(".extract")) {
                        // zip file.
                        if (file.getUpdateFilepath().exists()) {
                            extractionList.add(file);
                        }
                    }
                }

            }

            filteredClassFiles = new ArrayList<FileUpdate>();
            for (final FileUpdate file : classFiles) {
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                if (file.isUpdateRequired()) {
                    filteredClassFiles.add(file);
                    continue;
                }

            }
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_FILTERING));
        }
    }

    private void doUpdate() throws UpdateException, InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_DOWNLOAD_UPDATES));

        try {
            String installLog;
            ArrayList<InstalledFile> installedFiles = new ArrayList<InstalledFile>();
            final File logFile = getResource("installlog.json");
            if (logFile.exists()) {
                try {
                    installLog = IO.readFileToString(logFile);
                    installedFiles = JSonStorage.restoreFromString(installLog, new TypeReference<ArrayList<InstalledFile>>() {
                    }, installedFiles);
                } catch (final IOException e) {
                    Log.exception(Level.WARNING, e);
                }
            }

            int i = 0;
            final int total = filteredClassFiles.size() + filteredNonClassFiles.size();
            for (final FileUpdate file : filteredNonClassFiles) {
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_DOWNLOAD_UPDATES, (i++ * 100 / total)));
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_DOWNLOAD_FILE, file));

                file.update(repoList);

                if (file.getUpdateFilepath().getName().endsWith(".extract")) {
                    extractionList.add(file);
                    eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.ARCHIVE_ADDED, file));

                }
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_DOWNLOAD_FILE, file));

            }

            for (final FileUpdate file : filteredClassFiles) {
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_DOWNLOAD_UPDATES, (i++ * 100 / total)));
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_DOWNLOAD_FILE, file));

                file.update(repoList);
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_DOWNLOAD_FILE, file));

            }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_DOWNLOAD_UPDATES, 100));
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_DOWNLOAD_UPDATES));
        }

    }

    public void downloadUpdates() throws InterruptedException, UpdateException, ZipException, ZipIOException, IOException {
        Log.L.finer("Update ask for delay");
        stateMachine.setStatus(UpdaterController.SLOT_WAITING);
        waitDelay();
        stateMachine.setStatus(UpdaterController.DOWNLOADING_BRANCHLIST);
        Log.L.finer("Fetch Branch:");

        JSonStorage.getPlainStorage("WEBUPDATE").put(UpdaterConstants.BRANCHINUSE, branch = branchController.getCurrentBranch());
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.BRANCH_UPDATED, branch));
        Log.L.finer("Update Repolist:");
        stateMachine.setStatus(UpdaterController.DOWNLOADING_REPOLIST);
        updateRepos();
        Log.L.finer("Update Filelist:");
        stateMachine.setStatus(UpdaterController.DOWNLOADING_HASHLIST);
        loadFileList();

        Log.L.finer("Comparing");
        stateMachine.setStatus(UpdaterController.FILTERING);
        cleanUpUpatedir();
        createFilteredList();

        if (filteredNonClassFiles.size() > 0 || filteredClassFiles.size() > 0) {
            isRestartRequired = checkIfRestartIsRequired();
            Log.L.finer("Restart required: " + isRestartRequired);
            Log.L.finer("Update files now");
            stateMachine.setStatus(UpdaterController.DOWNLOADING);
            doUpdate();

        }
        if (extractionList.size() > 0) {
            Log.L.finer("Extract ARchives");
            stateMachine.setStatus(UpdaterController.EXTRACTING);
            extract();

        }

    }

    void errorWait(final Throwable e) throws InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.WAIT_PENALTY, 10000, e));

        Thread.sleep(10000);

    }

    private void extract() throws ZipException, ZipIOException, IOException {
        int i = 0;
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_EXTRACT_ARCHIVES));
        try {
            for (final FileUpdate file : extractionList) {

                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_EXTRACT_ARCHIVES, (i++ * 100 / extractionList.size())));

                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_EXTRACT_FILE, file));
                ZipIOReader zip;
                zip = new ZipIOReader(file.getUpdateFilepath());
                zip.setOverwrite(false);
                final ArrayList<File> files = zip.extractTo(file.getUpdateFilepath().getParentFile());
                zip.close();
                final String hash = Hash.getMD5(file.getUpdateFilepath());
                // delete zip file and create hash file instead. if a hashfile
                // is found, this means that the zip has been extracted
                file.getUpdateFilepath().delete();
                final File hashFile = new File(file.getUpdateFilepath() + ".md5");
                hashFile.delete();
                IO.writeStringToFile(hashFile, hash);
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_EXTRACT_FILE, files));
            }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_EXTRACT_ARCHIVES, 100));
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_EXTRACT_ARCHIVES));
        }
    }

    public Browser getBr() {
        return br;
    }

    public String getBranch() {
        return branch;
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

    public ArrayList<FileUpdate> getNonClassFiles() {
        return nonClassFiles;
    }

    public WebUpdaterOptions getOptions() {
        return options;
    }

    public ArrayList<Server> getRepoList() {
        return repoList;
    }

    public File getResource(final String relpath) {
        if (options.getWorkingDir() != null) {
            return new File(options.getWorkingDir(), relpath);
        } else {
            return Application.getResource(relpath);
        }

    }

    @Override
    public StateMachine getStateMachine() {

        return stateMachine;
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

    public void install() throws InterruptedException, InstallException {
        stateMachine.setStatus(UpdaterController.WAITING_FOR_UNLOCK);
        waitForJDownloaderJar();
        final File root = getResource("update/" + getBranch());
        final ArrayList<File> backups = new ArrayList<File>();
        try {
            stateMachine.setStatus(UpdaterController.INSTALLING);
            filesToInstall = Files.getFiles(false, true, root);
            install(root.getParentFile().getParentFile(), root, backups, root);
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_INSTALL, 100));
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            final ArrayList<File> installedFiles = cleanUp(backups);
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            removeOutdatedFiles(installedFiles);

        } catch (final Throwable e) {
            Log.exception(e);
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.INSTALL_FAILED, e));
            stateMachine.setStatus(UpdaterController.REVERTING);
            final int errors = revert(backups);
            throw new InstallException("Installation failed.", errors, e);
        } finally {
            stateMachine.setStatus(UpdaterController.DONE);
        }

    }

    private void install(final File installDir, final File update, final ArrayList<File> backups, final File updates) throws InstallException, InterruptedException {
        if (!updates.exists()) {
            updates.mkdirs();

        }

        for (final File f : updates.listFiles()) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            if (f.isDirectory()) {
                install(installDir, update, backups, f);
            } else {
                final String relPath = Files.getRelativePath(update, f);
                // final String hash = Hash.getMD5(f);
                // use length to find virtualisation this is faster than hashes
                final long length = f.length();
                final File installDirFile = new File(installDir, relPath);
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_INSTALL_FILE, installDirFile, relPath));
                try {
                    if (!installDirFile.exists()) {
                        installDirFile.getParentFile().mkdirs();
                        if (!f.renameTo(installDirFile)) {
                            //
                            throw new InstallException("Could not rename " + f + " to " + installDirFile);
                            //
                        }
                        // add to backup file. backup files without .backup
                        // extension will be deleted
                        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_INSTALL, Math.min(100, (backups.size() * 100 / filesToInstall.size()))));

                        backups.add(installDirFile);
                    } else {
                        final File backup = new File(installDirFile.getAbsolutePath() + UpdaterController.BACKUP_EXTENSION);
                        if (backup.exists()) {
                            Log.L.warning("Removed backupfile: " + backup);
                        }
                        if (!installDirFile.renameTo(backup)) { throw new InstallException("could not rename to backupfile: " + backup); }
                        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_INSTALL, Math.min(100, (backups.size() * 100 / filesToInstall.size()))));

                        backups.add(backup);
                        if (!f.renameTo(installDirFile)) { throw new InstallException("Could not rename to " + installDirFile); }
                    }
                    // check if we can delete dir

                    final long newLength = installDirFile.length();
                    if (newLength == 0 && !installDirFile.exists()) {
                        throw new InstallException("Virtual File System! Could not read file after moving " + installDirFile);
                    } else if (newLength != length) { throw new InstallException("Error While Moving. Length Mismatch"); }
                } finally {
                    eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_INSTALL_FILE, installDirFile, relPath));
                }

            }

            // cleans up directories.
            File down = f.getParentFile();
            String[] directSubFiles;
            while (down != null && !down.equals(update.getParentFile()) && (directSubFiles = down.list()) != null && directSubFiles.length == 0 && Files.getFiles(false, true, down).size() == 0) {
                down.delete();
                down = down.getParentFile();
            }
        }

    }

    public boolean isRestartRequired() {
        return isRestartRequired;
    }

    private void loadFileList() throws UpdateException, InterruptedException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_FILELIST_UPDATE));
        try {
            for (final RandomIterator<String> it = new RandomIterator<String>(UpdaterController.UPDATE_MIRROR); it.hasNext();) {
                it.next();
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                try {
                    loadFileList(it.curr());
                    return;
                } catch (final Exception e) {
                    e.printStackTrace();
                    errorWait(e);
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
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_FILELIST_UPDATE));

        }
    }

    private void loadFileList(final String curr) throws UpdateException, InterruptedException, IOException, ZipIOException {

        String path = curr + branchController.getCurrentBranch() + "_update.md5";
        final String serverHash = br.getPage(path + "?t=" + System.currentTimeMillis()).trim();
        if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
            Log.L.warning("No update.md5 on " + path);
            throw new UpdateException("No update.md5 on " + path);
        }
        final File localZipFile = getResource(UpdaterConstants.UPDATE_ZIP_LOCAL_PATH);
        final String localHash = localZipFile.exists() ? Hash.getMD5(localZipFile) : null;
        if (!serverHash.equalsIgnoreCase(localHash)) {
            path = curr + branchController.getCurrentBranch() + "_update.zip";
            final URLConnectionAdapter con = br.openGetConnection(path + "?t=" + System.currentTimeMillis());
            if (con.getResponseCode() != 200) {
                Log.L.warning("No update.zip on " + path);
                con.disconnect();
                throw new UpdateException("No update.zip on " + path);

            }
            Browser.download(localZipFile, con);

            if (!serverHash.equalsIgnoreCase(Hash.getMD5(localZipFile))) { throw new UpdateException("Hash mimatch in " + path);

            }
        }

        final ZipIOReader zip = new ZipIOReader(localZipFile);
        // make sure that extract directory is empty
        final File extractTo = getResource("tmp/update/" + System.currentTimeMillis());
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

    }

    private void parse() throws InterruptedException {

        final String pattern = "[\r\n\\;]*([^=]+)\\=(.*?)\\;";
        classFiles = new ArrayList<FileUpdate>();
        nonClassFiles = new ArrayList<FileUpdate>();
        FileUpdate entry;
        final String[][] matches = new Regex(hashList, pattern).getMatches();
        final HashMap<String, FileUpdate> map = new HashMap<String, FileUpdate>();

        for (final String[] m : matches) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
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
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                final FileUpdate file = map.get(m[0]);
                if (file != null) {
                    file.setUpdateOptions(UpdateOptions.create(m[1].split(";"), options));
                }
            }
        }
    }

    /**
     * write all new installed files to a config file. runs through this config
     * files. if we see, that we ever installed a file which is now not in list
     * any more.. remove it
     * 
     * @param installedFiles
     * @throws InterruptedException
     */
    private void removeOutdatedFiles(final ArrayList<File> newFiles) throws InterruptedException {
        String installLog;
        ArrayList<InstalledFile> installedFiles = new ArrayList<InstalledFile>();
        final File logFile = getResource("installlog.json");
        if (logFile.exists()) {
            try {
                installLog = IO.readFileToString(logFile);
                installedFiles = JSonStorage.restoreFromString(installLog, new TypeReference<ArrayList<InstalledFile>>() {
                }, installedFiles);
            } catch (final IOException e) {
                Log.exception(Level.WARNING, e);
            }
        }
        // create map
        final HashMap<File, FileUpdate> map = new HashMap<File, FileUpdate>();
        for (final FileUpdate f : classFiles) {
            map.put(f.getLocalFile(), f);
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
        }
        for (final FileUpdate f : nonClassFiles) {
            map.put(f.getLocalFile(), f);
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
        }
        // fileMap contains localfile ->FileUpdate Mappings
        InstalledFile ifile;
        for (final Iterator<InstalledFile> it = installedFiles.iterator(); it.hasNext();) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            ifile = it.next();
            final File localFile = getResource(ifile.getRelPath());
            if (!map.containsKey(localFile)) {
                // file has been installed someday, but is not in filellist any
                // more
                it.remove();
                if (localFile.exists()) {
                    localFile.delete();
                    eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.DELETED_FILE, localFile));
                }
            }
        }
        final File root = getResource("update/").getParentFile();
        for (final File f : newFiles) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
            // comare to hashlist here. we only want to add files to installlog
            // which have been downloaded.
            // extracted files will not be added, and thus cannot be removed by
            // removing the hashlist entry
            if (map.containsKey(f)) {
                ifile = new InstalledFile(Files.getRelativePath(root, f), f.lastModified());
                installedFiles.remove(ifile);
                installedFiles.add(ifile);
            }
        }
        final File backup = new File(logFile.getAbsolutePath() + ".tmp");
        try {

            backup.delete();
            logFile.renameTo(backup);
            IO.writeStringToFile(logFile, JSonStorage.serializeToJson(installedFiles));
            backup.delete();
        } catch (final Exception e) {
            Log.exception(Level.WARNING, e);
            if (backup.exists()) {
                logFile.delete();
                backup.renameTo(logFile);
            }

        }

    }

    public void requestExit() {
        Thread.currentThread().interrupt();
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.EXIT_REQUEST));
    }

    private int revert(final ArrayList<File> backups) {
        int error = 0;
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_REVERT));
        try {
            int i = 0;
            for (final File f : backups) {
                final String path = f.getAbsolutePath();
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_REVERT, (i++ * 100 / backups.size())));
                if (path.endsWith(UpdaterController.BACKUP_EXTENSION)) {
                    final String orgPath = path.substring(0, path.length() - UpdaterController.BACKUP_EXTENSION.length());
                    final File orgFile = new File(orgPath);
                    orgFile.delete();
                    if (!f.renameTo(orgFile)) {
                        Log.L.severe("Could not revert failed update: " + f);
                        error++;
                    }

                } else {
                    if (!f.delete()) {
                        Log.L.severe("Could not revert failed update: " + f);
                        error++;
                    }
                }

            }
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.PROGRESS_REVERT, 100));
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_REVERT, error));
        }
        return error;

    }

    public void setNonClassFiles(final ArrayList<FileUpdate> nonClassFiles) {
        this.nonClassFiles = nonClassFiles;
    }

    public void setRestartRequired(final boolean isRestartRequired) {
        this.isRestartRequired = isRestartRequired;
    }

    private void updateRepos() throws InterruptedException, UpdateException {
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_REPO_UPDATE));
        try {
            for (final RandomIterator<String> it = new RandomIterator<String>(UpdaterController.UPDATE_MIRROR); it.hasNext();) {
                it.next();
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                try {
                    final String path = it.curr() + branchController.getCurrentBranch() + "_server.list";

                    br.getPage(path + "?t=" + System.currentTimeMillis());

                    if (br.getRequest().getHttpConnection().getResponseCode() != 200) {
                        errorWait(new Exception("ResponseCode !=200: " + br.getRequest().getHttpConnection().getResponseCode()));
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
                    errorWait(e);
                }
                try {
                    Thread.sleep(250);
                } catch (final InterruptedException e) {
                    continue;
                }
            }
            throw new UpdateException("Could not update repos");
        } finally {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_REPO_UPDATE));

        }
    }

    private void waitDelay() {
        // by default wait 2 minutes before update
        long ticketTime = 120000l;
        if (options.isIgnoreSlotWaittime()) {
            Log.L.info("Ignore Slot waittime.");
            return;
        }
        try {

            // check tickettime
            ticketTime = Long.parseLong(br.getPage("http://update0.jdownloader.org/tickettime.txt").trim());
            if (ticketTime < 0) { throw new UpdateException("No Free Slots"); }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        if (ticketTime > 1000) {
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.START_WAIT_FOR_SLOT, ticketTime));
            Log.L.info("Wait for slot: " + ticketTime + " ms");
            try {
                Thread.sleep(ticketTime);
            } catch (final InterruptedException e) {
                Log.exception(Level.WARNING, e);

            }
            eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.END_WAIT_FOR_SLOT));

        } else {
            Log.L.info("No Slot waittime. Direct Start");
        }
    }

    private void waitForJDownloaderJar() throws InterruptedException {

        boolean startEvent = false;
        try {
            while (true) {
                if (canWriteToJDownloaderJar()) { return; }
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.JDOWNLOADER_LOCKED));
                startEvent = true;

                Thread.sleep(1000);
            }
        } finally {
            if (startEvent) {
                eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Types.JDOWNLOADER_UNLOCKED));
            }
        }
    }
}
