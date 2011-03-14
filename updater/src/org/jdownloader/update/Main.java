package org.jdownloader.update;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.UpdaterOptions;
import org.appwork.update.updateclient.gui.StandaloneUpdaterGui;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.locale.APPWORKUTILS;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging.LogFormatter;
import org.appwork.utils.net.DownloadProgress;
import org.appwork.utils.net.HTTPException;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.ShellParser;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.zip.ZipIOReader;
import org.jdownloader.update.translate.T;

public class Main {

    private static final SwitchParam    RESTART            = new SwitchParam("restart", "| Restartpath after update");
    private static final SwitchParam    WORKINGDIR         = new SwitchParam("dir", "| Set Installdirectory");

    private static StandaloneUpdaterGui GUI                = null;

    private static UpdaterOptions       OPTIONS;

    private static Updater              UPDATER;
    private static PrintStream          OUT                = null;
    static {
        if (Charset.defaultCharset() == Charset.forName("cp1252")) {
            try {
                Main.OUT = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "CP850");
            } catch (final UnsupportedEncodingException e) {
                Main.OUT = System.out;
            }
        } else {
            Main.OUT = System.out;
        }

    }
    private static final SwitchParam    DEBUG              = new SwitchParam("debug", "| Run In debug mode. does not write logfiles, but writes to stdout/stderr");

    private static final SwitchParam    GUILESS            = new SwitchParam("guiless", "| Run silently or in console mode");

    private static final SwitchParam    DISABLED_OS_FILTER = new SwitchParam("noosfilter", "| Update all files. Even files which are marked as invalid for your os");

    private static final SwitchParam    LOGLEVEL           = new SwitchParam("log", "LEVEL | Set the Loglevel: ALL,FINER,FINE,INFO,WARNING,SEVERE");

    private static final SwitchParam    BRANCH             = new SwitchParam("branch", "BRANCHNAME | Sets the desired Branch");

    private static final SwitchParam    APP                = new SwitchParam("app", "AppID | Sets the desired AppID");

    public static void downloadInDialog(final File file, final String url, final String hash) throws Exception {
        if (file.exists()) { throw new Exception("File exists"); }
        file.getParentFile().mkdirs();
        Exception ret = null;
        if (Main.OPTIONS.getGuiless()) {
            final DownloadProgress progress = new DownloadProgress() {
                @Override
                public void increaseLoaded(final long increase) {
                    super.increaseLoaded(increase);
                    System.out.println("Loaded: " + this.getLoaded());
                }

                @Override
                public void setLoaded(final long loaded) {
                    super.setLoaded(loaded);
                }

                @Override
                public void setTotal(final long total) {
                    super.setTotal(total);
                    System.out.println("Total: " + this.getTotal());
                }

            };

            Main.UPDATER.getHttpClient().download(file, url, progress);
        } else {
            ret = new EDTHelper<Exception>() {

                @Override
                public Exception edtRun() {
                    try {

                        final DownloadProgress progress = new DownloadProgress();
                        final ProgressGetter pg = new ProgressGetter() {

                            private long loaded = 0;
                            private long total  = 0;

                            @Override
                            public int getProgress() {
                                this.total = progress.getTotal();
                                this.loaded = progress.getLoaded();
                                if (this.total == 0) { return 0; }
                                return (int) (this.loaded * 100 / this.total);
                            }

                            @Override
                            public String getString() {
                                this.total = progress.getTotal();
                                this.loaded = progress.getLoaded();
                                if (this.total <= 0) { return APPWORKUTILS.T.connecting(); }
                                return APPWORKUTILS.T.progress(this.loaded, this.total, this.loaded * 10000f / this.total / 100.0);
                            }

                            @Override
                            public void run() throws Exception {
                                Main.UPDATER.getHttpClient().download(file, url, progress);
                                System.out.println("Download finished");
                            }

                        };
                        final ProgressDialog dialog = new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL | Dialog.BUTTONS_HIDE_OK, APPWORKUTILS.T.download_title(), APPWORKUTILS.T.download_msg(), ImageProvider.getImageIcon("download", 32, 32, true)) {
                            /**
                         * 
                         */
                            private static final long serialVersionUID = 5303387916537596967L;

                            @Override
                            public boolean closeAllowed() {

                                Dialog.getInstance().showMessageDialog(APPWORKUTILS.T.please_wait());

                                return false;
                            }
                        };
                        Dialog.getInstance().showDialog(dialog);
                    } catch (final Exception e) {
                        return e;
                    }
                    return null;
                }

            }.getReturnValue();
        }
        if (hash != null && !hash.equalsIgnoreCase(Hash.getMD5(file))) {
            //
            throw new Exception("Hash Mismatch");
        }
        if (ret != null) { throw ret; }
    }

    private static String getJarName() throws MalformedURLException, URISyntaxException {
        final String url = Application.getRessourceURL("tbs.jar").toString();
        System.out.println(url);
        ;
        final int index = url.indexOf(".jar!");
        System.out.println(url.substring(4, index + 4));
        return new File(new URL(url.substring(4, index + 4)).toURI()).getName();
    }

    private static void init() {
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(final String[] args) {

        Application.setApplication(".jd_home");

        // resolving debug flag is the first thing we do, because al other
        // actions might already do outputs
        boolean debug = false;
        for (final String a : args) {
            if (a.trim().equalsIgnoreCase("-debug")) {
                debug = true;
                break;
            }
        }
        if (!debug && Application.isJared(Main.class)) {
            final Calendar cal = Calendar.getInstance();

            cal.setTimeInMillis(new Date().getTime());

            final File file = Application.getResource("Updater_" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DATE) + ".log");

            try {
                file.getParentFile().mkdirs();

                if (!file.isFile()) {
                    file.createNewFile();
                }
                final FileOutputStream outStr = new FileOutputStream(file, true);
                final PrintStream printStream = new PrintStream(outStr);

                System.setErr(printStream);
                System.setOut(printStream);
                final org.appwork.update.updateclient.gui.ConsoleLogHandler cHandler = new org.appwork.update.updateclient.gui.ConsoleLogHandler(printStream);
                cHandler.setFormatter(new LogFormatter());
                Log.L.addHandler(cHandler);
            } catch (final IOException e) {
                Log.exception(e);
            }

        }

        Application.setApplication(".appwork");
        Main.init();
        if (Application.isJared(Main.class)) {
            Log.L.setLevel(Level.INFO);
        } else {
            Log.L.setLevel(Level.ALL);
        }
        Main.parseParams(args);

        Main.out(T._.start());
        Main.out(JSonStorage.toString(Main.OPTIONS));
        Main.UPDATER = new Updater(new UpdaterHttpClientImpl(), Main.OPTIONS);

        Application.getResource("tbs.jar").delete();
        try {

            // Main.UPDATER.getEventSender().addListener(new
            // ConsoleHandler(Main.UPDATER));

            if (!Main.OPTIONS.getGuiless()) {
                Main.GUI = new EDTHelper<StandaloneUpdaterGui>() {

                    @Override
                    public StandaloneUpdaterGui edtRun() {
                        // TODO Auto-generated method stub
                        final StandaloneUpdaterGui upd = new StandaloneUpdaterGui(Main.UPDATER);
                        upd.getFrame().setAlwaysOnTop(true);
                        upd.getFrame().toFront();
                        return upd;
                    }

                }.getReturnValue();

            } else {

                Main.UPDATER.getEventSender().addListener(new ConsoleHandler(Main.UPDATER));
            }
            while (true) {

                try {
                    Main.UPDATER.start();
                    if (Main.OPTIONS.getRestart() != null && Main.OPTIONS.getRestart().trim().length() > 0) {
                        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                            @Override
                            public void run() {
                                final ProcessBuilder pb = new ProcessBuilder(ShellParser.splitCommandString(Main.OPTIONS.getRestart()));
                                System.out.println(ShellParser.splitCommandString(Main.OPTIONS.getRestart()));
                                /*
                                 * needed because the root is different for
                                 * jre/class version
                                 */

                                System.out.println(Main.UPDATER.getInstallDirectory());
                                pb.directory(Main.UPDATER.getInstallDirectory());
                                try {
                                    pb.start();
                                } catch (final IOException e) {
                                }
                            }
                        });

                        ShutdownController.getInstance().requestShutdown();
                    }

                    break;
                } catch (final HTTPIOException e) {
                    if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                        final HTTPException cause = (HTTPException) e.getCause();

                        if (cause.getConnection().getResponseCode() == 500) {
                            // FGCI interface gone
                            if (Main.GUI != null) {
                                Main.GUI.onServiceNotAvailable(cause);

                            } else {
                                System.out.println("Service not available. Try again in 60 sec.");
                                throw e;
                            }
                        } else if (cause.getConnection().getResponseCode() == 503) {
                            // FGCI interface gone
                            if (Main.GUI != null) {
                                Main.GUI.onServiceNotAvailable(cause);

                            } else {
                                System.out.println("Service not available. Try again in 60 sec.");
                                throw e;
                            }
                        } else {
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }

            }
        } catch (final InterruptedException e) {
            Log.exception(Level.WARNING, e);
            if (Main.GUI != null) {
                // Main.GUI.onInterrupt();
            }

        } catch (final Exception e) {
            // if (e.getCause() != null && e.getCause() instanceof
            // RedirectException) {
            // update available:
            // Main.selfUpdate(((RedirectException) e.getCause()).getMessage());

            // } else {
            // Log.exception(Level.WARNING, e);
            // if (Main.GUI != null) {
            // Main.GUI.onException(e);
            // }
            // }

        }

    }

    public static void out(final String string) {

        Main.OUT.println("   " + string);
        if (Main.OUT != System.out) {
            System.out.println("   " + string);
        }
    }

    private static void parseParams(final String[] args) {
        Main.OPTIONS = new Options();
        for (int i = 0; i < args.length; i++) {
            final String p = args[i];
            if (Main.RESTART.matches(p)) {
                final String path = args[++i];
                Main.OPTIONS.setRestart(path);
                Main.RESTART.print();

            } else if (Main.WORKINGDIR.matches(p)) {
                final String path = args[++i];
                Main.OPTIONS.setWorkinfDirectory(path);
                Main.WORKINGDIR.print();

            } else if (Main.DEBUG.matches(p)) {
                Main.OPTIONS.setDebug(true);
            } else if (Main.GUILESS.matches(p)) {
                Main.OPTIONS.setGuiless(true);

            } else if (Main.DISABLED_OS_FILTER.matches(p)) {
                Main.OPTIONS.setOsFilter(false);

            } else if (Main.APP.matches(p)) {
                final String app = args[++i];
                Main.OPTIONS.setApp(app);
            } else if (Main.LOGLEVEL.matches(p)) {
                Log.L.setLevel(Level.parse(p.toUpperCase()));

            } else if (Main.BRANCH.matches(p)) {
                String br = args[++i];
                if (br.equalsIgnoreCase("reset") || br.equalsIgnoreCase("stable")) {
                    br = null;
                }
                Main.OPTIONS.setBranch(br);

            }
        }
    }

    private static void restart(final String path) throws MalformedURLException, URISyntaxException, InterruptedException, DialogClosedException, DialogCanceledException {

        final String tiny[] = new String[] { "java", "-jar", "tbs.jar", path };
        String args[] = null;
        /*
         * only if restart is wanted, we append binary arguments to
         * tinybootstrap
         */

        if (CrossSystem.isWindows()) {
            args = new String[] { "java", "-jar", Main.getJarName() };
            System.out.println(Main.getJarName() + " .exists " + new File(Main.getJarName()).exists());
            /*
             * windows starts exe launcher
             */
            // args = new String[] { Main.getJarName() };
            // System.out.println(Main.getJarName() + " " + new
            // File(Main.getJarName()).exists());
        } else if (CrossSystem.isMac()) {
            /*
             * windows starts exe launcher
             */
            String appname = Main.getJarName();
            try {
                appname = Application.getResource("../../../../").getCanonicalFile().getName();

                System.out.println("APPNAME " + appname + " - " + Application.getResource("../../../../").getCanonicalPath());
                args = new String[] { "open", "-n", "../../../../" + appname };
                System.out.println(appname + ".exists " + new File("../../../../" + appname).exists());

            } catch (final IOException e) {

                e.printStackTrace();
            }
            if (!appname.endsWith(".app")) {
                args = new String[] { "java", "-jar", Main.getJarName() };
                System.out.println(Main.getJarName() + " .exists " + new File(Main.getJarName()).exists());
            }

        } else {
            /*
             * all other start jar version
             */
            args = new String[] { "java", "-jar", Main.getJarName() };
            System.out.println(Main.getJarName() + " .exists " + new File(Main.getJarName()).exists());

        }

        /*
         * build complete call arguments for tinybootstrap
         */
        final StringBuilder sb = new StringBuilder();
        final String tbsargs[] = new String[tiny.length + (args == null ? 0 : args.length)];
        int i = 0;
        for (final String arg : tiny) {
            sb.append(arg + " ");
            tbsargs[i++] = arg;
        }
        if (args != null) {
            for (final String arg : args) {
                sb.append(arg + " ");
                tbsargs[i++] = arg;
            }
        }
        System.out.println("TinyBootStrap: " + sb.toString());

        final ProcessBuilder pb = new ProcessBuilder(tbsargs);
        /*
         * needed because the root is different for jre/class version
         */
        File pbroot = null;

        pbroot = new File(Application.getRoot());

        System.out.println("tbs.exists " + new File(pbroot, "tbs.jar").exists());
        pb.directory(pbroot);
        try {
            if (Main.OPTIONS.getGuiless()) {
                System.out.println("Restart in 5 Seconds");
                Thread.sleep(5000);
            } else {
                Dialog.getInstance().showConfirmDialog(0, T._.restart_title(), T._.restart_msg());
            }
            pb.start();
        } catch (final IOException e) {
        }
        System.exit(0);
    }

    private static void selfUpdate(final String message) {
        try {
            final int index = message.lastIndexOf('?');
            final String url = message.substring(0, index);
            final String hash = message.substring(index + 1).trim();
            final File file = Application.getResource("tmp/" + hash + ".zip");
            if (!file.exists() || Hash.getMD5(file).equals(hash)) {
                if (file.exists() && !file.delete()) { throw new Exception(T._.could_not_update_updater()); }
                Main.downloadInDialog(file, url, hash);
            }
            final ZipIOReader zip = new ZipIOReader(file);
            final File dest = Application.getResource("tmp/update/self");
            Files.deleteRecursiv(dest);
            dest.mkdirs();
            zip.extractTo(dest);
            file.delete();
            file.deleteOnExit();
            final File bootStrapper = Application.getResource("tbs.jar");
            bootStrapper.delete();
            IO.writeToFile(bootStrapper, IO.readURL(Application.getRessourceURL("tbs.jar")));
            Main.restart(dest.getAbsolutePath());

        } catch (final Throwable e) {
            // Main.GUI.onException(e);
        }
    }
}
