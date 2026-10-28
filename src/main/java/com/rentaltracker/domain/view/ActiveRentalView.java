package com.rentaltracker.domain.view;

import com.rentaltracker.domain.enums.RentalStatus;
import java.time.LocalDateTime;

public record ActiveRentalView(
        long rentalId,
        String itemName,
        String renterUsername,
        LocalDateTime startTime,
        LocalDateTime endTime,
        RentalStatus status
) {}