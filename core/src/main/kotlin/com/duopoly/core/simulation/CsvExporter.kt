package com.duopoly.core.simulation

import java.io.File
import java.io.Writer

/**
 * Exportador de métricas de simulación a formato CSV.
 *
 * Genera datasets compatibles con Pandas, Spark y cualquier
 * herramienta de análisis de datos. Delimitador: coma.
 * Encoding: UTF-8. Sin BOM.
 *
 * Esquema del CSV:
 *   game_id, seed, p1_difficulty, p2_difficulty, winner_id,
 *   win_reason, total_turns, p1_final_balance, p2_final_balance,
 *   p1_final_net_worth, p2_final_net_worth, p1_properties_count,
 *   p2_properties_count, p1_bankrupted, p2_bankrupted, game_duration_ms
 */
object CsvExporter {

    /**
     * Exporta una lista de métricas a un archivo CSV.
     *
     * @param metrics Lista de métricas de partidas individuales
     * @param filePath Ruta del archivo de salida
     * @param includeHeader Si se incluye la fila de cabeceras (default: true)
     */
    fun export(metrics: List<GameMetrics>, filePath: String, includeHeader: Boolean = true) {
        File(filePath).bufferedWriter(Charsets.UTF_8).use { writer ->
            writeToWriter(metrics, writer, includeHeader)
        }
    }

    /**
     * Exporta a un Writer arbitrario (facilita testing sin I/O de disco).
     */
    fun writeToWriter(
        metrics: List<GameMetrics>,
        writer: Writer,
        includeHeader: Boolean = true
    ) {
        if (includeHeader) {
            writer.write(GameMetrics.CSV_HEADERS.joinToString(","))
            writer.newLine()
        }

        metrics.forEach { m ->
            writer.write(m.toCsvRow())
            writer.newLine()
        }

        writer.flush()
    }

    /**
     * Genera el CSV como String (útil para testing y logging).
     */
    fun toSString(metrics: List<GameMetrics>, includeHeader: Boolean = true): String {
        val sb = StringBuilder()
        if (includeHeader) {
            sb.appendLine(GameMetrics.CSV_HEADERS.joinToString(","))
        }
        metrics.forEach { m ->
            sb.appendLine(m.toCsvRow())
        }
        return sb.toString()
    }

    /**
     * Exporta también las métricas agregadas como un archivo
     * JSON-like de resumen junto al CSV principal.
     */
    fun exportSummary(aggregate: AggregateMetrics, filePath: String) {
        File(filePath).writeText(aggregate.toSummary(), Charsets.UTF_8)
    }

    private fun Writer.newLine() {
        write("\n")
    }
}
