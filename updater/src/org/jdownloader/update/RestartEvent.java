package org.jdownloader.update;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;

public class RestartEvent extends ShutdownEvent {

    private static String getJarName() throws MalformedURLException, URISyntaxException {
        final String url = Application.getRessourceURL("tbs.jar").toString();
        System.out.println(url);
        final int index = url.indexOf(".jar!");
        System.out.println(url.substring(4, index + 4));
        return new File(new URL(url.substring(4, index + 4)).toURI()).getName();
    }

    private String         javaInterpreter;
    private final File     dest;
    private final String[] orgArgs;

    public RestartEvent(final File dest, final String[] orgArgs) {
        this.dest = dest;
        this.orgArgs = orgArgs;

        this.setHookPriority(Integer.MIN_VALUE);
        this.javaInterpreter = "java";
        try {
            final String javaInterpreter = new File(new File(System.getProperty("sun.boot.library.path")), "javaw.exe").getAbsolutePath();
            if (new File(javaInterpreter).exists()) {
                this.javaInterpreter = javaInterpreter;
            }
        } catch (final Throwable e) {
            // nothing
        }
    }

    protected String getRestartingJar() {

        try {
            return RestartEvent.getJarName();
        } catch (final Throwable e) {
            Log.exception(Level.WARNING, e);

        }
        return "Updater.jar";

    }

    public ArrayList<String> getRestartParameters() throws MalformedURLException, URISyntaxException {

        final ArrayList<String> nativeParameters = new ArrayList<String>();

        nativeParameters.add(this.javaInterpreter);
        nativeParameters.add("-jar");
        nativeParameters.add(this.getRestartingJar());
        nativeParameters.add("-tbs");
        for (final String a : this.orgArgs) {
            nativeParameters.add(a);
        }
        return nativeParameters;
    }

    @Override
    public void run() {
        String jarName;
        // if (!Application.isJared(this.getClass())) { return; }
        try {
            jarName = this.getRestartingJar();

            final File root = Application.getResource(jarName);
            if (!root.exists()) {
                System.err.println("WARNING: " + root + " is missing");
            }
            final ArrayList<String> call = new ArrayList<String>();
            call.add(this.javaInterpreter);
            call.add("-jar");
            call.add("tbs.jar");
            call.add(this.dest.getAbsolutePath());
            call.addAll(this.getRestartParameters());
            System.out.println("Call Restart: " + call);
            final String[] tiny = call.toArray(new String[] {});

            final ProcessBuilder pb = new ProcessBuilder(tiny);
            /*
             * needed because the root is different for jre/class version
             */
            File pbroot = null;

            pbroot = new File(Application.getHome());

            System.out.println("Root: " + pbroot);
            pb.directory(pbroot);
            try {
                pb.start();
            } catch (final IOException e) {
            }
        } catch (final MalformedURLException e1) {
            Log.exception(Level.WARNING, e1);

        } catch (final URISyntaxException e1) {
            Log.exception(Level.WARNING, e1);

        }
    }

}
