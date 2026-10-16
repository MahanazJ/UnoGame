package uno.gui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uno.Card;
import uno.Color;
import uno.Rank;

/** Title, player setup and the Start game button. */
final class WelcomeScreen extends Theme.FeltPanel {
    private static final long serialVersionUID = 1L;
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 10;
    private static final int[] TARGETS = {100, 250, 500};

    private final UnoApp app;
    private final List<JTextField> nameFields = new ArrayList<>();
    private final JPanel namesGrid = new JPanel();
    private final JLabel countLabel = new JLabel();
    private final JLabel error = new JLabel(" ");
    private final List<PillButton> targetButtons = new ArrayList<>();
    private int count = 2;
    private int target;

    WelcomeScreen(UnoApp app, int defaultTarget) {
        this.app = app;
        this.target = defaultTarget;
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(center(new Logo()));
        column.add(Box.createVerticalStrut(10));
        JLabel tagline = new JLabel("Pass-and-play Uno for 2 to 10 players on one computer");
        tagline.setFont(Theme.font(Font.PLAIN, 18f));
        tagline.setForeground(Theme.CHALK_DIM);
        column.add(center(tagline));
        column.add(Box.createVerticalStrut(28));

        // Player count.
        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        countRow.setOpaque(false);
        countRow.add(caption("Players"));
        PillButton minus = new PillButton("-", new java.awt.Color(0, 0, 0, 90), Theme.CHALK, 20f);
        PillButton plus = new PillButton("+", new java.awt.Color(0, 0, 0, 90), Theme.CHALK, 20f);
        minus.getAccessibleContext().setAccessibleName("Fewer players");
        plus.getAccessibleContext().setAccessibleName("More players");
        countLabel.setFont(Theme.font(Font.BOLD, 26f));
        countLabel.setForeground(Theme.CHALK);
        countLabel.setPreferredSize(new Dimension(44, 40));
        countLabel.setHorizontalAlignment(JLabel.CENTER);
        countRow.add(minus);
        countRow.add(countLabel);
        countRow.add(plus);
        minus.addActionListener(e -> setCount(count - 1));
        plus.addActionListener(e -> setCount(count + 1));
        column.add(center(countRow));
        column.add(Box.createVerticalStrut(16));

        namesGrid.setOpaque(false);
        column.add(center(namesGrid));
        column.add(Box.createVerticalStrut(20));

        // Target score.
        JPanel targetRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        targetRow.setOpaque(false);
        targetRow.add(caption("Play to"));
        for (int t : TARGETS) {
            PillButton b = new PillButton(t + " points", new java.awt.Color(0, 0, 0, 90), Theme.CHALK, 16f);
            b.addActionListener(e -> setTarget(t));
            targetButtons.add(b);
            targetRow.add(b);
        }
        column.add(center(targetRow));
        column.add(Box.createVerticalStrut(14));

        error.setFont(Theme.font(Font.BOLD, 15f));
        error.setForeground(new java.awt.Color(0xFF, 0xB4, 0xB4));
        column.add(center(error));
        column.add(Box.createVerticalStrut(10));

        PillButton start = new PillButton("Start game", Theme.GOLD, Theme.INK, 24f);
        start.addActionListener(e -> start());
        column.add(center(start));

        add(column);
        setCount(2);
        setTarget(defaultTarget);
    }

    /** Keeps the names already typed when the players come back for a new game. */
    void reset() {
        error.setText(" ");
    }

    private void setCount(int n) {
        count = Math.max(MIN_PLAYERS, Math.min(MAX_PLAYERS, n));
        countLabel.setText(String.valueOf(count));
        while (nameFields.size() < count) {
            JTextField field = new RoundField();
            field.addActionListener(e -> start());
            nameFields.add(field);
        }
        namesGrid.removeAll();
        int cols = count <= 4 ? count : (count + 1) / 2;
        cols = Math.min(cols, 5);
        namesGrid.setLayout(new GridLayout(0, Math.min(cols, count), 10, 10));
        for (int i = 0; i < count; i++) {
            JTextField f = nameFields.get(i);
            f.putClientProperty("placeholder", "Player " + (i + 1));
            f.getAccessibleContext().setAccessibleName("Name of player " + (i + 1));
            namesGrid.add(f);
        }
        namesGrid.revalidate();
        namesGrid.repaint();
        revalidate();
        repaint();
    }

