package com.duopoly.ai.easy

import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.AIStrategy

/**
 * Estrategia de IA basada en reglas simples (Nivel Easy).
 *
 * Refinada para Fase 4.1: Mejor control financiero y subastas.
 *
 * Reglas de decisión (Prioridad):
 *   1. Pagar multa de cárcel si balance > 3× multa.
 *   2. Comprar propiedad si balance > 2× precio o completa un grupo.
 *   3. Mejorar propiedades si balance > 4× coste (conservadora).
 *   4. Hipotecar solo si balance < 50 (emergencia).
 *   5. Pujar en subastas hasta el 70% del valor base.
 */
class RuleBasedStrategy : AIStrategy {

    override val difficulty: AIDifficulty = AIDifficulty.EASY

    override fun decideAction(state: GameState, validActions: List<GameAction>): GameAction {
        val player = state.currentPlayer

        // 1. Jail Management
        validActions.filterIsInstance<GameAction.PayJailFine>().firstOrNull()?.let {
            if (player.balance >= state.config.jailFine * 3) return it
        }

        // 2. Strategic Buying
        validActions.filterIsInstance<GameAction.BuyProperty>().firstOrNull()?.let { action ->
            val tileDef = state.tileDefAt(player.position)
            val completesGroup = state.board.filter { it.group?.id == tileDef.group?.id }
                .all { it.index == tileDef.index || state.tileStateAt(it.index).ownerId == player.id }
            
            if (player.balance >= tileDef.purchasePrice * 2 || completesGroup) return action
        }

        // 3. Conservative Upgrading
        validActions.filterIsInstance<GameAction.BuildUpgrade>().firstOrNull()?.let { action ->
            val tileDef = state.tileDefAt(action.tileIndex)
            if (player.balance >= tileDef.upgradeCost * 4) return action
        }

        // 4. Survival: Mortgage only if critical
        if (player.balance < 50) {
            validActions.filterIsInstance<GameAction.MortgageProperty>().firstOrNull()?.let {
                return it
            }
        }

        // 5. Basic Flow
        validActions.filterIsInstance<GameAction.RollDice>().firstOrNull()?.let { return it }
        validActions.filterIsInstance<GameAction.EndTurn>().firstOrNull()?.let { return it }
        validActions.filterIsInstance<GameAction.DeclineProperty>().firstOrNull()?.let { return it }
        validActions.filterIsInstance<GameAction.WithdrawFromAuction>().firstOrNull()?.let { return it }

        return validActions.first()
    }

    override fun decideBidAmount(state: GameState, tileIndex: Int, currentBid: Int): Int {
        val tileDef = state.tileDefAt(tileIndex)
        val player = state.currentPlayer

        // Puja máxima: 70% del precio base (Easy es tacaña/conservadora)
        val maxBid = (tileDef.purchasePrice * 0.7).toInt()
        val nextBid = currentBid + state.config.auctionMinimumBid

        return if (nextBid <= maxBid && player.balance >= nextBid) {
            nextBid
        } else {
            0 
        }
    }
}

