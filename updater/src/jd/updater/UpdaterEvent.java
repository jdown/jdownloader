package jd.updater;

import org.appwork.utils.event.SimpleEvent;

public class UpdaterEvent extends SimpleEvent<UpdaterController, Object, UpdaterEvent.Types> {

    public enum Types {
        EXIT_REQUEST,
        /**
         * Parameter[0]=(Long) Waittime in MS
         */
        WAIT_FOR_SLOT_START,
        WAIT_FOR_SLOT_END,

        /**
         * Parameter[0]=(Long) Waittime in MS
         * 
         * 
         */
        PENALTY_WAIT,

        /**
         * Parameter[0]=(String)branchname
         */
        BRANCH_UPDATED,
        REPO_UPDATE_START,
        REPO_UPDATE_END,
        FILELIST_UPDATE_START,
        FILELIST_UPDATE_END,
        FILTER_LIST_START,
        FILTER_LIST_END,
        DOWNLOAD_START,
        DOWNLOAD_END,
        EXTRACT_START,
        EXTRACT_END,
        /**
         * Parameter[0]=(int) Percent 0-100
         */

        EXTRACT_PROGRESS,
        /**
         * Parameter[0]=(int) Percent 0-100
         */

        DOWNLOAD_PROGRESS,
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        DOWNLOAD_FILE_START,
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        DOWNLOAD_FILE_END,
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        ARCHIVE_ADDED

    }

    public UpdaterEvent(final UpdaterController caller, final Types type, final Object... parameters) {
        super(caller, type, parameters);
    }

}
