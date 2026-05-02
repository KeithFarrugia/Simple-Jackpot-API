package com.keithfarrugia.jackpot_service.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BetRepositoryTest {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
 
    @Autowired BetRepository betRepo;

    UUID jackpotId = UUID.randomUUID();
    UUID nonUsedId  = new UUID(
        jackpotId.getMostSignificantBits (), 
        jackpotId.getLeastSignificantBits() + 1
    );
    
    Instant time = Instant.now();
    @BeforeEach
    void setUp() {
        // losing bet
        Bet loss = new Bet();
        loss.setJackpotId   (jackpotId);
        loss.setBetAmount   (10.0);
        loss.setPlayerAlias ("Bob");
        loss.setHasWon      (false);
        loss.setWinAmount   (0);
        loss.setTimestamp   (time.minusSeconds(60));
        betRepo.save        (loss);

        // winning bet
        Bet win = new Bet();
        win.setJackpotId    (jackpotId);
        win.setBetAmount    (20.0);
        win.setPlayerAlias  ("Bob");
        win.setHasWon       (true);
        win.setWinAmount    (200.0);
        win.setTimestamp    (time);
        betRepo.save        (win);
    }
    /* ========================================================================
     * This Test makes sure that the specification builder inside Bet
     * correctly generates a predicate that forces only winning bets.
     * 
     * This means that only winning bets are returned.
     * ========================================================================
     */
    @Test
    void Bet_returnWinsOnly() {
        Win.Request req     = new Win.Request(
            null, null, 
            null, null
        );
        Page<Bet> result    = betRepo.findAll(
            Bet.fromRequest(req), PageRequest.of(0, 10)
        );
        
        assertFalse(result.getContent().isEmpty());
        for (Bet bet : result) {
            assertTrue (bet.isHasWon());
        }
    }

    /* ========================================================================
     * This test makes sure that the specification builder inside Bet
     * correctly generates a predicate that filters results by jackpot ID.
     *
     * Only winning bets belonging to the given jackpot ID should be returned.
     * ========================================================================
     */
    @Test
    void Bet_filtersByJackpotId() {
        Win.Request req = new Win.Request(
            List.of(jackpotId), 
            null, 
            null, 
            null
        );

        Page<Bet> result = betRepo.findAll(
            Bet.fromRequest(req), PageRequest.of(0, 10)
        );
        
        assertFalse(result.getContent().isEmpty());
        for (Bet bet : result) {
            assertTrue  (bet.isHasWon());
            assertEquals(jackpotId, bet.getJackpotId());
        }
    }
    /* ========================================================================
     * This test makes sure that the specification builder inside Bet
     * returns no results when the given jackpot ID does not match any bets.
     *
     * An unknown ID should produce an empty result set rather than an error.
     * ========================================================================
     */
    @Test
    void Bet_unknownJackpotId() {
        Win.Request req = new Win.Request(
            List.of(nonUsedId), 
            null,
            null, 
            null
        );
        Page<Bet> result = betRepo.findAll(
            Bet.fromRequest(req), 
            PageRequest.of(0, 10)
        );
        
        
        assertTrue(result.getContent().isEmpty());
    }
    
    /* ========================================================================
     * This test makes sure that the specification builder inside Bet
     * correctly generates a predicate that filters results by player alias.
     *
     * Only winning bets placed by the given player alias should be returned.
     * ========================================================================
     */
    @Test
    void Bet_filterPlayerAlias() {
        Win.Request req = new Win.Request(
            null, 
            null,
            List.of("Bob"), 
            null
        );

        Page<Bet> result = betRepo.findAll(
            Bet.fromRequest(req), 
            PageRequest.of(0, 10)
        );

        assertFalse(result.getContent().isEmpty());
        for (Bet bet : result) {
            assertTrue  (bet.isHasWon());
            assertEquals("Bob", bet.getPlayerAlias());
        }
    }

    /* ========================================================================
     * This test makes sure that the specification builder inside Bet
     * correctly generates a predicate that filters results by win amount.
     *
     * Only winning bets matching the exact win amount should be returned.
     * ========================================================================
     */
    @Test
    void Bet_filtersWinAmount() {
        Win.Request req = new Win.Request(
            null, 
            List.of(200.0), 
            null, 
            null);
        Page<Bet> result = betRepo.findAll(
            Bet.fromRequest(req), 
            PageRequest.of(0, 10)
        );
        
        assertFalse(result.getContent().isEmpty());
        for (Bet bet : result) {
            assertTrue  (bet.isHasWon());
            assertEquals(200.0, bet.getWinAmount());
        }
    }

    /* ========================================================================
     * This test makes sure that the specification builder inside Bet
     * correctly generates a predicate that filters results by time range.
     *
     * Only winning bets whose timestamp falls within the given range should
     * be returned.
     * ========================================================================
     */
    @Test
    void Bet_filtersTimeRange() {
        Instant start = time.minusSeconds(10);
        Instant end   = time.plusSeconds(10);

        Win.Request req = new Win.Request(
            null, 
            null, 
            null,
            List.of(new Win.TimeRange(start, end))
        );

        Page<Bet> result = betRepo.findAll(
            Bet.fromRequest(req), 
            PageRequest.of(0, 10)
        );

        assertFalse(result.getContent().isEmpty());
        for (Bet bet : result) {
            assertTrue  (bet.isHasWon());
            assertTrue(
                !bet.getTimestamp().isBefore(start)  && // >= start
                !bet.getTimestamp().isAfter(end)        // <= end
            );
        }
    }
    
    /* ========================================================================
     * This test makes sure that the specification builder inside Bet
     * correctly excludes bets whose timestamp falls outside the given range.
     *
     * A range entirely in the future should return no results.
     * ========================================================================
     */
    @Test
    void Bet_filtersOutsideTimeRange() {
        Instant start = time.plusSeconds(100);
        Instant end   = time.plusSeconds(200);

        Win.Request req = new Win.Request(
            null, 
            null, 
            null,
            List.of(new Win.TimeRange(start, end))
        );

        Page<Bet> result = betRepo.findAll(
            Bet.fromRequest(req), 
            PageRequest.of(0, 10)
        );
        
        assertTrue(result.getContent().isEmpty());
    }
}