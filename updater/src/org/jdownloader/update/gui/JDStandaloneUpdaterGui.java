package org.jdownloader.update.gui;

import java.awt.Image;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.appwork.update.updateclient.Updater;
import org.appwork.update.updateclient.gui.StandaloneUpdaterGui;
import org.appwork.utils.images.IconIO;

public class JDStandaloneUpdaterGui extends StandaloneUpdaterGui {

    public JDStandaloneUpdaterGui(final Updater updater, final ActionListener restartAction) {
        super(updater, restartAction);
        this.setFrameIcons();
        this.getFrame().setVisible(true);

    }

    protected void setFrameIcons() {
        final ArrayList<Image> list = new ArrayList<Image>();

        list.add(IconIO.getImage(this.getClass().getResource("resource/updaterIcon128.png")));
        list.add(IconIO.getImage(this.getClass().getResource("resource/updaterIcon64.png")));
        list.add(IconIO.getImage(this.getClass().getResource("resource/updaterIcon32.png")));
        list.add(IconIO.getImage(this.getClass().getResource("resource/updaterIcon16.png")));
        this.getFrame().setIconImages(list);

    }

}
