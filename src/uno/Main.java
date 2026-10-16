package uno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** Entry point. Options: {@code --plain} disables colors, {@code --target=N} sets the winning score. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        boolean ansi = !Arrays.asList(args).contains("--plain");
        int target = 500;
        for (String arg : args) {
            if (arg.startsWith("--target=")) {
                target = Integer.parseInt(arg.substring("--target=".length()));
            }
        }

        try (Scanner in = new Scanner(System.in)) {
            System.out.println("Welcome to Uno!");
            System.out.println("Win rounds by emptying your hand; you score the cards left in everyone else's.");
            System.out.printf("First player to %d points wins.%n%n", target);

            int count = askPlayerCount(in);
            if (count < 0) {
                return;
            }
            List<Player> players = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                System.out.printf("Name of player %d: ", i);
                if (!in.hasNextLine()) {
                    return;
                }
                String name = in.nextLine().trim();
                players.add(new Player(name.isEmpty() ? "Player " + i : name));
            }
            new Game(players, in, target, ansi).play();
        }
    }

    private static int askPlayerCount(Scanner in) {
        while (true) {
            System.out.print("How many players (2-10)? ");
            if (!in.hasNextLine()) {
                return -1;
            }
            try {
                int n = Integer.parseInt(in.nextLine().trim());
                if (n >= 2 && n <= 10) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
                // fall through and ask again
            }
            System.out.println("Please enter a number from 2 to 10.");
        }
    }
}
