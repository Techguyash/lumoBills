package com.aynlabs.lumoBills.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Customer extends AbstractEntity {

    @NotEmpty
    private String firstName;
    private String lastName;

    @Email
    private String email;

    private String phone;
    
    private String address;
    
    private String city;
    
    public String getFullName() {
        return firstName + (lastName != null ? " " + lastName : "");
    }
}
