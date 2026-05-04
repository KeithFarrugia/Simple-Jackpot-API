package com.keithfarrugia.jackpot_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.keithfarrugia.jackpot_service.model.*;

@ExtendWith(MockitoExtension.class)
class JackpotControllerTest {

    @Mock JackpotRepository jackpotRepo;
    @InjectMocks JackpotController controller;

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
     * No new jackpot should be created,  the existing one should be saved
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
}