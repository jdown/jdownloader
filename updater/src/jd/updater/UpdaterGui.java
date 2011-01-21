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
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jd.updater.panel.UpdaterGuiPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;

public class UpdaterGui implements UpdaterListener, ActionListener, StateEventListener {

    private final JFrame            frame;
    private final Storage           storage;
    private UpdaterGuiPanel         panel;
    private JButton                 btnDetails;
    private JScrollPane             scrollPane;
    private JTextPane               logField;

    private final UpdaterController updateController;
    private ProgressLogo            progressLogo;
    private JLabel                  branchLabel;
    private JLabel                  lblDetailsLabel;
    private int                     currentStepSize = 2;

    public UpdaterGui(final UpdaterController updateController) {
        this.updateController = updateController;
        setLaf();
        storage = JSonStorage.getPlainStorage("WebUpdaterGUI");
        frame = new JFrame("JDownloader Updater");
        Dialog.getInstance().setParentOwner(frame);

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

        if (updateController != null) {

            this.updateController.getEventSender().addListener(this);
            updateController.getStateMachine().addListener(this);
        } else {
            System.out.println("TEST");
        }
        // restore location. use center of screen as default.
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = screenSize.width / 2 - frame.getSize().width / 2;
        final int y = screenSize.height / 2 - frame.getSize().height / 2;

        frame.setLocation(storage.get("LOCATION_X", x), storage.get("LOCATION_Y", y));

        frame.setVisible(true);

        frame.pack();

    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == btnDetails) {
            btnDetails.setVisible(false);
            scrollPane.setVisible(true);
            lblDetailsLabel.setVisible(true);
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
        frame.setLayout(new MigLayout("ins 0,wrap 1", "[]", " [][]"));
        panel = new UpdaterGuiPanel();
        progressLogo = new ProgressLogo();
        branchLabel = new JLabel();
        lblDetailsLabel = new JLabel("Update Details:");
        lblDetailsLabel.setVisible(false);
        SwingUtils.boldJLabel(lblDetailsLabel);
        branchLabel.setForeground(panel.getBackground().darker());

        frame.getContentPane().add(progressLogo, "split 2,gapright 10,gaptop 5,gapleft 5");
        frame.add(panel, "growx,pushx,gapright 5, gaptop 5");

        btnDetails = new JButton("Details");
        SwingUtils.boldJButton(btnDetails);
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
        frame.add(btnDetails, "hidemode 3,shrinky,alignx right,aligny top,gapright 5");
        frame.add(lblDetailsLabel, "hidemode 3,gaptop 5,gapleft 5");
        frame.add(scrollPane, "hidemode 3,height 100:300:n,pushx,growx,pushy,growy,gapleft 5,gapright 5");
        frame.add(new JSeparator(), "pushx,growx,gaptop 5");
        frame.add(branchLabel, "alignx right,gapbottom 5,gapright 5");

    }

    public void onException(final Exception e) {
        // TODO Auto-generated method stub

    }

    public void onInterrupt() {
        dispose();

    }

    @Override
    public void onStateChange(final StateEvent event) {
        final UpdaterState state = (UpdaterState) event.getNewState();
        currentStepSize = state.getChildren().size() == 0 ? 1 : ((UpdaterState) state.getChildren().get(0)).getPercent() - state.getPercent();
        if (event.getNewState() == UpdaterController.DONE) {
            setModuleProgress("Update Successfull", state.getPercent());
        } else if (event.getNewState() == UpdaterController.DOWNLOADING) {
            setModuleProgress("Downloading Updates", state.getPercent());
        } else if (event.getNewState() == UpdaterController.DOWNLOADING_BRANCHLIST) {
            setModuleProgress("Download Version Information", state.getPercent());

        } else if (event.getNewState() == UpdaterController.DOWNLOADING_HASHLIST) {
            setModuleProgress("Download Filelist", state.getPercent());

        } else if (event.getNewState() == UpdaterController.DOWNLOADING_REPOLIST) {
            setModuleProgress("Update Serverlist", state.getPercent());
        } else if (event.getNewState() == UpdaterController.EXTRACTING) {
            setModuleProgress("Expanding Updates", state.getPercent());
        } else if (event.getNewState() == UpdaterController.FILTERING) {
            setModuleProgress("Prepare Update", state.getPercent());
        } else if (event.getNewState() == UpdaterController.INSTALLING) {
            setModuleProgress("Installing Updates", state.getPercent());
        } else if (event.getNewState() == UpdaterController.REVERTING) {
            setModuleProgress("Update failed", state.getPercent());
        } else if (event.getNewState() == UpdaterController.SLOT_WAITING) {
            setModuleProgress("Please wait. Next Updateslot will be yours", state.getPercent());

        } else if (event.getNewState() == UpdaterController.WAITING_FOR_UNLOCK) {
            setModuleProgress("Wait for JDownloader", state.getPercent());
        }
    }

    @Override
    public void onStateUpdate(final StateEvent event) {

    }

    @Override
    public void onUpdaterEvent(final UpdaterEvent event) {

        System.out.println("Updater: " + event);

        switch (event.getType()) {
        case WAIT_PENALTY:
            ((Throwable) event.getParameter(1)).printStackTrace();
            break;
        case BRANCH_UPDATED:
            updateBranchLabel(updateController.getBranch());

            break;
        case EXIT_REQUEST:
            dispose();
            break;
        }

    }

    @Override
    public void onUpdaterModuleEnd(final UpdaterEvent event) {
        System.out.println("Finished: " + event.getType());
        switch (event.getType()) {
        case END_FILELIST_UPDATE:
            System.out.println("Files in List: " + (updateController.getNonClassFiles().size() + updateController.getClassFiles().size()));
            break;
        case END_FILTERING:
            System.out.println(updateController.getFilteredClassFiles() + " - " + updateController.getFilteredNonClassFiles());
            break;
        }

    }

    @Override
    public void onUpdaterModuleProgress(final UpdaterEvent event, final int percent) {
        System.out.println("Progress: " + event.getType() + " : " + percent);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                progressLogo.setProgress(percent / 100.0f);
                panel.getSubBar().setValue(percent);

                final int dynamicPercent = (int) (((UpdaterState) updateController.getStateMachine().getState()).getPercent() + currentStepSize * percent / 100.0f);
                panel.setModuleProgress(null, dynamicPercent);

            }
        };

    }

    @Override
    public void onUpdaterModuleStart(final UpdaterEvent event) {
        System.out.println("Started: " + event.getType());
        switch (event.getType()) {
        case START_DOWNLOAD_FILE:
            panel.getSubBar().setString(((FileUpdate) event.getParameter()).getRelURL());
            break;

        case START_EXTRACT_FILE:
            panel.getSubBar().setString(((FileUpdate) event.getParameter()).getRelURL());
            break;
        }

    }

    private void setLaf() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {

        }
    }

    private void setModuleProgress(final String status, final int percent) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                panel.setModuleProgress(status, percent);

            }
        };

    }

    private void updateBranchLabel(final String branch) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                branchLabel.setText("JDownloader " + branch + "-Edition");
                frame.pack();
            }
        };
    }

}
