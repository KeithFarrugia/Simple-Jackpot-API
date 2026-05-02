package com.keithfarrugia.jackpot_service.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BetRepository extends JpaRepository<Bet, UUID>, JpaSpecificationExecutor<Bet> {
}