package jd.updater;

import java.awt.Color;
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
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import jd.updater.panel.UpdaterGuiPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.formatter.TimeFormatter;
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
    private final SimpleDateFormat  logDateFormat;
    private final SimpleDateFormat  logTimeFormat;

    public UpdaterGui(final UpdaterController updateController) {
        this.updateController = updateController;
        this.setLaf();
        this.storage = JSonStorage.getPlainStorage("WebUpdaterGUI");
        this.frame = new JFrame("JDownloader Updater");
        Dialog.getInstance().setParentOwner(this.frame);

        this.frame.addWindowListener(new WindowListener() {

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
        this.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // set appicon
        final ArrayList<Image> list = new ArrayList<Image>();

        try {
            list.add(ImageProvider.getBufferedImage("icon", true));

            this.frame.setIconImages(list);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        this.logDateFormat = new SimpleDateFormat("dd.MM.yy");
        this.logTimeFormat = new SimpleDateFormat("HH:mm:ss");
        // Set Application dimensions and locations

        final Dimension dim = new Dimension(this.storage.get("DIMENSION_WIDTH", 300), this.storage.get("DIMENSION_HEIGHT", 60));
        // restore size
        this.frame.setSize(dim);
        // this.frame.setPreferredSize(dim);

        this.frame.setMinimumSize(new Dimension(100, 60));
        this.layoutGUI();

        if (updateController != null) {

            this.updateController.getEventSender().addListener(this);
            updateController.getStateMachine().addListener(this);
        } else {
            System.out.println("TEST");
        }
        // restore location. use center of screen as default.
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = screenSize.width / 2 - this.frame.getSize().width / 2;
        final int y = screenSize.height / 2 - this.frame.getSize().height / 2;

        this.frame.setLocation(this.storage.get("LOCATION_X", x), this.storage.get("LOCATION_Y", y));

        this.frame.setVisible(true);

        this.frame.pack();
        this.log("Started Webupdater " + this.logDateFormat.format(new Date()));

    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.btnDetails) {
            this.btnDetails.setVisible(false);
            this.scrollPane.setVisible(true);
            this.lblDetailsLabel.setVisible(true);
            this.frame.pack();

        }
    }

    public void dispose() {
        if (this.frame.getExtendedState() == Frame.NORMAL && this.frame.isShowing()) {

            this.storage.put("LOCATION_X", this.frame.getLocationOnScreen().x);
            this.storage.put("LOCATION_Y", this.frame.getLocationOnScreen().y);
            this.storage.put("DIMENSION_WIDTH", this.frame.getSize().width);
            this.storage.put("DIMENSION_HEIGHT", this.frame.getSize().height);

        }

        this.frame.setVisible(false);
        this.frame.dispose();
    }

    private void layoutGUI() {
        this.frame.setLayout(new MigLayout("ins 0,wrap 1", "[]", " [][]"));
        this.panel = new UpdaterGuiPanel();
        this.progressLogo = new ProgressLogo();
        this.branchLabel = new JLabel();
        this.lblDetailsLabel = new JLabel("Update Details:");
        this.lblDetailsLabel.setVisible(false);
        SwingUtils.boldJLabel(this.lblDetailsLabel);
        this.branchLabel.setForeground(this.panel.getBackground().darker());

        this.frame.getContentPane().add(this.progressLogo, "split 2,gapright 10,gaptop 5,gapleft 5");
        this.frame.add(this.panel, "growx,pushx,gapright 5, gaptop 5");

        this.btnDetails = new JButton("Details");
        SwingUtils.boldJButton(this.btnDetails);
        this.btnDetails.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.btnDetails.setFocusable(false);
        this.btnDetails.setContentAreaFilled(false);
        this.btnDetails.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, this.panel.getBackground().darker()));
        this.btnDetails.addActionListener(this);
        this.logField = new JTextPane();
        this.logField.setEditable(true);
        this.logField.setAutoscrolls(true);
        this.scrollPane = new JScrollPane(this.logField);
        this.scrollPane.setVisible(false);
        this.logField.setEditable(true);
        this.logField.setAutoscrolls(true);
        this.frame.add(this.btnDetails, "hidemode 3,shrinky,alignx right,aligny top,gapright 5");
        this.frame.add(this.lblDetailsLabel, "hidemode 3,gaptop 5,gapleft 5");
        this.frame.add(this.scrollPane, "hidemode 3,height 100:300:n,pushx,growx,pushy,growy,gapleft 5,gapright 5");
        this.frame.add(new JSeparator(), "pushx,growx,gaptop 5");
        this.frame.add(this.branchLabel, "alignx right,gapbottom 5,gapright 5");

    }

    private void log(final String msg) {
        final Document doc = this.logField.getDocument();

        final EditorKit editorkit = this.logField.getEditorKit();

        final StringReader r = new StringReader(this.logTimeFormat.format(new Date()) + " - " + msg + "\r\n");
        try {
            editorkit.read(r, doc, 0);
        } catch (final Exception e1) {
            e1.printStackTrace();
        }

    }

    public void onException(final Exception e) {
        // TODO Auto-generated method stub

    }

    public void onInterrupt() {
        this.dispose();

    }

    @Override
    public void onStateChange(final StateEvent event) {
        final UpdaterState state = (UpdaterState) event.getNewState();
        this.currentStepSize = state.getChildren().size() == 0 ? 1 : ((UpdaterState) state.getChildren().get(0)).getPercent() - state.getPercent();
        if (event.getNewState() == UpdaterController.DONE) {
            this.setModuleProgress("Update Successfull", state.getPercent());
        } else if (event.getNewState() == UpdaterController.DOWNLOADING) {
            this.setModuleProgress("Downloading Updates", state.getPercent());
        } else if (event.getNewState() == UpdaterController.DOWNLOADING_BRANCHLIST) {
            this.setModuleProgress("Download Version Information", state.getPercent());

        } else if (event.getNewState() == UpdaterController.DOWNLOADING_HASHLIST) {
            this.setModuleProgress("Download Filelist", state.getPercent());

        } else if (event.getNewState() == UpdaterController.DOWNLOADING_REPOLIST) {
            this.setModuleProgress("Update Serverlist", state.getPercent());
        } else if (event.getNewState() == UpdaterController.EXTRACTING) {
            this.setModuleProgress("Expanding Updates", state.getPercent());
        } else if (event.getNewState() == UpdaterController.FILTERING) {
            this.setModuleProgress("Prepare Update", state.getPercent());
        } else if (event.getNewState() == UpdaterController.INSTALLING) {
            this.setModuleProgress("Installing Updates", state.getPercent());
        } else if (event.getNewState() == UpdaterController.REVERTING) {
            this.setModuleProgress("Update failed", state.getPercent());
        } else if (event.getNewState() == UpdaterController.SLOT_WAITING) {
            this.setModuleProgress("Please wait. Next Updateslot will be yours", state.getPercent());

        } else if (event.getNewState() == UpdaterController.WAITING_FOR_UNLOCK) {
            this.setModuleProgress("Wait for JDownloader", state.getPercent());
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
            this.log("Error occured. please wait " + TimeFormatter.formatSeconds((Integer) event.getParameter(0) / 1000, 0) + " seconds.");
            ((Throwable) event.getParameter(1)).printStackTrace();
            break;
        case BRANCH_UPDATED:
            this.log("JDownloader Edition: " + this.updateController.getBranch());
            this.updateBranchLabel(this.updateController.getBranch());

            break;
        case EXIT_REQUEST:
            this.dispose();
            break;
        }

    }

    @Override
    public void onUpdaterModuleEnd(final UpdaterEvent event) {
        System.out.println("Finished: " + event.getType());
        switch (event.getType()) {
        case END_FILELIST_UPDATE:
            System.out.println("Files in List: " + (this.updateController.getNonClassFiles().size() + this.updateController.getClassFiles().size()));
            break;
        case END_FILTERING:
            this.log(this.updateController.getFilteredClassFiles().size() + this.updateController.getFilteredNonClassFiles().size() + " Updates found");
            System.out.println(this.updateController.getFilteredClassFiles() + " - " + this.updateController.getFilteredNonClassFiles());
            break;
        }

        this.panel.getSubBar().setString("");

    }

    @Override
    public void onUpdaterModuleProgress(final UpdaterEvent event, final int percent) {
        System.out.println("Progress: " + event.getType() + " : " + percent);
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                UpdaterGui.this.panel.getSubBar().setValue(percent);

                final int dynamicPercent = (int) (((UpdaterState) UpdaterGui.this.updateController.getStateMachine().getState()).getPercent() + UpdaterGui.this.currentStepSize * percent / 100.0f);
                UpdaterGui.this.progressLogo.setProgress(dynamicPercent / 100.0f);
                UpdaterGui.this.panel.setModuleProgress(null, dynamicPercent);

            }
        };

    }

    @Override
    public void onUpdaterModuleStart(final UpdaterEvent event) {
        System.out.println("Started: " + event.getType());
        switch (event.getType()) {
        case START_DOWNLOAD_FILE:
            this.panel.getSubBar().setString(((FileUpdate) event.getParameter()).getRelURL());
            this.log("Download " + ((FileUpdate) event.getParameter()).getRelURL());
            break;

        case START_EXTRACT_FILE:
            this.log("Extract " + ((FileUpdate) event.getParameter()).getRelURL());
            this.panel.getSubBar().setString(((FileUpdate) event.getParameter()).getRelURL());
            break;
        case START_INSTALL_FILE:
            this.log("Install " + ((FileUpdate) event.getParameter()).getRelURL());
            this.panel.getSubBar().setString(((FileUpdate) event.getParameter()).getRelURL());
            break;
        case START_REVERT:
            this.panel.getSubBar().setForeground(Color.RED);
            this.panel.getBar().setForeground(Color.RED);
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
                if (status != null) {
                    UpdaterGui.this.log(status);
                }
                UpdaterGui.this.progressLogo.setProgress(percent / 100.0f);
                UpdaterGui.this.panel.setModuleProgress(status, percent);

            }
        };

    }

    private void updateBranchLabel(final String branch) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                UpdaterGui.this.branchLabel.setText("JDownloader " + branch + "-Edition");
                UpdaterGui.this.frame.pack();
            }
        };
    }

}
