package com.duopoly.ai.hard

import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.AIStrategy
import com.duopoly.core.domain.engine.RuleEngine
import com.duopoly.core.domain.engine.DefaultDiceProvider
import kotlin.math.*

/**
 * Estrategia de IA de Nivel Alto (Hard).
 *
 * Implementa MCTS (Monte Carlo Tree Search) con UCB1 para exploración de acciones.
 * Realiza simulaciones (rollouts) para estimar el valor del estado.
 * 
 * Configurada para Phase 4.3: Preparada para integrar TFLite en la evaluación de nodos.
 */
class HardStrategy(
    private val ruleEngine: RuleEngine = RuleEngine(),
    private val diceProvider: DefaultDiceProvider = DefaultDiceProvider(),
    private var iterations: Int = 100 // Ajustable dinámicamente según latencia
) : AIStrategy {

    override val difficulty: AIDifficulty = AIDifficulty.HARD

    override fun decideAction(state: GameState, validActions: List<GameAction>): GameAction {
        if (validActions.size <= 1) return validActions.firstOrNull() ?: GameAction.EndTurn(state.players[state.currentPlayerIndex].id)

        val root = MCTSNode(state, null, null, ruleEngine)
        val startTime = System.currentTimeMillis()
        val timeLimit = 150 // Límite estricto de 150ms para mantener < 200ms total
        
        var completedIterations = 0
        while (System.currentTimeMillis() - startTime < timeLimit && completedIterations < iterations) {
            val node = select(root)
            val result = simulate(node)
            backpropagate(node, result)
            completedIterations++
        }

        return root.children.maxByOrNull { it.visits }?.action ?: validActions.first()
    }

    private fun select(node: MCTSNode): MCTSNode {
        var current = node
        while (!current.state.isGameOver) {
            if (current.untriedActions.isNotEmpty()) {
                return expand(current)
            }
            current = current.children.maxByOrNull { calculateUCB1(it) } ?: break
        }
        return current
    }

    private fun expand(node: MCTSNode): MCTSNode {
        val action = node.untriedActions.removeAt(0)
        val result = ruleEngine.applyAction(node.state, action, diceProvider)
        
        val newState = if (result is ActionResult.Success) result.newState else node.state
        val child = MCTSNode(newState, action, node, ruleEngine)
        node.children.add(child)
        return child
    }

    private fun simulate(node: MCTSNode): Double {
        var currentState = node.state
        var depth = 0
        val maxSimDepth = 30 // Límite de simulación rápida
        
        // rollout rápido (política aleatoria)
        while (!currentState.isGameOver && depth < maxSimDepth) {
            val actions = ruleEngine.getValidActions(currentState)
            if (actions.isEmpty()) break
            val randomAction = actions.random()
            val result = ruleEngine.applyAction(currentState, randomAction, diceProvider)
            if (result is ActionResult.Success) {
                currentState = result.newState
            }
            depth++
        }

        // Evaluar resultado (victoria/derrota o ventaja económica)
        val pId = node.parent?.state?.currentPlayer?.id ?: return 0.0
        val player = currentState.playerById(pId) ?: return 0.0
        
        if (player.isBankrupt) return 0.0
        val winner = currentState.activePlayers.size == 1 && currentState.activePlayers.first().id == pId
        if (winner) return 1.0

        // Ratio de patrimonio como proxy de valor
        val totalNetWorth = currentState.activePlayers.sumOf { currentState.calculateNetWorth(it.id) }
        return if (totalNetWorth > 0) currentState.calculateNetWorth(pId).toDouble() / totalNetWorth else 0.5
    }

    private fun backpropagate(node: MCTSNode, result: Double) {
        var current: MCTSNode? = node
        while (current != null) {
            current.visits++
            current.value += result
            current = current.parent
        }
    }

    private fun calculateUCB1(node: MCTSNode): Double {
        if (node.visits == 0) return Double.POSITIVE_INFINITY
        val parentVisits = node.parent?.visits ?: 1
        return (node.value / node.visits) + sqrt(2.0 * ln(parentVisits.toDouble()) / node.visits)
    }

    override fun decideBidAmount(state: GameState, tileIndex: Int, currentBid: Int): Int {
        val tile = state.tileDefAt(tileIndex)
        // Hard puja agresivo si ve valor estratégico (hasta el 130% si es el último del grupo)
        val multiplier = if (wouldCompleteGroup(state, tile)) 1.3 else 1.0
        val maxBid = (tile.purchasePrice * multiplier).toInt()
        
        val nextBid = currentBid + state.config.auctionMinimumBid
        return if (nextBid <= maxBid && state.currentPlayer.balance >= nextBid) nextBid else 0
    }

    private fun wouldCompleteGroup(state: GameState, tile: TileDefinition): Boolean {
        val group = tile.group ?: return false
        val ownedInGroup = state.tileStates.count { 
            val def = state.tileDefAt(it.tileIndex)
            def.group?.id == group.id && it.ownerId == state.currentPlayer.id 
        }
        return ownedInGroup == group.size - 1
    }

    private class MCTSNode(
        val state: GameState,
        val action: GameAction?,
        val parent: MCTSNode?,
        private val ruleEngine: RuleEngine
    ) {
        var visits: Int = 0
        var value: Double = 0.0
        val children = mutableListOf<MCTSNode>()
        val untriedActions: MutableList<GameAction> by lazy {
             ruleEngine.getValidActions(state).toMutableList()
        }
    }
}
