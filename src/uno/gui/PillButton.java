package uno.gui;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/** A chunky rounded button. Keeps a visible focus ring for keyboard players. */
class PillButton extends JButton {
    private static final long serialVersionUID = 1L;

    private java.awt.Color fill;
    private java.awt.Color ink;
    private final float fontSize;

    PillButton(String text, java.awt.Color fill, java.awt.Color ink, float fontSize) {
        super(text);
        this.fill = fill;
        this.ink = ink;
        this.fontSize = fontSize;
        setFont(Theme.font(Font.BOLD, fontSize));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void setColors(java.awt.Color fill, java.awt.Color ink) {
        this.fill = fill;
        this.ink = ink;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int padX = Math.round(fontSize * 1.4f);
        int padY = Math.round(fontSize * 0.7f);
        return new Dimension(fm.stringWidth(getText()) + 2 * padX, fm.getHeight() + 2 * padY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        int w = getWidth();
        int h = getHeight();
        double arc = h;
        boolean enabled = isEnabled();
        boolean pressed = getModel().isPressed();
        boolean hover = getModel().isRollover();

        java.awt.Color base = enabled ? fill : new java.awt.Color(0x55, 0x66, 0x5C);
        if (enabled && hover) {
            base = blend(base, java.awt.Color.WHITE, 0.15);
        }
        int drop = pressed ? 1 : 3;
        g2.setColor(new java.awt.Color(0, 0, 0, 70));
        g2.fill(new RoundRectangle2D.Double(1, drop + 1, w - 2, h - drop - 2, arc, arc));
        g2.setColor(base);
        g2.fill(new RoundRectangle2D.Double(1, pressed ? 2 : 1, w - 2, h - drop - 2, arc, arc));

        if (isFocusOwner()) {
            g2.setColor(Theme.CHALK);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {5, 4}, 0));
            g2.draw(new RoundRectangle2D.Double(4, (pressed ? 2 : 1) + 3, w - 8, h - drop - 8, arc - 6, arc - 6));
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (pressed ? 2 : 1) + (h - drop - 2 - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(enabled ? ink : new java.awt.Color(0xC8, 0xD2, 0xCC));
        g2.drawString(getText(), tx, ty);
        g2.dispose();
    }

    static java.awt.Color blend(java.awt.Color a, java.awt.Color b, double t) {
        return new java.awt.Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                a.getAlpha());
    }
}
