package com.rentaltracker.repository;

import com.rentaltracker.domain.User;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.RepositoryException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence operations for users.
 *
 * The repository is responsible only for communicating with the database.
 * Business rules and validation belong in the service layer.
 */
public class UserRepository {

    private final Database database;

    public UserRepository(Database database) {
        this.database = database;
    }

    /**
     * Finds a user by their ID.
     *
     * @param id user ID
     * @return the matching user
     * @throws NotFoundException if no user exists with the given ID
     * @throws RepositoryException if the database operation fails
     */
    public User findById(long id) {
        String sql = """
                SELECT id, username, password, created_at
                FROM users
                WHERE id = ?
                """;

        try (PreparedStatement statement =
                     database.connection().prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new NotFoundException(
                            "User with id " + id + " was not found"
                    );
                }

                return mapRow(resultSet);
            }

        } catch (SQLException e) {
            throw new RepositoryException(
                    "Failed to find user with id " + id,
                    e
            );
        }
    }

    /**
     * Finds a user by their username.
     *
     * @param username username to search for
     * @return the matching user
     * @throws NotFoundException if no user exists with the username
     * @throws RepositoryException if the database operation fails
     */
    public User findByUsername(String username) {
        String sql = """
                SELECT id, username, password, created_at
                FROM users
                WHERE username = ?
                """;

        try (PreparedStatement statement =
                     database.connection().prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new NotFoundException(
                            "User with username '" + username + "' was not found"
                    );
                }

                return mapRow(resultSet);
            }

        } catch (SQLException e) {
            throw new RepositoryException(
                    "Failed to find user with username '" + username + "'",
                    e
            );
        }
    }

    /**
     * Retrieves all users ordered by ID.
     *
     * @return list of all users
     * @throws RepositoryException if the database operation fails
     */
    public List<User> findAll() {
        String sql = """
                SELECT id, username, password, created_at
                FROM users
                ORDER BY id
                """;

        List<User> users = new ArrayList<>();

        try (
                PreparedStatement statement =
                        database.connection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }

            return users;

        } catch (SQLException e) {
            throw new RepositoryException(
                    "Failed to retrieve users",
                    e
            );
        }
    }

    /**
     * Creates a new user and returns the persisted record.
     *
     * <p>The RETURNING clause allows SQLite to return the newly created row
     * without requiring a second SELECT query.</p>
     *
     * @param user user to save
     * @return the persisted user
     * @throws RepositoryException if the database operation fails
     */
    public User save(User user) {
        String sql = """
                INSERT INTO users (username, password)
                VALUES (?, ?)
                RETURNING id, username, password, created_at
                """;

        try (PreparedStatement statement =
                     database.connection().prepareStatement(sql)) {

            // Parameterized SQL keeps user input separate from the query.
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new RepositoryException(
                            "Failed to retrieve the created user"
                    );
                }

                return mapRow(resultSet);
            }

        } catch (SQLException e) {
            throw new RepositoryException(
                    "Failed to save user '" + user.getUsername() + "'",
                    e
            );
        }
    }

    /**
     * Maps the current database row to a User domain object.
     */
    private User mapRow(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password"),
                resultSet.getString("created_at")
        );
    }
}