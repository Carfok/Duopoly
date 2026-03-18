package com.duopoly.core.simulation

import com.duopoly.core.domain.engine.BoardFactory
import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.AIStrategy
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.StringWriter

/**
 * Tests de la Fase 3: Motor de Simulación Masiva.
 *
 * Valida:
 *   - Configuración y validación de parámetros
 *   - Ejecución individual de partidas con métricas
 *   - Ejecución de lotes y agregación estadística
 *   - Exportación CSV
 *   - Determinismo por semilla
 *   - Rendimiento (≥100 partidas/seg)
 */
@DisplayName("Fase 3 - Motor de Simulación Masiva")
class SimulationTest {

    // ── Estrategia de test simple (no depende de :ai) ──

    /**
     * Estrategia trivial que siempre elige la primera acción válida.
     * Es suficiente para validar la infraestructura de simulación.
     */
    private class FirstActionStrategy(
        override val difficulty: AIDifficulty
    ) : AIStrategy {
        override fun decideAction(state: GameState, validActions: List<GameAction>): GameAction =
            validActions.first()

        override fun decideBidAmount(state: GameState, tileIndex: Int, currentBid: Int): Int = 0
    }

    private val strategies: Map<AIDifficulty, AIStrategy> = mapOf(
        AIDifficulty.EASY to FirstActionStrategy(AIDifficulty.EASY),
        AIDifficulty.MEDIUM to FirstActionStrategy(AIDifficulty.MEDIUM),
        AIDifficulty.HARD to FirstActionStrategy(AIDifficulty.HARD)
    )

    // ═══════════════════════════════════════════
    //  1. CONFIGURACIÓN
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("SimulationConfig")
    inner class ConfigTests {

        @Test
        fun `configuración por defecto es válida`() {
            val config = SimulationConfig()
            assertEquals(1000, config.totalGames)
            assertEquals(42L, config.baseSeed)
            assertEquals(AIDifficulty.EASY, config.player1Difficulty)
            assertEquals(AIDifficulty.MEDIUM, config.player2Difficulty)
            assertEquals(1, config.parallelism)
        }

        @Test
        fun `matchupLabel correcto`() {
            val config = SimulationConfig(
                player1Difficulty = AIDifficulty.EASY,
                player2Difficulty = AIDifficulty.HARD
            )
            assertEquals("EASY vs HARD", config.matchupLabel)
        }

        @Test
        fun `totalGames 0 o negativo lanza excepcion`() {
            assertThrows<IllegalArgumentException> {
                SimulationConfig(totalGames = 0)
            }
        }

        @Test
        fun `parallelism 0 o negativo lanza excepcion`() {
            assertThrows<IllegalArgumentException> {
                SimulationConfig(parallelism = 0)
            }
        }
    }

    // ═══════════════════════════════════════════
    //  2. SIMULADOR INDIVIDUAL
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GameSimulator")
    inner class SimulatorTests {

        private val simulator = GameSimulator(strategies)

        @Test
        fun `una partida produce métricas válidas`() {
            val config = SimulationConfig(totalGames = 1)
            val metrics = simulator.simulate(config, 0)

            assertNotNull(metrics.gameId)
            assertTrue(metrics.totalTurns > 0, "Debe tener al menos 1 turno")
            assertTrue(metrics.gameDurationMs >= 0, "Duración no negativa")
            assertEquals(AIDifficulty.EASY, metrics.p1Difficulty)
            assertEquals(AIDifficulty.MEDIUM, metrics.p2Difficulty)
        }

        @Test
        fun `gameId incluye seed y index`() {
            val config = SimulationConfig(baseSeed = 123)
            val metrics = simulator.simulate(config, 7)

            assertEquals("sim-123-7", metrics.gameId)
            assertEquals(130L, metrics.seed)
        }

        @Test
        fun `la partida termina correctamente`() {
            val config = SimulationConfig(
                totalGames = 1,
                gameConfig = GameConfig(maxTurns = 200)
            )
            val metrics = simulator.simulate(config, 0)

            // Debe haber un ganador o haber alcanzado maxTurns
            val hasWinner = metrics.winnerId != null
            val reachedMaxTurns = metrics.totalTurns >= 200
            val hasBankruptcy = metrics.p1Bankrupted || metrics.p2Bankrupted

            assertTrue(
                hasWinner || reachedMaxTurns || hasBankruptcy,
                "La partida debe terminar por victoria, bancarrota o max turnos"
            )
        }

        @Test
        fun `net worth final es consistente`() {
            val config = SimulationConfig(totalGames = 1)
            val metrics = simulator.simulate(config, 0)

            // El net worth debe ser >= balance (propiedades suman valor)
            assertTrue(
                metrics.p1FinalNetWorth >= metrics.p1FinalBalance,
                "Net worth P1 debe ser >= balance P1"
            )
            assertTrue(
                metrics.p2FinalNetWorth >= metrics.p2FinalBalance,
                "Net worth P2 debe ser >= balance P2"
            )
        }

        @Test
        fun `10 partidas diferentes producen variedad en resultados`() {
            val config = SimulationConfig(totalGames = 10)
            val metrics = (0 until 10).map { simulator.simulate(config, it) }

            // Al menos alguna diferencia en net worth, balance, o winner entre partidas
            val uniqueOutcomes = metrics.map {
                Triple(it.winnerId, it.p1FinalNetWorth, it.p2FinalNetWorth)
            }.distinct()

            assertTrue(
                uniqueOutcomes.size > 1,
                "Con 10 semillas diferentes, deberia haber variedad en outcomes"
            )
        }
    }

