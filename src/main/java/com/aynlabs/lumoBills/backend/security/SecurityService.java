package com.aynlabs.lumoBills.backend.security;

import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final AuthenticationContext authenticationContext;
    private final UserRepository userRepository;

    public User getAuthenticatedUser() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(userDetails -> userRepository.findByUsername(userDetails.getUsername()))
                .orElse(null);
    }

    public void logout() {
        authenticationContext.logout();
    }
}
