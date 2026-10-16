package uno.gui;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.JComponent;
import javax.swing.Timer;
import uno.Card;

/**
 * Lays the hand out along the bottom of the table. Playable cards sit raised with a gold edge,
 * the rest are dimmed; hovering lifts a playable card a little more, and clicking reports the card.
 */
final class HandPanel extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int LIFT = 34;

    private List<Card> cards = List.of();
    private Predicate<Card> playable = c -> false;
    private Card fresh;
    private int hover = -1;
    private Consumer<Card> onClick = c -> { };

    private Card shaking;
    private long shakeStart;
    private final Timer shakeTimer = new Timer(16, e -> {
        if (System.currentTimeMillis() - shakeStart > 360) {
            shaking = null;
            ((Timer) e.getSource()).stop();
        }
        repaint();
    });

    HandPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(900, 220));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX(), e.getY());
                if (i != hover) {
                    hover = i;
                    setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getX(), e.getY());
                if (i >= 0) {
                    onClick.accept(cards.get(i));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setOnClick(Consumer<Card> onClick) {
        this.onClick = onClick;
    }

    /** @param fresh the card just drawn, marked so the player can find it; may be null */
    void setHand(List<Card> hand, Predicate<Card> playable, Card fresh) {
        this.cards = new ArrayList<>(hand);
        this.playable = playable;
        this.fresh = fresh;
        this.hover = -1;
        repaint();
    }

    /** A quick side-to-side wobble on a card that can't be played. */
    void shake(Card card) {
        shaking = card;
        shakeStart = System.currentTimeMillis();
        shakeTimer.restart();
    }

    // ---- layout --------------------------------------------------------------------------------

    private double cardH() {
        return Math.min(178, getHeight() - LIFT - 26);
    }

    private double cardW() {
        return cardH() * CardPainter.ASPECT;
    }

    private Rectangle2D bounds(int i) {
        int n = cards.size();
        double w = cardW();
        double h = cardH();
        double margin = 28;
        double span = getWidth() - 2 * margin;
        double step = n <= 1 ? 0 : Math.min(w + 12, (span - w) / (n - 1));
        double total = w + step * (n - 1);
        double x = (getWidth() - total) / 2 + step * i;
        double y = getHeight() - h - 8;
        if (playable.test(cards.get(i))) {
            y -= LIFT * 0.6;
            if (i == hover) {
                y -= LIFT * 0.4;
            }
        }
        return new Rectangle2D.Double(x, y, w, h);
    }

    /** Topmost card under the point, or -1. Later cards overlap earlier ones. */
    private int indexAt(int px, int py) {
        for (int i = cards.size() - 1; i >= 0; i--) {
            Rectangle2D b = bounds(i);
            // Include the space under a raised card so the pointer doesn't fall through the gap.
            Rectangle2D hit = new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), getHeight() - b.getY());
            if (hit.contains(px, py)) {
                return i;
            }
        }
        return -1;
    }

    // ---- painting ------------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            Rectangle2D b = bounds(i);
            double x = b.getX();
            if (card == shaking) {
                double t = (System.currentTimeMillis() - shakeStart) / 360.0;
                x += Math.sin(t * Math.PI * 6) * 9 * (1 - t);
            }
            boolean canPlay = playable.test(card);
            if (canPlay) {
                paintGlow(g2, x, b.getY(), b.getWidth(), b.getHeight(), i == hover);
            }
            CardPainter.paintFace(g2, card, x, b.getY(), b.getWidth(), b.getHeight());
            if (!canPlay) {
                g2.setColor(new java.awt.Color(6, 30, 18, 140));
                double arc = b.getWidth() * 0.16;
                g2.fill(new RoundRectangle2D.Double(x, b.getY(), b.getWidth(), b.getHeight(), arc, arc));
            }
            if (card == fresh) {
                paintTag(g2, "Drawn", x + b.getWidth() / 2, b.getY() - 4);
            }
        }
        g2.dispose();
    }

    private void paintGlow(Graphics2D g2, double x, double y, double w, double h, boolean strong) {
        double arc = w * 0.16;
        int layers = strong ? 9 : 7;
        for (int i = layers; i >= 1; i--) {
            int alpha = (strong ? 40 : 30) + (layers - i) * 7;
            g2.setColor(new java.awt.Color(Theme.GOLD.getRed(), Theme.GOLD.getGreen(), Theme.GOLD.getBlue(), alpha));
            g2.setStroke(new BasicStroke(i * 2.6f));
            g2.draw(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
        }
        g2.setColor(Theme.GOLD);
        g2.setStroke(new BasicStroke(3.5f));
        g2.draw(new RoundRectangle2D.Double(x - 1.5, y - 1.5, w + 3, h + 3, arc, arc));
    }

    private void paintTag(Graphics2D g2, String text, double cx, double bottom) {
        g2.setFont(Theme.font(Font.BOLD, 12f));
        int tw = g2.getFontMetrics().stringWidth(text);
        double w = tw + 16;
        double h = 20;
        g2.setColor(Theme.GOLD);
        g2.fill(new RoundRectangle2D.Double(cx - w / 2, bottom - h, w, h, h, h));
        g2.setColor(Theme.INK);
        g2.drawString(text, (float) (cx - tw / 2.0), (float) (bottom - 6));
    }
}
