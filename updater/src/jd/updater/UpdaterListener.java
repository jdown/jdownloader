package jd.updater;

import java.util.EventListener;

public interface UpdaterListener extends EventListener {

    void onUpdaterEvent(UpdaterEvent event);

    void onUpdaterModuleEnd(UpdaterEvent event);

    void onUpdaterModuleProgress(UpdaterEvent event, int parameter);

    void onUpdaterModuleStart(UpdaterEvent event);

}
