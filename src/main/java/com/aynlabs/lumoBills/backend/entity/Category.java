package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Category extends AbstractEntity {

    @NotEmpty
    private String name;

    @Override
    public String toString() {
        return name;
    }
}
