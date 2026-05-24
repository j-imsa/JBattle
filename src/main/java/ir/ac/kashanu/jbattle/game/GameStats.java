package ir.ac.kashanu.jbattle.game;

/** Summary handed to the UI (and persisted) when a battle ends. */
public record GameStats(
        int monstersDefeated,
        int remainingHealth,
        long durationSeconds,
        int totalMonsters) {
}
