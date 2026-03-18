package com.duopoly.core.domain.engine

import com.duopoly.core.domain.model.*

/**
 * Funciones puras de operaciones económicas.
 *
 * Todas las funciones toman un GameState y retornan un NUEVO GameState.
 * Sin efectos secundarios. Garantiza el invariante I₁ (solvencia).
 */
object EconomyManager {

    /**
     * Transfiere dinero de un jugador a otro.
     * @throws IllegalArgumentException si el monto es negativo o el pagador no tiene fondos.
     */
    fun transferMoney(state: GameState, fromId: PlayerId, toId: PlayerId, amount: Int): GameState {
        require(amount >= 0) { "Monto de transferencia debe ser no negativo: $amount" }
        val from = state.playerById(fromId)
            ?: throw IllegalArgumentException("Jugador no encontrado: $fromId")
        require(from.balance >= amount) {
            "Saldo insuficiente: ${from.balance} < $amount (jugador: $fromId)"
        }

        return state
            .updatePlayer(fromId) { it.copy(balance = it.balance - amount) }
            .updatePlayer(toId) { it.copy(balance = it.balance + amount) }
    }

    /** El jugador paga al banco (impuestos, multas, compras). */
    fun payToBank(state: GameState, playerId: PlayerId, amount: Int): GameState {
        require(amount >= 0) { "Pago debe ser no negativo: $amount" }
        val player = state.playerById(playerId)
            ?: throw IllegalArgumentException("Jugador no encontrado: $playerId")
        require(player.balance >= amount) {
            "Saldo insuficiente: ${player.balance} < $amount (jugador: $playerId)"
        }

        return state.updatePlayer(playerId) { it.copy(balance = it.balance - amount) }
    }

    /** El banco paga al jugador (salario, premios). */
    fun receiveFromBank(state: GameState, playerId: PlayerId, amount: Int): GameState {
        require(amount >= 0) { "Monto debe ser no negativo: $amount" }
        return state.updatePlayer(playerId) { it.copy(balance = it.balance + amount) }
    }

    /** Verifica si un jugador puede pagar una cantidad dada. */
    fun canAfford(state: GameState, playerId: PlayerId, amount: Int): Boolean {
        val player = state.playerById(playerId) ?: return false
        return player.balance >= amount
    }

    /**
     * Calcula el máximo que un jugador puede obtener
     * hipotecando todas sus propiedades no hipotecadas.
     */
    fun maxLiquidatable(state: GameState, playerId: PlayerId): Int {
        val player = state.playerById(playerId) ?: return 0
        val mortgageableValue = state.propertiesOwnedBy(playerId)
            .filter { !state.tileStateAt(it).isMortgaged && state.tileStateAt(it).upgradeLevel == 0 }
            .sumOf { mortgageValue(state, it) }
        return player.balance + mortgageableValue
    }

    /** Valor de efectivo obtenido al hipotecar una propiedad. */
    fun mortgageValue(state: GameState, tileIndex: Int): Int {
        val def = state.tileDefAt(tileIndex)
        return (def.purchasePrice * state.config.mortgageRatio).toInt()
    }

    /** Coste de deshypotecar una propiedad (principal + penalización). */
    fun unmortgageCost(state: GameState, tileIndex: Int): Int {
        val def = state.tileDefAt(tileIndex)
        val mortgage = (def.purchasePrice * state.config.mortgageRatio).toInt()
        val penalty = (def.purchasePrice * state.config.unmortgagePenaltyRatio).toInt()
        return mortgage + penalty
    }
}
