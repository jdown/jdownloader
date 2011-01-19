package jd.updater;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jd.updater.panel.UpdaterGuiPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.dialog.Dialog;

public class UpdaterGui implements UpdaterListener, ActionListener {

    private final JFrame            frame;
    private final Storage           storage;
    private UpdaterGuiPanel         panel;
    private JButton                 btnDetails;
    private JScrollPane             scrollPane;
    private JTextPane               logField;
    private final UpdaterController updateController;

    public UpdaterGui(final UpdaterController updateController) {
        this.updateController = updateController;
        setLaf();
        storage = JSonStorage.getPlainStorage("WebUpdaterGUI");
        frame = new JFrame("JDownloader Updater");
        Dialog.getInstance().setParentOwner(frame);
        updateController.getEventSender().addListener(this);
        frame.addWindowListener(new WindowListener() {

            public void windowActivated(final WindowEvent arg0) {
            }

            public void windowClosed(final WindowEvent arg0) {
            }

            public void windowClosing(final WindowEvent arg0) {
                updateController.requestExit();
            }

            public void windowDeactivated(final WindowEvent arg0) {
            }

            public void windowDeiconified(final WindowEvent arg0) {
            }

            public void windowIconified(final WindowEvent arg0) {
            }

            public void windowOpened(final WindowEvent arg0) {
            }

        });
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // set appicon
        final ArrayList<Image> list = new ArrayList<Image>();

        try {
            list.add(ImageProvider.getBufferedImage("icon", true));

            frame.setIconImages(list);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        // Set Application dimensions and locations

        final Dimension dim = new Dimension(storage.get("DIMENSION_WIDTH", 300), storage.get("DIMENSION_HEIGHT", 60));
        // restore size
        frame.setSize(dim);
        // this.frame.setPreferredSize(dim);

        frame.setMinimumSize(new Dimension(100, 60));
        layoutGUI();
        // restore location. use center of screen as default.
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = screenSize.width / 2 - frame.getSize().width / 2;
        final int y = screenSize.height / 2 - frame.getSize().height / 2;

        frame.setLocation(storage.get("LOCATION_X", x), storage.get("LOCATION_Y", y));

        frame.pack();
        frame.setVisible(true);
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == btnDetails) {
            btnDetails.setVisible(false);
            scrollPane.setVisible(true);
            frame.pack();

        }
    }

    public void dispose() {
        if (frame.getExtendedState() == Frame.NORMAL && frame.isShowing()) {

            storage.put("LOCATION_X", frame.getLocationOnScreen().x);
            storage.put("LOCATION_Y", frame.getLocationOnScreen().y);
            storage.put("DIMENSION_WIDTH", frame.getSize().width);
            storage.put("DIMENSION_HEIGHT", frame.getSize().height);

        }

        frame.setVisible(false);
        frame.dispose();
    }

    private void layoutGUI() {
        frame.setLayout(new MigLayout("ins ,wrap 1", "[]", " [][grow,fill]"));
        panel = new UpdaterGuiPanel();
        frame.getContentPane().add(new JLabel(ImageProvider.getImageIcon("logo", 32, 32)), "split 2,gapright 10");
        frame.getContentPane().add(panel, "growx,pushx");

        btnDetails = new JButton("Details");
        btnDetails.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDetails.setFocusable(false);
        btnDetails.setContentAreaFilled(false);
        btnDetails.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, panel.getBackground().darker()));
        btnDetails.addActionListener(this);
        logField = new JTextPane();
        logField.setEditable(true);
        logField.setAutoscrolls(true);
        scrollPane = new JScrollPane(logField);
        scrollPane.setVisible(false);
        frame.getContentPane().add(btnDetails, "hidemode 3,alignx right");

        frame.getContentPane().add(scrollPane, "hidemode 3,height 100:300:n,pushx,growx,pushy,growy");

    }

    @Override
    public void onUpdaterEvent(final UpdaterEvent event) {
        switch (event.getType()) {

        case EXIT_REQUEST:
            dispose();
            break;
        }

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

    private void setLaf() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {

        }
    }

    public void start() {
    }
}
