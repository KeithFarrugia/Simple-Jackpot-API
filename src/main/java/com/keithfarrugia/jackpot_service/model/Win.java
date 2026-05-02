package com.keithfarrugia.jackpot_service.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

public class Win {

    
    public record TimeRange(
        Instant start, 
        Instant end
    ) {}
    


    public record Request (
        List<UUID>      jackpotIds,
        List<Double>    winAmounts,
        List<String>    playerAliases,
        List<TimeRange> timeRangeList
    ){ }


    public record Response (
        double  betAmount,
        String  playerAlias,
        boolean hasWon,
        double  winAmount
    ){
        public Response(Bet bet) {
            this(
                bet.getBetAmount(), 
                bet.getPlayerAlias(), 
                bet.isHasWon(), 
                bet.getWinAmount()
            );
        }
    }


    public record PagedResponse(
        List<Response> wins,
        int            currentPage,
        int            totalPages,
        long           totalWins
    ) {
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
