package uno;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs a hot-seat Uno match: deals rounds, enforces the rules, tracks the active color after wild
 * cards, and scores each round until someone reaches the target.
 */
public final class Game {
    private static final int HAND_SIZE = 7;

    private final List<Player> players;
    private final Scanner in;
    private final int targetScore;
    private final boolean ansi;
    private final Deck deck = new Deck();

    /** The color in force. After a wild card this differs from the top card's own color. */
    private Color activeColor;
    private boolean clockwise;
    private int round;

    /** Thrown when the players type Q or input runs out. */
    private static final class QuitException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public Game(List<Player> players, Scanner in, int targetScore, boolean ansi) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("Uno needs at least 2 players");
        }
        this.players = new ArrayList<>(players);
        this.in = in;
        this.targetScore = targetScore;
        this.ansi = ansi;
    }

    /** Plays rounds until a player reaches the target score or the players quit. */
    public void play() {
        try {
            while (players.stream().noneMatch(p -> p.score() >= targetScore)) {
                round++;
                playRound();
                printStandings();
            }
            announceWinner();
        } catch (QuitException e) {
            System.out.println("\nGame ended early. Thanks for playing!");
        }
    }

    // ---- round flow ----------------------------------------------------------------------------

    private void playRound() {
        System.out.printf("%n==================== ROUND %d ====================%n", round);

        deck.resetAndShuffle();
        for (Player p : players) {
            p.clearHand();
        }
        for (int i = 0; i < HAND_SIZE; i++) {
            for (Player p : players) {
                p.receive(deck.draw());
            }
        }
        activeColor = deck.flipStarter().color();
        clockwise = true;

        int current = (round - 1) % players.size();   // the opening player rotates each round

        while (true) {
            Player player = players.get(current);
            Card played = takeTurn(player);

            int seatsToAdvance = 1;
            if (played != null) {
                seatsToAdvance = applyEffects(player, played, current);
                if (player.hand().isEmpty()) {
                    scoreRound(player);
                    return;
                }
                if (player.hand().size() == 1) {
                    System.out.printf("%n*** UNO! %s has one card left! ***%n", player);
                }
            }
            current = Math.floorMod(current + (clockwise ? seatsToAdvance : -seatsToAdvance), players.size());
        }
    }

    /** @return the card played, or null if the player drew (and passed) instead */
    private Card takeTurn(Player player) {
        Card top = deck.topDiscard();

        System.out.printf("%n--------------------------------------------------%n");
        System.out.printf("Top card: %s    Active color: %s%n", top.describe(ansi), activeColor.paint(activeColor.toString(), ansi));
        System.out.println("Cards held: " + cardCounts());
        ask(player + ", press Enter to see your hand (Q quits at any prompt)... ");

        boolean anyLegal = player.hand().stream().anyMatch(c -> isLegal(player, c));
        showHand(player);

        if (!anyLegal) {
            System.out.println("\nNo playable card - you draw one.");
            return drawThenMaybePlay(player);
        }

        while (true) {
            String answer = ask(String.format("%nPlay a card (1-%d) or D to draw: ", player.hand().size()));
            if (answer.equalsIgnoreCase("d")) {
                return drawThenMaybePlay(player);
            }
            int choice;
            try {
                choice = Integer.parseInt(answer);
            } catch (NumberFormatException e) {
                System.out.println("Enter a card number or D.");
                continue;
            }
            if (choice < 1 || choice > player.hand().size()) {
                System.out.println("There's no card #" + choice + ".");
                continue;
            }
            Card card = player.hand().get(choice - 1);
            if (!isLegal(player, card)) {
                System.out.println(explainIllegal(player, card));
                continue;
            }
            player.hand().remove(choice - 1);
            deck.discard(card);
            System.out.printf("%n%s plays %s%n", player, card.describe(ansi));
            return card;
        }
    }

    private Card drawThenMaybePlay(Player player) {
        Card drawn = deck.draw();
        if (drawn == null) {
            System.out.println("There are no cards left to draw. Turn passes.");
            return null;
        }
        System.out.println("You drew: " + drawn.describe(ansi));
        if (isLegal(player, drawn) && askYesNo("Play it now?")) {
            deck.discard(drawn);
            System.out.printf("%n%s plays %s%n", player, drawn.describe(ansi));
            return drawn;
        }
        player.receive(drawn);
        return null;
    }

    /**
     * Applies a played card's effect: updates the active color, direction and penalties.
     *
     * @return how many seats to advance (2 means the next player is skipped)
     */
    private int applyEffects(Player player, Card card, int current) {
        if (card.isWild()) {
            if (!player.hand().isEmpty()) {          // no need to pick a color if the round just ended
                activeColor = askColor(player);
                System.out.printf("%s chose %s.%n", player, activeColor.paint(activeColor.toString(), ansi));
            }
        } else {
            activeColor = card.color();
        }

        Player next = players.get(Math.floorMod(current + (clockwise ? 1 : -1), players.size()));
        switch (card.rank()) {
            case REVERSE:
                clockwise = !clockwise;
                // with two players a reverse acts like a skip
                return players.size() == 2 ? 2 : 1;
            case SKIP:
                System.out.printf("%s is skipped.%n", next);
                return 2;
            case DRAW_TWO:
                forceDraw(next, 2);
                return 2;
            case WILD_DRAW_FOUR:
                forceDraw(next, 4);
                return 2;
            default:
                return 1;
        }
    }

    private void forceDraw(Player victim, int count) {
        System.out.printf("%s draws %d cards and loses their turn.%n", victim, count);
        for (int i = 0; i < count; i++) {
            Card card = deck.draw();
            if (card == null) {
                break;
            }
            victim.receive(card);
        }
    }

    // ---- rules ---------------------------------------------------------------------------------

    /**
     * A card is legal if it matches the top card / active color (or is wild). Wild Draw 4 is further
     * restricted to players holding no card of the active color.
     */
    private boolean isLegal(Player player, Card card) {
        if (!card.matches(deck.topDiscard(), activeColor)) {
            return false;
        }
        return card.rank() != Rank.WILD_DRAW_FOUR || !player.hasColor(activeColor);
    }

    private String explainIllegal(Player player, Card card) {
        if (card.rank() == Rank.WILD_DRAW_FOUR && player.hasColor(activeColor)) {
            return "Wild Draw 4 is only allowed when you have no " + activeColor + " card.";
        }
        return "That card doesn't match " + activeColor + " or the top card's value.";
    }

    // ---- scoring and reporting -----------------------------------------------------------------

    private void scoreRound(Player winner) {
        int points = 0;
        for (Player p : players) {
            points += p.handPoints();      // the winner's hand is empty, so it adds nothing
        }
        winner.addToScore(points);
        System.out.printf("%n*** %s wins round %d and scores %d points! ***%n", winner, round, points);
    }

    private void printStandings() {
        System.out.printf("%nStandings after round %d (first to %d wins):%n", round, targetScore);
        players.stream()
               .sorted((a, b) -> Integer.compare(b.score(), a.score()))
               .forEach(p -> System.out.printf("  %-15s %4d%n", p, p.score()));
    }

    private void announceWinner() {
        int best = players.stream().mapToInt(Player::score).max().orElse(0);
        List<String> winners = players.stream().filter(p -> p.score() == best).map(Player::name).toList();
        System.out.printf("%n%s with %d points. Game over!%n",
                          winners.size() == 1 ? winners.get(0) + " wins" : "Tie between " + String.join(" and ", winners),
                          best);
    }

    // ---- display and input helpers -------------------------------------------------------------

    private String cardCounts() {
        StringBuilder sb = new StringBuilder();
        for (Player p : players) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p).append(' ').append(p.hand().size());
        }
        return sb.toString();
    }

    private void showHand(Player player) {
        System.out.printf("%n%s's hand (* = playable):%n", player);
        for (int i = 0; i < player.hand().size(); i++) {
            Card card = player.hand().get(i);
            System.out.printf("  %2d. %s%s%n", i + 1, card.describe(ansi), isLegal(player, card) ? " *" : "");
        }
    }

    private Color askColor(Player player) {
        while (true) {
            Color chosen = Color.parseSuit(ask("Choose a color (R, Y, G, B): "));
            if (chosen != null) {
                return chosen;
            }
            System.out.println("Please enter R, Y, G or B.");
        }
    }

    private boolean askYesNo(String question) {
        while (true) {
            String answer = ask(question + " (y/n): ");
            if (answer.equalsIgnoreCase("y")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n")) {
                return false;
            }
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            throw new QuitException();
        }
        String line = in.nextLine().trim();
        if (line.equalsIgnoreCase("q")) {
            throw new QuitException();
        }
        return line;
    }
}
