package ir.ac.kashanu.jbattle.game;

import ir.ac.kashanu.jbattle.model.CharacterType;
import ir.ac.kashanu.jbattle.model.Faction;
import ir.ac.kashanu.jbattle.model.GameCharacter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Drives one battle. Each character runs on its own virtual thread, wandering
 * randomly and, on bumping into a member of the opposing faction, either fighting
 * or fleeing.
 */
public class GameEngine {

    /** Sprite edge length in pixels; used to keep characters inside the arena. */
    private static final double SPRITE = 48;
    /** Center-to-center distance at which two characters trigger an encounter. */
    private static final double ENCOUNTER_RADIUS = 54;
    private static final double STEP = 26;

    private final GameListener listener;
    private final List<GameCharacter> characters;
    private final GameCharacter player;
    private final int totalMonsters;

    private volatile double boundsWidth;
    private volatile double boundsHeight;

    private final ReentrantLock combatLock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger monstersDefeated = new AtomicInteger();
    private final List<Thread> threads = new ArrayList<>();
    private volatile long startNanos;

    public GameEngine(GameListener listener, String playerName, double width, double height) {
        this.listener = listener;
        this.boundsWidth = width;
        this.boundsHeight = height;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<GameCharacter> roster = new ArrayList<>();
        this.player = new GameCharacter(CharacterType.PLAYER, playerName, randX(rnd), randY(rnd));
        roster.add(player);
        for (int i = 1; i <= 3; i++) {
            roster.add(new GameCharacter(CharacterType.GOBLIN, "Goblin " + i, randX(rnd), randY(rnd)));
        }
        roster.add(new GameCharacter(CharacterType.DRAGON, "Dragon", randX(rnd), randY(rnd)));

        this.characters = List.copyOf(roster);
        this.totalMonsters = (int) characters.stream()
                .filter(c -> c.getFaction() == Faction.MONSTER)
                .count();
    }

