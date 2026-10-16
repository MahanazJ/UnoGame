package uno;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** The draw pile and discard pile together, so recycling the discards is one internal operation. */
public final class Deck {
    private final Deque<Card> drawPile = new ArrayDeque<>();
    private final Deque<Card> discardPile = new ArrayDeque<>();

    /** Puts all 108 cards back in the draw pile, shuffled, and empties the discard pile. */
    public void resetAndShuffle() {
        drawPile.clear();
        discardPile.clear();
        List<Card> all = new ArrayList<>(108);
        for (Rank rank : Rank.values()) {
            if (rank.isWild()) {
                for (int i = 0; i < rank.copies(); i++) {
                    all.add(Card.of(Color.WILD, rank));
                }
            } else {
                for (Color color : Color.suits()) {
                    for (int i = 0; i < rank.copies(); i++) {
                        all.add(Card.of(color, rank));
                    }
                }
            }
        }
        Collections.shuffle(all);
        drawPile.addAll(all);
    }

    /**
     * Draws the top card, first recycling the discards (all but the top one) if the draw pile is empty.
     *
     * @return the card, or null if every card is already in players' hands
     */
    public Card draw() {
        if (drawPile.isEmpty()) {
            recycleDiscards();
        }
        return drawPile.pollFirst();
    }

    public void discard(Card card) {
        discardPile.push(card);
    }

    public Card topDiscard() {
        return discardPile.peek();
    }

    public int drawPileSize() {
        return drawPile.size();
    }

    /**
     * Turns up the first discard. A wild or action card can't start the pile, so those go back under
     * the draw pile until a number card appears.
     */
    public Card flipStarter() {
        Card card = draw();
        while (!card.rank().isNumber()) {
            drawPile.addLast(card);
            card = draw();
        }
        discard(card);
        return card;
    }

    private void recycleDiscards() {
        if (discardPile.size() <= 1) {
            return;
        }
        Card top = discardPile.pop();
        List<Card> rest = new ArrayList<>(discardPile);
        discardPile.clear();
        Collections.shuffle(rest);
        drawPile.addAll(rest);
        discardPile.push(top);
        System.out.println("(Draw pile empty - reshuffled the discard pile.)");
    }
}
