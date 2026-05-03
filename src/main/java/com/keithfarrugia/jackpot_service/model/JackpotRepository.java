package com.keithfarrugia.jackpot_service.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @brief   Spring Data repository for Jackpot entities.
 *
 * @details Provides standard CRUD operations via JpaRepository and
 *          dynamic queries via JpaSpecificationExecutor. Also exposes
 *          two atomic native SQL operations — addToPot and claimPot —
 *          that use PostgreSQL RETURNING to avoid a separate read
 *          after each write.
 */
public interface JackpotRepository
        extends JpaRepository<Jackpot, UUID>,
                JpaSpecificationExecutor<Jackpot> {

    /**
     * @brief  Looks up a jackpot by its display name.
     *
     * @param  name The name to search for.
     * @return An Optional containing the jackpot if found,
     *         or empty if no jackpot with that name exists.
     */
    Optional<Jackpot> findByName(String name);

    /**
     * @brief   Atomically adds a bet amount to the jackpot pot.
     *
     * @details Executes a single UPDATE ... RETURNING statement so
     *          the new pot size is returned without a second query.
     *          Using RETURNING prevents a stale read that could
     *          occur if the pot is updated concurrently.
     *
     * @param  id  The UUID of the jackpot to update.
     * @param  bet The bet amount to add to the current pot size.
     * @return An Optional containing the new pot size after the
     *         update, or empty if no row matched the given ID.
     */
    @Transactional
    @Query(value = """
        UPDATE jackpot SET
            current_size = current_size + :bet
        WHERE id = :id
        RETURNING current_size AS new_size
    """, nativeQuery = true)
    Optional<Double> addToPot(
        @Param("id")  UUID   id,
        @Param("bet") double bet
    );

    /**
     * @brief   Atomically claims the jackpot pot for a winning bet.
     *
     * @details Resets the pot to zero, records the win timestamp,
     *          and increments the win counter in a single statement.
     *          The WHERE clause guards against out-of-order requests
     *          by rejecting any claim whose timestamp is older than
     *          the recorded last win. RETURNING captures the pot
     *          value before the reset and adds the triggering bet
     *          to produce the exact payout amount.
     *
     * @param  id  The UUID of the jackpot to claim.
     * @param  bet The triggering bet amount, added to the payout.
     * @param  now Timestamp of this claim, used as the new lastWin
     *             and compared against the existing lastWin guard.
     * @return An Optional containing the total win amount, or empty
     *         if the claim was rejected due to an older timestamp.
     */
    @Transactional
    @Query(value = """
        UPDATE jackpot SET
            current_size = 0,
            last_win     = :now,
            num_wins     = num_wins + 1
        WHERE id   = :id
        AND  (last_win IS NULL OR last_win < :now)
        RETURNING current_size + :bet AS win_amount
    """, nativeQuery = true)
    Optional<Double> claimPot(
        @Param("id")  UUID    id,
        @Param("bet") double  bet,
        @Param("now") Instant now
    );
}