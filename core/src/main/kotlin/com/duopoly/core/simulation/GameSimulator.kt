package com.duopoly.core.simulation

import com.duopoly.core.domain.engine.BoardFactory
import com.duopoly.core.domain.engine.GameEngine
import com.duopoly.core.domain.engine.RuleEngine
import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.AIStrategy
import com.duopoly.core.domain.ports.DiceProvider
import java.util.Random

/**
 * Simulador de una partida individual IA vs IA.
 *
 * Encapsula la creación del GameEngine, la ejecución headless
 * y la extracción de métricas. Cada invocación produce un
 * GameMetrics con toda la información necesaria para el dataset.
 *
 * Diseñado para ser llamado miles de veces por BatchRunner.
 * Sin estado mutable propio — thread-safe si las strategies lo son.
 */
class GameSimulator(
    private val strategies: Map<AIDifficulty, AIStrategy>
) {

    /**
     * Ejecuta una partida IA vs IA completa y retorna las métricas.
     *
     * @param config Configuración de simulación (dificultades, params del juego)
     * @param gameIndex Índice de la partida dentro del batch (para generar gameId y seed)
     * @return Métricas de la partida
     */
    fun simulate(config: SimulationConfig, gameIndex: Int): GameMetrics {
        val seed = config.baseSeed + gameIndex
        val gameId = "sim-${config.baseSeed}-$gameIndex"

        // Crear estado inicial IA vs IA
        val initialState = BoardFactory.createAIvsAIState(
            difficulty1 = config.player1Difficulty,
            difficulty2 = config.player2Difficulty,
            config = config.gameConfig
        )

        // Proveedor de dados con semilla determinista
        val diceProvider = SeededDiceProvider(seed)

        // Motor del juego
        val engine = GameEngine(
            initialState = initialState,
            ruleEngine = RuleEngine(),
            diceProvider = diceProvider,
            aiStrategies = strategies,
            gameId = gameId
        )

        // Ejecución y medición
        val startNanos = System.nanoTime()
        val finalState = engine.runHeadless()
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000

        // Extraer métricas
        return extractMetrics(
            gameId = gameId,
            seed = seed,
            config = config,
            engine = engine,
            finalState = finalState,
            durationMs = durationMs
        )
    }

    private fun extractMetrics(
        gameId: String,
        seed: Long,
        config: SimulationConfig,
        engine: GameEngine,
        finalState: GameState,
        durationMs: Long
    ): GameMetrics {
        val p1 = finalState.players[0]
        val p2 = finalState.players[1]

        val winnerId = engine.getWinner()
        val winReason = when {
            p1.isBankrupt -> "P1_BANKRUPT"
            p2.isBankrupt -> "P2_BANKRUPT"
            finalState.turnNumber >= finalState.config.maxTurns -> "MAX_TURNS"
            else -> "UNKNOWN"
        }

        return GameMetrics(
            gameId = gameId,
            seed = seed,
            p1Difficulty = config.player1Difficulty,
            p2Difficulty = config.player2Difficulty,
            winnerId = winnerId?.value,
            winReason = winReason,
            totalTurns = finalState.turnNumber,
            p1FinalBalance = p1.balance,
            p2FinalBalance = p2.balance,
            p1FinalNetWorth = finalState.calculateNetWorth(p1.id),
            p2FinalNetWorth = finalState.calculateNetWorth(p2.id),
            p1PropertiesCount = finalState.propertiesOwnedBy(p1.id).size,
            p2PropertiesCount = finalState.propertiesOwnedBy(p2.id).size,
            p1Bankrupted = p1.isBankrupt,
            p2Bankrupted = p2.isBankrupt,
            gameDurationMs = durationMs
        )
    }
}

/**
 * Proveedor de dados con semilla determinista.
 *
 * Permite reproducir exactamente la misma secuencia de dados
 * para debug y validación. Cada partida recibe su propia semilla.
 */
class SeededDiceProvider(seed: Long) : DiceProvider {
    private val random = Random(seed)

    override fun roll(): DiceResult = DiceResult(
        die1 = random.nextInt(6) + 1,
        die2 = random.nextInt(6) + 1
    )
}
