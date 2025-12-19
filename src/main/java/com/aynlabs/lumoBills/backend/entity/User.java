package com.aynlabs.lumoBills.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "application_user")
@Getter
@Setter
public class User extends AbstractEntity {

    private String username;
    private String name;

    @JsonIgnore
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Role> roles;

    private boolean active = true;

    // For page-based access control, we might add specific permissions later
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> accessibleViews;
}
