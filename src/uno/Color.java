package uno;

/** The four suit colors plus WILD, which belongs to no suit. */
public enum Color {
    RED("Red", "\u001B[31m"),
    YELLOW("Yellow", "\u001B[33m"),
    GREEN("Green", "\u001B[32m"),
    BLUE("Blue", "\u001B[34m"),
    WILD("Wild", "\u001B[35m");

    private final String label;
    private final String ansi;

    Color(String label, String ansi) {
        this.label = label;
        this.ansi = ansi;
    }

    /** The four colors a player can actually choose or hold in a suit. */
    public static Color[] suits() {
        return new Color[] {RED, YELLOW, GREEN, BLUE};
    }

    /** Parses R/Y/G/B (any case, or the full name); returns null if it isn't a suit color. */
    public static Color parseSuit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        char c = Character.toLowerCase(text.trim().charAt(0));
        for (Color color : suits()) {
            if (Character.toLowerCase(color.label.charAt(0)) == c) {
                return color;
            }
        }
        return null;
    }

    /** Wraps text in this color's terminal escape codes. */
    public String paint(String text, boolean useAnsi) {
        return useAnsi ? ansi + text + "\u001B[0m" : text;
    }

    @Override
    public String toString() {
        return label;
    }
}
