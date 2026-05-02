package com.keithfarrugia.jackpot_service.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface JackpotRepository extends 
    JpaRepository            <Jackpot, UUID>,
    JpaSpecificationExecutor <Jackpot>
{
    Optional<Jackpot> findByName(String name);

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
        @Param("id")    UUID    id, 
        @Param("bet")   double  bet, 
        @Param("now")   Instant now
    );
}