package uno.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import uno.Card;
import uno.Color;

/** Asks which color a wild card sets. Four big buttons; R, Y, G, B on the keyboard also work. */
final class ColorPicker {
    private ColorPicker() {
    }

    /** @return the chosen suit, or null if the player backed out */
    static Color ask(Component parent, Card wild) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Choose a color", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        Color[] result = new Color[1];
        dialog.setContentPane(buildContent(wild, c -> {
            result[0] = c;
            dialog.dispose();
        }, dialog::dispose));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);   // blocks until a choice is made
        return result[0];
    }

    /** The picker's contents, separate from the dialog so it can be laid out and painted on its own. */
    static JPanel buildContent(Card wild, Consumer<Color> onPick, Runnable onCancel) {
        JPanel content = new JPanel(new BorderLayout(0, 18)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(Theme.RAIL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.GOLD);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        content.setBorder(BorderFactory.createEmptyBorder(26, 30, 22, 30));

        JLabel title = new JLabel("Pick the color for your " + wild.rank(), JLabel.CENTER);
        title.setFont(Theme.font(Font.BOLD, 24f));
        title.setForeground(Theme.CHALK);
        content.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        for (Color c : Color.suits()) {
            JButton b = new Swatch(c);
            b.addActionListener(e -> onPick.accept(c));
            grid.add(b);
            bindKey(content, KeyStroke.getKeyStroke(Character.toUpperCase(c.toString().charAt(0)), 0),
                    () -> onPick.accept(c));
        }
        content.add(grid, BorderLayout.CENTER);

        PillButton back = new PillButton("Keep the Wild in my hand", new java.awt.Color(0, 0, 0, 0), Theme.CHALK_DIM, 14f);
        back.addActionListener(e -> onCancel.run());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        south.setOpaque(false);
        south.add(back);
        content.add(south, BorderLayout.SOUTH);
        bindKey(content, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), onCancel);
        return content;
    }

    private static void bindKey(JComponent c, KeyStroke key, Runnable action) {
        String name = "key-" + key;
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        c.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    /** A large rounded color button labeled with the color's name and its keyboard letter. */
    private static final class Swatch extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color color;

        Swatch(Color color) {
            this.color = color;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(190, 104));
            getAccessibleContext().setAccessibleName(color.toString());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover();
            java.awt.Color fill = Theme.suit(color);
            if (hover) {
                fill = PillButton.blend(fill, java.awt.Color.WHITE, 0.14);
            }
            int drop = getModel().isPressed() ? 1 : 4;
            g2.setColor(new java.awt.Color(0, 0, 0, 90));
            g2.fill(new RoundRectangle2D.Double(0, drop, w, h - drop, 22, 22));
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, getModel().isPressed() ? 3 : 0, w, h - 4, 22, 22));
            if (hover || isFocusOwner()) {
                g2.setColor(java.awt.Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new RoundRectangle2D.Double(2, 2, w - 4, h - 8, 20, 20));
            }
            java.awt.Color ink = Theme.onSuit(color);
            g2.setColor(ink);
            g2.setFont(Theme.font(Font.BOLD, 24f));
            FontMetrics fm = g2.getFontMetrics();
            String name = color.toString();
            g2.drawString(name, (w - fm.stringWidth(name)) / 2, h / 2 + 4);
            g2.setFont(Theme.font(Font.PLAIN, 13f));
            fm = g2.getFontMetrics();
            String key = "press " + name.charAt(0);
            g2.drawString(key, (w - fm.stringWidth(key)) / 2, h / 2 + 26);
            g2.dispose();
        }
    }
}
