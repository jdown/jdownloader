package jd.updater;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.zip.ZipException;

import jd.http.Browser;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.zip.ZipIOException;

public class Main {

    private static WebUpdaterOptions OPTIONS;
    private static UpdaterController updateController;

    private static void init() {
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(final String[] args) {
        Application.setApplication(".jd_home");
        Main.init();

        Main.parseParams(args);
        try {
            Main.OPTIONS.setGuiless(true);
            Main.OPTIONS.setBranch("NIGHTLY");
            Main.OPTIONS.setWorkingDir(new File("c:/test/jdupdate"));
            Main.updateController = new UpdaterController(Main.OPTIONS);
            Main.updateController.getEventSender().addListener(new UpdaterListener() {

                @Override
                public void onUpdaterEvent(final UpdaterEvent event) {
                    System.out.println("Updater: " + event.getType());

                }

                @Override
                public void onUpdaterModuleEnd(final UpdaterEvent event) {
                    System.out.println("Finished: " + event.getType());
                    switch (event.getType()) {
                    case FILELIST_UPDATE_END:
                        System.out.println("Files in List: " + (Main.updateController.getNonClassFiles().size() + Main.updateController.getClassFiles().size()));
                        break;
                    case FILTER_LIST_END:
                        System.out.println(Main.updateController.getFilteredClassFiles() + " - " + Main.updateController.getFilteredNonClassFiles());
                        break;
                    }

                }

                @Override
                public void onUpdaterModuleProgress(final UpdaterEvent event, final int parameter) {
                    System.out.println("Progress: " + event.getType() + " : " + parameter);
                }

                @Override
                public void onUpdaterModuleStart(final UpdaterEvent event) {
                    System.out.println("Started: " + event.getType());

                }
            });
            Main.updateController.start();
            System.out.println("Done");
            if (!Main.OPTIONS.isGuiless()) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        new UpdaterGui(Main.updateController).start();
                    }
                };
            }

        } catch (final InterruptedException e) {
            Log.exception(Level.WARNING, e);

        } catch (final UpdateException e) {
            Log.exception(Level.WARNING, e);

        } catch (final ZipException e) {
            Log.exception(Level.WARNING, e);

        } catch (final ZipIOException e) {
            Log.exception(Level.WARNING, e);

        } catch (final IOException e) {
            Log.exception(Level.WARNING, e);

        }
    }

    private static void parseParams(final String[] args) {
        Main.OPTIONS = new WebUpdaterOptions();
        for (int i = 0; i < args.length; i++) {
            final String p = args[i];
            if (p.trim().equalsIgnoreCase("-norestart")) {
                Main.OPTIONS.setRestart(false);
            } else if (p.trim().equalsIgnoreCase("-guiless")) {
                Main.OPTIONS.setGuiless(true);
            } else if (p.trim().equalsIgnoreCase("-noosfilter")) {
                Main.OPTIONS.setDisableOsfilter(true);
            } else if (p.trim().equalsIgnoreCase("-brdebug")) {
                Browser.setGlobalVerbose(true);
            } else if (p.trim().equalsIgnoreCase("-restore")) {
                Main.OPTIONS.setRestore(true);
            } else if (p.trim().equalsIgnoreCase("-branch")) {
                String br = args[++i];
                if (br.equalsIgnoreCase("reset")) {
                    br = null;
                }
                Main.OPTIONS.setBranch(br);

            }
        }
    }
}
