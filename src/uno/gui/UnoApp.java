package uno.gui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import uno.Player;
import uno.UnoEngine;
import uno.UnoEngine.TurnOutcome;

/**
 * The windowed version of the game. One window flips between four screens:
 * welcome, pass-the-computer, the table, and round results.
 *
 * <p>Options: {@code --target=N} sets the default winning score.
 */
public final class UnoApp {
    private static final String WELCOME = "welcome";
    private static final String PASS = "pass";
    private static final String TABLE = "table";
    private static final String RESULTS = "results";

    private final JFrame frame = new JFrame("Uno");
    private final CardLayout screens = new CardLayout();
    private final JPanel root = new JPanel(screens);

    private final WelcomeScreen welcome;
    private final PassScreen pass;
    private final TableScreen table;
    private final ResultScreen results;

    private UnoEngine engine;

    UnoApp(int defaultTarget) {
        welcome = new WelcomeScreen(this, defaultTarget);
        pass = new PassScreen(this);
        table = new TableScreen(this);
        results = new ResultScreen(this);
        root.add(welcome, WELCOME);
        root.add(pass, PASS);
        root.add(table, TABLE);
        root.add(results, RESULTS);

        frame.setContentPane(root);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                quit();
            }
        });
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(Math.min(1240, (int) (screen.width * 0.92)), Math.min(820, (int) (screen.height * 0.9)));
        frame.setMinimumSize(new Dimension(960, 680));
        frame.setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        int target = 500;
        for (String arg : args) {
            if (arg.startsWith("--target=")) {
                target = Integer.parseInt(arg.substring("--target=".length()));
            }
        }
        int chosen = target;
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> new UnoApp(chosen).open());
    }

    void open() {
        screens.show(root, WELCOME);
        frame.setVisible(true);
    }

    // ---- screen flow, called by the screens ----------------------------------------------------

    void startMatch(List<String> names, int target) {
        List<Player> players = new ArrayList<>();
        for (String name : names) {
            players.add(new Player(name));
        }
        engine = new UnoEngine(players, target);
        showPass(engine.startRound());
    }

    /** The current player has the computer and pressed Reveal. */
    void reveal() {
        table.showTurn(engine);
        screens.show(root, TABLE);
        frame.getRootPane().setDefaultButton(null);
    }

    void finishTurn(TurnOutcome outcome) {
        if (outcome.roundOver()) {
            screens.show(root, RESULTS);
            results.show(engine, outcome);
        } else {
            showPass(outcome.summary());
        }
    }

    void nextRound() {
        showPass(engine.startRound());
    }

    void newGame() {
        engine = null;
        welcome.reset();
        frame.getRootPane().setDefaultButton(null);
        screens.show(root, WELCOME);
    }

    void quit() {
        boolean inGame = engine != null;
        if (inGame) {
            int answer = JOptionPane.showConfirmDialog(frame, "Quit this game? Scores won't be saved.",
                    "Quit Uno", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
        }
        frame.dispose();
        System.exit(0);
    }

    private void showPass(String summary) {
        // Switch screens before anything else so no hand is ever on screen during the hand-off.
        screens.show(root, PASS);
        pass.show(engine, summary);
        pass.focusButton();
    }
}
