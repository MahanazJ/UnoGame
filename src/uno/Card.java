package uno;

/**
 * A single Uno card. Immutable. Subclasses decide how the card scores; the matching rule is shared.
 */
public abstract class Card {
    private final Color color;
    private final Rank rank;

    protected Card(Color color, Rank rank) {
        this.color = color;
        this.rank = rank;
    }

    /** Builds the right subclass for a color/rank pair. */
    public static Card of(Color color, Rank rank) {
        return rank.isNumber() ? new NumberCard(color, rank) : new ActionCard(color, rank);
    }

    public Color color() {
        return color;
    }

    public Rank rank() {
        return rank;
    }

    public boolean isWild() {
        return rank.isWild();
    }

    /** Points this card is worth to the round winner if it is left in a hand. */
    public abstract int points();

    /**
     * Basic matching rule: a wild is always allowed; otherwise match the color currently in force
     * (which a wild may have changed) or match the rank of the top card.
     */
    public boolean matches(Card top, Color activeColor) {
        return isWild() || color == activeColor || rank == top.rank;
    }

    public String describe(boolean useAnsi) {
        String text = isWild() ? rank.toString() : color + " " + rank;
        return color.paint(text, useAnsi);
    }

    @Override
    public String toString() {
        return describe(false);
    }
}
