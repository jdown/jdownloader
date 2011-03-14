package org.jdownloader.update;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.update.updateclient.Updater;

public class CtrlCHandler extends ShutdownEvent {

    private final Updater updater;

    public CtrlCHandler(final Updater uPDATER) {
        this.updater = uPDATER;
    }

    @Override
    public void run() {
        if (this.updater.getState() != this.updater.stateInit) {
            this.updater.requestExit();
            while (this.updater.getThread() != null && this.updater.getThread().isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        }

    }

}
