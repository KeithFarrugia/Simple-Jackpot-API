package com.keithfarrugia.jackpot_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.keithfarrugia.jackpot_service.model.*;

@ExtendWith(MockitoExtension.class)
class WinControllerTest {

    @Mock BetRepository betRepo;
    @InjectMocks WinController controller;

    /* ========================================================================
     * This test makes sure that getWins returns paginated win results.
     * With 2 total records and a page size of 1, we expect 2 total pages.
     * ========================================================================
     */
    @Test
    @SuppressWarnings("unchecked")
    void Controller_getWins_returnsMultiplePages() {
        Win.Request req = new Win.Request(
            null, 
            null, 
            null
        );

        Pageable pageable = PageRequest.of(
            0, 
            1
        );
        
        Bet bet1 = new Bet();
        bet1.setPlayerAlias("Bob");
        bet1.setBetAmount(5.0);
        bet1.setWinAmount(50.0);
        bet1.setHasWon(true);

        Page<Bet> pagedResult = new PageImpl<>(
            Collections.singletonList(bet1), 
            pageable, 
            2 
        );
        
        when(betRepo.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(pagedResult);

        Win.PagedResponse response = controller.getWins(req, pageable);

        assertNotNull(response);
        assertEquals(1, response.wins().size());
        assertEquals(0, response.currentPage());
        assertEquals(2, response.totalPages());
        assertEquals(2, response.totalWins());
        assertEquals("Bob", response.wins().get(0).playerAlias());
        
        verify(betRepo).findAll(any(Specification.class), eq(pageable));
    }
}