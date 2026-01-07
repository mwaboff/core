package com.aboff.core.model.enums;

public enum Role {
    USER(1),
    MODERATOR(2),
    ADMIN(3),
    OWNER(4);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean canPerformActionOn(Role targetRole) {
        return this.level > targetRole.getLevel();
    }
}
