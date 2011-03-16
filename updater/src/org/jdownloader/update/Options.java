package org.jdownloader.update;

import java.awt.GraphicsEnvironment;

import org.appwork.storage.config.JsonConfig;
import org.appwork.update.updateclient.UpdaterOptions;

public class Options implements UpdaterOptions {

    private String               app;
    private String               branch;
    private boolean              guiless;
    private boolean              osFilter;
    private String               restart;
    private String               workingDirectory;

    private final UpdaterOptions op;
    private boolean              debug;
    private boolean              noUpdates = false;

    public Options() {
        // some options shall not be written back to configfile.
        this.op = JsonConfig.create(UpdaterOptions.class);
        this.app = this.op.getApp();
        if (this.app == null) {
            this.app = "JDownloader";
        }

        this.branch = this.op.getBranch();
        this.guiless = this.op.getGuiless() || GraphicsEnvironment.isHeadless();
        this.osFilter = this.op.getOsFilter();
        this.restart = this.op.getRestart();
        this.workingDirectory = this.op.getWorkingDirectory();
        this.debug = this.op.getDebug();

    }

    @Override
    public String getApp() {
        return this.app;
    }

    @Override
    public String getBranch() {
        return this.branch;
    }

    @Override
    public String getCurrentBranch() {
        return this.op.getCurrentBranch();
    }

    @Override
    public boolean getDebug() {
        return this.debug;
    }

    @Override
    public boolean getGuiless() {
        return this.guiless || this.noUpdates;
    }

    @Override
    public boolean getOsFilter() {
        return this.osFilter;
    }

    @Override
    public long getPackagePollInterval() {
        // do not allow shorter intervals
        return Math.max(5000, this.op.getPackagePollInterval());
    }

    @Override
    public String getRestart() {
        return this.restart;
    }

    @Override
    public String[] getUpdServer() {

        return this.op.getUpdServer();
    }

    @Override
    public String getWorkingDirectory() {
        return this.workingDirectory;
    }

    public boolean isNoUpdates() {
        return this.noUpdates;
    }

    @Override
    public void setApp(final String app) {
        this.app = app;

    }

    @Override
    public void setBranch(final String branch) {
        this.branch = branch;
        this.op.setBranch(branch);
    }

    @Override
    public void setCurrentBranch(final String name) {
        this.op.setCurrentBranch(name);
    }

    @Override
    public void setDebug(final boolean b) {
        this.debug = b;

    }

    @Override
    public void setGuiless(final boolean b) {
        this.guiless = b;
    }

    public void setNoUpdate(final boolean b) {
        this.noUpdates = b;
    }

    @Override
    public void setOsFilter(final boolean b) {
        this.osFilter = b;
    }

    @Override
    public void setRestart(final String b) {
        this.restart = b;
    }

    @Override
    public void setWorkinfDirectory(final String dir) {
        this.workingDirectory = dir;
    }

}
