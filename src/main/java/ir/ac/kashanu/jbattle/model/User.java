package ir.ac.kashanu.jbattle.model;

/**
 * A registered account. Passwords are never stored; only a per-user random salt
 * and the resulting hash are persisted (both hex-encoded).
 */
public record User(String username, String saltHex, String hashHex) {
}
