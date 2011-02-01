package jd.updater.panel;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.SwingUtils;

public class UpdaterGuiPanel extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JProgressBar      bar;
    private JLabel            label;
    private JProgressBar      subBar;

    public UpdaterGuiPanel() {
        super(new MigLayout("ins 0, wrap 1", "[grow]", "[][]"));
        layoutPanel();

    }

    public JProgressBar getBar() {
        return bar;
    }

    public JLabel getLabel() {
        return label;
    }

    public JProgressBar getSubBar() {
        return subBar;
    }

    private void layoutPanel() {
        // init components
        label = new JLabel("Please wait. Installing Updates!");
        SwingUtils.toBold(label);

        bar = new JProgressBar(0, 100);
        subBar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        subBar.setStringPainted(true);
        // layout
        add(label);
        add(bar, "width 100:300:n,pushx,growx");
        add(subBar, "width 100:300:n,pushx,growx");
    }

    public void setModuleProgress(final String status, final int percent) {
        if (status != null) {
            bar.setString(status);
        }
        if (percent < 0) {
            bar.setForeground(Color.RED);
            bar.setValue(-percent);
        } else {
            bar.setValue(percent);
        }
    }

}
