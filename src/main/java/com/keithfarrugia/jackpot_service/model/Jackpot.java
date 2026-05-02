package com.keithfarrugia.jackpot_service.model;


import lombok.*;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
@Entity
@Table(name = "Jackpot")
@NoArgsConstructor
@AllArgsConstructor
public class Jackpot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) 
    private UUID    id;


    private String  name;
    private float   winProbability;
    private double  currentSize;
    private long    numWins;
    private Instant lastWin;



    
    public record Request(
        @NotBlank @Size(max = 100)  String  name,
        @Positive @Max(1)           float   winProbability
    ) {}

    public record Response(
        UUID    id,
        double  currentSize,
        long    numWins,
        Instant lastWin
    ) {
        public Response(Jackpot j){
            this(
                j.getId()       , j.getCurrentSize(),
                j.getNumWins()  , j.getLastWin()
            );
        }
    }

    public Jackpot(Request jr){
        this.name           = jr.name();
        this.winProbability = jr.winProbability();
        this.currentSize    = 0.0f;
        this.numWins        = 0;
        this.lastWin        = null;
    }

}