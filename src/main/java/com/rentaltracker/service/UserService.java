package com.rentaltracker.service;

import com.rentaltracker.domain.User;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.service.exception.BusinessRuleException;

import java.util.List;
import java.util.Optional;

public class UserService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Empty means first launch. */
    public Optional<User> findOwner() {
        return userRepository.findFirst();
    }

    /** Creates the owner account. Only allowed while no users exist. */
    public User createOwner(String username) {
        if (userRepository.findFirst().isPresent()) {
            throw new BusinessRuleException("An owner account already exists");
        }
        String normalized = normalizeUsername(username);
        validateUsername(normalized);
        return userRepository.save(new User(0, normalized, null, null));
    }


    public User getUser(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be greater than zero"
            );
        }

        return userRepository.findById(id);
    }

    public User getUserByUsername(String username) {

        String normalizedUsername = normalizeUsername(username);

        validateUsername(normalizedUsername);

        return userRepository.findByUsername(normalizedUsername);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private String normalizeUsername(String username) {

        if (username == null) {
            throw new IllegalArgumentException(
                    "Username cannot be null"
            );
        }

        return username.trim().toLowerCase();
    }

    private void validateUsername(String username) {

        if (username.isEmpty()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty"
            );
        }

        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be at least "
                            + MIN_USERNAME_LENGTH
                            + " characters"
            );
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must not exceed "
                            + MAX_USERNAME_LENGTH
                            + " characters"
            );
        }

      
        if (!username.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Username may only contain lowercase letters, "
                            + "numbers, and underscores"
            );
        }
    }
    
}