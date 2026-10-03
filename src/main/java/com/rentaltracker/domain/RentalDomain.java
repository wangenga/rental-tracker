package com.rentaltracker.domain;

import java.time.LocalDateTime;
import com.rentaltracker.domain.enums.RentalStatus;


public record RentalDomain(
    long id, long itemId, long renterId, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime returnTime, RentalStatus status
){}
