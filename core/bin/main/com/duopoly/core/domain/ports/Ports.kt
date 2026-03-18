package com.duopoly.core.domain.ports

import com.duopoly.core.domain.model.*

// ═══════════════════════════════════════════════════════════════
// Arquitectura Hexagonal — Interfaces de Puerto
//
// Estos puertos definen los límites del dominio core.
// Los adaptadores se implementan en módulos externos:
//   :ai    → AIStrategy
//   :data  → GameLogger, GamePersistence
//   :godot-bridge → GameEventListener
// ═══════════════════════════════════════════════════════════════

/**
 * Puerto: Estrategia de IA (Puerto Conducido)
 *
 * Implementado por el módulo :ai.
 * El GameEngine lo usa para solicitar decisiones de la IA.
 */
interface AIStrategy {
    val difficulty: AIDifficulty

    /**
     * Dado el estado actual y las acciones válidas,
     * elige la mejor acción a ejecutar.
     */
    fun decideAction(state: GameState, validActions: List<GameAction>): GameAction

    /**
     * Decide el monto de puja durante una subasta.
     * Retorna 0 para retirarse.
     */
    fun decideBidAmount(state: GameState, tileIndex: Int, currentBid: Int): Int
}

/**
 * Puerto: Proveedor de Dados (Puerto Conducido)
 *
 * Permite inyectar dados deterministas para testing
 * y dados estocásticos para juego real.
 */
interface DiceProvider {
    fun roll(): DiceResult
}

/**
 * Puerto: Logger del Juego (Puerto Conducido)
 *
 * Implementado por el módulo :data.
 * Captura eventos estructurados para análisis Big Data.
 */
interface GameLogger {
    fun logEvent(gameId: String, event: GameEvent)

    fun logAIDecision(
        gameId: String,
        turnNumber: Int,
        playerId: PlayerId,
        difficulty: AIDifficulty,
        chosenAction: GameAction,
        validActions: List<GameAction>,
        decisionTimeMs: Long,
        stateSnapshot: GameState
    )

    fun flush()
}

/**
 * Puerto: Persistencia del Juego (Puerto Conducido)
 *
 * Implementado por el módulo :data para almacenamiento SQLite/Room.
 */
interface GamePersistence {
    suspend fun saveGameRecord(record: GameRecord)
    suspend fun loadGameRecord(gameId: String): GameRecord?
    suspend fun getAllRecords(): List<GameRecord>
    suspend fun getRecordsByDifficulty(difficulty: AIDifficulty): List<GameRecord>
    suspend fun exportToCsv(filePath: String)
}

/**
 * Puerto: Listener de Eventos (Puerto Conductor)
 *
 * Usado para notificar a la capa de UI (Godot) sobre cambios de estado.
 */
interface GameEventListener {
    fun onGameStarted(state: GameState)
    fun onEvent(event: GameEvent)
    fun onStateChanged(oldState: GameState, newState: GameState, action: GameAction)
    fun onGameOver(finalState: GameState, winnerId: PlayerId?)
    fun onError(message: String)
}

// ───── Objetos de Transferencia de Datos ─────

/**
 * Registro completo de una partida finalizada.
 * Almacenado por GamePersistence para análisis.
 */
data class GameRecord(
    val gameId: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val winnerId: PlayerId?,
    val winReason: String,
    val totalTurns: Int,
    val playerConfigs: List<PlayerConfig>,
    val events: List<GameEvent>,
    val finalPlayerStates: List<PlayerState>,
    val gameConfig: GameConfig
)

data class PlayerConfig(
    val id: PlayerId,
    val name: String,
    val isAI: Boolean,
    val aiDifficulty: AIDifficulty?
)

/**
 * Métricas agregadas de un lote de partidas.
 * Usado para análisis comparativo (Big Data).
 */
data class BatchMetrics(
    val totalGames: Int,
    val winRateByPlayer: Map<PlayerId, Double>,
    val averageDuration: Double,
    val averageNetWorthAtEnd: Map<PlayerId, Double>,
    val averageDecisionTimeMs: Map<AIDifficulty, Double>,
    val bankruptcyRate: Map<PlayerId, Double>,
    val drawRate: Double,
    val medianTurns: Int,
    val stdDevTurns: Double
)
