package com.keithfarrugia.jackpot_service.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BetIdempotencyRepository extends 
    JpaRepository<BetIdempotencyRecord, UUID> {}