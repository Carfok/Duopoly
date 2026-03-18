package com.duopoly.ai.simulation

import com.duopoly.ai.easy.RuleBasedStrategy
import com.duopoly.ai.medium.HeuristicStrategy
import com.duopoly.ai.hard.HardStrategy
import com.duopoly.core.domain.model.AIDifficulty
import com.duopoly.core.domain.ports.AIStrategy
import com.duopoly.core.simulation.BatchRunner
import com.duopoly.core.simulation.CsvExporter
import com.duopoly.core.simulation.SimulationConfig
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis

/**
 * Aplicación principal para la Fase 5: Evaluación Experimental y Big Data.
 *
 * Ejecuta los enfrentamientos necesarios para el TFM:
 * 1. Easy vs Medium (20,000 partidas)
 * 2. Medium vs Hard (15,000 partidas)
 * 3. Easy vs Hard (15,000 partidas)
 *
 * Total: 50,000 partidas simuladas.
 * Los resultados se exportan a la carpeta 'data/datasets/'.
 */
class ExperimentalEvaluator {

    private val strategies: Map<AIDifficulty, AIStrategy> = mapOf(
        AIDifficulty.EASY to RuleBasedStrategy(),
        AIDifficulty.MEDIUM to HeuristicStrategy(),
        AIDifficulty.HARD to HardStrategy()
    )

    private val runner = BatchRunner(strategies) { completed, total ->
        if (completed % 10 == 0) { // Log más frecuente para debug
            println("DEBUG: Partida $completed de $total finalizada...")
        }
    }

    fun runAllExperiments() {
        println("=== INICIANDO EVALUACIÓN EXPERIMENTAL (FASE 5) ===")
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val dataDir = File("data/datasets/$timestamp")
        dataDir.mkdirs()

        val experiments = listOf(
            SimulationConfig(
                totalGames = 20000,
                player1Difficulty = AIDifficulty.EASY,
                player2Difficulty = AIDifficulty.MEDIUM,
                parallelism = Runtime.getRuntime().availableProcessors()
            ),
            SimulationConfig(
                totalGames = 15000,
                player1Difficulty = AIDifficulty.MEDIUM,
                player2Difficulty = AIDifficulty.HARD,
                parallelism = Runtime.getRuntime().availableProcessors()
            ),
            SimulationConfig(
                totalGames = 15000,
                player1Difficulty = AIDifficulty.EASY,
                player2Difficulty = AIDifficulty.HARD,
                parallelism = Runtime.getRuntime().availableProcessors()
            )
        )

        var grandTotalGames = 0
        val totalTime = measureTimeMillis {
            experiments.forEachIndexed { index, config ->
                println("\nExperimento ${index + 1}/${experiments.size}: ${config.matchupLabel}")
                val result = runner.run(config)
                
                val fileName = "${config.player1Difficulty}_vs_${config.player2Difficulty}.csv".lowercase()
                val file = File(dataDir, fileName)
                val summaryFile = File(dataDir, "${config.player1Difficulty}_vs_${config.player2Difficulty}_summary.txt".lowercase())
                CsvExporter.export(result.games, file.absolutePath)
                CsvExporter.exportSummary(result.aggregate, summaryFile.absolutePath)
                
                println("Completado en ${result.aggregate.totalSimulationTimeMs}ms")
                println("Ganador P1 (${config.player1Difficulty}): ${result.aggregate.p1Wins} (${String.format("%.2f", result.aggregate.p1WinRate * 100)}%)")
                println("Ganador P2 (${config.player2Difficulty}): ${result.aggregate.p2Wins} (${String.format("%.2f", result.aggregate.p2WinRate * 100)}%)")
                println("Empates: ${result.aggregate.draws}")
                println("Resultado guardado en: ${file.path}")
                
                grandTotalGames += result.games.size
            }
        }

        println("\n=== EVALUACIÓN FINALIZADA ===")
        println("Partidas totales: $grandTotalGames")
        println("Tiempo total: ${totalTime / 1000}s")
        println("Datasets generados en: ${dataDir.absolutePath}")
    }
}

fun main() {
    ExperimentalEvaluator().runAllExperiments()
}
