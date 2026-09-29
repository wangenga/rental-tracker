# Entity relationship diagram

```mermaid
erDiagram
    users ||--o{ listed_items : "owns (owner_id)"
    users ||--o{ rentals : "rents (renter_id)"
    listed_items ||--o{ rentals : "rented in (item_id)"

    users {
        INTEGER id PK "AUTOINCREMENT"
        TEXT username UK "NOT NULL, CHECK not blank"
        TEXT password "nullable, unused in project 1"
        TEXT created_at "NOT NULL, default now"
    }

    listed_items {
        INTEGER item_id PK "AUTOINCREMENT"
        INTEGER owner_id FK "NOT NULL, references users.id"
        TEXT item_name "NOT NULL, CHECK not blank"
        TEXT description "NOT NULL"
        INTEGER cost_per_day "NOT NULL, CHECK >= 0"
        TEXT status "NOT NULL, CHECK available/rented/unlisted"
        TEXT created_at "NOT NULL, default now"
    }

    rentals {
        INTEGER rental_id PK "AUTOINCREMENT"
        INTEGER item_id FK "NOT NULL, references listed_items.item_id"
        INTEGER renter_id FK "NOT NULL, references users.id"
        TEXT start_time "NOT NULL"
        TEXT end_time "NOT NULL, CHECK end_time > start_time"
        TEXT returned_at "NULL until returned"
        TEXT status "NOT NULL, CHECK active/closed"
    }
```

## Constraints not shown in the boxes
- `rentals`: `CHECK` ties `status` to `returned_at` (active means NULL, closed means NOT NULL).
- `rentals`: a partial unique index allows only one active rental per item.
- Indexes: `listed_items(owner_id, status)` and `rentals(item_id)`.