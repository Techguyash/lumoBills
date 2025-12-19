package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemSetting extends AbstractEntity {

    @Column(unique = true)
    private String settingKey;

    @Column(length = 2000)
    private String settingValue;
}
