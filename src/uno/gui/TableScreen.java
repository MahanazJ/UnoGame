package uno.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uno.Card;
import uno.Color;
import uno.Player;
import uno.UnoEngine;

/** The screen a player sees once they've revealed their hand. */
final class TableScreen extends Theme.FeltPanel {
    private static final long serialVersionUID = 1L;

    private final UnoApp app;
    private UnoEngine engine;

    private final OpponentsBar opponents = new OpponentsBar();
    private final CenterPiles piles = new CenterPiles();
    private final JLabel turnLabel = new JLabel();
    private final MessagePill message = new MessagePill();
    private final PillButton drawButton = new PillButton("Draw a card", Theme.CHALK, Theme.INK, 17f);
    private final HandPanel hand = new HandPanel();

    TableScreen(UnoApp app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        add(opponents, BorderLayout.NORTH);
        add(piles, BorderLayout.CENTER);

        turnLabel.setFont(Theme.font(Font.BOLD, 28f));
        turnLabel.setForeground(Theme.CHALK);

        JPanel bar = new JPanel(new BorderLayout(18, 0));
        bar.setOpaque(false);
        bar.add(turnLabel, BorderLayout.WEST);
        bar.add(message, BorderLayout.CENTER);
        JPanel buttonHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonHolder.setOpaque(false);
        buttonHolder.add(drawButton);
        bar.add(buttonHolder, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(bar, BorderLayout.NORTH);
        south.add(hand, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        drawButton.addActionListener(e -> drawOrPass());
        hand.setOnClick(this::tryPlay);
    }

    /** Shows the current player's turn. Only called after they pressed Reveal hand. */
    void showTurn(UnoEngine engine) {
        this.engine = engine;
        turnLabel.setText(possessive(engine.currentPlayer().name()) + " turn");
        refresh();
        message.show(defaultHint(), MessagePill.Kind.HINT);
    }

    private void refresh() {
        Player me = engine.currentPlayer();
        hand.setHand(me.hand(), engine::isPlayable, engine.drawnCard());
        opponents.repaint();
        piles.repaint();
        if (engine.hasDrawn()) {
            drawButton.setText(engine.drawnCard() != null && engine.isPlayable(engine.drawnCard()) ? "Keep & pass" : "Pass");
            drawButton.setColors(Theme.GOLD, Theme.INK);
        } else {
            drawButton.setText("Draw a card");
            boolean stuck = !engine.hasPlayableCard();
            drawButton.setColors(stuck ? Theme.GOLD : Theme.CHALK, Theme.INK);
        }
        revalidate();
    }

    private String defaultHint() {
        if (engine.hasDrawn()) {
            Card drawn = engine.drawnCard();
            if (drawn == null) {
                return "Every card is in someone's hand, so there's nothing to draw. Press Pass.";
            }
            String note = engine.takeDeckNote().trim();
            String prefix = note.isEmpty() ? "" : note + " ";
            return engine.isPlayable(drawn)
                    ? prefix + "You drew " + drawn + ". Click it to play it now, or keep it and pass."
                    : prefix + "You drew " + drawn + ". It doesn't match, so press Pass.";
        }
        return engine.hasPlayableCard()
                ? "Click a highlighted card to play it, or draw instead."
                : "None of your cards match. Press Draw a card.";
    }

    private void drawOrPass() {
        if (engine.hasDrawn()) {
            app.finishTurn(engine.pass());
            return;
        }
        engine.draw();
        refresh();
        message.show(defaultHint(), engine.drawnCard() != null && engine.isPlayable(engine.drawnCard())
                ? MessagePill.Kind.GOOD : MessagePill.Kind.HINT);
    }

    private void tryPlay(Card card) {
        String reason = engine.illegalReason(card);
        if (reason != null) {
            hand.shake(card);
            message.show(reason, MessagePill.Kind.ERROR);
            return;
        }
        Color chosen = null;
        if (engine.needsColorChoice(card)) {
            chosen = ColorPicker.ask(this, card);
            if (chosen == null) {
                message.show("No color picked, so the Wild is still in your hand.", MessagePill.Kind.HINT);
                return;
            }
        }
        app.finishTurn(engine.play(card, chosen));
    }

    static String possessive(String name) {
        return name.endsWith("s") ? name + "'" : name + "'s";
    }

    // ---- top: everyone else ---------------------------------------------------------------------

    /** Other players in the order they'll play, with a card-back fan and count for each. */
    private final class OpponentsBar extends JComponent {
        private static final long serialVersionUID = 1L;

        OpponentsBar() {
            setPreferredSize(new Dimension(900, 118));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (engine == null) {
                return;
            }
            Graphics2D g2 = Theme.smooth(g);
            List<Player> order = playOrderAfterCurrent();
            int n = order.size();
            double gap = 12;
            double badgeW = Math.min(210, (getWidth() - gap * (n - 1)) / n);
            double total = badgeW * n + gap * (n - 1);
            double x = (getWidth() - total) / 2;
            for (int i = 0; i < n; i++) {
                paintBadge(g2, order.get(i), x, 6, badgeW, getHeight() - 14, i == 0);
                x += badgeW + gap;
            }
            g2.dispose();
        }

        private List<Player> playOrderAfterCurrent() {
            List<Player> all = engine.players();
            int me = all.indexOf(engine.currentPlayer());
            int dir = engine.isClockwise() ? 1 : -1;
            List<Player> order = new ArrayList<>();
            for (int k = 1; k < all.size(); k++) {
                order.add(all.get(Math.floorMod(me + dir * k, all.size())));
            }
            return order;
        }

        private void paintBadge(Graphics2D g2, Player p, double x, double y, double w, double h, boolean next) {
            g2.setColor(new java.awt.Color(0, 0, 0, 70));
            g2.fill(new RoundRectangle2D.Double(x, y, w, h, 18, 18));
            if (next) {
                g2.setColor(Theme.GOLD);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 18, 18));
            }

            // Fan of card backs: one per card, up to eight, so a glance shows who is close to going out.
            int count = p.hand().size();
            int shown = Math.min(count, 8);
            double ch = h - 30;
            double cw = ch * CardPainter.ASPECT;
            double fanX = x + 12;
            double step = shown <= 1 ? 0 : Math.min(9, (w * 0.42 - cw) / (shown - 1));
            for (int i = 0; i < shown; i++) {
                CardPainter.paintBack(g2, fanX + i * step, y + 14, cw, ch);
            }

            double tx = fanX + cw + step * Math.max(0, shown - 1) + 12;
            double textW = x + w - tx - 8;
            g2.setColor(Theme.CHALK);
            g2.setFont(Theme.font(Font.BOLD, 16f));
            g2.drawString(fit(g2, p.name(), textW), (float) tx, (float) (y + 30));
            g2.setFont(Theme.font(Font.PLAIN, 14f));
            g2.setColor(Theme.CHALK_DIM);
            g2.drawString(count + (count == 1 ? " card" : " cards"), (float) tx, (float) (y + 52));
            g2.drawString(p.score() + " pts", (float) tx, (float) (y + 72));

            String tag = count == 1 ? "UNO!" : next ? "Plays next" : null;
            if (tag != null && textW > 40) {
                g2.setFont(Theme.font(Font.BOLD, 12f));
                double tw = g2.getFontMetrics().stringWidth(tag) + 14;
                g2.setColor(count == 1 ? Theme.RED : Theme.GOLD);
                g2.fill(new RoundRectangle2D.Double(tx, y + h - 26, tw, 19, 19, 19));
                g2.setColor(count == 1 ? java.awt.Color.WHITE : Theme.INK);
                g2.drawString(tag, (float) (tx + 7), (float) (y + h - 12));
            }
        }
    }

