# Rental Tracker

CLI app for renting out your items, backed by SQLite.

## Requirements

- JDK 21+
- Maven 3.6+

## Running the app

```bash
mvn clean package
mvn -q exec:java
```

Compiles if needed and starts the app with the default database `data/local.db`. This is your personal scratch database. It is created on first run and ignored by git.

```bash
mvn -q exec:java -Dexec.args="data/other.db"
```

Everything after `-Dexec.args=` is passed to `main`. The first argument is the database path. Use it when you want:

- a clean database for testing (a new file name gives you an empty one)
- the seeded demo data: `-Dexec.args="data/rental-tracker.db"` (available once the seeded file is committed)

The database file and its tables are created on first run. Make sure the `data/` folder exists (`mkdir -p data`).

## Tests and coverage

- `mvn clean test` - run the tests and write the coverage report to `docs/coverage/`
- `mvn verify -Pcoverage-check` - fail if line coverage is below 100%

## Layers

transport -> service -> repository -> infrastructure, with domain shared by all.


### Transport Layer Layout

```

transport/
├── Prompter            all console input/output goes through this
├── PaginatedList       pure paging logic: no input, no output
├── MainMenu            the loop: show menu, run a screen, repeat until exit
└── screens/
    ├── StartupScreen          first launch: ask for a username or load the account
    ├── ListItemScreen         3 prompts: name, description, cost per day
    ├── InventoryScreen        paged list of all your items, then detail with delist/relist
    ├── RecordRentalScreen     paged list of available items, then detail, then renter and days
    └── ConfirmReturnScreen    paged list of active rentals, then detail, then confirm
```

#### What each piece owns
- Prompter is the only class that touches System.in and System.out. It reads a line, reads an integer (re-prompting on bad input), asks for a non-blank string, and prints. Everything else goes through it, so tests can feed it scripted input.
- PaginatedList takes a list and a page size of 5. It answers “which rows are on this page”, “is there a next page”, and “is there a previous page”. It has no console code, so you can test it exhaustively with plain unit tests.
- Screens hold a service (never a repository) and a Prompter. Each has one run() method. A screen shows what it needs, handles the user’s choice, and returns to its caller.
- MainMenu owns the loop, and it’s the only place that catches exceptions from screens.

## Workflow

- Branch per change (for example `schema/users`), small focused commits, imperative messages.
- Every PR needs a review from someone else before it is merged.

