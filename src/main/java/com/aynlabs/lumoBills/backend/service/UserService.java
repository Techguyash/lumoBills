package com.aynlabs.lumoBills.backend.service;

import com.aynlabs.lumoBills.backend.entity.User;
import com.aynlabs.lumoBills.backend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void save(User user) {
        // If password is changed (basic check, simplified)
        // ideally we check if hashedPassword is set or we have a transient password field
        // For now, assuming if we save, we might need to re-encode if it's plain text
        // BUT, entity already has hashedPassword field.
        // We usually handle plain password in a DTO or a transient field.
        // For simplicity here, we assume the UI sets 'hashedPassword' with the plain text temporarily? 
        // No, that's bad practice.
        // Let's assume the UI handles calling a specific updatePassword method or we check a transient field.
        
        // This is a placeholder. Real impl needs robust password handling.
        userRepository.save(user);
    }
    
    public void registerUser(User user, String plainPassword) {
        user.setHashedPassword(passwordEncoder.encode(plainPassword));
        userRepository.save(user);
    }
    
    public void delete(User user) {
        userRepository.delete(user);
    }
}
