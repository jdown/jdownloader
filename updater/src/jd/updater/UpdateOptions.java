package jd.updater;

import java.io.File;

import org.appwork.utils.os.CrossSystem;

public class UpdateOptions {
    private enum Types {
        /**
         * do not update on MAC Systems
         */

        NOT_MAC,
        /**
         * do not update on windows systems
         */
        NOT_WIN,
        /**
         * Do not update on linux systems
         */
        NOT_LIN,
        /**
         * only update existing files
         */
        ONLY_IF_EXISTS,
        /**
         * Update files al,though the have an .noupdate file
         */
        FORCE,

    }

    public static UpdateOptions create(final String[] options, final WebUpdaterOptions controllerOptions) {
        final UpdateOptions ret = new UpdateOptions();
        ret.setControllerOptions(controllerOptions);
        for (final String o : options) {
            if (Types.NOT_MAC.name().equals(o)) {
                ret.updateOnMac = false;
            } else if (Types.NOT_WIN.name().equals(o)) {
                ret.updateOnWindows = false;
            } else if (Types.NOT_LIN.name().equals(o)) {
                ret.updateOnLinux = false;
            } else if (Types.ONLY_IF_EXISTS.name().equals(o)) {
                ret.updateOnlyExisting = true;
            } else if (Types.FORCE.name().equals(o)) {
                ret.forceNoUpdates = true;
            }
        }
        return ret;
    }

    private WebUpdaterOptions controllerOptions;

    private boolean           forceNoUpdates     = false;

    private boolean           updateOnlyExisting = false;

    private boolean           updateOnMac        = true;

    private boolean           updateOnWindows    = true;

    private boolean           updateOnLinux      = true;

    private UpdateOptions() {

    }

    public WebUpdaterOptions getControllerOptions() {
        return controllerOptions;
    }

    public boolean isForceNoUpdates() {
        return forceNoUpdates;
    }

    public boolean isUpdateAllowed(final FileUpdate file) {

        if (!isForceNoUpdates() && new File(file.getLocalFile(), ".noupdate").exists()) { return false; }
        if (controllerOptions.isDisableOsfilter()) {
            if (CrossSystem.isLinux() && !isUpdateOnLinux()) { return false; }
            if (CrossSystem.isWindows() && !isUpdateOnWindows()) { return false; }
            if (CrossSystem.isMac() && !isUpdateOnMac()) { return false; }
        }

        if (isUpdateOnlyExisting() && !file.exists()) { return false; }
        return true;
    }

    public boolean isUpdateOnLinux() {
        return updateOnLinux;
    }

    public boolean isUpdateOnlyExisting() {
        return updateOnlyExisting;
    }

    public boolean isUpdateOnMac() {
        return updateOnMac;
    }

    public boolean isUpdateOnWindows() {
        return updateOnWindows;
    }

    private void setControllerOptions(final WebUpdaterOptions controllerOptions) {
        this.controllerOptions = controllerOptions;

    }

    public void setForceNoUpdates(final boolean forceNoUpdates) {
        this.forceNoUpdates = forceNoUpdates;
    }

    public void setUpdateOnLinux(final boolean updateOnLinux) {
        this.updateOnLinux = updateOnLinux;
    }

    public void setUpdateOnlyExisting(final boolean updateOnlyExisting) {
        this.updateOnlyExisting = updateOnlyExisting;
    }

    public void setUpdateOnMac(final boolean updateOnMac) {
        this.updateOnMac = updateOnMac;
    }

    public void setUpdateOnWindows(final boolean updateOnWindows) {
        this.updateOnWindows = updateOnWindows;
    }

}
