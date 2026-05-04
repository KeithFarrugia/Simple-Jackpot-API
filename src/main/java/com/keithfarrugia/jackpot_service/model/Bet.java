package com.keithfarrugia.jackpot_service.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief   JPA entity representing a single bet placed on a jackpot.
 *
 * @details Stores the outcome of each bet, whether the player won,
 *          how much they won, and when the bet was placed. Also
 *          contains nested DTOs for requests and responses, and a
 *          JPA Specification builder for filtered win queries.
 */
@Data
@Entity
@Table(name = "Bet")
@NoArgsConstructor
@AllArgsConstructor
public class Bet {

    /** @brief Unique identifier for this bet, auto-generated. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID    id;

    /** @brief ID of the jackpot this bet was placed on. */
    private UUID    jackpotId;

    /** @brief Amount wagered by the player. */
    private double  betAmount;

    /** @brief Display name of the player who placed the bet. */
    private String  playerAlias;

    /** @brief Whether this bet resulted in a jackpot win. */
    private boolean hasWon;

    /**
     * @brief Amount won by the player.
     *        Zero if the bet did not result in a win.
     */
    private double  winAmount;

    /** @brief Timestamp of when the bet was placed. */
    private Instant timestamp;

    /* ======================================================================== */ 

    /**
     * @brief   Constructs a Bet entity from an incoming request.
     *
     * @details Win-related fields are left at their defaults and must be set 
     * explicitly after the outcome is known.
     *
     * @param req The validated bet request record.
     */
    public Bet(Bet.Request req) {
        this.betAmount   = req.betAmount();
        this.jackpotId   = req.jackpotId();
        this.playerAlias = req.playerAlias();
    }

    /* ======================================================================== */ 

    /**
     * @brief Inbound DTO carrying the data needed to place a bet.
     *
     * @param jackpotId   ID of the target jackpot. Must not be null.
     * @param betAmount   Amount to wager. Must be positive.
     * @param playerAlias Display name of the player.
     *                    Must not be blank, max 50 characters.
     */
    @Schema(name = "BetRequest")
    public record Request(
        @NotNull                  UUID   jackpotId,
        @Positive                 double betAmount,
        @NotBlank @Size(max = 50) String playerAlias
    ) {}

    /**
     * @brief Outbound DTO returned after a bet is processed.
     *
     * @param winAmount Amount won. Zero if the player lost.
     * @param newSize   Current jackpot size after the bet.
     */

    @Schema(name = "BetResponse")
    public record Response(double winAmount, double newSize) {}

    /* ======================================================================== */ 

    /**
     * @brief   Builds a JPA Specification to query winning bets
     *          with optional filters.
     *
     * @details Always restricts results to bets where hasWon is
     *          true. Additional filters are applied only when the
     *          corresponding field in the request is non-empty.
     *          Multiple time ranges are combined with OR so that
     *          any matching range qualifies a result.
     *
     * @param req The win query request containing filter criteria.
     * @return    A Specification that can be passed to findAll().
     */
    public static Specification<Bet> fromRequest(Win.Request req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter to winning bets only
            predicates.add(cb.isTrue(root.get("hasWon")));

            if (req.jackpotIds() != null
                    && !req.jackpotIds().isEmpty()) {
                predicates.add(
                    root.get("jackpotId").in(req.jackpotIds())
                );
            }

            if (req.playerAliases() != null
                    && !req.playerAliases().isEmpty()) {
                predicates.add(
                    root.get("playerAlias").in(req.playerAliases())
                );
            }

            if (req.winAmounts() != null
                    && !req.winAmounts().isEmpty()) {
                predicates.add(
                    root.get("winAmount").in(req.winAmounts())
                );
            }

            // A bet qualifies if it falls in ANY of the ranges
            if (req.timeRangeList() != null
                    && !req.timeRangeList().isEmpty()) {
                List<Predicate> rangePredicates = new ArrayList<>();
                for (Win.TimeRange range : req.timeRangeList()) {
                    rangePredicates.add(cb.between(
                        root.get("timestamp"),
                        range.start(),
                        range.end()
                    ));
                }
                predicates.add(
                    cb.or(rangePredicates.toArray(new Predicate[0]))
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}