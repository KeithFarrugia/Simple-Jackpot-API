package com.keithfarrugia.jackpot_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.keithfarrugia.jackpot_service.model.*;

@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock JackpotRepository        jackpotRepo;
    @Mock BetRepository            betRepo;
    @Mock BetIdempotencyRepository betIdemRepo;

    @InjectMocks Controller controller;

    UUID        jackpotId;
    UUID        idempotencyKey;
    Jackpot     jackpot;
    Bet.Request betReq;

    @BeforeEach
    void setUp() {
        jackpotId      = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();

        jackpot        = new Jackpot(
            UUID.randomUUID(), 
            "TestJackpot", 
            0.0f, 
            100.0, 
            0L, 
            null);

        betReq         = new Bet.Request(
            jackpotId, 
            10.0, 
            "Bob");
    }

    /* ========================================================================
     * This test makes sure that addJackpot creates and saves a new jackpot
     * when no jackpot with the given name exists in the repository.
     *
     * The returned UUID should match the ID of the newly saved jackpot.
     * ========================================================================
     */
    @Test
    void Controller_addJackpot_createsNew() {
        Jackpot.Request req = new Jackpot.Request(
            "NewJackpot",
            0.5f
        );

        Jackpot j = new Jackpot(
            UUID.randomUUID(),
            "NewJackpot",
            0.5f,
            0.0,
            0L,
            null
        );

        when(jackpotRepo.findByName("NewJackpot")).
            thenReturn(Optional.empty());
        when(jackpotRepo.save(any())).thenReturn(j);

        UUID result = controller.addJackpot(req);

        assertEquals(j.getId(), result);
        verify(jackpotRepo).save(any(Jackpot.class));
    }

    /* ========================================================================
     * This test makes sure that addJackpot updates the win probability of
     * an existing jackpot when one with the given name already exists.
     *
     * No new jackpot should be created — the existing one should be saved
     * with the updated probability.
     * ========================================================================
     */
    @Test
    void Controller_addJackpot_updates() {
        Jackpot.Request req      = new Jackpot.Request(
            "TestJackpot", 
            0.9f
        );
        Jackpot j = new Jackpot(
            UUID.randomUUID(), 
            "TestJackpot",
            0.1f,
            50.0,
            0L,
            null
        );

        when(jackpotRepo.findByName("TestJackpot")).
            thenReturn(Optional.of(j));

        when(jackpotRepo.save(j)).thenReturn(j);
        
        controller.addJackpot(req);

        assertEquals(0.9f, j.getWinProbability());
        verify(jackpotRepo).save(j);
    }

    /* ========================================================================
     * This test makes sure that makeBet returns the original result without
     * processing a new bet when the idempotency key has already been used.
     *
     * The jackpot repository should never be touched in a replay scenario.
     * ========================================================================
     */
    @Test
    void Controller_makeBet_IdempotencyDup() {
        UUID betId = UUID.randomUUID();
        Bet  bet   = new Bet();

        bet.setId(betId);
        bet.setWinAmount(110.0);

        // Say that the Idempotency ID already Exists.
        when(betIdemRepo.findById(idempotencyKey))
            .thenReturn(Optional.of(
                new BetIdempotencyRecord(idempotencyKey, betId)
        ));

        when(betRepo.findById(betId)).thenReturn(Optional.of(bet));

        Bet.Response response = controller.makeBet(betReq, idempotencyKey);

        assertEquals(110.0, response.winAmount());

        // shouldn't touch the jackpot table if it is idempotent
        verify(jackpotRepo, never()).findById(any());
    }

    /* ========================================================================
     * This test makes sure that makeBet throws a 404 when the jackpot ID
     * in the request does not match any jackpot in the repository.
     *
     * No bet should be saved and no pot operations should be performed.
     * ========================================================================
     */
    @Test
    void Controller_makeBet_JackpotNotFound() {
        when(betIdemRepo.findById(any())).thenReturn(Optional.empty());
        when(jackpotRepo.findById(jackpotId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class, () -> 
                controller.makeBet(betReq, idempotencyKey));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /* ========================================================================
     * This test makes sure that makeBet calls addToPot and never claimPot
     * when the random roll results in a loss, and that the response contains
     * zero winAmount and the correct updated pot size.
     *
     * A win probability of 0.0 guarantees a loss on every roll.
     * ========================================================================
     */
    @Test
    void Controller_makeBet_addsToPot() {
        jackpot.setWinProbability(0.0f);
        double expectedSize = jackpot.getCurrentSize() + betReq.betAmount();

        when(betIdemRepo.findById(any()))
            .thenReturn(Optional.empty());

        when(jackpotRepo.findById(jackpotId))
            .thenReturn(Optional.of(jackpot));

        when(jackpotRepo.addToPot(jackpotId, 10.0))
            .thenReturn(Optional.of(expectedSize));

        when(betRepo.save(any()))
            .thenAnswer(i -> i.getArgument(0));

        Bet.Response response = controller.makeBet(betReq, idempotencyKey);

        assertEquals(0.0, response.winAmount());
        assertEquals(expectedSize, response.newSize());

        verify(jackpotRepo).addToPot(jackpotId, 10.0);
        verify(jackpotRepo, never()).claimPot(any(), anyDouble(), any());
    }

    /* ========================================================================
     * This test makes sure that makeBet calls claimPot and never addToPot
     * when the random roll results in a win, and that the correct win amount
     * is returned in the response.
     *
     * A win probability of 1.0 guarantees a win on every roll.
     * ========================================================================
     */
    @Test
    void Controller_makeBet_claimsPot() {
        jackpot.setWinProbability(1.0f);

        double expectedWin = jackpot.getCurrentSize() + betReq.betAmount();

        when(betIdemRepo.findById(any()))
            .thenReturn(Optional.empty());

        when(jackpotRepo.findById(jackpotId))
            .thenReturn(Optional.of(jackpot));

        when(jackpotRepo.claimPot(
            eq(jackpotId), eq(betReq.betAmount()), 
            any(Instant.class)
        ))
            .thenReturn(Optional.of(expectedWin));

        when(betRepo.save(any()))
            .thenAnswer(i -> i.getArgument(0));

        Bet.Response response = controller.makeBet(betReq, idempotencyKey);

        assertEquals(expectedWin, response.winAmount());
        assertEquals(0.0, response.newSize());
        
        verify(jackpotRepo).claimPot(
            eq(jackpotId), eq(betReq.betAmount()), 
            any(Instant.class)
        );
        verify(jackpotRepo, never()).addToPot(any(), anyDouble());
    }

    /* ========================================================================
     * This test makes sure that makeBet saves an idempotency record pointing
     * at the newly created bet after every successful fresh bet placement.
     *
     * This record is what allows duplicate requests to be replayed correctly.
     * ========================================================================
     */
    @Test
    void Controller_makeBet_savesIdempotencyRecord() {
        jackpot.setWinProbability(0.0f);

        double expectedSize = jackpot.getCurrentSize() + betReq.betAmount();

        when(betIdemRepo.findById(any()))
            .thenReturn(Optional.empty());

        when(jackpotRepo.findById(jackpotId))
            .thenReturn(Optional.of(jackpot));

        when(jackpotRepo.addToPot(jackpotId, betReq.betAmount()))
            .thenReturn(Optional.of(expectedSize));

        when(betRepo.save(any()))
            .thenAnswer(i -> i.getArgument(0));

        controller.makeBet(betReq, idempotencyKey);

        verify(betIdemRepo).save(any(BetIdempotencyRecord.class));
    }

    /* ========================================================================
     * This test makes sure that makeBet throws a 409 when claimPot returns
     * an empty optional, indicating that a concurrent request already claimed
     * the jackpot with a later timestamp.
     *
     * The conflict should be surfaced immediately rather than silently ignored.
     * ========================================================================
     */
    @Test
    void Controller_makeBet_throws409() {
        jackpot.setWinProbability(1.0f);

        when(betIdemRepo.findById(any()))
            .thenReturn(Optional.empty());

        when(jackpotRepo.findById(jackpotId))
            .thenReturn(Optional.of(jackpot));

        when(jackpotRepo.claimPot(any(), anyDouble(), any()))
            .thenReturn(Optional.empty());

        ResponseStatusException ex = 
            assertThrows(ResponseStatusException.class,
            () -> controller.makeBet(betReq, idempotencyKey));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}