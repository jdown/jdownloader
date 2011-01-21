package jd.updater;

import org.appwork.utils.event.Eventsender;

public class UpdaterEventSender extends Eventsender<UpdaterListener, UpdaterEvent> {

    @Override
    protected void fireEvent(final UpdaterListener listener, final UpdaterEvent event) {
        switch (event.getType()) {
        case START_DOWNLOAD_UPDATES:
        case START_EXTRACT_ARCHIVES:
        case START_FILELIST_UPDATE:
        case START_REPO_UPDATE:
        case START_FILTERING:
        case START_WAIT_FOR_SLOT:
        case START_DOWNLOAD_FILE:
        case START_EXTRACT_FILE:
        case START_REVERT:
            listener.onUpdaterModuleStart(event);
            break;

        case PROGRESS_DOWNLOAD_UPDATES:
        case PROGRESS_EXTRACT_ARCHIVES:
        case PROGRESS_INSTALL:
        case PROGRESS_REVERT:
            listener.onUpdaterModuleProgress(event, (Integer) event.getParameter());
            break;
        case END_DOWNLOAD_UPDATES:
        case END_EXTRACT_ARCHIVES:
        case END_FILELIST_UPDATE:
        case END_FILTERING:
        case END_REPO_UPDATE:
        case END_WAIT_FOR_SLOT:
        case END_DOWNLOAD_FILE:
        case END_EXTRACT_FILE:
        case END_REVERT:

            listener.onUpdaterModuleEnd(event);
            break;
        default:
            listener.onUpdaterEvent(event);

        }
    }
}
