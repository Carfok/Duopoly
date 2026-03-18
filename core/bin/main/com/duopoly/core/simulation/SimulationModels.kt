package com.duopoly.core.simulation

import com.duopoly.core.domain.model.*

/**
 * Configuración de una simulación masiva.
 *
 * Define todos los parámetros necesarios para ejecutar un lote
 * de partidas IA vs IA de forma reproducible.
 *
 * @param totalGames Número total de partidas a simular
 * @param baseSeed Semilla base; cada partida usa baseSeed + gameIndex
 * @param player1Difficulty Dificultad de IA del jugador 1
 * @param player2Difficulty Dificultad de IA del jugador 2
 * @param gameConfig Parámetros del juego (balance, turnos, etc.)
 * @param parallelism Hilos para simulación paralela (1 = secuencial)
 */
data class SimulationConfig(
    val totalGames: Int = 1000,
    val baseSeed: Long = 42L,
    val player1Difficulty: AIDifficulty = AIDifficulty.EASY,
    val player2Difficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val gameConfig: GameConfig = GameConfig(),
    val parallelism: Int = 1
) {
    init {
        require(totalGames > 0) { "totalGames debe ser > 0" }
        require(parallelism > 0) { "parallelism debe ser > 0" }
    }

    /** Descripción legible del enfrentamiento. */
    val matchupLabel: String
        get() = "${player1Difficulty.name} vs ${player2Difficulty.name}"
}

/**
 * Métricas capturadas de UNA partida individual.
 *
 * Estructura plana para exportación directa a CSV.
 * Cada campo corresponde a una columna del dataset.
 */
data class GameMetrics(
    val gameId: String,
    val seed: Long,
    val p1Difficulty: AIDifficulty,
    val p2Difficulty: AIDifficulty,
    val winnerId: String?,
    val winReason: String,
    val totalTurns: Int,
    val p1FinalBalance: Int,
    val p2FinalBalance: Int,
    val p1FinalNetWorth: Int,
    val p2FinalNetWorth: Int,
    val p1PropertiesCount: Int,
    val p2PropertiesCount: Int,
    val p1Bankrupted: Boolean,
    val p2Bankrupted: Boolean,
    val gameDurationMs: Long
) {
    companion object {
        /** Cabeceras CSV para exportación. */
        val CSV_HEADERS = listOf(
            "game_id", "seed",
            "p1_difficulty", "p2_difficulty",
            "winner_id", "win_reason", "total_turns",
            "p1_final_balance", "p2_final_balance",
            "p1_final_net_worth", "p2_final_net_worth",
            "p1_properties_count", "p2_properties_count",
            "p1_bankrupted", "p2_bankrupted",
            "game_duration_ms"
        )
    }

    /** Convierte las métricas a una fila CSV. */
    fun toCsvRow(): String = listOf(
        gameId, seed,
        p1Difficulty.name, p2Difficulty.name,
        winnerId ?: "DRAW", winReason, totalTurns,
        p1FinalBalance, p2FinalBalance,
        p1FinalNetWorth, p2FinalNetWorth,
        p1PropertiesCount, p2PropertiesCount,
        p1Bankrupted, p2Bankrupted,
        gameDurationMs
    ).joinToString(",")
}

/**
 * Métricas agregadas de un lote completo de partidas.
 *
 * Calculadas por BatchRunner tras ejecutar todas las partidas.
 * Contiene toda la información necesaria para tablas comparativas.
 */
data class AggregateMetrics(
    val config: SimulationConfig,
    val totalGames: Int,
    val p1Wins: Int,
    val p2Wins: Int,
    val draws: Int,
    val p1WinRate: Double,
    val p2WinRate: Double,
    val drawRate: Double,
    val avgTurns: Double,
    val medianTurns: Int,
    val stdDevTurns: Double,
    val minTurns: Int,
    val maxTurns: Int,
    val avgP1NetWorth: Double,
    val avgP2NetWorth: Double,
    val avgP1FinalBalance: Double,
    val avgP2FinalBalance: Double,
    val p1BankruptcyRate: Double,
    val p2BankruptcyRate: Double,
    val totalSimulationTimeMs: Long,
    val avgGameTimeMs: Double,
    val gamesPerSecond: Double
) {
    /** Resumen legible para impresión rápida. */
    fun toSummary(): String = buildString {
        appendLine("═══════════════════════════════════════════")
        appendLine("  RESULTADOS: ${config.matchupLabel}")
        appendLine("═══════════════════════════════════════════")
        appendLine("  Partidas totales:    $totalGames")
        appendLine("  P1 (${config.player1Difficulty}): $p1Wins victorias (${"%.1f".format(p1WinRate * 100)}%)")
        appendLine("  P2 (${config.player2Difficulty}): $p2Wins victorias (${"%.1f".format(p2WinRate * 100)}%)")
        appendLine("  Empates:             $draws (${"%.1f".format(drawRate * 100)}%)")
        appendLine("───────────────────────────────────────────")
        appendLine("  Turnos promedio:     ${"%.1f".format(avgTurns)}")
        appendLine("  Turnos mediana:      $medianTurns")
        appendLine("  Turnos σ:            ${"%.1f".format(stdDevTurns)}")
        appendLine("  Turnos min/max:      $minTurns / $maxTurns")
        appendLine("───────────────────────────────────────────")
        appendLine("  Net Worth P1 avg:    ${"%.0f".format(avgP1NetWorth)}")
        appendLine("  Net Worth P2 avg:    ${"%.0f".format(avgP2NetWorth)}")
        appendLine("  Bancarrota P1:       ${"%.1f".format(p1BankruptcyRate * 100)}%")
        appendLine("  Bancarrota P2:       ${"%.1f".format(p2BankruptcyRate * 100)}%")
        appendLine("───────────────────────────────────────────")
        appendLine("  Tiempo total:        ${totalSimulationTimeMs}ms")
        appendLine("  Velocidad:           ${"%.0f".format(gamesPerSecond)} partidas/seg")
        appendLine("═══════════════════════════════════════════")
    }
}
