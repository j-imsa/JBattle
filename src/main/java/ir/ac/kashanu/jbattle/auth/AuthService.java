package ir.ac.kashanu.jbattle.auth;

import ir.ac.kashanu.jbattle.model.User;
import ir.ac.kashanu.jbattle.persistence.UserRepository;

import java.util.HexFormat;
import java.util.Optional;

/** Signup/login rules layered over the {@link UserRepository}. */
public class AuthService {

    /** Letters, digits and underscore, 3-20 chars. Keeps usernames file-delimiter safe. */
    private static final String USERNAME_PATTERN = "[A-Za-z0-9_]{3,20}";
    private static final int MIN_PASSWORD_LENGTH = 4;

    private final UserRepository repository;

    public AuthService(UserRepository repository) {
        this.repository = repository;
    }

    public AuthResult signup(String username, String password) {
        String name = username == null ? "" : username.trim();
        if (!name.matches(USERNAME_PATTERN)) {
            return new AuthResult(false, "Username must be 3-20 letters, digits or underscore.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return new AuthResult(false, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (repository.exists(name)) {
            return new AuthResult(false, "That username is already taken.");
        }
        byte[] salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);
        repository.save(new User(name, HexFormat.of().formatHex(salt), hash));
        return new AuthResult(true, "Account created. Welcome, " + name + "!");
    }

    public AuthResult login(String username, String password) {
        String name = username == null ? "" : username.trim();
        Optional<User> user = repository.find(name);
        if (user.isEmpty()) {
            return new AuthResult(false, "No account found for that username.");
        }
        byte[] salt = HexFormat.of().parseHex(user.get().saltHex());
        if (!PasswordHasher.verify(password == null ? "" : password, salt, user.get().hashHex())) {
            return new AuthResult(false, "Incorrect password.");
        }
        return new AuthResult(true, "Welcome back, " + name + "!");
    }
}
