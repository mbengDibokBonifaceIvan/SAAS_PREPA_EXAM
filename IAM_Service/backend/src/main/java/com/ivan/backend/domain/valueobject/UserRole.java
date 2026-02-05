package com.ivan.backend.domain.valueobject;

public enum UserRole {
    CENTER_OWNER(4),    // Niveau max
    UNIT_MANAGER(3),    // Responsable de sous-centre
    STAFF_MEMBER(2),    // Secrétaire / Enseignant
    CANDIDATE(1);       // Niveau min

    private final int weight;

    UserRole(int weight) {
        this.weight = weight;
    }

    /** * Règle de base : Un utilisateur ne peut créer ou gérer 
     * que des rôles strictement inférieurs au sien.
     */
    public boolean canCreate(UserRole targetRole) {
        return this.weight > targetRole.weight;
    }

    public boolean isHigherThan(UserRole other) {
        return this.weight > other.weight;
    }

    /**
     * Utile pour vérifier si l'utilisateur a le droit de 
     * voir ou d'assigner des unitId (sous-centres).
     */
    public boolean canManageUnits() {
        return this == CENTER_OWNER || this == UNIT_MANAGER;
    }
}