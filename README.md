# Rental Tracker

CLI app for renting out your items, backed by SQLite.

## Requirements
- JDK 21+
- Maven 3.6+

## Commands
- `mvn clean test` - run tests and write the coverage report to `docs/coverage/`
- `mvn -q exec:java` - run the app
- `mvn verify -Pcoverage-check` - fail if line coverage is below 100%

## Layers
transport -> service -> repository -> infrastructure, with domain shared by all.
