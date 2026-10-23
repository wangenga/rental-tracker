package com.rentaltracker.service;

import com.rentaltracker.domain.User;
import com.rentaltracker.repository.AppStateRepository;
import com.rentaltracker.repository.UserRepository;

import java.sql.SQLException;
import java.util.Optional;

public class CurrentUserService {

    public static final String KEY_CURRENT_USER_ID = "current_user_id";

    private final AppStateRepository appState;
    private final UserRepository userRepository;

    public CurrentUserService(AppStateRepository appState, UserRepository userRepository) {
        this.appState = appState;
        this.userRepository = userRepository;
    }

    /** Returns the identified user, or empty if no one has claimed yet. Never prompts. */
    public Optional<User> loadCurrentUser() throws SQLException {
        Optional<Integer> userId = appState.getInt(KEY_CURRENT_USER_ID);
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        Optional<User> user = Optional.ofNullable(userRepository.findById(userId.get()));
        if (user.isEmpty()) {
            throw new IllegalStateException(
                    "app_state points at missing user_id=" + userId.get()
                            + " — DB was likely reseeded. Delete that app_state row to re-prompt.");
        }
        return user;
    }

    /** First-launch claim: find-or-create the username, then persist the pointer. */
    public User claim(String username) throws SQLException {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        Optional<User> existing = userRepository.findByUsername(trimmed);
        User user = existing.orElseGet(() -> userRepository.save(new User(0, trimmed, null, null)));


        appState.setInt(KEY_CURRENT_USER_ID, (int) user.getId());
        return user;
    }
}