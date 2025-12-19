package com.aynlabs.lumoBills.backend.repository;

import com.aynlabs.lumoBills.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
