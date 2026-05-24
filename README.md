# JBattle: Goblin Siege

A JavaFX desktop game where your hero battles a band of goblins and a dragon. Every
combatant — your hero, 3 goblins, and 1 dragon — lives on its own Java **virtual thread**,
wandering the arena in real time. The monsters roam randomly; **you steer your hero by
clicking** where you want to go. When fighters from opposing sides meet, they fight or flee,
and every blow, escape, and death is narrated on the battle chronicle to the right.

## Features

- **Accounts** — sign up and log in; credentials are stored locally (salted SHA-256, never
  plaintext).
- **Many plays per user** — each finished battle is saved; your history greets you next time.
- **Live battle** — one hero, three goblins, one dragon, all spawned at random spots and each
  driven by its own virtual thread.
- **Click-to-move hero** — click anywhere in the arena to send your hero there; a marker shows
  the destination. Walk into a monster to attack it.
- **Health bar** — your 100 HP shown as a progress bar that shifts green → orange → red.
- **Story board** — a running chronicle of encounters, hits, flees, and the final result.
- **Responsive** — the arena grows and shrinks with the window.

## Requirements

- **Java 21** (uses virtual threads)
- **Maven** — a wrapper (`./mvnw`) is included but gitignored, so a fresh clone may only have
  system `mvn`. Either works in the commands below.
- JavaFX 21 is pulled in automatically by Maven; no separate SDK install needed.

## Run

```bash
./mvnw clean javafx:run
```

## How to play

1. **Sign up** with a username (3–20 letters/digits/underscore) and a password (4+ chars), or
   **log in** if you already have an account.
2. Click **New Play** to start a battle. Your hero and the monsters appear at random positions.
3. **Click in the arena** to move your hero toward that spot (a glowing marker shows the target).
4. Walk your hero into a monster to **attack** it. Monsters may stand and fight or turn and flee
   depending on how wounded they are.
5. Watch your **health bar** at the top and follow the action on the **battle chronicle**.
6. The battle ends when either you fall (**Defeat**) or every monster is slain (**Victory**).
   The result and your stats are saved to your play history.
7. Press **New Play** to fight again, or **Logout** to switch accounts.

## Data

The "database" is plain files written to a `data/` directory beside where you run the game:

- `data/users.db` — accounts (`username:saltHex:hashHex`)
- `data/plays.db` — one tab-separated line per completed battle

This directory is gitignored. Delete it to reset all accounts and history.

## Build & test

```bash
./mvnw clean compile   # compile only
./mvnw test            # run the test suite (JUnit 5)
```
