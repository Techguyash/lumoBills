package com.aynlabs.lumoBills.backend.util;

import com.aynlabs.lumoBills.backend.entity.Role;
import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures at least one admin user exists in the system regardless of the active
 * profile.
 * Only runs if the database has no users.
 */
@Component
@RequiredArgsConstructor
public class UserBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("System Administrator");
            admin.setUsername("admin");
            // Default production-ready bootstrap password
            admin.setHashedPassword(passwordEncoder.encode("admin"));
            admin.setRoles(Set.of(Role.ADMIN, Role.USER));

            // Default permissions for the bootstrap admin
            admin.setAccessibleViews(Set.of(
                    "Dashboard", "Stock", "Categories", "Billing",
                    "Invoices", "Purchase", "Customers", "Reports", "Ledger"));

            userRepository.save(admin);

            System.out.println("************************************************************");
            System.out.println("BOOTSTRAP: No users found. Created initial administrator:");
            System.out.println("Username: admin");
            System.out.println("Password: admin");
            System.out.println("IMPORTANT: Please change this password after your first login.");
            System.out.println("************************************************************");
        }
    }
}
