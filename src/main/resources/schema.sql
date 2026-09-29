-- Users of the rental tracker
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Items listed by users for rental
CREATE TABLE listed_items (
    item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id INTEGER NOT NULL,
    item_name TEXT NOT NULL,
    description TEXT NOT NULL,
    cost_per_day REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'available'
        CHECK (status IN ('available', 'rented', 'unlisted')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (owner_id)
        REFERENCES users(id)
);

-- Rental transactions between users and listed items
CREATE TABLE rentals (
    rental_id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    renter_id INTEGER NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    returned_at TEXT,
    status TEXT NOT NULL DEFAULT 'active',

    FOREIGN KEY (item_id)
        REFERENCES listed_items(item_id),

    FOREIGN KEY (renter_id)
        REFERENCES users(id)
);