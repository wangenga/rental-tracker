package com.rentaltracker.domain;

public class User {

    private final long id;
    private final String username;
    private final String password;
    private final String createdAt;

    public User(
            long id,
            String username,
            String password,
            String createdAt
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}