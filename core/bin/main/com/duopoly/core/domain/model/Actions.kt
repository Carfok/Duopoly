package com.duopoly.core.domain.model

/**
 * Conjunto exhaustivo de acciones del juego A.
 *
 * Sealed class para pattern matching seguro en el compilador.
 * Cada acción está asociada al jugador que la ejecuta.
 *
 * El RuleEngine valida que la acción sea legal en el estado actual
 * antes de aplicar la función de transición T(S, A) → S'.
 */
sealed class GameAction {
    abstract val playerId: PlayerId

    /** Lanzar dados para determinar movimiento. Válida en fase PRE_ROLL. */
    data class RollDice(override val playerId: PlayerId) : GameAction()

    /** Comprar la propiedad donde aterrizó el jugador. Válida en BUYING_DECISION. */
    data class BuyProperty(override val playerId: PlayerId) : GameAction()

    /** Declinar compra; inicia subasta. Válida en BUYING_DECISION. */
    data class DeclineProperty(override val playerId: PlayerId) : GameAction()

    /** Realizar una puja en una subasta activa. Válida en AUCTION. */
    data class PlaceBid(override val playerId: PlayerId, val amount: Int) : GameAction()

    /** Retirarse de una subasta activa. Válida en AUCTION. */
    data class WithdrawFromAuction(override val playerId: PlayerId) : GameAction()

    /** Construir mejora en una propiedad propia. Válida en PRE_ROLL / POST_ROLL. */
    data class BuildUpgrade(override val playerId: PlayerId, val tileIndex: Int) : GameAction()

    /** Hipotecar propiedad para obtener efectivo. Válida en PRE_ROLL / POST_ROLL / PAYING_RENT. */
    data class MortgageProperty(override val playerId: PlayerId, val tileIndex: Int) : GameAction()

    /** Deshypotecar propiedad. Válida en PRE_ROLL / POST_ROLL. */
    data class UnmortgageProperty(override val playerId: PlayerId, val tileIndex: Int) : GameAction()

    /** Pagar multa para salir de la cárcel. Válida en PRE_ROLL cuando está en cárcel. */
    data class PayJailFine(override val playerId: PlayerId) : GameAction()

    /** Terminar el turno actual. Válida en POST_ROLL. */
    data class EndTurn(override val playerId: PlayerId) : GameAction()

    /** Declarar bancarrota (voluntaria o forzada). */
    data class DeclareBankruptcy(override val playerId: PlayerId) : GameAction()
}

/**
 * Resultado de aplicar una acción al estado del juego.
 * Sigue el patrón Result para manejo explícito de errores.
 */
sealed class ActionResult {
    /** Acción válida. Contiene nuevo estado y eventos generados. */
    data class Success(
        val newState: GameState,
        val events: List<GameEvent>
    ) : ActionResult()

    /** Acción inválida en el estado actual. */
    data class Invalid(val reason: String) : ActionResult()
}
