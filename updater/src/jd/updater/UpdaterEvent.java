package jd.updater;

import org.appwork.utils.event.SimpleEvent;

public class UpdaterEvent extends SimpleEvent<UpdaterController, Object, UpdaterEvent.Types> {

    public enum Types {
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        ARCHIVE_ADDED,
        /**
         * Parameter[0]=(String)branchname
         */
        BRANCH_UPDATED,
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        END_DOWNLOAD_FILE,

        END_DOWNLOAD_UPDATES,

        END_EXTRACT_ARCHIVES,
        /**
         * Parameter[0]= (ArrayList<File>) extracted Files
         */
        END_EXTRACT_FILE,
        END_FILELIST_UPDATE,
        END_FILTERING,
        END_REPO_UPDATE,
        /**
         * Parameter[0]=(int) errorcounter files which could not be reverted
         */
        END_REVERT,
        END_WAIT_FOR_SLOT,
        EXIT_REQUEST,
        /**
         * Parameter[0]=(Throwable) exception
         */
        INSTALL_FAILED,
        JDOWNLOADER_LOCKED,
        JDOWNLOADER_UNLOCKED,
        /**
         * Parameter[0]=(int) Percent 0-100
         */

        PROGRESS_DOWNLOAD_UPDATES,
        /**
         * Parameter[0]=(int) Percent 0-100
         */

        PROGRESS_EXTRACT_ARCHIVES,
        /**
         * Parameter[0]=(int) Percent 0-100
         */

        PROGRESS_INSTALL,
        /**
         * Parameter[0]=(int) Percent 0-100
         */

        PROGRESS_REVERT,
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        START_DOWNLOAD_FILE,
        START_DOWNLOAD_UPDATES,
        START_EXTRACT_ARCHIVES,
        /**
         * Parameter[0]=(FIleUpdate)file
         */
        START_EXTRACT_FILE,
        START_FILELIST_UPDATE,

        START_FILTERING,
        START_REPO_UPDATE,
        START_REVERT,
        /**
         * Parameter[0]=(Long) Waittime in MS
         */
        START_WAIT_FOR_SLOT,

        /**
         * Parameter[0]=(Long) Waittime in MS<br>
         * Parameter[1]=(Throwable) cause
         * 
         */
        WAIT_PENALTY,
        /**
         * Parameter[0]=(File) removed file
         */
        DELETED_FILE,
        /**
         * Parameter[0]: (File) Final File<br>
         * Parameter[1]: (String) relative Path
         */
        START_INSTALL_FILE,

        /**
         * Parameter[0]: (File) Final File<br>
         * Parameter[1]: (String) relative Path
         */
        END_INSTALL_FILE

    }

    public UpdaterEvent(final UpdaterController caller, final Types type, final Object... parameters) {
        super(caller, type, parameters);
    }

}
