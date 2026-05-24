package ir.ac.kashanu.jbattle.model;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The kinds of combatants in the game. Each type carries its faction, base stats
 * and the classpath location of its sprite. Resource paths are absolute so they
 * resolve the same regardless of which package loads them.
 */
public enum CharacterType {
    PLAYER("Hero", Faction.PLAYER, 100, 8, 20, "/ir/ac/kashanu/jbattle/image/user.png"),
    GOBLIN("Goblin", Faction.MONSTER, 40, 2, 6, "/ir/ac/kashanu/jbattle/image/goblin.png"),
    DRAGON("Dragon", Faction.MONSTER, 120, 12, 24, "/ir/ac/kashanu/jbattle/image/dragon.png");

    private final String displayName;
    private final Faction faction;
    private final int maxHealth;
    private final int minAttack;
    private final int maxAttack;
    private final String imageResource;

    CharacterType(String displayName, Faction faction, int maxHealth,
                  int minAttack, int maxAttack, String imageResource) {
        this.displayName = displayName;
        this.faction = faction;
        this.maxHealth = maxHealth;
        this.minAttack = minAttack;
        this.maxAttack = maxAttack;
        this.imageResource = imageResource;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Faction getFaction() {
        return faction;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public String getImageResource() {
        return imageResource;
    }

    /** Rolls a random hit within this type's attack range. */
    public int rollDamage() {
        return ThreadLocalRandom.current().nextInt(minAttack, maxAttack + 1);
    }
}
