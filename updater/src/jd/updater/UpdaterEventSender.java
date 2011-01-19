package jd.updater;

import org.appwork.utils.event.Eventsender;

public class UpdaterEventSender extends Eventsender<UpdaterListener, UpdaterEvent> {

    @Override
    protected void fireEvent(final UpdaterListener listener, final UpdaterEvent event) {
        switch (event.getType()) {
        case DOWNLOAD_START:
        case EXTRACT_START:
        case FILELIST_UPDATE_START:
        case REPO_UPDATE_START:
        case FILTER_LIST_START:
        case WAIT_FOR_SLOT_START:
            listener.onUpdaterModuleStart(event);
            break;

        case DOWNLOAD_PROGRESS:
        case EXTRACT_PROGRESS:
            listener.onUpdaterModuleProgress(event, (Integer) event.getParameter());
            break;
        case DOWNLOAD_END:
        case EXTRACT_END:
        case FILELIST_UPDATE_END:
        case FILTER_LIST_END:
        case REPO_UPDATE_END:
        case WAIT_FOR_SLOT_END:
            listener.onUpdaterModuleEnd(event);
            break;
        default:
            listener.onUpdaterEvent(event);

        }
    }
}
