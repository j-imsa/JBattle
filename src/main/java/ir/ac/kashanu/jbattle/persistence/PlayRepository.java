package ir.ac.kashanu.jbattle.persistence;

import ir.ac.kashanu.jbattle.model.PlayRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only log of completed plays. One tab-separated line per play; usernames
 * are validated elsewhere to contain no whitespace, so the delimiter is safe.
 */
public class PlayRepository {

    private final Path file;

    public PlayRepository(Path file) {
        this.file = file;
    }

    public synchronized void add(PlayRecord record) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String line = String.join("\t",
                    record.username(),
                    Long.toString(record.timestamp()),
                    record.result(),
                    Integer.toString(record.monstersDefeated()),
                    Integer.toString(record.remainingHealth()),
                    Long.toString(record.durationSeconds()));
            Files.writeString(file, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to append play record", e);
        }
    }

    public synchronized List<PlayRecord> findByUser(String username) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<PlayRecord> out = new ArrayList<>();
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split("\t");
                if (p.length >= 6 && p[0].equals(username)) {
                    out.add(new PlayRecord(p[0], Long.parseLong(p[1]), p[2],
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]), Long.parseLong(p[5])));
                }
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read plays file", e);
        }
    }
}
