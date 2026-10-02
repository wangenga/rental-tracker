package com.rentaltracker.repository;

import java.sql.*;

import java.time.format.DateTimeFormatter;

import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;

public class RentalRepository {
     private static final DateTimeFormatter DB_Time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    
}

