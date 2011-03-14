package org.jdownloader.update;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import org.appwork.update.exchange.Mirror;
import org.appwork.update.exchange.UpdatePackage;
import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging.Log;
import org.jdownloader.update.translate.T;

public class ConsoleHandler implements UpdaterListener {

    private final Updater updater;
    private int           lastPercent;
    private File          locked = null;

    public ConsoleHandler(final Updater updater) {
        this.updater = updater;
    }

    @Override
    public void onStateEnter(final UpdaterState state) {
        System.out.println(state);
        if (state == this.updater.stateBranchUpdate) {

        } else if (state == this.updater.stateFilter) {
            Main.out(T._.guiless_filtering());
        } else if (state == this.updater.stateDone) {
            Main.out(T._.guiless_done());
        } else if (state == this.updater.stateDownloadData) {
            Main.out(T._.guiless_download_data());

        } else if (state == this.updater.stateExtract) {
            Main.out(T._.guiless_extract());
        } else if (state == this.updater.stateInstall) {
            final ArrayList<File> files = this.updater.getFilesToInstall();
            Main.out(T._.guiless_start_install(files.size()));
        } else if (state == this.updater.stateCreatePackage) {

            Main.out(T._.guiless_create_package());
        } else if (state == this.updater.stateError) {

            Throwable cause = this.updater.getException();
            if (cause != null) {
                Log.exception(Level.SEVERE, cause);
            }
            while (cause != null && cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause != null) {
                Main.out(T._.guiless_update_failed(cause.getLocalizedMessage()));
            } else {
                Main.out(T._.guiless_update_failed_unknown());
            }
        }

    }

    @Override
    public void onStateExit(final UpdaterState state) {
        System.out.println(state);
        if (this.updater.getNext() != this.updater.stateError) {

            if (state == this.updater.stateDownloadHashList) {
                Main.out(T._.guiless_downloaded_hashlist(this.updater.getHashList().size()));
            } else if (state == this.updater.stateFilter) {
                if (this.updater.getNext() == this.updater.stateDone) {
                    Main.out(T._.guiless_you_are_up2date());

                } else {
                    final ArrayList<File> files = this.updater.getFilesToInstall();
                    final UpdatePackage updates = this.updater.getUpdates();
                    Main.out(T._.guiless_files_ready_for_install(files.size()));
                    Main.out(T._.guiless_updates_waiting_for_download(updates.size()));
                }

            } else if (state == this.updater.stateDownloadData) {
                Main.out(T._.guiless_downloadfinished());
            } else if (state == this.updater.stateInstall) {
                Main.out(T._.guiless_installFinished());
            }
        }
    }

    @Override
    public void onUpdaterEvent(final UpdaterEvent event) {
        System.out.println(event);
        switch (event.getType()) {
            case BRANCH_UPDATED:
                Main.out(T._.guiless_branch_updated(this.updater.getAppID(), event.getParameter() + ""));
                break;
            case LOCKED:
                if (this.locked == null || !this.locked.equals(event.getParameter())) {
                    this.locked = (File) event.getParameter();
                    Main.out(T._.guiless_locked_close_app(this.updater.getAppID(), this.locked.getAbsolutePath()));
                }
                break;
            case UNLOCKED:
                if (this.locked != null) {
                    Main.out(T._.guiless_locked_closed_app(this.updater.getAppID()));
                }
                break;

        }

    }

    @Override
    public void onUpdaterModuleEnd(final UpdaterEvent event) {
        System.out.println(event);
    }

    @Override
    public void onUpdaterModuleProgress(final UpdaterEvent event, final int percent) {
        if (this.lastPercent == percent) { return; }
        this.lastPercent = percent;
        Main.out(T._.guiless_progress(percent));
    }

    @Override
    public void onUpdaterModuleStart(final UpdaterEvent event) {
        System.out.println(event);

        // System.out.println(event.getParameter());
        switch (event.getType()) {
            case START_DOWNLOAD_FILE:
                final Mirror mirror = (Mirror) event.getParameter();
                Main.out(T._.guiless_started_download(SizeFormatter.formatBytes(mirror.getSize())));
                break;
            case START_REVERT:
                Main.out(T._.guiless_error_occured_start_reverting());
                break;

        }
    }
}
