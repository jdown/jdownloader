package org.jdownloader.update;

import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;

public abstract class UpdaterAdapter implements UpdaterListener {

    @Override
    public void onStateEnter(final UpdaterState state) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStateExit(final UpdaterState event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdaterEvent(final UpdaterEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdaterModuleEnd(final UpdaterEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdaterModuleProgress(final UpdaterEvent event, final int parameter) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdaterModuleStart(final UpdaterEvent event) {
        // TODO Auto-generated method stub

    }

}
