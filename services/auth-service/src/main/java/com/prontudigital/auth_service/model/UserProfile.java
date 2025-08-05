package com.prontudigital.auth_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @EmbeddedId
    private UserProfileId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("profileId")
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = Instant.now();
    }
}