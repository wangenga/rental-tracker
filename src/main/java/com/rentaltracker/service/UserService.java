package com.rentaltracker.service;

import com.rentaltracker.domain.User;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.repository.exception.NotFoundException;

import java.util.List;

public class UserService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String password) {

        String normalizedUsername = normalizeUsername(username);

        validateUsername(normalizedUsername);
        validatePassword(password);

        try {
            userRepository.findByUsername(normalizedUsername);

            throw new IllegalArgumentException(
                    "Username already exists: " + normalizedUsername
            );

        } catch (NotFoundException exception) {
            // Username does not exist, so creation can continue.
        }

        User user = new User(
                0,
                normalizedUsername,
                password,
                null
        );

        return userRepository.save(user);
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

    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty"
            );
        }
    }
}