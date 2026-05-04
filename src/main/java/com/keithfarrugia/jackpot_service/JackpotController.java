package com.keithfarrugia.jackpot_service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.keithfarrugia.jackpot_service.model.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * @class JackpotController
 * @brief REST controller for managing jackpot game definitions.
 */
@RestController
@RequestMapping("/jackpots")
@Tag(name = "Jackpots", description = "Manage jackpot games")
public class JackpotController {
    private final JackpotRepository jackpotRepo;

    /** @brief Constructor for dependency injection. */
    public JackpotController(JackpotRepository jackpotRepo) {
        this.jackpotRepo = jackpotRepo;
    }

    /**
     * @param req Jackpot request data.
     * @return UUID of the jackpot.
     */
    @PostMapping
    @Operation(summary = "Create a jackpot")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID addJackpot(@RequestBody @Valid Jackpot.Request req) {
        return jackpotRepo.save(new Jackpot(req)).getId();
    }

    /**
     * @brief Retrieves all jackpots.
     * @return List of jackpot responses.
     */
    @GetMapping
    @Operation(summary = "Retrieve All Jackpots")
    public List<Jackpot.Response> getAllJackpots() {
        return jackpotRepo.findAll().stream()
                .map(Jackpot.Response::new).toList();
    }
}