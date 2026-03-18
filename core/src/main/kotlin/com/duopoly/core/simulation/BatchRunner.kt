package com.duopoly.core.simulation

import com.duopoly.core.domain.model.AIDifficulty
import com.duopoly.core.domain.ports.AIStrategy
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * Ejecutor de lotes de simulaciones masivas.
 *
 * Orquesta N partidas IA vs IA, recopila métricas individuales
 * y calcula estadísticas agregadas. Soporta ejecución secuencial
 * y paralela mediante coroutines.
 *
 * Uso típico:
 * ```
 * val runner = BatchRunner(strategies)
 * val result = runner.run(SimulationConfig(totalGames = 10000))
 * println(result.aggregate.toSummary())
 * CsvExporter.export(result.games, "results.csv")
 * ```
 */
class BatchRunner(
    strategies: Map<AIDifficulty, AIStrategy>,
    private val onProgress: ((completed: Int, total: Int) -> Unit)? = null
) {
    private val simulator = GameSimulator(strategies)

    /**
     * Resultado completo de un batch de simulaciones.
     */
    data class BatchResult(
        val config: SimulationConfig,
        val games: List<GameMetrics>,
        val aggregate: AggregateMetrics
    )

    /**
     * Ejecuta el lote completo de simulaciones.
     *
     * @param config Configuración del batch
     * @return Resultado con métricas individuales y agregadas
     */
    fun run(config: SimulationConfig): BatchResult {
        val startTime = System.nanoTime()

        val results: List<GameMetrics> = if (config.parallelism <= 1) {
            runSequential(config)
        } else {
            runParallel(config)
        }

        val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000

        val aggregate = computeAggregate(config, results, totalTimeMs)

        return BatchResult(
            config = config,
            games = results,
            aggregate = aggregate
        )
    }

    private fun runSequential(config: SimulationConfig): List<GameMetrics> {
        return (0 until config.totalGames).map { i ->
            val metrics = simulator.simulate(config, i)
            onProgress?.invoke(i + 1, config.totalGames)
            metrics
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runParallel(config: SimulationConfig): List<GameMetrics> {
        return runBlocking {
            val dispatcher = Dispatchers.Default.limitedParallelism(config.parallelism)
            val completed = java.util.concurrent.atomic.AtomicInteger(0)

            (0 until config.totalGames).map { i ->
                async(dispatcher) {
                    val metrics = simulator.simulate(config, i)
                    val done = completed.incrementAndGet()
                    onProgress?.invoke(done, config.totalGames)
                    metrics
                }
            }.awaitAll()
        }
    }

    /**
     * Calcula métricas agregadas del dataset completo.
     *
     * Incluye: win rates, medias, medianas, desviaciones estándar,
     * tasas de bancarrota, y rendimiento de simulación.
     */
    private fun computeAggregate(
        config: SimulationConfig,
        games: List<GameMetrics>,
        totalTimeMs: Long
    ): AggregateMetrics {
        val n = games.size
        if (n == 0) return emptyAggregate(config, totalTimeMs)

        // Victorias
        val p1Wins = games.count { it.winnerId == "p1" }
        val p2Wins = games.count { it.winnerId == "p2" }
        val draws = n - p1Wins - p2Wins

        // Turnos
        val turnsList = games.map { it.totalTurns }
        val avgTurns = turnsList.average()
        val medianTurns = median(turnsList)
        val stdDevTurns = stdDev(turnsList, avgTurns)

        // Net worth
        val avgP1NW = games.map { it.p1FinalNetWorth.toDouble() }.average()
        val avgP2NW = games.map { it.p2FinalNetWorth.toDouble() }.average()

        // Balance final
        val avgP1Balance = games.map { it.p1FinalBalance.toDouble() }.average()
        val avgP2Balance = games.map { it.p2FinalBalance.toDouble() }.average()

        // Bancarrota
        val p1Bankruptcies = games.count { it.p1Bankrupted }
        val p2Bankruptcies = games.count { it.p2Bankrupted }

        // Tiempo promedio por partida
        val avgGameTime = games.map { it.gameDurationMs.toDouble() }.average()

        return AggregateMetrics(
            config = config,
            totalGames = n,
            p1Wins = p1Wins,
            p2Wins = p2Wins,
            draws = draws,
            p1WinRate = p1Wins.toDouble() / n,
            p2WinRate = p2Wins.toDouble() / n,
            drawRate = draws.toDouble() / n,
            avgTurns = avgTurns,
            medianTurns = medianTurns,
            stdDevTurns = stdDevTurns,
            minTurns = turnsList.min(),
            maxTurns = turnsList.max(),
            avgP1NetWorth = avgP1NW,
            avgP2NetWorth = avgP2NW,
            avgP1FinalBalance = avgP1Balance,
            avgP2FinalBalance = avgP2Balance,
            p1BankruptcyRate = p1Bankruptcies.toDouble() / n,
            p2BankruptcyRate = p2Bankruptcies.toDouble() / n,
            totalSimulationTimeMs = totalTimeMs,
            avgGameTimeMs = avgGameTime,
            gamesPerSecond = if (totalTimeMs > 0) n * 1000.0 / totalTimeMs else 0.0
        )
    }

    // ── Funciones estadísticas ──

    private fun median(values: List<Int>): Int {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    private fun stdDev(values: List<Int>, mean: Double): Double {
        if (values.size < 2) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    private fun emptyAggregate(config: SimulationConfig, totalTimeMs: Long): AggregateMetrics =
        AggregateMetrics(
            config = config,
            totalGames = 0,
            p1Wins = 0, p2Wins = 0, draws = 0,
            p1WinRate = 0.0, p2WinRate = 0.0, drawRate = 0.0,
            avgTurns = 0.0, medianTurns = 0, stdDevTurns = 0.0,
            minTurns = 0, maxTurns = 0,
            avgP1NetWorth = 0.0, avgP2NetWorth = 0.0,
            avgP1FinalBalance = 0.0, avgP2FinalBalance = 0.0,
            p1BankruptcyRate = 0.0, p2BankruptcyRate = 0.0,
            totalSimulationTimeMs = totalTimeMs,
            avgGameTimeMs = 0.0,
            gamesPerSecond = 0.0
        )
}
