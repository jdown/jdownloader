package jd.updater;

import java.io.File;

public class WebUpdaterOptions {
    private ClassLoader pluginClassloader = null;
    private boolean     restart           = true;

    private boolean     guiless           = false;

    private boolean     disableOsfilter   = false;
    private boolean     restore           = false;

    private String      branch;

    private File        workingDir;

    public String getBranch() {
        return branch;
    }

    public ClassLoader getPluginClassloader() {
        return pluginClassloader;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public boolean isDisableOsfilter() {
        return disableOsfilter;
    }

    public boolean isGuiless() {
        return guiless;
    }

    public boolean isRestart() {
        return restart;
    }

    public boolean isRestore() {
        return restore;
    }

    public void setBranch(final String branch) {
        this.branch = branch;
    }

    public void setDisableOsfilter(final boolean disableOsfilter) {
        this.disableOsfilter = disableOsfilter;
    }

    public void setGuiless(final boolean guiless) {
        this.guiless = guiless;
    }

    public void setPluginClassloader(final ClassLoader pluginClassloader) {
        this.pluginClassloader = pluginClassloader;
    }

    public void setRestart(final boolean restart) {
        this.restart = restart;
    }

    public void setRestore(final boolean restore) {
        this.restore = restore;
    }

    public void setWorkingDir(final File workingDir) {
        this.workingDir = workingDir;
    }
}
