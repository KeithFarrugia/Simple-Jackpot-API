package com.keithfarrugia.jackpot_service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.keithfarrugia.jackpot_service.model.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * @class BetController
 * @brief REST controller for processing user bets.
 */
@RestController
@RequestMapping("/bets")
@Tag(name = "Bets", description = "Process and manage bets")
public class BetController {
    private final SecureRandom              rand = new SecureRandom();
    private final JackpotRepository         jackpotRepo;
    private final BetRepository             betRepo;
    private final BetIdempotencyRepository  betIdemRepo;

    /** @brief Constructor for dependency injection. */
    public BetController(JackpotRepository jackpotRepo, 
                         BetRepository betRepo, 
                         BetIdempotencyRepository betIdemRepo) {
        this.jackpotRepo = jackpotRepo;
        this.betRepo     = betRepo;
        this.betIdemRepo = betIdemRepo;
    }

    /**
     * @brief Processes a bet with idempotency.
     * @details Uses a unique Idempotency-Key to ensure a bet is only processed once.
     * @param req Bet details.
     * @param idempotencyKey Unique transaction ID.
     * @return Bet result details.
     */
    @Transactional
    @PutMapping
    @Operation(summary = "Place a bet on a jackpot")
    public Bet.Response makeBet(
        @RequestBody @Valid Bet.Request req,
        @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        Optional<BetIdempotencyRecord> existing = 
            betIdemRepo.findById(idempotencyKey);
        if (existing.isPresent()) {
            Bet b = betRepo.findById(existing.get().getBetId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Bet missing"));
            return new Bet.Response(b.getWinAmount(), 0);
        }

        Jackpot j = jackpotRepo.findById(req.jackpotId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Jackpot not found"));

        boolean won = rand.nextDouble() < j.getWinProbability();
        double winAmount = 0, newSize = 0;

        if (won) {
            winAmount = jackpotRepo.claimPot(req.jackpotId(), 
                req.betAmount(), Instant.now())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.CONFLICT, "Already claimed"));
        } else {
            newSize = jackpotRepo.addToPot(req.jackpotId(), req.betAmount())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Update failed"));
        }

        Bet b = new Bet(req);
        b.setHasWon(won);
        b.setWinAmount(winAmount);
        b.setTimestamp(Instant.now());
        betRepo.save(b);
        betIdemRepo.save(new BetIdempotencyRecord(idempotencyKey, b.getId()));

        return new Bet.Response(winAmount, newSize);
    }
}