    /** Spawns one virtual thread per character and begins the battle. */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        startNanos = System.nanoTime();
        for (GameCharacter c : characters) {
            threads.add(Thread.ofVirtual().name("char-" + c.getName()).unstarted(() -> runCharacter(c)));
        }
        threads.forEach(Thread::start);
        narrate("⚔ The battle begins! " + player.getName()
                + " faces " + totalMonsters + " monsters.");
    }

    /** Halts the battle and wakes any sleeping character threads. */
    public void stop() {
        running.set(false);
        threads.forEach(Thread::interrupt);
    }

    /** Updates the playable area after a window/arena resize. */
    public void setBounds(double width, double height) {
        this.boundsWidth = width;
        this.boundsHeight = height;
    }

    /** Sends the player walking toward an arena point (aligned to the sprite center). */
    public void setPlayerTarget(double centerX, double centerY) {
        if (!running.get() || !player.isAlive()) {
            return;
        }
        double tx = clamp(centerX - SPRITE / 2, 0, maxX());
        double ty = clamp(centerY - SPRITE / 2, 0, maxY());
        player.setTarget(tx, ty);
    }

    public List<GameCharacter> getCharacters() {
        return characters;
    }

    public GameCharacter getPlayer() {
        return player;
    }

    public boolean isRunning() {
        return running.get();
    }

    // --- character life cycle -------------------------------------------------

    private void runCharacter(GameCharacter c) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        try {
            while (running.get() && c.isAlive()) {
                Thread.sleep(pauseFor(c, rnd));
                if (!running.get() || !c.isAlive()) {
                    break;
                }
                advance(c);
                handleEncounter(c);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** The player reacts quickly to clicks; monsters amble. */
    private int pauseFor(GameCharacter c, ThreadLocalRandom rnd) {
        return c.getFaction() == Faction.PLAYER ? rnd.nextInt(120, 220) : rnd.nextInt(250, 700);
    }

    private void advance(GameCharacter c) {
        if (c.hasTarget()) {
            moveTowardTarget(c);
        } else if (c.getFaction() == Faction.MONSTER) {
            randomStep(c);
        }
        // A player with no target waits where it is for the next click.
    }

    private void randomStep(GameCharacter c) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double nx = c.getX() + rnd.nextDouble(-STEP, STEP);
        double ny = c.getY() + rnd.nextDouble(-STEP, STEP);
        c.setPosition(clamp(nx, 0, maxX()), clamp(ny, 0, maxY()));
    }

    private void moveTowardTarget(GameCharacter c) {
        double dx = c.getTargetX() - c.getX();
        double dy = c.getTargetY() - c.getY();
        double dist = Math.hypot(dx, dy);
        if (dist <= STEP) {
            c.setPosition(clamp(c.getTargetX(), 0, maxX()), clamp(c.getTargetY(), 0, maxY()));
            c.clearTarget();
        } else {
            double nx = c.getX() + dx / dist * STEP;
            double ny = c.getY() + dy / dist * STEP;
            c.setPosition(clamp(nx, 0, maxX()), clamp(ny, 0, maxY()));
        }
    }

    // --- combat (all under combatLock) ---------------------------------------

    private void handleEncounter(GameCharacter mover) {
        combatLock.lock();
        try {
            if (!running.get() || !mover.isAlive()) {
                return;
            }
            GameCharacter opponent = null;
            for (GameCharacter other : characters) {
                if (other == mover || !other.isAlive() || other.getFaction() == mover.getFaction()) {
                    continue;
                }
                if (distance(mover, other) <= ENCOUNTER_RADIUS) {
                    opponent = other;
                    break;
                }
            }
            if (opponent != null) {
                resolveEncounter(mover, opponent);
                checkGameOver();
            }
        } finally {
            combatLock.unlock();
        }
    }

    private void resolveEncounter(GameCharacter a, GameCharacter b) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        boolean aFights = decideToFight(a, rnd);
        boolean bFights = decideToFight(b, rnd);

        narrate("⚔ " + a.getName() + " meets " + b.getName() + "!");

        if (aFights && bFights) {
            strike(a, b);
            if (b.isAlive()) {
                strike(b, a);
            }
        } else if (aFights) {
            narrate(b.getName() + " turns to flee.");
            strike(a, b); // a parting blow as the coward runs
            if (b.isAlive()) {
                flee(b, a);
                narrate(b.getName() + " slips away.");
            }
        } else if (bFights) {
            narrate(a.getName() + " turns to flee.");
            strike(b, a);
            if (a.isAlive()) {
                flee(a, b);
                narrate(a.getName() + " slips away.");
            }
        } else {
            narrate(a.getName() + " and " + b.getName() + " warily circle and back off.");
            flee(a, b);
            flee(b, a);
        }
    }

    /**
     * The player always engages — the human chose to walk into the foe. Monsters
     * are bolder when healthy and more likely to run when wounded.
     */
    private boolean decideToFight(GameCharacter c, ThreadLocalRandom rnd) {
        if (c.getFaction() == Faction.PLAYER) {
            return true;
        }
        return rnd.nextDouble() < 0.25 + 0.6 * Math.max(0, c.healthRatio());
    }

    private void strike(GameCharacter attacker, GameCharacter defender) {
        int damage = attacker.getType().rollDamage();
        int remaining = defender.applyDamage(damage);
        if (defender.isAlive()) {
            narrate(attacker.getName() + " hits " + defender.getName()
                    + " for " + damage + " (" + remaining + " hp left).");
        } else {
            narrate(attacker.getName() + " strikes " + defender.getName()
                    + " for " + damage + " — a killing blow!");
            onDeath(defender);
        }
    }

    private void onDeath(GameCharacter c) {
        if (c.getFaction() == Faction.MONSTER) {
            monstersDefeated.incrementAndGet();
        }
        narrate("💀 " + c.getName() + " has fallen!");
    }

    /** Pushes {@code mover} away from {@code from} so the encounter clears. */
    private void flee(GameCharacter mover, GameCharacter from) {
        double dx = mover.getX() - from.getX();
        double dy = mover.getY() - from.getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            dx = 1;
            dy = 0;
            len = 1;
        }
        double dist = ENCOUNTER_RADIUS + 30;
        double nx = mover.getX() + dx / len * dist;
        double ny = mover.getY() + dy / len * dist;
        mover.setPosition(clamp(nx, 0, maxX()), clamp(ny, 0, maxY()));
    }

    private void checkGameOver() {
        if (!running.get()) {
            return;
        }
        if (!player.isAlive()) {
            finish(GameResult.DEFEAT);
            return;
        }
        boolean monstersRemain = characters.stream()
                .anyMatch(c -> c.getFaction() == Faction.MONSTER && c.isAlive());
        if (!monstersRemain) {
            finish(GameResult.VICTORY);
        }
    }

    private void finish(GameResult result) {
        if (running.compareAndSet(true, false)) {
            long durationSeconds = (System.nanoTime() - startNanos) / 1_000_000_000L;
            GameStats stats = new GameStats(
                    monstersDefeated.get(),
                    Math.max(0, player.getHealth()),
                    durationSeconds,
                    totalMonsters);
            listener.onGameOver(result, stats);
        }
    }

    // --- helpers --------------------------------------------------------------

    private void narrate(String message) {
        listener.onNarration(message);
    }

    private double maxX() {
        return Math.max(0, boundsWidth - SPRITE);
    }

    private double maxY() {
        return Math.max(0, boundsHeight - SPRITE);
    }

    private double randX(ThreadLocalRandom rnd) {
        return rnd.nextDouble(0, Math.max(1, boundsWidth - SPRITE));
    }

    private double randY(ThreadLocalRandom rnd) {
        return rnd.nextDouble(0, Math.max(1, boundsHeight - SPRITE));
    }

    private static double distance(GameCharacter a, GameCharacter b) {
        double dx = (a.getX() + SPRITE / 2) - (b.getX() + SPRITE / 2);
        double dy = (a.getY() + SPRITE / 2) - (b.getY() + SPRITE / 2);
        return Math.hypot(dx, dy);
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
