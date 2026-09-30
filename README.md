# Rental Tracker

CLI app for renting out your items, backed by SQLite.

## Requirements

- JDK 21+
- Maven 3.6+

## Running the app

```bash
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

## Workflow

- Branch per change (for example `schema/users`), small focused commits, imperative messages.
- Every PR needs a review from someone else before it is merged.
