package com.keithfarrugia.jackpot_service.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "BetIdempotencyRecord")
@NoArgsConstructor
@AllArgsConstructor
public class BetIdempotencyRecord {

    @Id
    private UUID    idempotencyKey;
    private UUID    betId;
}