    private void setTarget(int t) {
        target = t;
        for (int i = 0; i < TARGETS.length; i++) {
            boolean on = TARGETS[i] == t;
            targetButtons.get(i).setColors(on ? Theme.CHALK : new java.awt.Color(0, 0, 0, 90), on ? Theme.INK : Theme.CHALK);
        }
    }

    private void start() {
        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String name = nameFields.get(i).getText().trim();
            if (name.isEmpty()) {
                name = "Player " + (i + 1);
            }
            if (!seen.add(name.toLowerCase())) {
                error.setText("Two players are called " + name + ". Give one a different name so turns aren't mixed up.");
                nameFields.get(i).requestFocusInWindow();
                return;
            }
            names.add(name);
        }
        error.setText(" ");
        app.startMatch(names, target);
    }

    private static JLabel caption(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 18f));
        l.setForeground(Theme.CHALK);
        return l;
    }

    private static JComponent center(JComponent c) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.add(c);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    /** A rounded white name field with a grey placeholder. */
    private static final class RoundField extends JTextField {
        private static final long serialVersionUID = 1L;

        RoundField() {
            super(11);
            setOpaque(false);
            setFont(Theme.font(Font.PLAIN, 17f));
            setForeground(Theme.INK);
            setCaretColor(Theme.INK);
            setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(java.awt.Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            if (isFocusOwner()) {
                g2.setColor(Theme.GOLD);
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new RoundRectangle2D.Double(1.5, 1.5, getWidth() - 3, getHeight() - 3, getHeight(), getHeight()));
            }
            g2.dispose();
            super.paintComponent(g);
            Object placeholder = getClientProperty("placeholder");
            if (getText().isEmpty() && placeholder != null) {
                Graphics2D p = Theme.smooth(g);
                p.setFont(getFont());
                p.setColor(new java.awt.Color(0x8A, 0x92, 0x8D));
                p.drawString(placeholder.toString(), getInsets().left, getBaseline(getWidth(), getHeight()));
                p.dispose();
            }
        }
    }

    /** A fan of five cards behind the game's oval logo. */
    private static final class Logo extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Card[] fan = {
            Card.of(Color.RED, Rank.SEVEN), Card.of(Color.YELLOW, Rank.SKIP), Card.of(Color.WILD, Rank.WILD_DRAW_FOUR),
            Card.of(Color.GREEN, Rank.REVERSE), Card.of(Color.BLUE, Rank.DRAW_TWO),
        };

        Logo() {
            setPreferredSize(new Dimension(560, 250));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            double cx = getWidth() / 2.0;
            double ch = 170;
            double cw = ch * CardPainter.ASPECT;
            double pivotY = 300;
            for (int i = 0; i < fan.length; i++) {
                Graphics2D c = (Graphics2D) g2.create();
                c.rotate(Math.toRadians((i - 2) * 13), cx, pivotY);
                CardPainter.paintFace(c, fan[i], cx - cw / 2, 18, cw, ch);
                c.dispose();
            }

            // The title sits across the bottom of the fan in the table's own chalk white.
            Font title = Theme.font(Font.BOLD | Font.ITALIC, 104f);
            Graphics2D l = (Graphics2D) g2.create();
            l.rotate(Math.toRadians(-4), cx, 190);
            CardPainter.drawOutlinedText(l, "Uno", cx + 4, 198, title, new java.awt.Color(0, 0, 0, 90),
                    new java.awt.Color(0, 0, 0, 0), 0);
            CardPainter.drawOutlinedText(l, "Uno", cx, 190, title, Theme.CHALK, Theme.RAIL, 5);
            l.dispose();
            g2.dispose();
        }
    }
}
