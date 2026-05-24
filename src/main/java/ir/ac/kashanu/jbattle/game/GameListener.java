package ir.ac.kashanu.jbattle.game;

/**
 * Engine callbacks. Invoked from character virtual threads (and while the combat
 * lock is held), so implementations must marshal any UI work onto the JavaFX
 * thread and return quickly.
 */
public interface GameListener {

    /** A line of battle narration for the story board. */
    void onNarration(String message);

    /** The battle has ended; no further narration follows. */
    void onGameOver(GameResult result, GameStats stats);
}
