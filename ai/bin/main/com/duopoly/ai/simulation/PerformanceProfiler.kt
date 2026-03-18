package com.duopoly.ai.simulation

import com.duopoly.ai.easy.RuleBasedStrategy
import com.duopoly.ai.medium.HeuristicStrategy
import com.duopoly.ai.hard.HardStrategy
import com.duopoly.core.domain.engine.BoardFactory
import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.AIStrategy
import kotlin.system.measureNanoTime

/**
 * Herramienta de Profiling para la Fase 8.
 * Mide latencia de decisión y eficiencia de las estrategias de IA.
 * Requisito académico: Validar latencia < 200ms en todos los niveles.
 */
object PerformanceProfiler {

    @JvmStatic
    fun main(args: Array<String>) {
        runBenchmarks()
    }

    fun runBenchmarks() {
        println("\n=== DUOPOLY Fase 8: AI Performance Profiling ===")
        
        val board = BoardFactory.createStandardBoard()
        val initialState = createInitialState(board)

        val strategies: List<AIStrategy> = listOf(
            RuleBasedStrategy(),
            HeuristicStrategy(),
            HardStrategy()
        )

        strategies.forEach { strategy ->
            profileStrategy(strategy, initialState)
        }
        println("================================================\n")
    }

    private fun profileStrategy(strategy: AIStrategy, state: GameState) {
        val iterations = 50
        val warmup = 10
        val times = mutableListOf<Long>()

        val name = strategy.difficulty.name
        println("\nProfiling $name Strategy ($iterations iterations + $warmup warmup)...")

        val validActions = listOf(GameAction.RollDice(state.players[state.currentPlayerIndex].id))

        // Warmup
        repeat(warmup) {
            strategy.decideAction(state, validActions)
        }

        repeat(iterations) {
            val nanoTime = measureNanoTime {
                strategy.decideAction(state, validActions)
            }
            times.add(nanoTime)
        }

        val totalNs = times.sum().toDouble()
        val avgMs = (totalNs / iterations) / 1_000_000.0
        val maxMs = (times.maxOrNull()?.toDouble() ?: 0.0) / 1_000_000.0
        
        println("  - Latencia Media: ${"%.4f".format(avgMs)} ms")
        println("  - Latencia Máxima: ${"%.4f".format(maxMs)} ms")
        
        checkObjective(name, avgMs)
    }

    private fun checkObjective(name: String, avgMs: Double) {
        val objective = when(name) {
            "EASY" -> 10.0
            "MEDIUM" -> 50.0
            "HARD" -> 200.0
            else -> 0.0
        }
        
        if (avgMs <= objective) {
            println("  [PASS] ✅ Dentro del objetivo (< $objective ms)")
        } else {
            println("  [WARN] ⚠️ Supera el objetivo de latencia académica (> $objective ms)")
        }
    }

    private fun createInitialState(board: List<TileDefinition>): GameState {
        val players = listOf(
            PlayerState(id = PlayerId("P1"), name = "Player 1", balance = 1500, position = 0),
            PlayerState(id = PlayerId("P2"), name = "Player 2", balance = 1500, position = 0)
        )
        val initialTileStates = board.map { tile ->
            TileState(tileIndex = tile.index, ownerId = null, upgradeLevel = 0, isMortgaged = false)
        }
        
        return GameState(
            players = players,
            tileStates = initialTileStates,
            board = board,
            config = GameConfig(),
            currentPlayerIndex = 0,
            phase = GamePhase.PRE_ROLL,
            turnNumber = 1
        )
    }
}