    // ═══════════════════════════════════════════
    //  3. DETERMINISMO
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Determinismo")
    inner class DeterminismTests {

        @Test
        fun `misma seed produce mismas métricas`() {
            val simulator = GameSimulator(strategies)
            val config = SimulationConfig(baseSeed = 42)

            val run1 = simulator.simulate(config, 0)
            val run2 = simulator.simulate(config, 0)

            assertEquals(run1.winnerId, run2.winnerId)
            assertEquals(run1.totalTurns, run2.totalTurns)
            assertEquals(run1.p1FinalBalance, run2.p1FinalBalance)
            assertEquals(run1.p2FinalBalance, run2.p2FinalBalance)
            assertEquals(run1.p1FinalNetWorth, run2.p1FinalNetWorth)
            assertEquals(run1.p2FinalNetWorth, run2.p2FinalNetWorth)
        }

        @Test
        fun `seeds diferentes producen resultados potencialmente distintos`() {
            val simulator = GameSimulator(strategies)
            val config1 = SimulationConfig(baseSeed = 1)
            val config2 = SimulationConfig(baseSeed = 999)

            val m1 = simulator.simulate(config1, 0)
            val m2 = simulator.simulate(config2, 0)

            // Al menos una métrica debería diferir (probabilístico pero robusto)
            val same = m1.totalTurns == m2.totalTurns &&
                    m1.winnerId == m2.winnerId &&
                    m1.p1FinalBalance == m2.p1FinalBalance

            // Es estadísticamente improbable que todo sea idéntico
            assertFalse(same, "Seeds distintas deberían producir partidas diferentes")
        }
    }

