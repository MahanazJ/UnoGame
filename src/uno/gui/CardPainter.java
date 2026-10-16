package uno.gui;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import uno.Card;
import uno.Color;
import uno.Rank;

/** Draws cards with plain Java2D shapes, so there are no image files and nothing depends on fonts having symbols. */
final class CardPainter {
    /** Width : height of a real Uno card. */
    static final double ASPECT = 0.66;

    private CardPainter() {
    }

    static void paintFace(Graphics2D g, Card card, double x, double y, double w, double h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        paintShadow(g2, w, h);

        double arc = w * 0.16;
        g2.setColor(java.awt.Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));

        double inset = w * 0.065;
        Shape inner = new RoundRectangle2D.Double(inset, inset, w - 2 * inset, h - 2 * inset, arc * 0.7, arc * 0.7);
        java.awt.Color suit = Theme.suit(card.color());
        g2.setColor(suit);
        g2.fill(inner);

        // The tilted oval in the middle of every Uno card.
        double cx = w / 2;
        double cy = h / 2;
        Shape oval = tiltedOval(cx, cy, w * 0.78, h * 0.62);
        if (card.isWild()) {
            paintFourColors(g2, oval, cx, cy, w, h);
        } else {
            g2.setColor(java.awt.Color.WHITE);
            g2.fill(oval);
        }

        // Center symbol: suit-colored on white, or white on the four-color oval for wilds.
        java.awt.Color symbolFill = card.isWild() ? java.awt.Color.WHITE : suit;
        java.awt.Color outline = card.isWild() ? Theme.INK : suit.darker();
        drawSymbol(g2, card.rank(), cx, cy, w * 0.46, symbolFill, outline, w);

