package uno.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uno.Player;
import uno.UnoEngine;

/**
 * Shown between turns with no cards visible: what just happened, who to hand the computer to, and a
 * button only that player should press.
 */
final class PassScreen extends Theme.FeltPanel {
    private static final long serialVersionUID = 1L;

    private final Recap recap = new Recap();
    private final JLabel nameLabel = new JLabel();
    private final JLabel privacyLabel = new JLabel();
    private final PillButton reveal = new PillButton("Reveal hand", Theme.GOLD, Theme.INK, 22f);

    PassScreen(UnoApp app) {
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(centered(recap));
        column.add(Box.createVerticalStrut(46));

        JLabel passTo = new JLabel("Pass the computer to");
        passTo.setFont(Theme.font(Font.PLAIN, 22f));
        passTo.setForeground(Theme.CHALK_DIM);
        column.add(centered(passTo));

        nameLabel.setFont(Theme.font(Font.BOLD, 64f));
        nameLabel.setForeground(Theme.CHALK);
        column.add(centered(nameLabel));
        column.add(Box.createVerticalStrut(28));

        column.add(centered(reveal));
        column.add(Box.createVerticalStrut(16));

        privacyLabel.setFont(Theme.font(Font.PLAIN, 15f));
        privacyLabel.setForeground(Theme.CHALK_DIM);
        column.add(centered(privacyLabel));

        add(column);
        reveal.addActionListener(e -> app.reveal());
    }

    void show(UnoEngine engine, String summary) {
        Player next = engine.currentPlayer();
        nameLabel.setText(next.name());
        reveal.setText("Reveal " + TableScreen.possessive(next.name()) + " hand");
        privacyLabel.setText("Everyone else, look away. Press the button once only " + next.name() + " can see the screen.");
        recap.set(engine, summary);
        revalidate();
        repaint();
    }

    void focusButton() {
        reveal.requestFocusInWindow();
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(reveal);
        }
    }

    private static JComponent centered(JComponent c) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.add(c);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    /** The public state of the table: what just happened, the top card and the color in force. */
    private static final class Recap extends JComponent {
        private static final long serialVersionUID = 1L;

        Recap() {
            setPreferredSize(new Dimension(640, 150));
        }
        private UnoEngine engine;
        private String summary = "";

        void set(UnoEngine engine, String summary) {
            this.engine = engine;
            this.summary = summary;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (engine == null) {
                return;
            }
            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new java.awt.Color(0, 0, 0, 70));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 24, 24));

            double ch = h - 28;
            double cw = ch * CardPainter.ASPECT;
            CardPainter.paintFace(g2, engine.topCard(), 18, 14, cw, ch);

            double tx = 18 + cw + 24;
            g2.setFont(Theme.font(Font.BOLD, 13f));
            g2.setColor(Theme.CHALK_DIM);
            g2.drawString("Last turn", (float) tx, 32f);

            g2.setFont(Theme.font(Font.PLAIN, 17f));
            g2.setColor(Theme.CHALK);
            FontMetrics fm = g2.getFontMetrics();
            List<String> lines = TableScreen.MessagePill.wrap(fm, summary, w - tx - 20);
            double y = 56;
            for (String line : lines.subList(0, Math.min(3, lines.size()))) {
                g2.drawString(line, (float) tx, (float) y);
                y += fm.getHeight();
            }

            // Color in force, with a swatch so it reads at a glance.
            g2.setColor(Theme.suit(engine.activeColor()));
            g2.fill(new Ellipse2D.Double(tx, h - 36, 18, 18));
            g2.setColor(java.awt.Color.WHITE);
            g2.draw(new Ellipse2D.Double(tx, h - 36, 18, 18));
            g2.setFont(Theme.font(Font.BOLD, 15f));
            g2.setColor(Theme.CHALK);
            g2.drawString("Color to match: " + engine.activeColor(), (float) (tx + 26), h - 21f);
            g2.dispose();
        }
    }
}
