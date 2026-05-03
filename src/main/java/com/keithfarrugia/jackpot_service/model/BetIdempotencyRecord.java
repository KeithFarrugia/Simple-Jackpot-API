package com.keithfarrugia.jackpot_service.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief   JPA entity used to enforce bet idempotency.
 *
 * @details Records a mapping between a client-supplied idempotency
 *          key and the bet that was created for that key. On any
 *          duplicate request carrying the same key, the original
 *          bet is looked up and its result is replayed without
 *          processing a new bet.
 */
@Data
@Entity
@Table(name = "BetIdempotencyRecord")
@NoArgsConstructor
@AllArgsConstructor
public class BetIdempotencyRecord {

    /**
     * @brief Client-generated UUID used to deduplicate requests.
     *        Acts as the primary key for this table.
     */
    @Id
    private UUID idempotencyKey;

    /**
     * @brief ID of the Bet entity created for this idempotency key.
     *        Used to fetch and replay the original bet result.
     */
    private UUID betId;
}