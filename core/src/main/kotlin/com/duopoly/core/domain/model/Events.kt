package com.duopoly.core.domain.model

/**
 * Eventos estructurados del juego para logging, analytics y notificación de UI.
 *
 * Cada transición de estado produce uno o más eventos.
 * Estos forman la base del dataset de Big Data:
 *   [GameId, TurnNumber, PlayerId, EventType, Payload, Timestamp]
 */
sealed class GameEvent {
    abstract val turnNumber: Int
    abstract val playerId: PlayerId

    data class DiceRolled(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val result: DiceResult
    ) : GameEvent()

    data class PlayerMoved(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val fromPosition: Int,
        val toPosition: Int,
        val passedGo: Boolean
    ) : GameEvent()

    data class SalaryCollected(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val amount: Int
    ) : GameEvent()

    data class PropertyPurchased(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val tileIndex: Int,
        val price: Int
    ) : GameEvent()

    data class RentPaid(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val toPlayerId: PlayerId,
        val amount: Int,
        val tileIndex: Int
    ) : GameEvent()

    data class TaxPaid(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val amount: Int
    ) : GameEvent()

    data class PropertyUpgraded(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val tileIndex: Int,
        val newLevel: Int,
        val cost: Int
    ) : GameEvent()

    data class PropertyMortgaged(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val tileIndex: Int,
        val cashReceived: Int
    ) : GameEvent()

    data class PropertyUnmortgaged(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val tileIndex: Int,
        val costPaid: Int
    ) : GameEvent()

    data class SentToJail(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val reason: String
    ) : GameEvent()

    data class ReleasedFromJail(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val method: String
    ) : GameEvent()

    data class PlayerBankrupt(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val finalBalance: Int,
        val finalNetWorth: Int
    ) : GameEvent()

    data class AuctionStarted(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val tileIndex: Int
    ) : GameEvent()

    data class BidPlaced(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val amount: Int,
        val tileIndex: Int
    ) : GameEvent()

    data class AuctionEnded(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val tileIndex: Int,
        val winningBid: Int,
        val winnerId: PlayerId?
    ) : GameEvent()

    data class GameStarted(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val playerCount: Int,
        val config: GameConfig
    ) : GameEvent()

    data class GameEnded(
        override val turnNumber: Int,
        override val playerId: PlayerId,
        val winnerId: PlayerId?,
        val reason: String,
        val totalTurns: Int
    ) : GameEvent()
}
