package uno;

/** Skip, Reverse, Draw 2, Wild or Wild Draw 4. The game applies the effect; this class owns the scoring. */
public final class ActionCard extends Card {
    ActionCard(Color color, Rank rank) {
        super(color, rank);
        if (rank.isNumber()) {
            throw new IllegalArgumentException("Not an action card: " + color + " " + rank);
        }
        if (rank.isWild() != (color == Color.WILD)) {
            throw new IllegalArgumentException("Wild ranks use Color.WILD, others use a suit: " + color + " " + rank);
        }
    }

    @Override
    public int points() {
        return rank().points();
    }
}
