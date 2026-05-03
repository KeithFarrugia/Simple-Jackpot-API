package com.keithfarrugia.jackpot_service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.keithfarrugia.jackpot_service.model.*;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * @class Controller
 * @brief REST controller for jackpots, bets, and win history.
 */
@RestController
@OpenAPIDefinition(
    info = @Info(title = "Jackbox API", description = "Jackpot interface")
)
@Tag(name = "Jackpots", description = "Manage jackpot games")
public class Controller {
    SecureRandom rand = new SecureRandom();
    private JackpotRepository jackpotRepo;
    private BetRepository betRepo;
    private BetIdempotencyRepository betIdemRepo;

    /** @brief Constructor for dependency injection. */
    public Controller(JackpotRepository jackpotRepo, 
                      BetRepository betRepo, 
                      BetIdempotencyRepository betIdemRepo) {
        this.jackpotRepo = jackpotRepo;
        this.betRepo     = betRepo;
        this.betIdemRepo = betIdemRepo;
    }

    /**
     * @brief Creates or updates a jackpot by name.
     * @param req Jackpot request data.
     * @return UUID of the jackpot.
     */
    @PostMapping("addjackpot")
    @Operation(summary = "Add or Update Jackpot")
    public UUID addJackpot(@RequestBody @Valid Jackpot.Request req) {
        Optional<Jackpot> result = jackpotRepo.findByName(req.name());
        if (result.isPresent()){
            Jackpot jackpot = result.get();
            jackpot.setWinProbability(req.winProbability());
            return jackpotRepo.save(jackpot).getId();
        }
        return jackpotRepo.save(new Jackpot(req)).getId();
    }

    /**
     * @brief Retrieves all jackpots.
     * @return List of jackpot responses.
     */
    @GetMapping("/jackpots")
    @Operation(summary = "Retrieve All Jackpots")
    public List<Jackpot.Response> getAllJackpots() {
        return jackpotRepo.findAll().stream()
                .map(Jackpot.Response::new).toList();
    }

    /**
     * @brief Processes a bet with idempotency.
     * @param req Bet details.
     * @param idempotencyKey Unique transaction ID.
     * @return Bet result details.
     */
    @Transactional
    @PostMapping("/bet")
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

    /**
     * @brief Retrieves paginated wins.
     * @param req Filter criteria.
     * @param pageable Pagination settings.
     * @return Paged win results.
     */
    @PostMapping("/wins")
    @Operation(summary = "Retrieve all wins")
    public Win.PagedResponse getWins(
        @RequestBody Win.Request req,
        @PageableDefault(size = 10, sort = "timestamp", 
                         direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Bet> page = betRepo.findAll(Bet.fromRequest(req), pageable);
        return new Win.PagedResponse(page);
    }
}