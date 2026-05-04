package com.keithfarrugia.jackpot_service.model;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @brief   Namespace class grouping all win-related DTOs.
 *
 * @details Contains the records used to filter, return, and page
 *          winning bet results. This class is never instantiated, 
 *          it exists solely to keep win-related types together
 *          under a single, descriptive name.
 */
public class Win {

    /**
     * @brief   Inbound DTO for querying wins with optional filters.
     *
     * @details Any field left null is ignored,  only non-null,
     *          non-empty lists are applied as filters. Multiple
     *          time ranges are combined with OR so a bet qualifies
     *          if it falls within any one of them.
     *
     * @param jackpotIds    Filter by one or more jackpot IDs.
     * @param winAmounts    Filter by exact win amounts.
     * @param playerAliases Filter by one or more player aliases.
     */
    @Schema(name = "WinRequest")
    public record Request(
        List<UUID>      jackpotIds,
        List<Double>    winAmounts,
        List<String>    playerAliases
    ) {}

    /**
     * @brief Outbound DTO representing a single winning bet.
     *
     * @param betAmount   Amount wagered on the winning bet.
     * @param playerAlias Alias of the player who won.
     * @param hasWon      Always true for records returned here.
     * @param winAmount   Total amount paid out to the player.
     */
    @Schema(name = "WinResponse")
    public record Response(
        double  betAmount,
        String  playerAlias,
        boolean hasWon,
        double  winAmount
    ) {
        /**
         * @brief Convenience constructor that maps from a Bet entity.
         *
         * @param bet The winning bet entity to convert.
         */
        public Response(Bet bet) {
            this(
                bet.getBetAmount(),
                bet.getPlayerAlias(),
                bet.isHasWon(),
                bet.getWinAmount()
            );
        }
    }

    /**
     * @brief   Outbound DTO wrapping a page of winning bet results.
     *
     * @details Includes pagination metadata alongside the result
     *          list so callers know how many pages exist in total
     *          and can request subsequent pages.
     *
     * @param wins        The winning bets on the current page.
     * @param currentPage Zero-indexed number of the current page.
     * @param totalPages  Total number of pages available.
     * @param totalWins   Total number of matching winning bets.
     */
    @Schema(name = "WinPagedResponse")
    public record PagedResponse(
        List<Response> wins,
        int            currentPage,
        int            totalPages,
        long           totalWins
    ) {
        /**
         * @brief Convenience constructor that maps from a Spring
         *        Data Page of Bet entities.
         *
         * @param page The page returned by the repository query.
         */
        public PagedResponse(Page<Bet> page) {
            this(
                page.getContent().stream().map(Response::new).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements()
            );
        }
    }
}