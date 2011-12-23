package org.jdownloader.update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;

import org.appwork.app.launcher.parameterparser.CommandSwitch;
import org.appwork.app.launcher.parameterparser.CommandSwitchListener;
import org.appwork.app.launcher.parameterparser.ParameterParser;
import org.appwork.resources.AWUTheme;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.simplejson.mapper.JSonMapper;
import org.appwork.update.updateclient.ConsoleHandler;
import org.appwork.update.updateclient.CtrlCHandler;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.gui.StandaloneUpdaterGui;
import org.appwork.update.updateclient.http.ClientUpdateRequiredException;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging.LogFormatter;
import org.appwork.utils.net.HTTPException;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.ShellParser;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.update.gui.JDStandaloneUpdaterGui;
import org.jdownloader.update.translate.T;

public class Main {
    static {
        Application.setApplication(".jd_home");
        Log.L.setLevel(Level.OFF);
        final JSonMapper mapper = ((SimpleMapper) JSonStorage.getMapper()).getMapper();
        // ignore Mapping errors. If client is not up2date, illegal server
        // responses shall not kill him.
        mapper.setIgnoreIllegalArgumentMappings(true);

        // do this call to keep the correct root in Application Cache

        Application.getRoot(Main.class);
        AWUTheme.I().setNameSpace("org/jdownloader/updater/");

    }
    private static final SwitchParam    AUTOCLOSE          = new SwitchParam("autoclose", "Restartpath after update","TIMEOUT_IN_SECONDS");
    private static final SwitchParam    RESTART            = new SwitchParam("restart", "Restartpath after update","RESTART_COMMAND");

    private static final SwitchParam    WORKINGDIR         = new SwitchParam("dir", "Set Installdirectory");
    private static final SwitchParam    INSTALL_PACKAGE    = new SwitchParam("install", "Install optional package","PACKAGE_ID_OR_FILENAME");
    private static final SwitchParam    UNINSTALL_PACKAGE  = new SwitchParam("uninstall", "Uninstall optional package","PACKAGE_ID_OR_FILENAME");
    private static final SwitchParam    HELP               = new SwitchParam("help", "Show this help");
    private static final SwitchParam    HELP2              = new SwitchParam("?", "Show this help");
    private static StandaloneUpdaterGui GUI                = null;

    private static Options              OPTIONS;

    private static Updater              UPDATER;
    private static PrintStream          OUT                = null;

    private static ParameterParser pp;
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
  
    private static final SwitchParam    GUILESS            = new SwitchParam("guiless", "Run silently or in console mode");

    private static final SwitchParam    DISABLED_OS_FILTER = new SwitchParam("noosfilter", "Update all files. Even files which are marked as invalid for your os");

    private static final SwitchParam    LOGLEVEL           = new SwitchParam("log", "Set the Loglevel: ALL,FINER,FINE,INFO,WARNING,SEVERE","LEVEL");

    private static final SwitchParam    BRANCH             = new SwitchParam("branch", "Sets the desired Branch","BRANCHNAME");

