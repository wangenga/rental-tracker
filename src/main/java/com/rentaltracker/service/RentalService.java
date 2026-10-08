package com.rentaltracker.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.RentalDomain;
// TODO(users): import com.rentaltracker.domain.UserDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.domain.enums.RentalStatus;
import com.rentaltracker.repository.ItemRepository;
import com.rentaltracker.repository.RentalRepository;
import com.rentaltracker.repository.Transactor;
// TODO(users): import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;
import com.rentaltracker.service.exception.BusinessRuleException;

public class RentalService {

    // Team decision: the spec sets no limit, this just prevents absurd dates
    private static final int MAX_DAYS = 365;

    private final ItemRepository items;
    private final RentalRepository rentals;
    // TODO(users): private final UserRepository users;
    private final Transactor tx;
    private final Clock clock;

    // TODO(users): add UserRepository users back as the third parameter
    public RentalService(ItemRepository items, RentalRepository rentals,
                         Transactor tx, Clock clock) {
        this.items = items;
        this.rentals = rentals;
        // TODO(users): this.users = users;
        this.tx = tx;
        this.clock = clock;
    }

    // ---------- reads ----------

    /** The owner's items that can be rented right now (status available). */
    public List<ItemDomain> getRentableItems(int ownerId) {
        return db("list items", items::findAll).stream()
                .filter(i -> i.getOwnerId() == ownerId && i.getStatus() == ItemStatus.available)
                .toList();
    }

    /** Active rentals on the owner's items, soonest due date first. */
    public List<RentalDomain> getActiveRentals(int ownerId) {
        return rentals.findActiveByOwner(ownerId);
    }

    // ---------- record a rental ----------

    public RentalDomain recordRental(int itemId, String renterName, int days) {
        if (renterName == null || renterName.isBlank()) {
            throw new IllegalArgumentException("Renter name cannot be blank");
        }
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("Days must be between 1 and " + MAX_DAYS);
        }
        String username = renterName.trim();

        return tx.inTransaction(() -> {
            ItemDomain item = requireItem(itemId);

            if (item.getStatus() == ItemStatus.rented) {
                throw new BusinessRuleException("Item is already rented: " + item.getItemName());
            }
            if (item.getStatus() == ItemStatus.unlisted) {
                throw new BusinessRuleException("Item is unlisted and cannot be rented: " + item.getItemName());
            }

            long renterId = findOrCreateRenterId(username);

            LocalDateTime start = now();
            LocalDateTime end = start.plusDays(days);

            long rentalId = rentals.insert(
                    new RentalDomain(0, itemId, renterId, start, end, null, RentalStatus.ACTIVE));

            item.setStatus(ItemStatus.rented);
            saveItem(item);

            return rentals.findById(rentalId);
        });
    }

    // ---------- confirm a return ----------

    /** Closes the rental. The item goes back to available, unless it was delisted while out. */
    public ItemDomain confirmReturn(long rentalId) {
        return tx.inTransaction(() -> {
            RentalDomain rental = rentals.findById(rentalId);
            if (rental.status() != RentalStatus.ACTIVE) {
                throw new BusinessRuleException("Rental is already closed: " + rentalId);
            }

            ItemDomain item = requireItem((int) rental.itemId());

            rentals.close(rentalId, now());

            if (item.getStatus() == ItemStatus.rented) {
                item.setStatus(ItemStatus.available);
                saveItem(item);
            }
            // unlisted stays unlisted
            return item;
        });
    }

    // ---------- helpers ----------

    // TODO(users): replace this stub with the real lookup once UserRepository exists:
    //
    // private long findOrCreateRenterId(String username) {
    //     Optional<UserDomain> existing = users.findByUsername(username);
    //     if (existing.isPresent()) {
    //         return existing.get().getId();
    //     }
    //     return users.insert(username, null).getId();
    // }
    private long findOrCreateRenterId(String username) {
        throw new UnsupportedOperationException("TODO(users): waiting for UserRepository");
    }

    /** Minute precision, because the database stores yyyy-MM-dd HH:mm. */
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private ItemDomain requireItem(int itemId) {
        return db("find item", () -> items.findByItemId(itemId))
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }

    private void saveItem(ItemDomain item) {
        db("update item", () -> {
            items.update(item);
            return null;
        });
    }

    // ItemRepository throws checked SQLException. Delete this wrapper once it translates its own errors.
    private interface SqlCall<T> {
        T run() throws SQLException;
    }

    private <T> T db(String operation, SqlCall<T> call) {
        try {
            return call.run();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(operation, e);
        }
    }
}
