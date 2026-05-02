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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.keithfarrugia.jackpot_service.model.Bet;
import com.keithfarrugia.jackpot_service.model.BetIdempotencyRecord;
import com.keithfarrugia.jackpot_service.model.BetIdempotencyRepository;
import com.keithfarrugia.jackpot_service.model.BetRepository;
import com.keithfarrugia.jackpot_service.model.Jackpot;
import com.keithfarrugia.jackpot_service.model.JackpotRepository;
import com.keithfarrugia.jackpot_service.model.Win;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@OpenAPIDefinition(
		info = @Info(
				title = "Jackbox API",
				description = "Api interface for Jackpots"
		)
)

@Tag(name = "Jackpots", description = "Manage jackpot games")
public class Controller {
    SecureRandom rand = new SecureRandom();

    private JackpotRepository jackpotRepo = null;
    private BetRepository betRepo = null;
    private BetIdempotencyRepository betIdemRepo = null;


    public Controller(JackpotRepository jackpotRepo, BetRepository betRepo, BetIdempotencyRepository betIdemRepo) {
        this.jackpotRepo = jackpotRepo;
        this.betRepo     = betRepo;
        this.betIdemRepo = betIdemRepo;
    }

    // CREATE (POST)
    @PostMapping("addjackpot")
    @Operation(
        summary = "Add or Update Jackpot",
        description = "If no Jackpot exists with the given name then it is created, else the existing entry is updated"
    )
    public UUID addJackpot(@RequestBody @Valid Jackpot.Request req) {
        Optional<Jackpot> result = jackpotRepo.findByName(req.name());
        if (result.isPresent()){
            Jackpot jackpot = result.get();
            jackpot.setWinProbability(req.winProbability());
            return jackpotRepo.save(jackpot).getId();
        }else{
            return jackpotRepo.save(new Jackpot(req)).getId();
        }
    }

    // READ (GET ALL)
    @GetMapping("/jackpots")
    @Operation(
        summary = "Retrieve All Jackpots",
        description = "Retrieves all Jackpots in the format {ID, Curr-Size, Num Wins, Last Win}"
    )
    public List<Jackpot.Response> getAllJackpots() {
        return jackpotRepo.findAll()
                .stream()
                .map(Jackpot.Response::new)
                .toList();
    }

    @Transactional
    @PostMapping("/bet")
    public Bet.Response makeBet(
        @RequestBody @Valid  Bet.Request req,
        @RequestHeader("Idempotency-Key") UUID idempotencyKey
    ) {
        Optional<BetIdempotencyRecord> existing = betIdemRepo.findById(idempotencyKey);
        if (existing.isPresent()) {
            Bet b = betRepo.findById(existing.get().getBetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bet record missing"));
            return new Bet.Response(b.getWinAmount(), 0);
        }

        Jackpot j = jackpotRepo.findById(req.jackpotId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jackpot not found"));

        boolean won      = rand.nextDouble() < j.getWinProbability();
        double winAmount = 0;
        double newSize   = 0;

        if (won) {
            winAmount = jackpotRepo.claimPot(req.jackpotId(), req.betAmount(), Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Jackpot already claimed"));
            newSize = 0;
        } else {
            newSize = jackpotRepo.addToPot(req.jackpotId(), req.betAmount())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Pot update failed"));
        }

        Bet b = new Bet(req);
        b.setHasWon(won);
        b.setWinAmount(winAmount);
        b.setTimestamp(Instant.now());
        betRepo.save(b);

        betIdemRepo.save(new BetIdempotencyRecord(idempotencyKey, b.getId()));

        return new Bet.Response(winAmount, newSize);
    }

    @PostMapping("/wins")
    @Operation(
        summary     = "Retrieve all wins",
        description = "Filtered wins with pagination. Query params: page, size, sort=field,dir (e.g. sort=winAmount,desc)"
    )
    public Win.PagedResponse getWins(
        @RequestBody Win.Request req,
        @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Bet> page = betRepo.findAll(Bet.fromRequest(req), pageable);
        return new Win.PagedResponse(page);
    }
}