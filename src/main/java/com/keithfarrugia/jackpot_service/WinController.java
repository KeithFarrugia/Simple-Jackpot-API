package com.keithfarrugia.jackpot_service;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.keithfarrugia.jackpot_service.model.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * @class WinController
 * @brief REST controller for retrieving win history.
 */
@RestController
@RequestMapping("/wins")
@Tag(name = "Wins", description = "Query win history")
public class WinController {
    private final BetRepository betRepo;

    /** @brief Constructor for dependency injection. */
    public WinController(BetRepository betRepo) {
        this.betRepo = betRepo;
    }

    /**
     * @brief Retrieves paginated wins.
     * @details Uses GET with query parameters to remain RESTful.
     * @param req Filter criteria.
     * @param pageable Pagination settings.
     * @return Paged win results.
     */
    @GetMapping
    @Operation(summary = "Retrieve all wins")
    public Win.PagedResponse getWins(
        @ParameterObject Win.Request req,
        @PageableDefault(size = 10, sort = "timestamp", 
                         direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Bet> page = betRepo.findAll(Bet.fromRequest(req), pageable);
        return new Win.PagedResponse(page);
    }
}