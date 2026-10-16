package uno;

/** Every face value in the deck, with its scoring value and how many copies exist per color. */
public enum Rank {
    ZERO("0", 0, 1),
    ONE("1", 1, 2),
    TWO("2", 2, 2),
    THREE("3", 3, 2),
    FOUR("4", 4, 2),
    FIVE("5", 5, 2),
    SIX("6", 6, 2),
    SEVEN("7", 7, 2),
    EIGHT("8", 8, 2),
    NINE("9", 9, 2),
    SKIP("Skip", 20, 2),
    REVERSE("Reverse", 20, 2),
    DRAW_TWO("Draw 2", 20, 2),
    WILD("Wild", 50, 4),
    WILD_DRAW_FOUR("Wild Draw 4", 50, 4);

    private final String symbol;
    private final int points;
    private final int copies;

    Rank(String symbol, int points, int copies) {
        this.symbol = symbol;
        this.points = points;
        this.copies = copies;
    }

    public int points() {
        return points;
    }

    /** Copies per color for suit ranks; total copies for the two wild ranks. */
    public int copies() {
        return copies;
    }

    public boolean isNumber() {
        return ordinal() <= NINE.ordinal();
    }

    public boolean isWild() {
        return this == WILD || this == WILD_DRAW_FOUR;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
