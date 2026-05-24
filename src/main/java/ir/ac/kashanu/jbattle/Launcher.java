package ir.ac.kashanu.jbattle;

/**
 * Plain (non-{@code Application}) entry point used when the game is run from a
 * packaged jar, to sidestep the "JavaFX runtime components are missing" error.
 * Delegates to the real JavaFX application.
 */
public class Launcher {
    public static void main(String[] args) {
        JBattleApplication.main(args);
    }
}
