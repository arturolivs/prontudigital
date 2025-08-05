package com.prontudigital.auth_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false,  unique = true, length = 20)
    private String name;

    @Column(length = 200)
    private String description;

    @Builder.Default
    @ManyToMany(mappedBy = "profiles", fetch = FetchType.EAGER)
    private Set<User> users = new HashSet<>();
}