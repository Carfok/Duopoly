package com.duopoly.core.domain.engine

import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.*

/**
 * Orquestador principal del juego.
 *
 * Coordina el RuleEngine con los puertos externos (IA, logging, UI).
 * Soporta tanto juego interactivo como simulación headless.
 *
 * Flujo de ejecución por turno de IA:
 *   1. TurnManager detecta turno de IA
 *   2. Se crea snapshot inmutable del estado
 *   3. AIStrategy seleccionada según dificultad
 *   4. Cómputo de decisión en background
 *   5. RuleEngine válida y aplica la acción
 *   6. Logger registra decisión + latencia
 *   7. Listeners notificados para UI
 */
class GameEngine(
    initialState: GameState,
    private val ruleEngine: RuleEngine = RuleEngine(),
    private val diceProvider: DiceProvider = DefaultDiceProvider(),
    private val aiStrategies: Map<AIDifficulty, AIStrategy> = emptyMap(),
    private val logger: GameLogger? = null,
    private val listeners: List<GameEventListener> = emptyList(),
    val gameId: String = java.util.UUID.randomUUID().toString()
) {
    /** Estado actual del juego (solo lectura externa). */
    var currentState: GameState = initialState
        private set

    /** Si el juego ha terminado. */
    val isGameOver: Boolean get() = currentState.isGameOver

    init {
        listeners.forEach { it.onGameStarted(currentState) }
        logger?.logEvent(gameId, GameEvent.GameStarted(
            1, currentState.currentPlayer.id,
            currentState.players.size, currentState.config
        ))
    }

    /** Obtiene acciones válidas para el estado actual. */
    fun getValidActions(): List<GameAction> =
        ruleEngine.getValidActions(currentState)

    /**
     * Ejecuta una acción (humana o de IA) y actualiza el estado.
     * Notifica a todos los listeners y loggers.
     */
    fun executeAction(action: GameAction): ActionResult {
        val oldState = currentState
        val result = ruleEngine.applyAction(currentState, action, diceProvider)

        when (result) {
            is ActionResult.Success -> {
                currentState = result.newState
                result.events.forEach { event ->
                    logger?.logEvent(gameId, event)
                    listeners.forEach { it.onEvent(event) }
                }
                listeners.forEach { it.onStateChanged(oldState, currentState, action) }

                if (currentState.isGameOver) {
                    val winner = currentState.activePlayers.firstOrNull()?.id
                    listeners.forEach { it.onGameOver(currentState, winner) }
                }
            }
            is ActionResult.Invalid -> {
                listeners.forEach { it.onError("Acción inválida: ${result.reason}") }
            }
        }

        return result
    }

    /**
     * Ejecuta un turno completo de IA.
     * Selecciona la estrategia según la dificultad del jugador actual.
     *
     * @return El resultado de la acción ejecutada, o null si el jugador no es IA.
     */
    fun executeAITurn(): ActionResult? {
        val player = currentState.currentPlayer
        if (!player.isAI) return null

        val difficulty = player.aiDifficulty ?: return null
        val strategy = aiStrategies[difficulty] ?: return null

        val validActions = getValidActions()
        if (validActions.isEmpty()) return null

        val startTime = System.currentTimeMillis()
        val action = strategy.decideAction(currentState, validActions)
        val decisionTime = System.currentTimeMillis() - startTime

        logger?.logAIDecision(
            gameId, currentState.turnNumber, player.id,
            difficulty, action, validActions, decisionTime, currentState
        )

        return executeAction(action)
    }

    /**
     * Ejecuta una partida completa en modo headless (IA versus IA).
     *
     * Requisitos:
     *   - Todos los jugadores deben ser IA
     *   - Todas las dificultades deben tener estrategia registrada
     *
     * @return El estado final del juego.
     * @throws IllegalStateException si algún jugador no es IA.
     */
    /**
     * Ejecuta una partida completa en modo headless (IA vs IA).
     *
     * @param maxTurns Límite de turnos para evitar bucles infinitos.
     * @return El estado final del juego.
     */
    fun runHeadless(maxTurns: Int = 1000): GameState {
        var turnCount = 0
        while (!currentState.isGameOver && turnCount < maxTurns) {
            val player = currentState.currentPlayer
            if (!player.isAI) {
                throw IllegalStateException("Modo headless requiere que todos los jugadores sean IA.")
            }

            // Ejecutar acciones hasta que el turno cambie o el juego acabe
            val currentPlayerId = player.id
            var actionCount = 0
            val maxActionsPerTurn = 100

            while (!currentState.isGameOver && actionCount < maxActionsPerTurn) {
                val result = executeAITurn()
                if (result == null || result is ActionResult.Invalid) break
                actionCount++

                // Si el jugador cambió, el turno terminó
                if (currentState.currentPlayer.id != currentPlayerId) break
            }
            turnCount++
        }

        // Si se alcanzó el límite de turnos, forzamos un ganador por patrimonio neto
        if (!currentState.isGameOver && turnCount >= maxTurns) {
            forceBankruptcyByAssetValue()
        }

        return currentState
    }

    /** Reinicia el motor con una nueva partida 1v1. */
    fun startNewGame(
        player1Name: String,
        player2Name: String,
        player2IsAI: Boolean = true,
        player2Difficulty: AIDifficulty = AIDifficulty.EASY
    ) {
        currentState = BoardFactory.createInitialGameState(
            player1Name = player1Name,
            player2Name = player2Name,
            player2IsAI = player2IsAI,
            player2Difficulty = player2Difficulty
        )
    }

    private fun forceBankruptcyByAssetValue() {
        val active = currentState.activePlayers
        val winnerId = when {
            active.size == 1 -> active.first().id
            active.isEmpty() -> null
            else -> active.maxByOrNull { currentState.calculateNetWorth(it.id) }?.id
        }
        
        // Finalizar el juego por límite de turnos
        listeners.forEach { it.onGameOver(currentState, winnerId) }
    }

    /**
     * Determina el ganador de la partida.
     * @return PlayerId del ganador, o null si es empate/juego no terminado.
     */
    fun getWinner(): PlayerId? {
        val active = currentState.activePlayers
        return when {
            active.size == 1 -> active.first().id
            active.isEmpty() -> null
            else -> active.maxByOrNull { currentState.calculateNetWorth(it.id) }?.id
        }
    }
}
