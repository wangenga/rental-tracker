package com.rentaltracker.repository;

import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.domain.enums.RentalStatus;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.MappingException;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;

public class RentalRepository {
     private static final DateTimeFormatter DB_Time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Database db;

    public RentalRepository (Database db){ 
        this.db = db ;
    }


    public long insert(RentalDomain rental) {

        String sql = "INSERT INTO rentals (item_id, renter_id, start_time, end_time)" + "VALUES (?,?,?,?)";
        try(PreparedStatement ps = db.connection()
            .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
                ps.setLong(1, rental.itemId());
                ps.setLong(2, rental.renterId());
                ps.setString(3, rental.startTime().format(DB_Time));
                ps.setString(4, rental.endTime().format(DB_Time));
                ps.executeUpdate();
            
            try (ResultSet keys = ps.getGeneratedKeys()){
                keys.next();
                return keys.getLong(1);
            }

        } catch (SQLException e){
            throw SqlExceptionTranslator.translate("insert rental", e);
        }
    }

    /** @throws NotFoundException if no rental has this id */
    public RentalDomain findById(long rentalId) {
        String sql = "SELECT * FROM rentals WHERE rental_id = ?";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setLong(1, rentalId);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()){
                    throw new NotFoundException("Rental not found " + rentalId);
                }
                return map(rs);
            }
        }catch (SQLException e){
            throw SqlExceptionTranslator.translate("find rental by id", e);
        }
    }

    /** Active rentals on items owned by this user, soonest due date first. */
    public List<RentalDomain> findActiveByOwner(long ownerId) {
        String sql = "SELECT r.* FROM rentals r "
                   + "JOIN listed_items i ON i.item_id = r.item_id "
                   + "WHERE i.owner_id = ? AND r.status = 'active' "
                   + "ORDER BY r.end_time ASC ";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)){
            ps.setLong(1, ownerId);
            try( ResultSet rs = ps.executeQuery()){
                List<RentalDomain> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
            
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find active rental by owner", e);
        }
    }

    /** The active rental for an item, or empty if the item is not out. */
    public Optional<RentalDomain> findActiveByItem (long itemId) {
        String sql = "SELECT * FROM rentals WHERE item_id = ? AND status = 'active'";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)){
            ps.setLong(1, itemId);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }catch (SQLException e) {
            throw SqlExceptionTranslator.translate("find active rental by item", e);
        }
    }

    /**
     * Closes an active rental. Status and returned_at are set in one UPDATE,
     * because the schema CHECK requires them to change together.
     * @throws NotFoundException if there is no active rental with this id
     */
    public void close (long rentalId, LocalDateTime returnedAt){
        String sql = "UPDATE rentals SET status = 'closed', returned_at = ? "
                   + "WHERE rental_id = ? AND status = 'active'";
        try (PreparedStatement ps = db.connection().prepareStatement(sql)){
            ps.setString(1, returnedAt.format(DB_Time));
            ps.setLong(2, rentalId);
            if (ps.executeUpdate() == 0) {
                throw new NotFoundException("No active rental with id " + rentalId);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("close rental", e);
        }
    }

    private RentalDomain map(ResultSet rs){
        try{
            String returned = rs.getString("returned_at");
            return new RentalDomain(
                rs.getLong("rental_id"),
                rs.getLong("item_id"),
                rs.getLong("renter_id"),
                LocalDateTime.parse(rs.getString("start_time"), DB_Time),
                LocalDateTime.parse(rs.getString("end_time"), DB_Time),
                returned == null ? null : LocalDateTime.parse(returned, DB_Time),
                RentalStatus.fromDb(rs.getString("status")));
            
        }catch (SQLException | RuntimeException e) {
            throw new MappingException("Cannot map rental row", e);
        }
    }

    
}

