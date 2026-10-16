# Console Uno

A multiplayer, hot-seat Uno game for the terminal, written in Java 21 with no external dependencies.

## Run it

```bash
javac -d out src/uno/*.java
java -cp out uno.Main
```

Options: `--plain` turns off terminal colors; `--target=200` changes the winning score (default 500).

### Windowed version

```bash
javac -d out src/uno/*.java src/uno/gui/*.java
java -cp out uno.gui.UnoApp
```

`--target=N` sets the default winning score shown on the welcome screen. The console game still works exactly as before.

## How it plays

2-10 players share one keyboard. Each turn the game shows the top card, the active color and everyone's card counts, then shows the current player's hand with playable cards marked `*`. Enter a card's number to play it, or `D` to draw. `Q` quits at any prompt.

## Design

| Piece | Role |
|---|---|
| `Card` (abstract) -> `NumberCard`, `ActionCard` | Inheritance: shared matching rule in the base class, subclasses own validation and scoring |
| `Color`, `Rank` enums | Every face value, its point value and per-color quantity are defined in one place; the 108-card deck is generated from them |
| `Deck` | Draw pile and discard pile as `ArrayDeque`s; reshuffles the discards when the draw pile runs out |
| `Player` | Hand (`ArrayList`, kept sorted) and running score |
| `Game` | Console turn loop, rule enforcement, active-color tracking, round scoring |
| `UnoEngine` | The same rules with no input or output, advanced one action at a time so a GUI can drive it |
| `uno.gui` | Swing front end: `UnoApp` switches between `WelcomeScreen`, `PassScreen`, `TableScreen` and `ResultScreen`; `CardPainter` draws every card with Java2D, so there are no image files |

## Windowed screens

- **Welcome:** set 2-10 players with the - and + buttons, type names (blank ones become "Player N"; duplicates are rejected), choose 100, 250 or 500 points, then Start game.
- **Pass the computer:** shown between every turn with no hand visible. It recaps the last turn, the top card and the color to match, then waits for the named player to press Reveal hand (Enter works too).
- **Table:** green felt with the draw pile and discard in the middle, a ring around the discard in the active color, and a loop with arrowheads showing the direction of play. Opponents sit across the top in play order with a fan of card backs, their count, their score, and "Plays next" or "UNO!" tags. Your hand runs along the bottom: playable cards are raised with a gold edge, the rest are dimmed. Click a card to play it, or Draw a card (clicking the draw pile works too). After drawing, the button becomes Keep & pass or Pass.
- **Invalid moves** shake the card and explain exactly what would be legal, for example "Red 4 can't go here. You need a Green card, any 8, or a Wild."
- **Wild color picker:** four large color buttons (R, Y, G, B on the keyboard, Esc to keep the Wild).
- **Round results:** winner, points earned, each player's leftover cards and what they were worth, progress toward the target, and Next round (or New game once someone wins).

## Rules implemented

- A card is playable if it is wild, matches the **active color**, or matches the top card's rank. Illegal plays are rejected with an explanation and the player is asked again.
- **Wild-card color tracking:** the color chosen for a wild card is held separately from the card's own color and enforced until the next card changes it.
- Wild Draw 4 may only be played when you hold no card of the active color.
- Skip, Reverse and Draw 2 work as in the standard game (Reverse acts as a Skip with two players). Draw 2 and Wild Draw 4 make the next player draw and lose their turn.
- With no playable card you draw one; if the drawn card is playable you may play it immediately.
- A round starts with a number card (wilds and actions are returned to the deck).
- **Scoring:** the player who empties their hand wins the round and scores the points left in all opponents' hands (number cards = face value, Skip/Reverse/Draw 2 = 20, Wild/Wild Draw 4 = 50). The first player to reach the target wins.
