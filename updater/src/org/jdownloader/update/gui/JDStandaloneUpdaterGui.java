package org.jdownloader.update.gui;

import java.awt.Image;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.appwork.resources.AWUTheme;
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


        list.add(        AWUTheme.I().getImage("updatericon", 64));
        list.add(        AWUTheme.I().getImage("updatericon", 48));
        list.add(        AWUTheme.I().getImage("updatericon", 32));
        list.add(        AWUTheme.I().getImage("updatericon", 16));

        this.getFrame().setIconImages(list);

    }

}