    // ---- middle: draw pile and discard ----------------------------------------------------------

    private final class CenterPiles extends JComponent {
        private static final long serialVersionUID = 1L;
        private Rectangle2D drawPileBounds = new Rectangle2D.Double();
        private boolean hoverDraw;

        CenterPiles() {
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean over = canDraw() && drawPileBounds.contains(e.getPoint());
                    if (over != hoverDraw) {
                        hoverDraw = over;
                        setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (canDraw() && drawPileBounds.contains(e.getPoint())) {
                        drawOrPass();
                        hoverDraw = false;
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private boolean canDraw() {
            return engine != null && !engine.hasDrawn();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (engine == null) {
                return;
            }
            Graphics2D g2 = Theme.smooth(g);
            double h = Math.min(200, getHeight() - 70);
            double w = h * CardPainter.ASPECT;
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0 - 14;

            paintDirection(g2, cx, cy, w * 4.2, h * 1.45);

            // Draw pile: a short stack of backs.
            double dx = cx - w - 34;
            double dy = cy - h / 2;
            int layers = Math.min(4, 1 + engine.drawPileSize() / 15);
            if (engine.drawPileSize() == 0) {
                g2.setColor(new java.awt.Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {6, 6}, 0));
                g2.draw(new RoundRectangle2D.Double(dx, dy, w, h, w * 0.16, w * 0.16));
            } else {
                double lift = hoverDraw ? -6 : 0;
                for (int i = layers - 1; i >= 0; i--) {
                    CardPainter.paintBack(g2, dx - i * 2.5, dy - i * 2.5 + lift, w, h);
                }
            }
            drawPileBounds = new Rectangle2D.Double(dx - 10, dy - 10, w + 10, h + 10);
            label(g2, engine.drawPileSize() + " to draw", dx + w / 2, dy + h + 26, Theme.CHALK_DIM);

            // Discard pile with a ring in the active color.
            Card top = engine.topCard();
            double px = cx + 34;
            java.awt.Color active = Theme.suit(engine.activeColor());
            for (int i = 6; i >= 1; i--) {
                g2.setColor(new java.awt.Color(active.getRed(), active.getGreen(), active.getBlue(), 18 + (6 - i) * 14));
                g2.setStroke(new BasicStroke(i * 3f));
                g2.draw(new RoundRectangle2D.Double(px - 4, dy - 4, w + 8, h + 8, w * 0.2, w * 0.2));
            }
            CardPainter.paintFace(g2, top, px, dy, w, h);
            paintActiveChip(g2, px + w / 2, dy + h + 26, top.isWild());
            g2.dispose();
        }

        /** A faint loop around the piles whose arrowheads show which way play is going. */
        private void paintDirection(Graphics2D g2, double cx, double cy, double rw, double rh) {
            g2.setColor(new java.awt.Color(255, 255, 255, 34));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(cx - rw / 2, cy - rh / 2, rw, rh, 20, 140, Arc2D.OPEN));
            g2.draw(new Arc2D.Double(cx - rw / 2, cy - rh / 2, rw, rh, 200, 140, Arc2D.OPEN));
            // Arrowheads at the end of each arc, pointing the way play moves around the table.
            boolean cw = engine.isClockwise();
            arrowHead(g2, cx, cy, rw, rh, cw ? 20 : 160, cw);
            arrowHead(g2, cx, cy, rw, rh, cw ? 200 : 340, cw);
        }

        private void arrowHead(Graphics2D g2, double cx, double cy, double rw, double rh, double deg, boolean cw) {
            double a = Math.toRadians(deg);
            double px = cx + Math.cos(a) * rw / 2;
            double py = cy - Math.sin(a) * rh / 2;
            // Tangent of the ellipse, flipped for the travel direction.
            double tx = -Math.sin(a) * rw / 2;
            double ty = -Math.cos(a) * rh / 2;
            if (cw) {
                tx = -tx;
                ty = -ty;
            }
            double len = Math.hypot(tx, ty);
            tx /= len;
            ty /= len;
            double s = 13;
            Path2D.Double p = new Path2D.Double();
            p.moveTo(px + tx * s, py + ty * s);
            p.lineTo(px - ty * s * 0.6, py + tx * s * 0.6);
            p.lineTo(px + ty * s * 0.6, py - tx * s * 0.6);
            p.closePath();
            g2.setColor(new java.awt.Color(255, 255, 255, 60));
            g2.fill(p);
        }

        private void paintActiveChip(Graphics2D g2, double cx, double baseline, boolean fromWild) {
            Color c = engine.activeColor();
            String text = (fromWild ? "Wild set the color: " : "Match color: ") + c;
            g2.setFont(Theme.font(Font.BOLD, 15f));
            FontMetrics fm = g2.getFontMetrics();
            double w = fm.stringWidth(text) + 44;
            double h = 30;
            double x = cx - w / 2;
            double y = baseline - h + 9;
            g2.setColor(new java.awt.Color(0, 0, 0, 90));
            g2.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
            g2.setColor(Theme.suit(c));
            g2.fill(new Ellipse2D.Double(x + 9, y + 7, 16, 16));
            g2.setColor(java.awt.Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Ellipse2D.Double(x + 9, y + 7, 16, 16));
            g2.setColor(Theme.CHALK);
            g2.drawString(text, (float) (x + 33), (float) (y + 20));
        }

        private void label(Graphics2D g2, String text, double cx, double baseline, java.awt.Color color) {
            g2.setFont(Theme.font(Font.PLAIN, 15f));
            g2.setColor(color);
            g2.drawString(text, (float) (cx - g2.getFontMetrics().stringWidth(text) / 2.0), (float) baseline);
        }
    }

