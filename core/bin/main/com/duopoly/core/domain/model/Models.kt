package com.duopoly.core.domain.model

import java.io.Serializable

// ══════════════════════════════════════════════════════════════
// Modelo Formal del Estado del Juego
//
//   S = (P, B, τ, φ, δ, C)
//   P = estados de jugadores    B = estados de casillas
//   τ = turno                   φ = fase del turno
//   δ = último dado             C = configuración
//
// INVARIANTES:
//   I₁: balance ≥ 0 ∨ bankrupt    I₂: conservación monetaria
//   I₃: 0 ≤ pos < |Board|         I₄: owner ∈ {null, p₁, p₂}
//   I₅: 0 ≤ level ≤ max           I₆: level > 0 → ¬mortgaged
// ══════════════════════════════════════════════════════════════

/** Identificador fuertemente tipado de jugador. */
@JvmInline
value class PlayerId(val value: String) : Serializable {
    override fun toString(): String = value
}

/** Niveles de dificultad de la IA. */
enum class AIDifficulty { EASY, MEDIUM, HARD }

// ───── Definición del Tablero (Estática / Inmutable) ─────

/** Clasificación de casillas del tablero. */
enum class TileType {
    START,
    PROPERTY,
    TAX,
    CHANCE,
    COMMUNITY_CHEST,
    JAIL,
    FREE_PARKING,
    GO_TO_JAIL
}

/** Agrupación de propiedades (equivalente a un grupo de color). */
data class PropertyGroup(
    val id: Int,
    val name: String,
    val size: Int
) : Serializable

/**
 * Definición estática de una casilla del tablero.
 * Nunca cambia durante una partida.
 */
data class TileDefinition(
    val index: Int,
    val name: String,
    val type: TileType,
    val group: PropertyGroup? = null,
    val purchasePrice: Int = 0,
    val baseRent: Int = 0,
    val rentByLevel: List<Int> = emptyList(),
    val upgradeCost: Int = 0,
    val taxAmount: Int = 0
) : Serializable

// ───── Estado Dinámico por Casilla ─────

/**
 * Estado en tiempo de ejecución de una casilla.
 * Propiedad, nivel de mejora, estado de hipoteca.
 */
data class TileState(
    val tileIndex: Int,
    val ownerId: PlayerId? = null,
    val upgradeLevel: Int = 0,
    val isMortgaged: Boolean = false
) : Serializable

// ───── Estado del Jugador ─────

/**
 * Snapshot inmutable de un jugador en un instante dado.
 */
data class PlayerState(
    val id: PlayerId,
    val name: String,
    val balance: Int,
    val position: Int,
    val isInJail: Boolean = false,
    val turnsInJail: Int = 0,
    val isBankrupt: Boolean = false,
    val isAI: Boolean = false,
    val aiDifficulty: AIDifficulty? = null
) : Serializable

// ───── Dados ─────

data class DiceResult(
    val die1: Int,
    val die2: Int
) : Serializable {
    val total: Int get() = die1 + die2
    val isDoubles: Boolean get() = die1 == die2

    init {
        require(die1 in 1..6) { "die1 debe estar en 1..6, recibido $die1" }
        require(die2 in 1..6) { "die2 debe estar en 1..6, recibido $die2" }
    }
}

// ───── Fase del Juego ─────

/**
 * Representa la fase actual dentro de un turno.
 * Determina qué acciones son válidas.
 */
enum class GamePhase {
    /** Jugador aún no ha lanzado dados. */
    PRE_ROLL,
    /** Jugador aterrizó; esperando resolución o fin de turno. */
    POST_ROLL,
    /** Jugador aterrizó en propiedad sin dueño; debe comprar o declinar. */
    BUYING_DECISION,
    /** Propiedad declinada; subasta en progreso. */
    AUCTION,
    /** Se debe pagar renta (jugador podría necesitar hipotecar). */
    PAYING_RENT,
    /** Estado terminal. */
    GAME_OVER
}

// ───── Estado de Subasta ─────

data class AuctionState(
    val tileIndex: Int,
    val highestBid: Int = 0,
    val highestBidderId: PlayerId? = null,
    val participants: List<PlayerId>,
    val currentBidderIndex: Int = 0,
    val passedCount: Int = 0
) : Serializable

// ───── Configuración del Juego ─────

/**
 * Parámetros tunables del juego.
 * Clave para reproducibilidad experimental.
 */
