package ir.ac.kashanu.jbattle.game;

import ir.ac.kashanu.jbattle.model.Faction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest {

    /**
     * A driver continuously sends the player at the nearest monster (exercising
     * {@link GameEngine#setPlayerTarget}) while every character runs on its own
     * virtual thread, so the battle must reach VICTORY or DEFEAT. This covers the
     * movement, click-to-move targeting, and locked combat resolution end to end.
     */
    @Test
    void battleReachesAResult() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<GameResult> result = new AtomicReference<>();
        AtomicReference<GameStats> stats = new AtomicReference<>();

        GameListener listener = new GameListener() {
            @Override
            public void onNarration(String message) {
                // ignored in the test
            }

            @Override
            public void onGameOver(GameResult r, GameStats s) {
                result.set(r);
                stats.set(s);
                done.countDown();
            }
        };

        GameEngine engine = new GameEngine(listener, "Tester", 400, 400);
        engine.start();

        // Stand in for the human: keep aiming the hero at a living monster.
        Thread hunter = Thread.ofVirtual().start(() -> {
            try {
                while (done.getCount() > 0 && engine.isRunning()) {
                    engine.getCharacters().stream()
                            .filter(c -> c.getFaction() == Faction.MONSTER && c.isAlive())
                            .findFirst()
                            .ifPresent(m -> engine.setPlayerTarget(m.getX() + 24, m.getY() + 24));
                    Thread.sleep(80);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            assertTrue(done.await(20, TimeUnit.SECONDS), "an actively played battle should finish");
        } finally {
            hunter.interrupt();
            engine.stop();
        }

        assertNotNull(result.get());
        GameStats s = stats.get();
        assertNotNull(s);
        assertTrue(s.monstersDefeated() >= 0 && s.monstersDefeated() <= s.totalMonsters());
        assertTrue(s.remainingHealth() >= 0);
        // Exactly one side won: defeat means the player is dead, victory means all monsters are.
        if (result.get() == GameResult.VICTORY) {
            assertTrue(s.monstersDefeated() == s.totalMonsters(), "victory clears every monster");
        } else {
            assertTrue(s.remainingHealth() == 0, "defeat means the player fell");
        }
    }
}
