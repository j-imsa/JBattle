package ir.ac.kashanu.jbattle.auth;

/** Outcome of a signup or login attempt, with a user-facing message. */
public record AuthResult(boolean success, String message) {
}
