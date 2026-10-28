package com.rentaltracker.service;

import com.rentaltracker.domain.ItemDomain;

/** An item plus the owner's username, ready for the detail screen. */
public record ItemDetail(ItemDomain item, String ownerUsername) {}