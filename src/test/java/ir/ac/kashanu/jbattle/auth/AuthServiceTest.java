package ir.ac.kashanu.jbattle.auth;

import ir.ac.kashanu.jbattle.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void signupThenLoginRoundTrip(@TempDir Path dir) {
        AuthService auth = new AuthService(new UserRepository(dir.resolve("users.db")));

        assertTrue(auth.signup("hero", "secret").success());
        assertFalse(auth.signup("hero", "secret").success(), "duplicate username rejected");
        assertTrue(auth.login("hero", "secret").success());
        assertFalse(auth.login("hero", "wrong").success(), "wrong password rejected");
        assertFalse(auth.login("ghost", "secret").success(), "unknown user rejected");
    }

    @Test
    void rejectsInvalidInput(@TempDir Path dir) {
        AuthService auth = new AuthService(new UserRepository(dir.resolve("users.db")));

        assertFalse(auth.signup("ab", "secret").success(), "username too short");
        assertFalse(auth.signup("bad name", "secret").success(), "whitespace not allowed");
        assertFalse(auth.signup("hero", "x").success(), "password too short");
    }

    @Test
    void usersPersistAcrossRepositoryReload(@TempDir Path dir) {
        Path file = dir.resolve("users.db");
        new AuthService(new UserRepository(file)).signup("hero", "secret");

        // A fresh repository must read the previously saved account from disk.
        AuthService reopened = new AuthService(new UserRepository(file));
        assertTrue(reopened.login("hero", "secret").success());
    }
}
