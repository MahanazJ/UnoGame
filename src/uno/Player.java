package uno;

import java.util.ArrayList;
import java.util.List;

/** A player's name, hand, and running match score. */
public final class Player {
    private final String name;
    private final List<Card> hand = new ArrayList<>();
    private int score;

    public Player(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public List<Card> hand() {
        return hand;
    }

    public int score() {
        return score;
    }

    public void addToScore(int points) {
        score += points;
    }

    public void receive(Card card) {
        hand.add(card);
        hand.sort((a, b) -> {
            int byColor = a.color().compareTo(b.color());
            return byColor != 0 ? byColor : a.rank().compareTo(b.rank());
        });
    }

    public boolean hasColor(Color color) {
        return hand.stream().anyMatch(c -> c.color() == color);
    }

    public int handPoints() {
        return hand.stream().mapToInt(Card::points).sum();
    }

    public void clearHand() {
        hand.clear();
    }

    @Override
    public String toString() {
        return name;
    }
}
