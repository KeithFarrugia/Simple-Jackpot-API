package com.keithfarrugia.jackpot_service.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @brief   Spring Data repository for Bet entities.
 *
 * @details Extends both JpaRepository for standard CRUD operations
 *          and JpaSpecificationExecutor to support dynamic filtered
 *          queries built via Bet.fromRequest(). Used primarily to
 *          save new bets and to query winning bets with filters
 *          such as jackpot ID, player alias, and time range.
 */
public interface BetRepository
        extends JpaRepository<Bet, UUID>,
                JpaSpecificationExecutor<Bet> {}