package com.keithfarrugia.jackpot_service.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @brief   Spring Data repository for BetIdempotencyRecord entities.
 *
 * @details Provides standard CRUD operations inherited from
 *          JpaRepository. The primary use is findById() to check
 *          whether an idempotency key has already been used, and
 *          save() to register a new key after a bet is processed.
 */
public interface BetIdempotencyRepository
        extends JpaRepository<BetIdempotencyRecord, UUID> {}