    static String fit(Graphics2D g2, String text, double width) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "...") > width) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    // ---- turn bar message -------------------------------------------------------------------

    /** One line of guidance. Errors get a red background so they're noticed but don't block play. */
    static final class MessagePill extends JComponent {
        private static final long serialVersionUID = 1L;

        enum Kind { HINT, GOOD, ERROR }

        private String text = "";
        private Kind kind = Kind.HINT;

        MessagePill() {
            setPreferredSize(new Dimension(400, 46));
        }

        void show(String text, Kind kind) {
            this.text = text;
            this.kind = kind;
            setToolTipText(text);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (text.isEmpty()) {
                return;
            }
            Graphics2D g2 = Theme.smooth(g);
            g2.setFont(Theme.font(kind == Kind.HINT ? Font.PLAIN : Font.BOLD, 15f));
            FontMetrics fm = g2.getFontMetrics();
            double maxW = getWidth();
            List<String> lines = wrap(fm, text, maxW - 36);
            double lineH = fm.getHeight();
            double h = Math.min(getHeight(), lines.size() * lineH + 14);
            double w = Math.min(maxW, lines.stream().mapToInt(fm::stringWidth).max().orElse(0) + 36);
            double x = (getWidth() - w) / 2;
            double y = (getHeight() - h) / 2;
            java.awt.Color bg = switch (kind) {
                case HINT -> new java.awt.Color(0, 0, 0, 60);
                case GOOD -> new java.awt.Color(0xFF, 0xD5, 0x4A, 60);
                case ERROR -> Theme.ERROR_BG;
            };
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Double(x, y, w, h, 22, 22));
            g2.setColor(Theme.CHALK);
            double ty = y + 7 + fm.getAscent();
            for (String line : lines) {
                g2.drawString(line, (float) (x + 18), (float) ty);
                ty += lineH;
            }
            g2.dispose();
        }

        static List<String> wrap(FontMetrics fm, String text, double width) {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String trial = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(trial) > width && line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(trial);
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
            return lines;
        }
    }
}
