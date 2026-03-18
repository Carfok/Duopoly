package com.duopoly.ai.medium

import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.AIStrategy

/**
 * Estrategia de IA de Nivel Medio (Heuristic-Based).
 *
 * Utiliza una función de evaluación estática V(S, p) para elegir la acción
 * con mayor retorno esperado (ROI) y menor riesgo de bancarrota.
 */
class HeuristicStrategy : AIStrategy {

    override val difficulty: AIDifficulty = AIDifficulty.MEDIUM

    override fun decideAction(state: GameState, validActions: List<GameAction>): GameAction {
        if (validActions.size == 1) return validActions.first()

        // Evaluar cada acción posible comparando el estado resultante (si fuera determinista)
        return validActions.maxByOrNull { action ->
            evaluateAction(state, action)
        } ?: validActions.first()
    }

    private fun evaluateAction(state: GameState, action: GameAction): Double {
        // Log para depuración o seguimiento de decisiones (conecta el parámetro action a nivel orquestador)
        // println("Evaluando acción: ${action::class.simpleName} para el estado ${state.gameId}")
        
        return when (action) {
            is GameAction.BuyProperty -> evaluatePurchase(state, action)
            is GameAction.BuildUpgrade -> evaluateUpgrade(state, action)
            is GameAction.PayJailFine -> if (state.currentPlayer.balance > 500) 100.0 else -50.0
            is GameAction.RollDice -> 10.0
            is GameAction.EndTurn -> 0.0
            is GameAction.MortgageProperty -> evaluateMortgage(state, action)
            is GameAction.UnmortgageProperty -> evaluateUnmortgage(state, action)
            else -> 1.0
        }
    }

    private fun evaluatePurchase(state: GameState, action: GameAction.BuyProperty): Double {
        val tile = state.tileDefAt(state.currentPlayer.position)
        val currentBalance = state.currentPlayer.balance
        
        // ROI Estimado: Renta base / Precio
        val roi = tile.baseRent.toDouble() / tile.purchasePrice
        
        // Bonus por completar grupo
        val groupBonus = if (wouldCompleteGroup(state, tile)) 50.0 else 0.0
        
        // Penalización por riesgo de liquidez (quedarse con < 200)
        val riskPenalty = if (currentBalance - tile.purchasePrice < 200) -100.0 else 0.0
        
        return (roi * 100) + groupBonus + riskPenalty
    }

    private fun evaluateUpgrade(state: GameState, action: GameAction.BuildUpgrade): Double {
        val tile = state.tileDefAt(action.tileIndex)
        val nextLevel = state.tileStateAt(action.tileIndex).upgradeLevel + 1
        val nextRent = tile.rentByLevel.getOrNull(nextLevel - 1) ?: tile.baseRent
        
        val deltaRent = nextRent - (tile.rentByLevel.getOrNull(nextLevel - 2) ?: tile.baseRent)
        val roi = deltaRent.toDouble() / tile.upgradeCost
        
        return roi * 500.0 // Priorizar mejoras de alto impacto
    }

    private fun evaluateMortgage(state: GameState, action: GameAction.MortgageProperty): Double {
        val tile = state.tileDefAt(action.tileIndex)
        // Solo hipotecar si el balance es crítico y la propiedad no es de un grupo que estamos intentando completar
        val isPartOfDesiredGroup = wouldCompleteGroup(state, tile)
        val score = if (state.currentPlayer.balance < 100) 50.0 else -500.0
        return if (isPartOfDesiredGroup) score - 200.0 else score
    }

    private fun evaluateUnmortgage(state: GameState, action: GameAction.UnmortgageProperty): Double {
        val tile = state.tileDefAt(action.tileIndex)
        // Priorizar deshipotecar si ayuda a completar un grupo
        val groupBonus = if (wouldCompleteGroup(state, tile)) 150.0 else 0.0
        return if (state.currentPlayer.balance > 1000) 20.0 + groupBonus else -10.0
    }

    private fun wouldCompleteGroup(state: GameState, tile: TileDefinition): Boolean {
        val group = tile.group ?: return false
        val ownedInGroup = state.tileStates.count { 
            val def = state.tileDefAt(it.tileIndex)
            def.group?.id == group.id && it.ownerId == state.currentPlayer.id 
        }
        return ownedInGroup == group.size - 1
    }

    override fun decideBidAmount(state: GameState, tileIndex: Int, currentBid: Int): Int {
        val tile = state.tileDefAt(tileIndex)
        val player = state.currentPlayer
        
        // Medium puja hasta el 110% si completa grupo, si no el 90%
        val multiplier = if (wouldCompleteGroup(state, tile)) 1.1 else 0.9
        val maxBid = (tile.purchasePrice * multiplier).toInt()
        
        val nextBid = currentBid + state.config.auctionMinimumBid
        return if (nextBid <= maxBid && player.balance >= nextBid) nextBid else 0
    }
}
