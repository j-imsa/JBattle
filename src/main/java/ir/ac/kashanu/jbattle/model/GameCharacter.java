package ir.ac.kashanu.jbattle.model;

/**
 * A single combatant on the battlefield.
 *
 * <p>Mutable state is read by the JavaFX render thread every frame while being
 * written by this character's own virtual thread, so position and vitals are
 * {@code volatile}. Health mutations ({@link #applyDamage(int)}) must happen
 * while the caller holds the game engine's combat lock so that concurrent fights
 * cannot interleave.
 */
public class GameCharacter {

    private final CharacterType type;
    private final String name;

    private volatile double x;
    private volatile double y;
    private volatile int health;
    private volatile boolean alive = true;

    private volatile boolean hasTarget = false;
    private volatile double targetX;
    private volatile double targetY;

    public GameCharacter(CharacterType type, String name, double x, double y) {
        this.type = type;
        this.name = name;
        this.x = x;
        this.y = y;
        this.health = type.getMaxHealth();
    }

    public CharacterType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Faction getFaction() {
        return type.getFaction();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return alive;
    }

    public double healthRatio() {
        return (double) health / type.getMaxHealth();
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    /** Directs this character to walk toward the given top-left position. */
    public void setTarget(double x, double y) {
        this.targetX = x;
        this.targetY = y;
        this.hasTarget = true;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    /**
     * Applies damage and returns the remaining health. The character is marked
     * dead when health reaches zero. Call only while holding the combat lock.
     */
    public int applyDamage(int damage) {
        int remaining = health - damage;
        if (remaining <= 0) {
            remaining = 0;
            alive = false;
        }
        health = remaining;
        return remaining;
    }
}
