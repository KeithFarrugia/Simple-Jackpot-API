package com.keithfarrugia.jackpot_service.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
@Entity
@Table(name = "Bet")
@NoArgsConstructor
@AllArgsConstructor
public class Bet {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    private UUID    id;

    private UUID    jackpotId;
    private double  betAmount;
    private String  playerAlias;
    private boolean hasWon;
    private double  winAmount;
    private Instant timestamp;

    public Bet(Bet.Request req){
        this.betAmount      = req.betAmount();
        this.jackpotId      = req.jackpotId();
        this.playerAlias    = req.playerAlias();
    }


    public record Request(
        @NotNull                    UUID    jackpotId,
        @Positive                   double  betAmount,
        @NotBlank @Size(max = 50)   String  playerAlias
    ) {}

    public record Response(double winAmount, double newSize) {}


    public static Specification<Bet> fromRequest(Win.Request req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("hasWon")));

            if (req.jackpotIds() != null && !req.jackpotIds().isEmpty()) {
                predicates.add(root.get("jackpotId").in(req.jackpotIds()));
            }

            if (req.playerAliases() != null && !req.playerAliases().isEmpty()) {
                predicates.add(root.get("playerAlias").in(req.playerAliases()));
            }

            if (req.winAmounts() != null && !req.winAmounts().isEmpty()) {
                predicates.add(root.get("winAmount").in(req.winAmounts()));
            }

            // For time ranges: any bet whose timestamp falls in ANY of the ranges (OR)
            if (req.timeRangeList() != null && !req.timeRangeList().isEmpty()) {
                List<Predicate> rangePredicates = new ArrayList<>();
                for (Win.TimeRange range : req.timeRangeList()) {
                    rangePredicates.add(
                        cb.between(root.get("timestamp"), range.start(), range.end())
                    );
                }
                predicates.add(cb.or(rangePredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
