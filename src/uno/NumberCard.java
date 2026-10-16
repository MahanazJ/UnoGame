package uno;

/** A 0-9 card; worth its face value. */
public final class NumberCard extends Card {
    NumberCard(Color color, Rank rank) {
        super(color, rank);
        if (!rank.isNumber() || color == Color.WILD) {
            throw new IllegalArgumentException("Not a number card: " + color + " " + rank);
        }
    }

    @Override
    public int points() {
        return rank().points();
    }
}
