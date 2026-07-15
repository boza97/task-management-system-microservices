package com.bozidar.tms.project_service.project;

import com.bozidar.tms.project_service.project.membership.ProjectMembership;
import com.bozidar.tms.project_service.project.membership.ProjectRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_key", nullable = false, unique = true, length = 10)
    private String key;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<ProjectMembership> memberships = new HashSet<>();

    public Project(String key, String name, String description, UUID ownerId) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isOwner(UUID userId) {
        return ownerId.equals(userId);
    }

    public boolean hasRole(UUID userId, ProjectRole role) {
        return memberships.stream()
                          .anyMatch(m ->
                                            m.getUserId().equals(userId) &&
                                            m.getRole() == role
                          );
    }

    public boolean canBeUpdatedBy(UUID userId) {
        return isOwner(userId) || hasRole(userId, ProjectRole.PROJECT_MANAGER);
    }
}
