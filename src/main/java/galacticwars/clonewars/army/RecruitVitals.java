package galacticwars.clonewars.army;

public record RecruitVitals(
        int currentHealth,
        int maxHealth,
        int morale,
        int hunger,
        int unpaidTicks
) {
    private static final int CRITICAL_HEALTH_PERCENT = 25;
    private static final int BROKEN_MORALE = 20;
    private static final int LOW_MORALE = 40;
    private static final int EXHAUSTED_HUNGER = 10;
    private static final int UPKEEP_OVERDUE_TICKS = 24000;

    public RecruitVitals {
        if (maxHealth < 1) {
            throw new IllegalArgumentException("maxHealth must be at least 1");
        }
        if (currentHealth < 0 || currentHealth > maxHealth) {
            throw new IllegalArgumentException("currentHealth must be between 0 and maxHealth");
        }
        if (morale < 0 || morale > 100) {
            throw new IllegalArgumentException("morale must be between 0 and 100");
        }
        if (hunger < 0 || hunger > 100) {
            throw new IllegalArgumentException("hunger must be between 0 and 100");
        }
        if (unpaidTicks < 0) {
            throw new IllegalArgumentException("unpaidTicks cannot be negative");
        }
    }

    public int healthPercent() {
        return (int) ((long) currentHealth * 100 / maxHealth);
    }

    public boolean isCriticalHealth() {
        return healthPercent() <= CRITICAL_HEALTH_PERCENT;
    }

    public boolean isBrokenMorale() {
        return morale < BROKEN_MORALE;
    }

    public boolean isLowMorale() {
        return morale < LOW_MORALE;
    }

    public boolean isExhausted() {
        return hunger <= EXHAUSTED_HUNGER;
    }

    public boolean isUpkeepOverdue() {
        return unpaidTicks >= UPKEEP_OVERDUE_TICKS;
    }
}