    private static final SwitchParam    APP                = new SwitchParam("app",  "Sets the desired AppID","APPID");
    private static final SwitchParam    NOUPDATE           = new SwitchParam("noupdate", "Bypasses updatesystem");

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
            // Log.L.setLevel(Level.INFO);
        } else {
            Log.L.setLevel(Level.ALL);
        }
        
        Main.OPTIONS = new Options();
         pp = new ParameterParser(args);
         pp.getEventSender().addListener(new CommandSwitchListener() {
            
            @Override
            public void executeCommandSwitch(CommandSwitch event) {
                
     
             
         
                    final String p = event.getSwitchCommand();
                    System.out.println("Parameter " + p);
                  if (Main.RESTART.matches(p)) {
                     
                        Main.OPTIONS.setRestartCommand(event.getParameters()[0]);
                        System.out.println(Main.RESTART);

                    } else if (Main.WORKINGDIR.matches(p)) {
                     
                        Main.OPTIONS.setWorkingDirectory(event.getParameters()[0]);
                        System.out.println(Main.WORKINGDIR);
                    } else if (Main.INSTALL_PACKAGE.matches(p)) {
                    
                        Main.OPTIONS.setOptionalList(event.getParameters()[0].split("\\,\\s*"));

                    } else if (Main.UNINSTALL_PACKAGE.matches(p)) {
                
                        Main.OPTIONS.setUninstallList(event.getParameters()[0].split("\\,\\s*"));

                 
                    } else if (Main.GUILESS.matches(p)) {
                        Main.OPTIONS.setGuiless(true);
                    } else if (Main.AUTOCLOSE.matches(p)) {
                        OPTIONS.setAutoCloseTimeout(Integer.parseInt(event.getParameters()[0]));
                        System.out.println(AUTOCLOSE);
                    } else if (Main.NOUPDATE.matches(p)) {
                        Main.OPTIONS.setNoUpdate(true);

                    } else if (Main.DISABLED_OS_FILTER.matches(p)) {
                        Main.OPTIONS.setOsFilterEnabled(false);

                    } else if (Main.APP.matches(p)) {
                        final String app = event.getParameters()[0];
                        Main.OPTIONS.setApp(app);
                    } else if (Main.LOGLEVEL.matches(p)) {
                        Log.L.setLevel(Level.parse(event.getParameters()[0].toUpperCase()));

                    } else if (Main.BRANCH.matches(p)) {
                      
                        if (event.getParameters().length==0||event.getParameters()[0].equalsIgnoreCase("reset") || event.getParameters()[0].equalsIgnoreCase("stable")) {
                            Main.OPTIONS.setBranch(null);
                        }else{
                        String br = event.getParameters()[0];
                        Main.OPTIONS.setBranch(br);
                        }
                    }else{
                        System.out.println(Main.HELP);
                        System.out.println(Main.APP);
                        System.out.println(Main.BRANCH);
                    
                        System.out.println(Main.DISABLED_OS_FILTER);
                        System.out.println(Main.GUILESS);
                        System.out.println(Main.INSTALL_PACKAGE);
                        System.out.println(Main.LOGLEVEL);
                        System.out.println(Main.NOUPDATE);
                        System.out.println(Main.RESTART);
                        System.out.println(Main.UNINSTALL_PACKAGE);
                        System.out.println(Main.WORKINGDIR);
                    }
                
            }
        });
         pp.parse();
       
        Main.out(T._.start());

        Log.L.info(JSonStorage.toString(Main.OPTIONS));

        Main.UPDATER = new Updater(new UpdaterHttpClientImpl(), Main.OPTIONS);
        if (!Application.isJared(Main.class)) {
            // Main.UPDATER.setVersion(-1);
        }

        Application.getResource("tbs.jar").delete();
        try {

            // Main.UPDATER.getEventSender().addListener(new
            // ConsoleHandler(Main.UPDATER));
            // OPTIONS.setGuiless(true);
            if (!Main.OPTIONS.isGuiless()) {
                Main.GUI = new EDTHelper<StandaloneUpdaterGui>() {

                    @Override
                    public StandaloneUpdaterGui edtRun() {
                        final JDStandaloneUpdaterGui upd = new JDStandaloneUpdaterGui(Main.UPDATER, new ActionListener() {

                            @Override
                            public void actionPerformed(final ActionEvent e) {
                                String rest = Main.OPTIONS.getRestartCommand();
                                if (rest == null || rest.trim().length() == 0) {
                                    if (CrossSystem.isWindows()) {
                                        rest = "JDownloader.exe -rfu -scan";
                                    } else if (CrossSystem.isLinux()) {
                                        rest = CrossSystem.getJavaBinary() + " -jar JDownloader.jar -rfu -scan";
                                    } else {
                                        rest = "open ../../../JDownloader.app -rfu";
                                    }

                                    Main.OPTIONS.setRestartCommand(rest);
                                    Main.restart();
                                }

                            }
                        });
                        // upd.getFrame().setAlwaysOnTop(true);
                        upd.getFrame().toFront();
                        return upd;
                    }

                }.getReturnValue();
               
            } else {

                Main.UPDATER.getEventSender().addListener(new ConsoleHandler(Main.UPDATER) {
                    protected void out(String str) {
                        Main.out(str);
                    }
                });
                ShutdownController.getInstance().addShutdownEvent(new CtrlCHandler(UPDATER));
            }
            while (true) {

                try {
                    if (!Main.OPTIONS.isNoUpdates()) {
                        Main.UPDATER.runStateApp();
                    } else {
                        Main.out(T._.guiless_noupdates());
                    }
                    boolean doRestart = Main.OPTIONS.getRestartCommand() != null && Main.OPTIONS.getRestartCommand().trim().length() > 0;

                    if (!Main.OPTIONS.isGuiless() && OPTIONS.getAutoCloseTimeout() >= 0) {
                        long endtime = System.currentTimeMillis() + OPTIONS.getAutoCloseTimeout() * 1000;
                        while (System.currentTimeMillis() < endtime && OPTIONS.getAutoCloseTimeout() >= 0) {

                            GUI.setAutoClose(endtime - System.currentTimeMillis(), doRestart, new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    OPTIONS.setAutoCloseTimeout(-1);
                                }
                            });
                            Thread.sleep(1000);
                        }
                        if (OPTIONS.getAutoCloseTimeout() >= 0) {
                            if (doRestart) {
                                Main.restart();

                            } else {
                                Main.out(T._.literally_exit());

                                ShutdownController.getInstance().requestShutdown();
                            }
                        }

                    } else if (doRestart) {
                        Main.restart();

                    }

                    break;
                } catch (final HTTPIOException e) {
                    if (Main.UPDATER.isInterrupted()) { throw e; }
                    if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                        final HTTPException cause = (HTTPException) e.getCause();

                        if (cause.getConnection().getResponseCode() == 500) {
                            // FGCI interface gone
                            if (Main.GUI != null) {
                                Main.GUI.onServiceNotAvailable(cause);

                            } else {

                                Main.out(T._.error_service_not_available_right_now());
                                throw e;
                            }
                        } else if (cause.getConnection().getResponseCode() == 503) {
                            // FGCI interface gone
                            if (Main.GUI != null) {
                                Main.GUI.onServiceNotAvailable(cause);

                            } else {
                                Main.out(T._.error_service_not_available_right_now());
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

        } catch (final ClientUpdateRequiredException e) {
            e.printStackTrace();
     
            if(pp.hasCommandSwitch("tbs")) {
                    if (Main.OPTIONS.isGuiless()) {
                        Main.out(T._.updateloop());
                        ShutdownController.getInstance().requestShutdown();
                    } else {
                        Dialog.getInstance().showMessageDialog(Dialog.LOGIC_COUNTDOWN, T._.updateloop_title(), T._.updateloop());
                        ShutdownController.getInstance().requestShutdown();
                    }

                    return;
                }
            
            UPDATER.selfUpdate(e, pp.getRawArguments());

        } catch (final Exception e) {
            if (!Main.UPDATER.isInterrupted()) {
                Log.exception(Level.INFO, e);
            }

        }
        if (Main.OPTIONS.isGuiless()) {
            Main.out(T._.literally_exit());
            ShutdownController.getInstance().requestShutdown();
        }
    }

    public static void out(final String string) {

        Main.OUT.println("   " + string);
        if (Main.OUT != System.out) {
            System.out.println("   " + string);
        }
    }

  

    private static void restart() {
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            {
                this.setHookPriority(Integer.MIN_VALUE);
            }

            @Override
            public void run() {
                final ProcessBuilder pb = new ProcessBuilder(ShellParser.splitCommandString(Main.OPTIONS.getRestartCommand()));
                System.out.println("Restart: " + ShellParser.splitCommandString(Main.OPTIONS.getRestartCommand()));
                /*
                 * needed because the root is different for jre/class version
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

}
