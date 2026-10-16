package uno.gui;

import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import uno.Color;

/** Colors, fonts and painting helpers shared by every screen. */
final class Theme {
    private Theme() {
    }

    // Card suits, chosen to stay distinct for most kinds of color blindness when paired with symbols.
    static final java.awt.Color RED = new java.awt.Color(0xD9, 0x2B, 0x3A);
    static final java.awt.Color YELLOW = new java.awt.Color(0xF3, 0xC2, 0x1B);
    static final java.awt.Color GREEN = new java.awt.Color(0x2F, 0x9E, 0x44);
    static final java.awt.Color BLUE = new java.awt.Color(0x1C, 0x6D, 0xD0);
    static final java.awt.Color INK = new java.awt.Color(0x1A, 0x1A, 0x1D);

    // The table.
    static final java.awt.Color FELT_LIGHT = new java.awt.Color(0x22, 0x7A, 0x4B);
    static final java.awt.Color FELT_DARK = new java.awt.Color(0x0B, 0x3D, 0x24);
    static final java.awt.Color RAIL = new java.awt.Color(0x06, 0x2A, 0x18);
    static final java.awt.Color CHALK = new java.awt.Color(0xF4, 0xF7, 0xF2);
    static final java.awt.Color CHALK_DIM = new java.awt.Color(0xF4, 0xF7, 0xF2, 0xB0);
    static final java.awt.Color GOLD = new java.awt.Color(0xFF, 0xD5, 0x4A);
    static final java.awt.Color ERROR_BG = new java.awt.Color(0x7A, 0x12, 0x1C);

    private static final String FAMILY = pickFamily(
            "Arial Rounded MT Bold", "Nunito", "Segoe UI", "Helvetica Neue", "Avenir Next", "Arial", "DejaVu Sans");

    static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size)).deriveFont(size);
    }

    static java.awt.Color suit(Color color) {
        return switch (color) {
            case RED -> RED;
            case YELLOW -> YELLOW;
            case GREEN -> GREEN;
            case BLUE -> BLUE;
            case WILD -> INK;
        };
    }

    /** Text that reads well on top of a suit color. */
    static java.awt.Color onSuit(Color color) {
        return color == Color.YELLOW ? INK : java.awt.Color.WHITE;
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    /** A panel painted like a card table: a lit center fading to a dark rail at the edges. */
    static class FeltPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        FeltPanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, RAIL, 0, h, RAIL));
            g2.fillRect(0, 0, w, h);
            float radius = Math.max(w, h) * 0.75f;
            g2.setPaint(new RadialGradientPaint(w / 2f, h * 0.45f, radius,
                    new float[] {0f, 0.55f, 1f},
                    new java.awt.Color[] {FELT_LIGHT, FELT_DARK, RAIL}));
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    private static String pickFamily(String... preferred) {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String family : preferred) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }
}
