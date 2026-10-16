package uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The rules of a match with no input or output, so a graphical front end can drive it one click at a
 * time. It follows the same rules as the console {@link Game}: same deck, same matching and Wild Draw 4
 * restriction, same effects, same scoring.
 */
public final class UnoEngine {
    public static final int HAND_SIZE = 7;

    /** What happened on a turn, in words the next player can read on the hand-off screen. */
    public record TurnOutcome(String summary, boolean roundOver, Player winner, int points) {
    }

    private final List<Player> players;
    private final int targetScore;
    private final Deck deck = new Deck();

    private Color activeColor;
    private boolean clockwise = true;
    private int round;
    private int current;

    /** A player may draw once per turn; after that only the drawn card can be played. */
    private boolean hasDrawn;
    private Card drawnCard;
    private String deckNote = "";

    public UnoEngine(List<Player> players, int targetScore) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("Uno needs at least 2 players");
        }
        this.players = new ArrayList<>(players);
        this.targetScore = targetScore;
    }

    // ---- round flow ----------------------------------------------------------------------------

    /** Shuffles, deals and turns up the starter. @return a sentence describing the new round */
    public String startRound() {
        round++;
        deck.resetAndShuffle();
        for (Player p : players) {
            p.clearHand();
        }
        for (int i = 0; i < HAND_SIZE; i++) {
            for (Player p : players) {
                p.receive(deck.draw());
            }
        }
        Card starter = deck.flipStarter();
        activeColor = starter.color();
        clockwise = true;
        current = (round - 1) % players.size();   // the opening player rotates each round
        resetTurn();
        return "Round " + round + " begins. The starting card is " + starter + ".";
    }

    /** @return null if the current player may play this card, otherwise a sentence saying why not */
    public String illegalReason(Card card) {
        Player player = currentPlayer();
        if (hasDrawn && card != drawnCard) {
            return "You've already drawn this turn. Play the card you drew or pass.";
        }
        if (!card.matches(topCard(), activeColor)) {
            Card top = topCard();
            String rankOption = top.isWild() ? "" : ", any " + top.rank();
            return card + " can't go here. You need a " + activeColor + " card" + rankOption + ", or a Wild.";
        }
        if (card.rank() == Rank.WILD_DRAW_FOUR && player.hasColor(activeColor)) {
            return "Wild Draw 4 is only allowed when you hold no " + activeColor
                    + " cards, and you have at least one. Play that instead, or a plain Wild.";
        }
        return null;
    }

    public boolean isPlayable(Card card) {
        return illegalReason(card) == null;
    }

    public boolean hasPlayableCard() {
        return currentPlayer().hand().stream().anyMatch(this::isPlayable);
    }

    /** A wild needs a color unless it is the player's last card (the round ends instead). */
    public boolean needsColorChoice(Card card) {
        return card.isWild() && currentPlayer().hand().size() > 1;
    }

    /**
     * Plays a card from the current player's hand and moves the turn on.
     *
     * @param chosen the color picked for a wild; ignored for other cards
     */
    public TurnOutcome play(Card card, Color chosen) {
        String reason = illegalReason(card);
        if (reason != null) {
            throw new IllegalStateException(reason);
        }
        Player player = currentPlayer();
        if (!player.hand().remove(card)) {
            throw new IllegalArgumentException(card + " is not in " + player + "'s hand");
        }
        deck.discard(card);
        StringBuilder msg = new StringBuilder(player + " played " + card + ".");

        if (card.isWild()) {
            if (!player.hand().isEmpty()) {
                if (chosen == null || chosen == Color.WILD) {
                    throw new IllegalArgumentException("A wild card needs a suit color");
                }
                activeColor = chosen;
                msg.append(" They chose ").append(chosen).append('.');
            }
        } else {
            activeColor = card.color();
        }

        // Effects happen before scoring, so a final Draw 2 still counts toward the winner's points.
        Player next = players.get(step(1));
        int advance = 1;
        switch (card.rank()) {
            case REVERSE -> {
                clockwise = !clockwise;
                if (players.size() == 2) {
                    advance = 2;                          // with two players a reverse acts like a skip
                    msg.append(' ').append(next).append(" is skipped.");
                } else {
                    msg.append(" Play now goes ").append(clockwise ? "clockwise." : "counter-clockwise.");
                }
            }
            case SKIP -> {
                advance = 2;
                msg.append(' ').append(next).append(" is skipped.");
            }
            case DRAW_TWO -> {
                advance = 2;
                msg.append(' ').append(forceDraw(next, 2));
            }
            case WILD_DRAW_FOUR -> {
                advance = 2;
                msg.append(' ').append(forceDraw(next, 4));
            }
            default -> {
            }
        }
        msg.append(takeDeckNote());

        if (player.hand().isEmpty()) {
            int points = players.stream().mapToInt(Player::handPoints).sum();
            player.addToScore(points);
            return new TurnOutcome(msg.toString(), true, player, points);
        }
        if (player.hand().size() == 1) {
            msg.append(" UNO! ").append(player).append(" has one card left.");
        }
        current = step(advance);
        resetTurn();
        return new TurnOutcome(msg.toString(), false, null, 0);
    }

    /** Draws one card into the current player's hand. @return the card, or null if none are left */
    public Card draw() {
        if (hasDrawn) {
            throw new IllegalStateException("Already drew this turn");
        }
        hasDrawn = true;
        drawnCard = drawOne();
        if (drawnCard != null) {
            currentPlayer().receive(drawnCard);
        }
        return drawnCard;
    }

    /** Ends the turn after drawing, keeping the drawn card. */
    public TurnOutcome pass() {
        if (!hasDrawn) {
            throw new IllegalStateException("Draw before passing");
        }
        Player player = currentPlayer();
        String msg = drawnCard == null ? player + " couldn't draw and passed." : player + " drew a card and passed.";
        current = step(1);
        resetTurn();
        return new TurnOutcome(msg, false, null, 0);
    }

    /** Explains a reshuffle caused by the last draw, once; empty if there was none. */
    public String takeDeckNote() {
        String note = deckNote;
        deckNote = "";
        return note;
    }

    // ---- state for the screens -----------------------------------------------------------------

    public List<Player> players() {
        return Collections.unmodifiableList(players);
    }

    public Player currentPlayer() {
        return players.get(current);
    }

    /** Who plays next if this turn ends normally (no skip). */
    public Player nextPlayer() {
        return players.get(step(1));
    }

    public Card topCard() {
        return deck.topDiscard();
    }

    public Color activeColor() {
        return activeColor;
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public int round() {
        return round;
    }

    public int targetScore() {
        return targetScore;
    }

    public int drawPileSize() {
        return deck.drawPileSize();
    }

    public boolean hasDrawn() {
        return hasDrawn;
    }

    public Card drawnCard() {
        return drawnCard;
    }

    public boolean isMatchOver() {
        return players.stream().anyMatch(p -> p.score() >= targetScore);
    }

    /** Everyone tied for the highest score. */
    public List<Player> leaders() {
        int best = players.stream().mapToInt(Player::score).max().orElse(0);
        return players.stream().filter(p -> p.score() == best).toList();
    }

    // ---- internals -----------------------------------------------------------------------------

    private int step(int seats) {
        return Math.floorMod(current + (clockwise ? seats : -seats), players.size());
    }

    private void resetTurn() {
        hasDrawn = false;
        drawnCard = null;
    }

    private String forceDraw(Player victim, int count) {
        int drawn = 0;
        for (int i = 0; i < count; i++) {
            Card card = drawOne();
            if (card == null) {
                break;
            }
            victim.receive(card);
            drawn++;
        }
        return victim + " draws " + drawn + (drawn == 1 ? " card" : " cards") + " and loses their turn.";
    }

    private Card drawOne() {
        boolean recycling = deck.drawPileSize() == 0;
        Card card = deck.draw();
        if (recycling && card != null) {
            deckNote = " The draw pile ran out, so the discards were reshuffled.";
        }
        return card;
    }
}
