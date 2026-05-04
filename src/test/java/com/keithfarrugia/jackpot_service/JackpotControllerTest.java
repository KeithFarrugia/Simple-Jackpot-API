package com.keithfarrugia.jackpot_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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
        when(jackpotRepo.save(any())).thenReturn(j);

        UUID result = controller.addJackpot(req);

        assertEquals(j.getId(), result);
        verify(jackpotRepo).save(any(Jackpot.class));
    }
}