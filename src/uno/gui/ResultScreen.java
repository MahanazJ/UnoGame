package uno.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.geom.RoundRectangle2D;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uno.Card;
import uno.Player;
import uno.UnoEngine;
import uno.UnoEngine.TurnOutcome;

/** End of a round: who won, how many points, the cards that were left, and the match standings. */
final class ResultScreen extends Theme.FeltPanel {
    private static final long serialVersionUID = 1L;

    private final UnoApp app;
    private final JLabel headline = new JLabel();
    private final JLabel points = new JLabel();
    private final JLabel subline = new JLabel();
    private final Standings standings = new Standings();
    private final PillButton next = new PillButton("Next round", Theme.GOLD, Theme.INK, 22f);
    private final PillButton quit = new PillButton("Quit", new java.awt.Color(0, 0, 0, 90), Theme.CHALK, 18f);
    private boolean matchOver;

    ResultScreen(UnoApp app) {
        this.app = app;
        setLayout(new GridBagLayout());
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        headline.setFont(Theme.font(Font.BOLD, 40f));
        headline.setForeground(Theme.CHALK);
        points.setFont(Theme.font(Font.BOLD | Font.ITALIC, 76f));
        points.setForeground(Theme.GOLD);
        subline.setFont(Theme.font(Font.PLAIN, 17f));
        subline.setForeground(Theme.CHALK_DIM);

        column.add(row(headline));
        column.add(row(points));
        column.add(row(subline));
        column.add(Box.createVerticalStrut(24));
        column.add(row(standings));
        column.add(Box.createVerticalStrut(26));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        buttons.add(quit);
        buttons.add(next);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(buttons);
        add(column);

        next.addActionListener(e -> {
            if (matchOver) {
                app.newGame();
            } else {
                app.nextRound();
            }
        });
        quit.addActionListener(e -> app.quit());
    }

    void show(UnoEngine engine, TurnOutcome outcome) {
        matchOver = engine.isMatchOver();
        Player winner = outcome.winner();
        if (matchOver) {
            List<Player> leaders = engine.leaders();
            headline.setText(leaders.size() == 1
                    ? leaders.get(0).name() + " wins the game!"
                    : "It's a tie: " + leaders.stream().map(Player::name).collect(Collectors.joining(" and ")));
            subline.setText(winner.name() + " took round " + engine.round() + " for " + outcome.points()
                    + " points, which reached the target of " + engine.targetScore() + ".");
            next.setText("New game");
        } else {
            headline.setText(winner.name() + " wins round " + engine.round());
            subline.setText("Points come from the cards left in everyone else's hands. First to "
                    + engine.targetScore() + " wins.");
            next.setText("Next round");
        }
        points.setText("+" + outcome.points() + (outcome.points() == 1 ? " point" : " points"));
        standings.set(engine, winner);
        revalidate();
        repaint();
        next.requestFocusInWindow();
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(next);
        }
    }

    private static JComponent row(JComponent c) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.add(c);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        return row;
    }

    /** One row per player, best score first: leftover cards, what they were worth, and progress to the target. */
    private static final class Standings extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final int ROW = 58;
        private List<Player> order = List.of();
        private Player winner;
        private int target;

        void set(UnoEngine engine, Player winner) {
            this.winner = winner;
            this.target = engine.targetScore();
            this.order = engine.players().stream()
                    .sorted(Comparator.comparingInt(Player::score).reversed())
                    .toList();
            setPreferredSize(new Dimension(780, 34 + ROW * order.size()));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth();
            g2.setColor(new java.awt.Color(0, 0, 0, 70));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, getHeight(), 24, 24));

            double nameX = 24;
            double cardsX = 210;
            double handX = 470;
            double barX = 560;
            double barW = w - barX - 90;

            g2.setFont(Theme.font(Font.BOLD, 13f));
            g2.setColor(Theme.CHALK_DIM);
            g2.drawString("Player", (float) nameX, 24f);
            g2.drawString("Cards left", (float) cardsX, 24f);
            g2.drawString("Worth", (float) handX, 24f);
            g2.drawString("Match score", (float) barX, 24f);

            double y = 34;
            for (Player p : order) {
                boolean won = p == winner;
                if (won) {
                    g2.setColor(new java.awt.Color(0xFF, 0xD5, 0x4A, 36));
                    g2.fill(new RoundRectangle2D.Double(8, y + 2, w - 16, ROW - 4, 16, 16));
                }
                double mid = y + ROW / 2.0;
                g2.setFont(Theme.font(Font.BOLD, 18f));
                g2.setColor(won ? Theme.GOLD : Theme.CHALK);
                g2.drawString(TableScreen.fit(g2, p.name(), cardsX - nameX - 12), (float) nameX, (float) (mid + 6));

                List<Card> hand = p.hand();
                if (hand.isEmpty()) {
                    g2.setFont(Theme.font(Font.PLAIN, 15f));
                    g2.setColor(Theme.GOLD);
                    g2.drawString("Went out", (float) cardsX, (float) (mid + 5));
                } else {
                    double ch = ROW - 14;
                    double cw = ch * CardPainter.ASPECT;
                    double room = handX - cardsX - 20 - cw;
                    int shown = hand.size();
                    double step = shown <= 1 ? 0 : Math.min(cw * 0.55, room / (shown - 1));
                    for (int i = 0; i < shown; i++) {
                        CardPainter.paintFace(g2, hand.get(i), cardsX + i * step, y + 7, cw, ch);
                    }
                }

                g2.setFont(Theme.font(Font.PLAIN, 16f));
                g2.setColor(Theme.CHALK);
                g2.drawString(hand.isEmpty() ? "-" : String.valueOf(p.handPoints()), (float) handX, (float) (mid + 6));

                // Progress toward the target.
                double fraction = Math.min(1.0, p.score() / (double) target);
                g2.setColor(new java.awt.Color(255, 255, 255, 40));
                g2.fill(new RoundRectangle2D.Double(barX, mid - 6, barW, 12, 12, 12));
                g2.setColor(won ? Theme.GOLD : Theme.CHALK);
                if (fraction > 0) {
                    g2.fill(new RoundRectangle2D.Double(barX, mid - 6, Math.max(12, barW * fraction), 12, 12, 12));
                }
                g2.setFont(Theme.font(Font.BOLD, 17f));
                FontMetrics fm = g2.getFontMetrics();
                String score = String.valueOf(p.score());
                g2.setColor(Theme.CHALK);
                g2.drawString(score, (float) (w - 24 - fm.stringWidth(score)), (float) (mid + 6));
                y += ROW;
            }
            g2.dispose();
        }
    }
}
