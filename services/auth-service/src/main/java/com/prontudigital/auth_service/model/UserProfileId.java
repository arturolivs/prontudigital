package com.prontudigital.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserProfileId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "profile_id")
    private UUID profileId;
}