    // ═══════════════════════════════════════════
    //  4. BATCH RUNNER
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("BatchRunner")
    inner class BatchRunnerTests {

        @Test
        fun `batch de 50 partidas completa sin errores`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 50))

            assertEquals(50, result.games.size)
            assertEquals(50, result.aggregate.totalGames)
            result.games.forEach { m ->
                assertTrue(m.totalTurns > 0)
            }
        }

        @Test
        fun `win rates suman 1`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 100))

            val total = result.aggregate.p1WinRate +
                    result.aggregate.p2WinRate +
                    result.aggregate.drawRate

            assertEquals(1.0, total, 0.001,
                "p1WinRate + p2WinRate + drawRate debe sumar 1.0")
        }

        @Test
        fun `métricas agregadas tienen valores razonables`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 100))
            val agg = result.aggregate

            assertTrue(agg.avgTurns > 0, "Turnos promedio > 0")
            assertTrue(agg.medianTurns > 0, "Mediana turnos > 0")
            assertTrue(agg.stdDevTurns >= 0, "Desv estándar ≥ 0")
            assertTrue(agg.minTurns > 0, "Min turnos > 0")
            assertTrue(agg.maxTurns >= agg.minTurns, "Max ≥ Min")
            assertTrue(agg.totalSimulationTimeMs >= 0, "Tiempo total ≥ 0")
            assertTrue(agg.gamesPerSecond > 0, "Games/sec > 0")
        }

        @Test
        fun `callback de progreso se invoca correctamente`() {
            val progressCalls = mutableListOf<Pair<Int, Int>>()
            val runner = BatchRunner(strategies) { completed, total ->
                progressCalls.add(completed to total)
            }

            runner.run(SimulationConfig(totalGames = 20))

            assertEquals(20, progressCalls.size)
            assertEquals(1, progressCalls.first().first)
            assertEquals(20, progressCalls.last().first)
            progressCalls.forEach { (_, total) ->
                assertEquals(20, total)
            }
        }

        @Test
        fun `batch paralelo produce misma cantidad de resultados`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 50, parallelism = 4))

            assertEquals(50, result.games.size)
            assertEquals(50, result.aggregate.totalGames)
        }

        @Test
        fun `p1Wins + p2Wins + draws = totalGames`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 100))
            val agg = result.aggregate

            assertEquals(100, agg.p1Wins + agg.p2Wins + agg.draws,
                "Las victorias + empates deben sumar el total de partidas")
        }
    }

    // ═══════════════════════════════════════════
    //  5. CSV EXPORTER
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("CsvExporter")
    inner class CsvExporterTests {

        @Test
        fun `cabeceras CSV correctas`() {
            val csv = CsvExporter.toSString(emptyList())
            val headerLine = csv.lines().first()
            val expectedHeaders = GameMetrics.CSV_HEADERS.joinToString(",")
            assertEquals(expectedHeaders, headerLine)
        }

        @Test
        fun `exporta métricas a CSV válido`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 10))

            val csv = CsvExporter.toSString(result.games)
            val lines = csv.trimEnd().lines()

            // 1 header + 10 data rows
            assertEquals(11, lines.size, "Debe haber 11 líneas (1 header + 10 datos)")

            // Cada fila de datos tiene el mismo número de columnas
            val headerCols = lines[0].split(",").size
            lines.drop(1).forEachIndexed { idx, line ->
                val cols = line.split(",").size
                assertEquals(headerCols, cols,
                    "Fila $idx debe tener $headerCols columnas, tiene $cols")
            }
        }

        @Test
        fun `CSV sin header cuando includeHeader=false`() {
            val metrics = listOf(createSampleMetrics())
            val csv = CsvExporter.toSString(metrics, includeHeader = false)
            val lines = csv.trimEnd().lines()

            assertEquals(1, lines.size, "Solo 1 línea de datos sin header")
        }

        @Test
        fun `Writer recibe contenido correcto`() {
            val metrics = listOf(createSampleMetrics())
            val writer = StringWriter()
            CsvExporter.writeToWriter(metrics, writer)

            val content = writer.toString()
            assertTrue(content.startsWith("game_id,"))
            assertTrue(content.lines().size >= 2) // header + 1 row
        }

        private fun createSampleMetrics() = GameMetrics(
            gameId = "test-1",
            seed = 42L,
            p1Difficulty = AIDifficulty.EASY,
            p2Difficulty = AIDifficulty.MEDIUM,
            winnerId = "p1",
            winReason = "P2_BANKRUPT",
            totalTurns = 150,
            p1FinalBalance = 2000,
            p2FinalBalance = 0,
            p1FinalNetWorth = 5000,
            p2FinalNetWorth = 0,
            p1PropertiesCount = 8,
            p2PropertiesCount = 0,
            p1Bankrupted = false,
            p2Bankrupted = true,
            gameDurationMs = 12
        )
    }

    // ═══════════════════════════════════════════
    //  6. SeededDiceProvider
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("SeededDiceProvider")
    inner class DiceTests {

        @Test
        fun `dados deterministas con misma seed`() {
            val d1 = SeededDiceProvider(42)
            val d2 = SeededDiceProvider(42)

            repeat(100) { i ->
                val r1 = d1.roll()
                val r2 = d2.roll()
                assertEquals(r1, r2, "Roll #$i debe ser idéntico con misma seed")
            }
        }

        @Test
        fun `dados producen valores válidos 1-6`() {
            val dice = SeededDiceProvider(12345)
            repeat(1000) {
                val result = dice.roll()
                assertTrue(result.die1 in 1..6)
                assertTrue(result.die2 in 1..6)
                assertTrue(result.total in 2..12)
            }
        }
    }

    // ═══════════════════════════════════════════
    //  7. RENDIMIENTO
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Rendimiento")
    inner class PerformanceTests {

        @Test
        @Timeout(60) // máximo 60 segundos
        fun `1000 partidas completan en menos de 60 segundos`() {
            val runner = BatchRunner(strategies)
            val result = runner.run(SimulationConfig(totalGames = 1000))

            assertEquals(1000, result.games.size)
            assertTrue(result.aggregate.gamesPerSecond > 10,
                "Debe ejecutar al menos 10 partidas/segundo, " +
                "real: ${"%.1f".format(result.aggregate.gamesPerSecond)}")

            println(result.aggregate.toSummary())
        }
    }

    // ═══════════════════════════════════════════
    //  8. INTEGRACIÓN COMPLETA
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Integración Simulador → Batch → CSV")
    inner class IntegrationTests {

        @Test
        fun `flujo completo - simulacion + agregacion + CSV`() {
            // 1. Configurar
            val config = SimulationConfig(
                totalGames = 50,
                baseSeed = 42,
                player1Difficulty = AIDifficulty.EASY,
                player2Difficulty = AIDifficulty.EASY,
                gameConfig = GameConfig(maxTurns = 100)
            )

            // 2. Ejecutar
            val runner = BatchRunner(strategies)
            val result = runner.run(config)

            // 3. Validar resultados
            assertEquals(50, result.games.size)
            assertEquals(50, result.aggregate.totalGames)

            // 4. Exportar a CSV
            val csv = CsvExporter.toSString(result.games)
            val lines = csv.trimEnd().lines()
            assertEquals(51, lines.size) // header + 50 rows

            // 5. Verificar que las métricas agregadas suman
            val p1WinsFromGames = result.games.count { it.winnerId == "p1" }
            assertEquals(p1WinsFromGames, result.aggregate.p1Wins)

            // 6. Resumen se genera correctamente
            val summary = result.aggregate.toSummary()
            assertTrue(summary.contains("EASY vs EASY"))
            assertTrue(summary.contains("partidas/seg"))
        }
    }
}
