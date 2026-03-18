package com.duopoly.core.domain.engine

import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.DiceProvider

/**
 * Utilidades de gestión de turnos.
 *
 * Capa de conveniencia sobre RuleEngine para operaciones comunes.
 * Todas las funciones son puras (sin estado interno).
 */
class TurnManager(private val ruleEngine: RuleEngine = RuleEngine()) {

    /** Obtiene todas las acciones válidas para el estado actual. */
    fun getValidActions(state: GameState): List<GameAction> =
        ruleEngine.getValidActions(state)

    /** Ejecuta una acción y retorna el resultado. */
    fun executeAction(
        state: GameState,
        action: GameAction,
        diceProvider: DiceProvider? = null
    ): ActionResult = ruleEngine.applyAction(state, action, diceProvider)

    /** Verifica si el jugador actual es IA. */
    fun isCurrentPlayerAI(state: GameState): Boolean =
        state.currentPlayer.isAI

    /** Obtiene la dificultad de IA del jugador actual. */
    fun getCurrentAIDifficulty(state: GameState): AIDifficulty? =
        state.currentPlayer.aiDifficulty

    /** Verifica si el juego debería terminar. */
    fun shouldGameEnd(state: GameState): Boolean =
        state.activePlayers.size <= 1 || state.turnNumber > state.config.maxTurns

    /** Determina el ganador de una partida finalizada. */
    fun determineWinner(state: GameState): PlayerId? {
        if (!state.isGameOver) return null
        val active = state.activePlayers
        return when {
            active.size == 1 -> active.first().id
            active.isEmpty() -> null
            else -> active.maxByOrNull { state.calculateNetWorth(it.id) }?.id
        }
    }
}
