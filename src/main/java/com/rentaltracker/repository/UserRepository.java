package com.rentaltracker.repository;

import com.rentaltracker.domain.User;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.MappingException;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.RepositoryException;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles persistence operations for users.
 *
 * The repository only talks to the database. Business rules and validation
 * belong in the service layer.
 */
public class UserRepository {

    private static final String COLUMNS = "id, username, password, created_at";

    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    /**
     * @throws NotFoundException   if no user has this id
     * @throws RepositoryException if the database operation fails
     */
    public User findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE id = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new NotFoundException("User with id " + id + " was not found");
                }
                return mapRow(resultSet);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find user by id", e);
        }
    }

    /**
     * @return the matching user, or empty if nobody has this username
     * @throws RepositoryException if the database operation fails
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find user by username", e);
        }
    }

    /**
     * The lowest-id user. By convention this is the owner.
     *
     * @return the first user, or empty if there are no users yet
     */
    public Optional<User> findFirst() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY id LIMIT 1";

        try (PreparedStatement statement = database.connection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find first user", e);
        }
    }

    /** All users ordered by id. */
    public List<User> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (PreparedStatement statement = database.connection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
            return users;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find all users", e);
        }
    }

    /**
     * Creates a user and returns the stored row.
     * RETURNING lets SQLite hand back the new row without a second query.
     *
     * @throws RepositoryException if the database operation fails
     *         (a duplicate username surfaces as a UNIQUE constraint violation)
     */
    public User save(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?) "
                   + "RETURNING " + COLUMNS;

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RepositoryException("Failed to retrieve the created user");
                }
                return mapRow(resultSet);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("save user", e);
        }
    }

    /** Maps the current row to a User. A row that cannot be read is a MappingException. */
    private User mapRow(ResultSet resultSet) {
        try {
            return new User(
                    resultSet.getLong("id"),
                    resultSet.getString("username"),
                    resultSet.getString("password"),
                    resultSet.getString("created_at"));
        } catch (SQLException e) {
            throw new MappingException("Cannot map user row", e);
        }
    }
}