data class GameConfig(
    val startingBalance: Int = 1500,
    val goSalary: Int = 400,
    val maxTurns: Int = 200,
    val maxConsecutiveDoubles: Int = 3,
    val jailFine: Int = 50,
    val maxJailTurns: Int = 3,
    val mortgageRatio: Double = 0.5,
    val unmortgagePenaltyRatio: Double = 0.1,
    val groupBonusMultiplier: Double = 2.0,
    val auctionMinimumBid: Int = 10
) : Serializable

// ═══════════════════════════════════════════════════════════════
// GAME STATE — Snapshot inmutable del estado completo del juego
//
// Cada acción produce un NUEVO GameState. Esto permite:
//   - Búsqueda de árbol en MCTS sin clonación
//   - Snapshots para Big Data
//   - Replay determinista
//   - Testing sin side effects
// ═══════════════════════════════════════════════════════════════

data class GameState(
    val gameId: String = java.util.UUID.randomUUID().toString(),
    val players: List<PlayerState>,
    val tileStates: List<TileState>,
    val board: List<TileDefinition>,
    val config: GameConfig,
    val currentPlayerIndex: Int,
    val phase: GamePhase,
    val turnNumber: Int,
    val lastDiceResult: DiceResult? = null,
    val consecutiveDoubles: Int = 0,
    val auctionState: AuctionState? = null
) : Serializable {

    // ── Propiedades Derivadas ──

    val currentPlayer: PlayerState
        get() = players[currentPlayerIndex]

    val isGameOver: Boolean
        get() = phase == GamePhase.GAME_OVER

    val activePlayers: List<PlayerState>
        get() = players.filter { !it.isBankrupt }

    val boardSize: Int
        get() = board.size

    // ── Consultas ──

    fun playerById(id: PlayerId): PlayerState? =
        players.find { it.id == id }

    fun tileStateAt(index: Int): TileState =
        tileStates[index]

    fun tileDefAt(index: Int): TileDefinition =
        board[index]

    fun propertiesOwnedBy(playerId: PlayerId): List<Int> =
        tileStates.filter { it.ownerId == playerId }.map { it.tileIndex }

    fun ownsFullGroup(playerId: PlayerId, group: PropertyGroup): Boolean {
        val groupTileIndices = board
            .filter { it.group?.id == group.id }
            .map { it.index }
        return groupTileIndices.isNotEmpty() &&
                groupTileIndices.all { tileStates[it].ownerId == playerId }
    }

    /**
     * Patrimonio neto = efectivo + valor de propiedades + valor de mejoras.
     */
    fun calculateNetWorth(playerId: PlayerId): Int {
        val player = playerById(playerId) ?: return 0
        val propertyValue = propertiesOwnedBy(playerId).sumOf { idx ->
            val def = board[idx]
            val state = tileStates[idx]
            if (state.isMortgaged) {
                (def.purchasePrice * config.mortgageRatio).toInt()
            } else {
                def.purchasePrice + (state.upgradeLevel * def.upgradeCost)
            }
        }
        return player.balance + propertyValue
    }

    /**
     * Calcula la renta de una casilla considerando mejoras, grupo completo e hipoteca.
     */
    fun calculateRent(tileIndex: Int): Int {
        val tileDef = board[tileIndex]
        val tileState = tileStates[tileIndex]

        if (tileDef.type != TileType.PROPERTY) return 0
        if (tileState.ownerId == null) return 0
        if (tileState.isMortgaged) return 0

        val baseAmount = if (tileState.upgradeLevel > 0 &&
            tileState.upgradeLevel <= tileDef.rentByLevel.size
        ) {
            tileDef.rentByLevel[tileState.upgradeLevel - 1]
        } else {
            tileDef.baseRent
        }

        val group = tileDef.group
        return if (group != null &&
            ownsFullGroup(tileState.ownerId, group) &&
            tileState.upgradeLevel == 0
        ) {
            (baseAmount * config.groupBonusMultiplier).toInt()
        } else {
            baseAmount
        }
    }

    // ── Helpers de Actualización Inmutable ──

    fun updatePlayer(playerId: PlayerId, transform: (PlayerState) -> PlayerState): GameState =
        copy(players = players.map { if (it.id == playerId) transform(it) else it })

    fun updateTile(tileIndex: Int, transform: (TileState) -> TileState): GameState =
        copy(tileStates = tileStates.map { if (it.tileIndex == tileIndex) transform(it) else it })
}
