package com.keithfarrugia.jackpot_service.model;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief   JPA entity representing a jackpot game.
 *
 * @details Tracks the current pot size, win probability, number of
 *          wins recorded, and the timestamp of the last win. The
 *          pot grows with each losing bet and resets to zero when
 *          a player wins. Contains nested DTOs for inbound requests
 *          and outbound responses.
 */
@Data
@Entity
@Table(name = "Jackpot")
@NoArgsConstructor
@AllArgsConstructor
public class Jackpot {

    /** @brief Unique identifier for this jackpot, auto-generated. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID    id;

    /** @brief Display name of the jackpot. Must be unique. */
    private String  name;

    /**
     * @brief Probability of winning on any single bet.
     *        Must be a value between 0 (exclusive) and 1 (inclusive).
     */
    private float   winProbability;

    /**
     * @brief Current accumulated size of the jackpot pool.
     *        Grows with each losing bet and resets to zero on a win.
     */
    private double  currentSize;

    /** @brief Total number of times this jackpot has been won. */
    private long    numWins;

    /**
     * @brief Timestamp of the most recent win.
     *        Null if the jackpot has never been won.
     */
    private Instant lastWin;

    /* ======================================================================== */ 

    /**
     * @brief   Constructs a new Jackpot from a creation request.
     *
     * @details Initialises the pot at zero with no wins recorded.
     *          The ID is left null and will be assigned by JPA on
     *          first save.
     *
     * @param jr The validated jackpot creation request.
     */
    public Jackpot(Request jr) {
        this.name           = jr.name();
        this.winProbability = jr.winProbability();
        this.currentSize    = 0.0;
        this.numWins        = 0;
        this.lastWin        = null;
    }

    /* ======================================================================== */ 

    /**
     * @brief Inbound DTO for creating or updating a jackpot.
     *
     * @param name           Display name. Not blank, max 100 chars.
     * @param winProbability Win chance per bet. Must be in (0, 1].
     */

    @Schema(name = "JackpotRequest")
    public record Request(
        @NotBlank @Size(max = 100) String name,
        @Positive @Max(1)          float  winProbability
    ) {}

    /**
     * @brief Outbound DTO returned when listing jackpots.
     *
     * @details Intentionally omits internal fields like name and
     *          win probability, exposing only the data relevant to
     *          a player viewing the current state of a jackpot.
     *
     * @param id          Unique identifier of the jackpot.
     * @param currentSize Current pot size in currency units.
     * @param numWins     Total number of times the pot was won.
     * @param lastWin     Timestamp of the most recent win, or null.
     */

    @Schema(name = "JackpotResponse")
    public record Response(
        UUID    id,
        double  currentSize,
        long    numWins,
        Instant lastWin,
        double  winProbability,
        String  name
    ) {
        /**
         * @brief Convenience constructor that maps from a Jackpot
         *        entity directly.
         *
         * @param j The jackpot entity to convert.
         */
        public Response(Jackpot j) {
            this(
                j.getId(),              j.getCurrentSize(),
                j.getNumWins(),         j.getLastWin(),
                j.getWinProbability(),  j.getName()
            );
        }
    }
}