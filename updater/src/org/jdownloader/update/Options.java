package org.jdownloader.update;

import java.awt.GraphicsEnvironment;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.update.updateclient.UpdaterOptions;

public class Options implements UpdaterOptions {

    private String               app;
    private String               branch;
    private boolean              guiless;
    private boolean              osFilter;
    private String               restart;
    private String               workingDirectory;

    private final UpdaterOptions op;
 
    private boolean              fullUpdate;
    private boolean              noUpdates        = false;
    private String[]             optionalList;
    private String[]             uninstallList;
    private int                  autoCloseTimeout = -1;
    private String savedBranch;
   
    public Options() {
        // some options shall not be written back to configfile.
        this.op = JsonConfig.create(UpdaterOptions.class);
        this.app = this.op.getApp();
        if (this.app == null) {
            this.app = "JDownloader";
        }

        this.branch = this.op.getBranch();
        savedBranch=branch;
      
        this.guiless = this.op.isGuiless() || GraphicsEnvironment.isHeadless();
        this.osFilter = this.op.isOsFilterEnabled();
        this.restart = this.op.getRestartCommand();
        this.workingDirectory = this.op.getWorkingDirectory();
     
        this.fullUpdate = this.op.isFullUpdate();
        this.optionalList = this.op.getOptionalList();
        if (this.optionalList == null) {
            this.optionalList = new String[] {};
        }

    }

    @Override
    public String getApp() {
        return this.app;
    }

    @Override
    public String getBranch() {
        return this.branch;
    }
//
//    @Override
//    public String getCurrentBranch() {
//        return this.op.getCurrentBranch();
//    }

    @Override
    public String[] getOptionalList() {
        return this.optionalList;
    }

    @Override
    public long getPackagePollInterval() {
        // do not allow shorter intervals
        return Math.max(5000, this.op.getPackagePollInterval());
    }

    @Override
    public String getRestartCommand() {
        return this.restart;
    }

    @Override
    public StorageHandler<?> getStorageHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getUninstallList() {
        return this.uninstallList;
    }

    @Override
    public String[] getUpdServerList() {

        return this.op.getUpdServerList();
    }

    @Override
    public String getWorkingDirectory() {
        return this.workingDirectory;
    }

 
    public boolean isFullUpdate() {
        return this.fullUpdate;
    }

    @Override
    public boolean isGuiless() {
        return this.guiless || this.noUpdates;
    }

    public boolean isNoUpdates() {
        return this.noUpdates;
    }

    @Override
    public boolean isOsFilterEnabled() {
        return this.osFilter;
    }

    @Override
    public void setApp(final String app) {
        this.app = app;

    }

    @Override
    public void setBranch(final String branch) {
        if (this.branch == branch) return;

      
            this.branch = branch;        
            this.op.setBranch(branch);
        
    }

    public boolean isSavedBranch() {
        return savedBranch!=null;
    }

 
    public String getSavedBranch() {
        return savedBranch;
    }

    public void setFullUpdate(final boolean fullUpdate) {
        this.fullUpdate = fullUpdate;
    }

    @Override
    public void setGuiless(final boolean b) {
        this.guiless = b;
    }

    public void setNoUpdate(final boolean b) {
        this.noUpdates = b;
    }

    public void setOptionalList(final String[] split) {
        this.optionalList = split;

    }

    @Override
    public void setOsFilterEnabled(final boolean b) {
        this.osFilter = b;
    }

    @Override
    public void setRestartCommand(final String b) {
        this.restart = b;
    }

    public void setUninstallList(final String[] split) {
        this.uninstallList = split;

    }

    @Override
    public void setWorkingDirectory(final String dir) {
        this.workingDirectory = dir;
    }

    public void setAutoCloseTimeout(int parseInt) {
        this.autoCloseTimeout = parseInt;
    }

    public int getAutoCloseTimeout() {
        return autoCloseTimeout;
    }

}
