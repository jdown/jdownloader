package jd.updater;

import java.io.File;
import java.util.logging.Level;

import jd.http.Browser;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;

public class Main {

    private static final SwitchParam NO_RESTART         = new SwitchParam("norestart", "| Do no restart after update");

    private static WebUpdaterOptions OPTIONS;

    private static UpdaterController updateController;

    private static UpdaterGui        GUI                = null;

    private static final SwitchParam GUILESS            = new SwitchParam("guiless", "| Run silently or in console mode");

    private static final SwitchParam FAST               = new SwitchParam("fast", "| Ignore waittimes and update as fast as possible");

    private static final SwitchParam DISABLED_OS_FILTER = new SwitchParam("noosfilter", "| Update all files. Even files which are marked as invalid for your os");

    private static final SwitchParam BRDEBUG            = new SwitchParam("brdebug", "| Writes all http connections and contents to console");

    private static final SwitchParam LOGLEVEL           = new SwitchParam("log", "LEVEL | Set the Loglevel: ALL,FINER,FINE,INFO,WARNING,SEVERE");

    private static final SwitchParam RESTORE            = new SwitchParam("restore", "| Redownloads all archives and extracts them");

    private static final SwitchParam BRANCH             = new SwitchParam("branch", "BRANCHNAME | Sets the desired Branch");

    private static void init() {
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(final String[] args) {
        Application.setApplication(".jd_home");
        Main.init();
        if (Application.isJared(Main.class)) {
            Log.L.setLevel(Level.INFO);
        }
        Log.L.setLevel(Level.INFO);
        Main.parseParams(args);
        try {
            // Main.OPTIONS.setGuiless(true);
            Main.OPTIONS.setBranch("last09");
            Main.OPTIONS.setWorkingDir(new File("c:/test/jdupdate2"));
            Main.OPTIONS.setIgnoreSlotWaittime(true);
            Main.updateController = new UpdaterController(Main.OPTIONS);
            if (!Main.OPTIONS.isGuiless()) {
                Main.GUI = new EDTHelper<UpdaterGui>() {

                    @Override
                    public UpdaterGui edtRun() {
                        // TODO Auto-generated method stub
                        return new UpdaterGui(Main.updateController);
                    }

                }.getReturnValue();

            } else {

                Main.updateController.getEventSender().addListener(new ConsoleHandler(Main.updateController));
            }
            Main.updateController.downloadUpdates();

            Main.updateController.install();

            System.out.println("Done");

        } catch (final InterruptedException e) {
            Log.exception(Level.WARNING, e);
            Main.GUI.onInterrupt();

        } catch (final Exception e) {
            Log.exception(Level.WARNING, e);
            Main.GUI.onException(e);

        }

    }

    private static void parseParams(final String[] args) {
        Main.OPTIONS = new WebUpdaterOptions();
        for (int i = 0; i < args.length; i++) {
            final String p = args[i];
            if (Main.NO_RESTART.matches(p)) {
                Main.OPTIONS.setRestart(false);
                Main.NO_RESTART.print();
            } else if (Main.GUILESS.matches(p)) {
                Main.OPTIONS.setGuiless(true);
            } else if (Main.FAST.matches(p)) {
                Main.OPTIONS.setIgnoreSlotWaittime(true);
            } else if (Main.DISABLED_OS_FILTER.matches(p)) {
                Main.OPTIONS.setDisableOsfilter(true);
            } else if (Main.BRDEBUG.matches(p)) {
                Browser.setGlobalVerbose(true);
            } else if (Main.LOGLEVEL.matches(p)) {
                Log.L.setLevel(Level.parse(p.toUpperCase()));
            } else if (Main.RESTORE.matches(p)) {
                Main.OPTIONS.setRestore(true);
            } else if (Main.BRANCH.matches(p)) {
                String br = args[++i];
                if (br.equalsIgnoreCase("reset") || br.equalsIgnoreCase("stable")) {
                    br = null;
                }
                Main.OPTIONS.setBranch(br);

            }
        }
    }
}
