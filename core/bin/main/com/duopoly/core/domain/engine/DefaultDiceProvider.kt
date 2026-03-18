package com.duopoly.core.domain.engine

import com.duopoly.core.domain.model.DiceResult
import com.duopoly.core.domain.ports.DiceProvider
import kotlin.random.Random

/**
 * Proveedor de dados aleatorio estándar.
 * Usa un Random con semilla configurable para reproducibilidad.
 */
class DefaultDiceProvider(
    private val random: Random = Random.Default
) : DiceProvider {

    override fun roll(): DiceResult = DiceResult(
        die1 = random.nextInt(1, 7),
        die2 = random.nextInt(1, 7)
    )
}

/**
 * Proveedor de dados determinista para testing y replay.
 * Retorna resultados de dados de una secuencia predefinida.
 *
 * Uso en tests:
 * ```
 * val dice = SequentialDiceProvider(listOf(
 *     DiceResult(1, 2),  // primer lanzamiento: 3
 *     DiceResult(3, 4),  // segundo lanzamiento: 7
 * ))
 * ```
 *
 * Uso en simulación reproducible:
 * ```
 * val seed = 42L
 * val dice = DefaultDiceProvider(Random(seed))
 * ```
 */
class SequentialDiceProvider(
    private val results: List<DiceResult>
) : DiceProvider {

    private var index = 0

    override fun roll(): DiceResult {
        require(index < results.size) {
            "No quedan resultados de dados en la secuencia (usados $index de ${results.size})"
        }
        return results[index++]
    }
}
