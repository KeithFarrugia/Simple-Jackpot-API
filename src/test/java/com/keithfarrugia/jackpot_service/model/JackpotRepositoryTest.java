package com.keithfarrugia.jackpot_service.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JackpotRepositoryTest {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired JackpotRepository jackpotRepo;

    Jackpot jackpot;
    Instant time = Instant.now();

    @BeforeEach
    void setUp() {
        jackpot = jackpotRepo.save(
            new Jackpot(
                null, 
                "TestJackpot", 
                0.5f, 
                100.0, 
                0L,
                null
            )
        );
    }

    /* ========================================================================
     * This test makes sure that findByName returns the correct jackpot
     * when a jackpot with the given name exists in the database.
     *
     * The returned jackpot should have the same name as the one searched for.
     * ========================================================================
     */
    @Test
    void Jackpot_findByName() {
        Optional<Jackpot> result = jackpotRepo.findByName("TestJackpot");

        assertTrue (result.isPresent());
        assertEquals("TestJackpot", result.get().getName());
    }

    /* ========================================================================
     * This test makes sure that findByName returns an empty result
     * when no jackpot with the given name exists in the database.
     *
     * An unknown name should produce an empty optional rather than an error.
     * ========================================================================
     */
    @Test
    void Jackpot_nonExistingName() {
        Optional<Jackpot> result = jackpotRepo.findByName("NonExist");

        assertTrue(result.isEmpty());
    }

    /* ========================================================================
     * This test makes sure that addToPot correctly increments the
     * current size of the jackpot by the given bet amount.
     *
     * Starting at 100.0 and adding 50.0 should result in a size of 150.0.
     * ========================================================================
     */
    @Test
    void Jackpot_addToPot_Increments() {
        Optional<Double> newSize = 
            jackpotRepo.addToPot(jackpot.getId(), 50.0);

        assertTrue  (newSize.isPresent());
        assertEquals(150.0, newSize.get());
    }

    /* ========================================================================
     * This test makes sure that addToPot correctly accumulates across
     * multiple calls, adding each bet amount on top of the previous total.
     *
     * Starting at 100.0, adding 50.0 then 25.0 the second call should
     * return 175.0.
     * ========================================================================
     */
    @Test
    void Jackpot_addToPot_MultipleIncrements() {
        jackpotRepo.addToPot(jackpot.getId(), 50.0);
        Optional<Double> newSize = jackpotRepo.addToPot(jackpot.getId(), 25.0);

        assertTrue  (newSize.isPresent());
        assertEquals(175.0, newSize.get());
    }

    /* ========================================================================
     * This test makes sure that claimPot correctly resets the current size
     * to zero, increments the win counter, and sets the last win timestamp.
     *
     * All three fields must be updated atomically in a single operation.
     * ========================================================================
     */
    @Test
    void Jackpot_claimPot_ResetsAndIncrements() {
        jackpotRepo.claimPot(jackpot.getId(), 10.0, time);

        Jackpot updated = jackpotRepo.findById(jackpot.getId()).orElseThrow();
        assertEquals (0.0, updated.getCurrentSize());
        assertEquals (1L,  updated.getNumWins());
        assertNotNull(updated.getLastWin());
    }

    /* ========================================================================
     * This test makes sure that claimPot returns the correct win amount,
     * which is the current pot size plus the triggering bet amount.
     *
     * With a pot of 100.0 and a bet of 10.0 the returned amount should be 
     * 110.0.
     * ========================================================================
     */
    @Test
    void Jackpot_claimPot_returnsCorrectAmount() {
        Optional<Double> winAmount = jackpotRepo.claimPot(
            jackpot.getId(), 10.0, time
        );

        assertTrue (winAmount.isPresent());
        assertEquals(110.0, winAmount.get());
    }

    /* ========================================================================
     * This test makes sure that claimPot rejects a claim whose timestamp
     * is older than the jackpot's recorded last win.
     *
     * An out-of-order claim should return an empty optional and leave
     * the jackpot state unchanged.
     * ========================================================================
     */
    @Test
    void Jackpot_claimPot_rejectsOlderTimestamp() {
        Instant older = time.minusSeconds(60);

        jackpotRepo.claimPot(jackpot.getId(), 10.0, time);
        Optional<Double> result = 
            jackpotRepo.claimPot(jackpot.getId(), 10.0, older);

        assertTrue(result.isEmpty());
    }
}