package jd.updater;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;

public class ProgressLogo extends JPanel {

    private final ImageIcon icon;

    private final int       ICONSIZE = 48;
    private float           progress = 0.0f;

    private final Dimension prefsize;

    private final ImageIcon iconBorder;

    public ProgressLogo() {
        icon = ImageProvider.getImageIcon("logo", ICONSIZE, ICONSIZE);
        iconBorder = ImageProvider.getImageIcon("logo_border", ICONSIZE, ICONSIZE);
        prefsize = new Dimension(ICONSIZE, ICONSIZE);
    }

    @Override
    public Dimension getPreferredSize() {
        return prefsize;
    }

    public float getProgress() {

        return progress;
    }

    @Override
    public void paint(final Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setClip(new Rectangle(ICONSIZE, ICONSIZE));

        iconBorder.paintIcon(this, g2, 0, 0);

        final int height = (int) (ICONSIZE * progress);
        g2.setClip(0, ICONSIZE - height, ICONSIZE, height);

        icon.paintIcon(this, g2, 0, 0);

    }

    public void setProgress(final float prg) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setToolTipText("Update Installation Progress: " + (int) (progress * 100) / 100.0 + "%");
                progress = prg;
                System.out.println(progress);
                repaint();

            }
        };

    }
}
