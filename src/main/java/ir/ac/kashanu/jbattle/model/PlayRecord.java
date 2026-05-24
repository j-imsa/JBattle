package ir.ac.kashanu.jbattle.model;

/** One completed battle belonging to a user. */
public record PlayRecord(
        String username,
        long timestamp,
        String result,
        int monstersDefeated,
        int remainingHealth,
        long durationSeconds) {
}
