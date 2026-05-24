package ir.ac.kashanu.jbattle.persistence;

import ir.ac.kashanu.jbattle.model.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * File-backed user store. The whole file is read into memory on construction and
 * rewritten on every {@link #save(User)}; fine for the handful of accounts a
 * desktop game has. Lines are {@code username:saltHex:hashHex}.
 */
public class UserRepository {

    private final Path file;
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();

    public UserRepository(Path file) {
        this.file = file;
        load();
    }

    private synchronized void load() {
        users.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    users.put(parts[0], new User(parts[0], parts[1], parts[2]));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read users file", e);
        }
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public Optional<User> find(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public synchronized void save(User user) {
        users.put(user.username(), user);
        persist();
    }

    private void persist() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            List<String> lines = users.values().stream()
                    .map(u -> u.username() + ":" + u.saltHex() + ":" + u.hashHex())
                    .toList();
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write users file", e);
        }
    }
}
