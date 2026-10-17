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

    // ---------- the owner (first launch) ----------

    /** Empty means first launch: nobody has an account yet. */
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

    // ---------- other users (renters) ----------

    /** Creates a user with no password. Rejects a username that is already taken. */
    public User createUser(String username) {
        String normalized = normalizeUsername(username);
        validateUsername(normalized);

        if (userRepository.findByUsername(normalized).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + normalized);
        }
        return userRepository.save(new User(0, normalized, null, null));
    }

    /** Finds the user, or creates one with no password. Used when recording a rental. */
    public User findOrCreate(String username) {
        String normalized = normalizeUsername(username);
        validateUsername(normalized);

        return userRepository.findByUsername(normalized)
                .orElseGet(() -> userRepository.save(new User(0, normalized, null, null)));
    }

    // ---------- reads ----------

    public User getUser(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("User ID must be greater than zero");
        }
        return userRepository.findById(id);
    }

    public User getUserByUsername(String username) {
        String normalized = normalizeUsername(username);
        validateUsername(normalized);

        return userRepository.findByUsername(normalized)
                .orElseThrow(() -> new NotFoundException("User not found: " + normalized));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ---------- helpers ----------

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        return username.trim().toLowerCase();
    }

    private void validateUsername(String username) {
        if (username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be at least " + MIN_USERNAME_LENGTH + " characters");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must not exceed " + MAX_USERNAME_LENGTH + " characters");
        }
        if (!username.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Username may only contain lowercase letters, numbers, and underscores");
        }
    }
}