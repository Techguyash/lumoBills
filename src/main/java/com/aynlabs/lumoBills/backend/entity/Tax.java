package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Tax extends AbstractEntity {
    private String name;
    private BigDecimal percentage;
    private boolean active = true;
}