        // Corner indexes, the bottom one upside-down like a real card.
        double cornerSize = w * 0.2;
        java.awt.Color cornerFill = card.isWild() ? java.awt.Color.WHITE : Theme.onSuit(card.color());
        java.awt.Color cornerLine = card.isWild() ? Theme.INK : suit.darker();
        drawCorner(g2, card.rank(), w * 0.2, h * 0.15, cornerSize, cornerFill, cornerLine, w);
        Graphics2D flipped = (Graphics2D) g2.create();
        flipped.rotate(Math.PI, w / 2, h / 2);
        drawCorner(flipped, card.rank(), w * 0.2, h * 0.15, cornerSize, cornerFill, cornerLine, w);
        flipped.dispose();
        g2.dispose();
    }

    static void paintBack(Graphics2D g, double x, double y, double w, double h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        paintShadow(g2, w, h);
        double arc = w * 0.16;
        g2.setColor(java.awt.Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
        double inset = w * 0.065;
        g2.setColor(Theme.INK);
        g2.fill(new RoundRectangle2D.Double(inset, inset, w - 2 * inset, h - 2 * inset, arc * 0.7, arc * 0.7));
        // An original back: a thin gold frame around a diamond split into the four suit colors.
        double frame = w * 0.14;
        g2.setColor(Theme.GOLD);
        g2.setStroke(new BasicStroke((float) Math.max(1, w * 0.02)));
        g2.draw(new RoundRectangle2D.Double(frame, frame, w - 2 * frame, h - 2 * frame, arc * 0.5, arc * 0.5));
        double cx = w / 2;
        double cy = h / 2;
        double rx = w * 0.27;
        double ry = h * 0.22;
        java.awt.Color[] quads = {Theme.RED, Theme.YELLOW, Theme.GREEN, Theme.BLUE};
        double[][] tips = {{cx, cy - ry}, {cx + rx, cy}, {cx, cy + ry}, {cx - rx, cy}};
        for (int i = 0; i < 4; i++) {
            double[] p1 = tips[i];
            double[] p2 = tips[(i + 1) % 4];
            Path2D.Double tri = new Path2D.Double();
            tri.moveTo(cx, cy);
            tri.lineTo(p1[0], p1[1]);
            tri.lineTo(p2[0], p2[1]);
            tri.closePath();
            g2.setColor(quads[i]);
            g2.fill(tri);
        }
        Path2D.Double diamond = new Path2D.Double();
        diamond.moveTo(tips[0][0], tips[0][1]);
        for (int i = 1; i < 4; i++) {
            diamond.lineTo(tips[i][0], tips[i][1]);
        }
        diamond.closePath();
        g2.setColor(java.awt.Color.WHITE);
        g2.setStroke(new BasicStroke((float) Math.max(1, w * 0.025), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(diamond);
        g2.dispose();
    }

    // ---- pieces --------------------------------------------------------------------------------

    private static void paintShadow(Graphics2D g2, double w, double h) {
        double arc = w * 0.16;
        for (int i = 3; i >= 1; i--) {
            g2.setColor(new java.awt.Color(0, 0, 0, 22));
            g2.fill(new RoundRectangle2D.Double(-i * 0.5, i * 1.2, w + i, h + i * 0.4, arc, arc));
        }
    }

    private static Shape tiltedOval(double cx, double cy, double ow, double oh) {
        Shape oval = new Ellipse2D.Double(cx - ow / 2, cy - oh / 2, ow, oh);
        return AffineTransform.getRotateInstance(Math.toRadians(32), cx, cy).createTransformedShape(oval);
    }

    private static void paintFourColors(Graphics2D g2, Shape oval, double cx, double cy, double w, double h) {
        Graphics2D q = (Graphics2D) g2.create();
        q.clip(oval);
        java.awt.Color[] quads = {Theme.RED, Theme.BLUE, Theme.YELLOW, Theme.GREEN};
        Rectangle2D[] areas = {
            new Rectangle2D.Double(0, 0, cx, cy), new Rectangle2D.Double(cx, 0, w - cx, cy),
            new Rectangle2D.Double(0, cy, cx, h - cy), new Rectangle2D.Double(cx, cy, w - cx, h - cy),
        };
        for (int i = 0; i < 4; i++) {
            q.setColor(quads[i]);
            q.fill(areas[i]);
        }
        q.dispose();
        g2.setColor(java.awt.Color.WHITE);
        g2.setStroke(new BasicStroke((float) (w * 0.03)));
        g2.draw(oval);
    }

    private static void drawSymbol(Graphics2D g2, Rank rank, double cx, double cy, double size,
                                   java.awt.Color fill, java.awt.Color outline, double cardW) {
        float stroke = (float) (cardW * 0.03);
        switch (rank) {
            case SKIP -> drawSkip(g2, cx, cy, size * 0.42, fill, outline, stroke);
            case REVERSE -> drawReverse(g2, cx, cy, size * 0.9, fill, outline, stroke);
            case WILD -> {
                // The four-color oval is the symbol.
            }
            default -> {
                Font font = Theme.font(Font.BOLD | Font.ITALIC, (float) (rank.isNumber() ? size * 1.15 : size * 0.8));
                drawOutlinedText(g2, label(rank), cx, cy, font, fill, outline, stroke * 1.4);
                if (rank == Rank.SIX || rank == Rank.NINE) {
                    // Underline so a 6 and an upside-down 9 can't be confused.
                    g2.setColor(fill);
                    g2.fill(new RoundRectangle2D.Double(cx - size * 0.24, cy + size * 0.62, size * 0.48, size * 0.08, 4, 4));
                }
            }
        }
    }

    private static void drawCorner(Graphics2D g2, Rank rank, double cx, double cy, double size,
                                   java.awt.Color fill, java.awt.Color outline, double cardW) {
        float stroke = (float) Math.max(1, cardW * 0.016);
        switch (rank) {
            case SKIP -> drawSkip(g2, cx, cy, size * 0.36, fill, outline, stroke);
            case REVERSE -> drawReverse(g2, cx, cy, size * 0.8, fill, outline, stroke);
            default -> drawOutlinedText(g2, rank == Rank.WILD ? "W" : label(rank), cx, cy,
                    Theme.font(Font.BOLD | Font.ITALIC, (float) (size * (rank.isNumber() ? 0.95 : 0.7))),
                    fill, outline, stroke);
        }
    }

    private static String label(Rank rank) {
        return switch (rank) {
            case DRAW_TWO -> "+2";
            case WILD_DRAW_FOUR -> "+4";
            default -> rank.toString();
        };
    }

    /** A ring with a slash through it. */
    private static void drawSkip(Graphics2D g2, double cx, double cy, double r,
                                 java.awt.Color fill, java.awt.Color outline, float stroke) {
        float weight = (float) (r * 0.3);
        Shape ring = new BasicStroke(weight).createStrokedShape(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        double d = r * Math.cos(Math.PI / 4);
        Shape slash = new BasicStroke(weight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
                .createStrokedShape(new Line2D.Double(cx - d, cy + d, cx + d, cy - d));
        java.awt.geom.Area area = new java.awt.geom.Area(ring);
        area.add(new java.awt.geom.Area(slash));
        fillOutlined(g2, area, fill, outline, stroke);
    }

    /** Two opposite arrows on a diagonal. */
    private static void drawReverse(Graphics2D g2, double cx, double cy, double len,
                                    java.awt.Color fill, java.awt.Color outline, float stroke) {
        for (int k = 0; k < 2; k++) {
            AffineTransform t = new AffineTransform();
            t.translate(cx, cy);
            t.rotate(Math.toRadians(-45 + 180 * k));
            t.translate(0, -len * 0.17);
            fillOutlined(g2, t.createTransformedShape(arrow(len, len * 0.26)), fill, outline, stroke);
        }
    }

    /** An arrow pointing right, centered on the origin. */
    private static Shape arrow(double len, double width) {
        double shaft = width * 0.42;
        double headLen = len * 0.38;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(-len / 2, -shaft / 2);
        p.lineTo(len / 2 - headLen, -shaft / 2);
        p.lineTo(len / 2 - headLen, -width / 2);
        p.lineTo(len / 2, 0);
        p.lineTo(len / 2 - headLen, width / 2);
        p.lineTo(len / 2 - headLen, shaft / 2);
        p.lineTo(-len / 2, shaft / 2);
        p.closePath();
        return p;
    }

    static void drawOutlinedText(Graphics2D g2, String text, double cx, double cy, Font font,
                                 java.awt.Color fill, java.awt.Color outline, double stroke) {
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(text, font, frc);
        Rectangle2D b = layout.getBounds();
        AffineTransform at = AffineTransform.getTranslateInstance(cx - b.getCenterX(), cy - b.getCenterY());
        Shape shape = layout.getOutline(at);
        fillOutlined(g2, shape, fill, outline, (float) stroke);
    }

    private static void fillOutlined(Graphics2D g2, Shape shape, java.awt.Color fill, java.awt.Color outline, float stroke) {
        g2.setColor(outline);
        g2.setStroke(new BasicStroke(stroke * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);
        g2.setColor(fill);
        g2.fill(shape);
    }
}
