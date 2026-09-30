-- Users of the rental tracker
CREATE TABLE IF NOT EXISTS  users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE
        CHECK (length(trim(username)) > 0),
    password TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M', 'now', 'localtime'))
);

-- Items listed by users for rental
CREATE TABLE IF NOT EXISTS  listed_items (
    item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id INTEGER NOT NULL,
    item_name TEXT NOT NULL
        CHECK (length(trim(item_name)) > 0),
    description TEXT NOT NULL,
    cost_per_day INTEGER NOT NULL
        CHECK (cost_per_day > 0),
    status TEXT NOT NULL DEFAULT 'available'
        CHECK (status IN ('available', 'rented', 'unlisted')),
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M', 'now', 'localtime')),

    FOREIGN KEY (owner_id)
        REFERENCES users(id)
);

-- Rental transactions between users and listed items
CREATE TABLE IF NOT EXISTS  rentals (
    rental_id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    renter_id INTEGER NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL
        CHECK(end_time > start_time),
    returned_at TEXT,
    status TEXT NOT NULL DEFAULT 'active'
        CHECK(status IN('active', 'closed')),

    -- active rentals have no return time while closed always have one
    CHECK(
        (status = 'active' AND returned_at IS NULL)
        OR (status = 'closed' AND returned_at IS NOT NULL)
    ),

    FOREIGN KEY (item_id)
        REFERENCES listed_items(item_id),

    FOREIGN KEY (renter_id)
        REFERENCES users(id)
);

--only one active rental per item
CREATE UNIQUE INDEX IF NOT EXISTS index_one_active_rental_per_item
    ON rentals(item_id) WHERE status = 'active';