package com.duopoly.core.domain.engine

import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.ports.DiceProvider

/**
 * Motor de Reglas Determinista.
 *
 * Implementa la función de transición T(S, A) → (S', [Events])
 * Este es el núcleo de la lógica del juego.
 *
 * Todas las funciones son PURAS — sin efectos secundarios.
 * El engine valida acciones, aplica transiciones y genera eventos
 * estructurados para logging.
 *
 * Diseño:
 *   - Cada handler recibe un estado inmutable y retorna un nuevo estado + eventos
 *   - Los dados se inyectan externamente (DiceProvider) para determinismo
 *   - Los errores de validación se retornan como ActionResult.Invalid
 */
class RuleEngine {

    // ═══════════════════════════════════════════════════════
    // VALIDACIÓN DE ACCIONES
    // ═══════════════════════════════════════════════════════

    /**
     * Obtiene todas las acciones válidas para el jugador actual
     * en el estado de juego dado.
     */
    fun getValidActions(state: GameState): List<GameAction> {
        if (state.isGameOver) return emptyList()

        val player = state.currentPlayer
        val pid = player.id
        val actions = mutableListOf<GameAction>()

        when (state.phase) {
            GamePhase.PRE_ROLL -> {
                if (player.isInJail) {
                    if (EconomyManager.canAfford(state, pid, state.config.jailFine)) {
                        actions.add(GameAction.PayJailFine(pid))
                    }
                }
                actions.add(GameAction.RollDice(pid))
                addPropertyManagementActions(state, pid, actions)
            }

            GamePhase.POST_ROLL -> {
                actions.add(GameAction.EndTurn(pid))
                addPropertyManagementActions(state, pid, actions)
            }

            GamePhase.BUYING_DECISION -> {
                val tileDef = state.tileDefAt(player.position)
                if (EconomyManager.canAfford(state, pid, tileDef.purchasePrice)) {
                    actions.add(GameAction.BuyProperty(pid))
                }
                actions.add(GameAction.DeclineProperty(pid))
            }

            GamePhase.AUCTION -> {
                val auction = state.auctionState
                    ?: return listOf(GameAction.EndTurn(pid))

                if (auction.participants.isNotEmpty()) {
                    val bidderId = auction.participants[auction.currentBidderIndex]
                    val bidder = state.playerById(bidderId) ?: return emptyList()
                    val minBid = auction.highestBid + state.config.auctionMinimumBid
                    if (bidder.balance >= minBid) {
                        actions.add(GameAction.PlaceBid(bidderId, minBid))
                    }
                    actions.add(GameAction.WithdrawFromAuction(bidderId))
                }
            }

            GamePhase.PAYING_RENT -> {
                val rent = state.calculateRent(player.position)
                // Permitir hipotecar para obtener fondos
                addMortgageActions(state, pid, actions)
                // Si después de hipotecar puede pagar, la renta se resuelve al pasar a POST_ROLL
                if (player.balance >= rent) {
                    // Auto-resolución: el jugador ya tiene suficiente para pagar
                    actions.add(GameAction.EndTurn(pid))
                }
                if (EconomyManager.maxLiquidatable(state, pid) < rent) {
                    actions.add(GameAction.DeclareBankruptcy(pid))
                }
            }

            GamePhase.GAME_OVER -> { /* sin acciones */ }
        }

        return actions
    }

    /**
     * Valida y aplica una acción al estado del juego.
     * Retorna Success con (nuevo estado, eventos) o Invalid con razón.
     */
    fun applyAction(
        state: GameState,
        action: GameAction,
        diceProvider: DiceProvider? = null
    ): ActionResult {
        if (state.isGameOver) {
            return ActionResult.Invalid("El juego ya ha terminado")
        }

        return when (action) {
            is GameAction.RollDice -> handleRollDice(state, action, diceProvider)
            is GameAction.BuyProperty -> handleBuyProperty(state, action)
            is GameAction.DeclineProperty -> handleDeclineProperty(state, action)
            is GameAction.PlaceBid -> handlePlaceBid(state, action)
            is GameAction.WithdrawFromAuction -> handleWithdrawFromAuction(state, action)
            is GameAction.BuildUpgrade -> handleBuildUpgrade(state, action)
            is GameAction.MortgageProperty -> handleMortgageProperty(state, action)
            is GameAction.UnmortgageProperty -> handleUnmortgageProperty(state, action)
            is GameAction.PayJailFine -> handlePayJailFine(state, action)
            is GameAction.EndTurn -> handleEndTurn(state, action)
            is GameAction.DeclareBankruptcy -> handleDeclareBankruptcy(state, action)
        }
    }

    // ═══════════════════════════════════════════════════════
    // HANDLERS DE ACCIONES (Funciones de Transición)
    // ═══════════════════════════════════════════════════════

    // ── RollDice ──

    private fun handleRollDice(
        state: GameState,
        action: GameAction.RollDice,
        diceProvider: DiceProvider?
    ): ActionResult {
        if (state.phase != GamePhase.PRE_ROLL) {
            return ActionResult.Invalid("No se puede lanzar dados en fase ${state.phase}")
        }
        if (action.playerId != state.currentPlayer.id) {
            return ActionResult.Invalid("No es el turno de este jugador")
        }

        val dice = diceProvider?.roll()
            ?: return ActionResult.Invalid("No hay proveedor de dados disponible")

        val player = state.currentPlayer
        val events = mutableListOf<GameEvent>()

        events.add(GameEvent.DiceRolled(state.turnNumber, player.id, dice))

        // Manejar cárcel
        if (player.isInJail) {
            return handleJailRoll(state, player, dice, events)
        }

        // Verificar dobles consecutivos → cárcel
        val newConsecutiveDoubles = if (dice.isDoubles) state.consecutiveDoubles + 1 else 0
        if (newConsecutiveDoubles >= state.config.maxConsecutiveDoubles) {
            return sendToJail(state, player, events, "Tres dobles consecutivos")
        }

        // Mover jugador
        val oldPosition = player.position
        val newPosition = (oldPosition + dice.total) % state.boardSize
        val passedGo = (oldPosition + dice.total) >= state.boardSize

        events.add(GameEvent.PlayerMoved(state.turnNumber, player.id, oldPosition, newPosition, passedGo))

        var newState = state
            .updatePlayer(player.id) { it.copy(position = newPosition) }
            .copy(
                lastDiceResult = dice,
                consecutiveDoubles = newConsecutiveDoubles
            )

        // Salario por pasar por Salida
        if (passedGo) {
            newState = EconomyManager.receiveFromBank(newState, player.id, state.config.goSalary)
            events.add(GameEvent.SalaryCollected(state.turnNumber, player.id, state.config.goSalary))
        }

        // Resolver aterrizaje
        return resolveLanding(newState, player.id, events)
    }

    private fun handleJailRoll(
        state: GameState,
        player: PlayerState,
        dice: DiceResult,
        events: MutableList<GameEvent>
    ): ActionResult {
        if (dice.isDoubles) {
            // Liberado por sacar dobles
            events.add(GameEvent.ReleasedFromJail(state.turnNumber, player.id, "Dobles"))
            val newPosition = (player.position + dice.total) % state.boardSize
            val passedGo = (player.position + dice.total) >= state.boardSize
            events.add(GameEvent.PlayerMoved(state.turnNumber, player.id, player.position, newPosition, passedGo))

            var newState = state
                .updatePlayer(player.id) {
                    it.copy(position = newPosition, isInJail = false, turnsInJail = 0)
                }
                .copy(lastDiceResult = dice, consecutiveDoubles = 0)

            if (passedGo) {
                newState = EconomyManager.receiveFromBank(newState, player.id, state.config.goSalary)
                events.add(GameEvent.SalaryCollected(state.turnNumber, player.id, state.config.goSalary))
            }

            return resolveLanding(newState, player.id, events)
        }

        val newTurnsInJail = player.turnsInJail + 1
        if (newTurnsInJail >= state.config.maxJailTurns) {
            // Liberación forzada: pagar multa
            if (player.balance >= state.config.jailFine) {
                events.add(GameEvent.ReleasedFromJail(state.turnNumber, player.id, "Multa forzada"))
                val newState = EconomyManager.payToBank(
                    state.updatePlayer(player.id) {
                        it.copy(isInJail = false, turnsInJail = 0)
                    },
                    player.id,
                    state.config.jailFine
                ).copy(lastDiceResult = dice, phase = GamePhase.POST_ROLL)
                return ActionResult.Success(newState, events)
            } else {
                return handleDeclareBankruptcy(state, GameAction.DeclareBankruptcy(player.id))
            }
        }

        // Permanece en cárcel
        val newState = state
            .updatePlayer(player.id) { it.copy(turnsInJail = newTurnsInJail) }
            .copy(lastDiceResult = dice, phase = GamePhase.POST_ROLL)
        return ActionResult.Success(newState, events)
    }

    // ── Resolución de Aterrizaje ──

    private fun resolveLanding(
        state: GameState,
        playerId: PlayerId,
        events: MutableList<GameEvent>
    ): ActionResult {
        val player = state.playerById(playerId)!!
        val tileDef = state.tileDefAt(player.position)
        val tileState = state.tileStateAt(player.position)

        return when (tileDef.type) {
            TileType.START, TileType.JAIL, TileType.FREE_PARKING -> {
                ActionResult.Success(state.copy(phase = GamePhase.POST_ROLL), events)
            }

            TileType.PROPERTY -> {
                when {
                    tileState.ownerId == null -> {
                        // Sin dueño: decisión de compra
                        ActionResult.Success(state.copy(phase = GamePhase.BUYING_DECISION), events)
                    }
                    tileState.ownerId == playerId -> {
                        // Propiedad propia: nada que pagar
                        ActionResult.Success(state.copy(phase = GamePhase.POST_ROLL), events)
                    }
                    tileState.isMortgaged -> {
                        // Hipotecada: sin renta
                        ActionResult.Success(state.copy(phase = GamePhase.POST_ROLL), events)
                    }
                    else -> {
                        // Pagar renta
                        val rent = state.calculateRent(player.position)
                        if (player.balance >= rent) {
                            val newState = EconomyManager.transferMoney(
                                state, playerId, tileState.ownerId, rent
                            )
                            events.add(GameEvent.RentPaid(
                                state.turnNumber, playerId, tileState.ownerId,
                                rent, player.position
                            ))
                            ActionResult.Success(newState.copy(phase = GamePhase.POST_ROLL), events)
                        } else {
                            // No puede pagar: entrar en PAYING_RENT para hipotecar o bancarrota
                            ActionResult.Success(state.copy(phase = GamePhase.PAYING_RENT), events)
                        }
                    }
                }
            }

            TileType.TAX -> {
                val tax = tileDef.taxAmount
                if (player.balance >= tax) {
                    val newState = EconomyManager.payToBank(state, playerId, tax)
                    events.add(GameEvent.TaxPaid(state.turnNumber, playerId, tax))
                    ActionResult.Success(newState.copy(phase = GamePhase.POST_ROLL), events)
                } else {
                    // No puede pagar el impuesto
                    ActionResult.Success(state.copy(phase = GamePhase.PAYING_RENT), events)
                }
            }

            TileType.CHANCE, TileType.COMMUNITY_CHEST -> {
                // TODO: Fase posterior — implementar mazo de cartas con efectos predefinidos.
                // Por ahora, casilla neutral (no bloquea el flujo del juego).
                ActionResult.Success(state.copy(phase = GamePhase.POST_ROLL), events)
            }

            TileType.GO_TO_JAIL -> {
                return sendToJail(state, player, events, "Casilla Ir a Cárcel")
            }
        }
    }

    private fun sendToJail(
        state: GameState,
        player: PlayerState,
        events: MutableList<GameEvent>,
        reason: String
    ): ActionResult {
        val jailPosition = state.board.indexOfFirst { it.type == TileType.JAIL }
        events.add(GameEvent.SentToJail(state.turnNumber, player.id, reason))

        val newState = state
            .updatePlayer(player.id) {
                it.copy(position = jailPosition, isInJail = true, turnsInJail = 0)
            }
            .copy(phase = GamePhase.POST_ROLL, consecutiveDoubles = 0)

        return ActionResult.Success(newState, events)
    }

    // ── BuyProperty ──

    private fun handleBuyProperty(state: GameState, @Suppress("UNUSED_PARAMETER") action: GameAction.BuyProperty): ActionResult {
        if (state.phase != GamePhase.BUYING_DECISION) {
            return ActionResult.Invalid("No está en fase de decisión de compra")
        }

        val player = state.currentPlayer
        val tileDef = state.tileDefAt(player.position)
        val tileState = state.tileStateAt(player.position)

        if (tileState.ownerId != null) return ActionResult.Invalid("La propiedad ya tiene dueño")
        if (player.balance < tileDef.purchasePrice) return ActionResult.Invalid("Fondos insuficientes")

        val events = mutableListOf<GameEvent>()
        val newState = EconomyManager.payToBank(state, player.id, tileDef.purchasePrice)
            .updateTile(player.position) { it.copy(ownerId = player.id) }
            .copy(phase = GamePhase.POST_ROLL)

        events.add(GameEvent.PropertyPurchased(
            state.turnNumber, player.id, player.position, tileDef.purchasePrice
        ))
        return ActionResult.Success(newState, events)
    }

    // ── DeclineProperty ──

    private fun handleDeclineProperty(state: GameState, @Suppress("UNUSED_PARAMETER") action: GameAction.DeclineProperty): ActionResult {
        if (state.phase != GamePhase.BUYING_DECISION) {
            return ActionResult.Invalid("No está en fase de decisión de compra")
        }

        // Iniciar subasta
        val activePlayers = state.activePlayers.map { it.id }
        val auction = AuctionState(
            tileIndex = state.currentPlayer.position,
            participants = activePlayers,
            currentBidderIndex = 0
        )

        val events = listOf(
            GameEvent.AuctionStarted(state.turnNumber, state.currentPlayer.id, state.currentPlayer.position)
        )

        return ActionResult.Success(
            state.copy(phase = GamePhase.AUCTION, auctionState = auction),
            events
        )
    }

    // ── PlaceBid ──

    private fun handlePlaceBid(state: GameState, action: GameAction.PlaceBid): ActionResult {
        val auction = state.auctionState
            ?: return ActionResult.Invalid("No hay subasta en progreso")
        if (state.phase != GamePhase.AUCTION) {
            return ActionResult.Invalid("No está en fase de subasta")
        }
        if (action.amount <= auction.highestBid) {
            return ActionResult.Invalid("Puja demasiado baja")
        }

        val bidder = state.playerById(action.playerId)
            ?: return ActionResult.Invalid("Pujador no encontrado")
        if (bidder.balance < action.amount) {
            return ActionResult.Invalid("No puede pagar la puja")
        }

        val events = mutableListOf<GameEvent>(
            GameEvent.BidPlaced(state.turnNumber, action.playerId, action.amount, auction.tileIndex)
        )

        val newAuction = auction.copy(
            highestBid = action.amount,
            highestBidderId = action.playerId,
            currentBidderIndex = (auction.currentBidderIndex + 1) % auction.participants.size,
            passedCount = 0
        )

        return ActionResult.Success(state.copy(auctionState = newAuction), events)
    }

    // ── WithdrawFromAuction ──

    private fun handleWithdrawFromAuction(
        state: GameState,
        action: GameAction.WithdrawFromAuction
    ): ActionResult {
        val auction = state.auctionState
            ?: return ActionResult.Invalid("No hay subasta en progreso")
        if (state.phase != GamePhase.AUCTION) {
            return ActionResult.Invalid("No está en fase de subasta")
        }

        val newParticipants = auction.participants.filter { it != action.playerId }
        val events = mutableListOf<GameEvent>()

        // Si queda 1 o menos participantes, la subasta termina
        if (newParticipants.size <= 1) {
            val winnerId = auction.highestBidderId
            events.add(GameEvent.AuctionEnded(
                state.turnNumber, action.playerId, auction.tileIndex,
                auction.highestBid, winnerId
            ))

            var newState = state.copy(auctionState = null, phase = GamePhase.POST_ROLL)
            if (winnerId != null && auction.highestBid > 0) {
                newState = EconomyManager.payToBank(newState, winnerId, auction.highestBid)
                    .updateTile(auction.tileIndex) { it.copy(ownerId = winnerId) }
                events.add(GameEvent.PropertyPurchased(
                    state.turnNumber, winnerId, auction.tileIndex, auction.highestBid
                ))
            }
            return ActionResult.Success(newState, events)
        }

        val newBidderIndex = if (auction.currentBidderIndex >= newParticipants.size) 0
        else auction.currentBidderIndex
        val newAuction = auction.copy(
            participants = newParticipants,
            currentBidderIndex = newBidderIndex
        )

        return ActionResult.Success(state.copy(auctionState = newAuction), events)
    }

    // ── BuildUpgrade ──

    private fun handleBuildUpgrade(state: GameState, action: GameAction.BuildUpgrade): ActionResult {
        if (state.phase !in listOf(GamePhase.PRE_ROLL, GamePhase.POST_ROLL)) {
            return ActionResult.Invalid("No se puede construir en fase ${state.phase}")
        }

        val player = state.currentPlayer
        val tileDef = state.tileDefAt(action.tileIndex)
        val tileState = state.tileStateAt(action.tileIndex)

        if (tileState.ownerId != player.id) return ActionResult.Invalid("No es tu propiedad")
        if (tileState.isMortgaged) return ActionResult.Invalid("Propiedad hipotecada")
        if (tileDef.group == null) return ActionResult.Invalid("No es propiedad de grupo")
        if (!state.ownsFullGroup(player.id, tileDef.group)) {
            return ActionResult.Invalid("Debes poseer el grupo completo")
        }
        if (tileState.upgradeLevel >= tileDef.rentByLevel.size) {
            return ActionResult.Invalid("Nivel máximo alcanzado")
        }
        if (player.balance < tileDef.upgradeCost) return ActionResult.Invalid("Fondos insuficientes")

        val events = mutableListOf<GameEvent>()
        val newState = EconomyManager.payToBank(state, player.id, tileDef.upgradeCost)
            .updateTile(action.tileIndex) { it.copy(upgradeLevel = it.upgradeLevel + 1) }

        events.add(GameEvent.PropertyUpgraded(
            state.turnNumber, player.id, action.tileIndex,
            tileState.upgradeLevel + 1, tileDef.upgradeCost
        ))

        return ActionResult.Success(newState, events)
    }

    // ── MortgageProperty ──

    private fun handleMortgageProperty(state: GameState, action: GameAction.MortgageProperty): ActionResult {
        if (state.phase !in listOf(GamePhase.PRE_ROLL, GamePhase.POST_ROLL, GamePhase.PAYING_RENT)) {
            return ActionResult.Invalid("No se puede hipotecar en fase ${state.phase}")
        }

        val tileState = state.tileStateAt(action.tileIndex)
        if (tileState.ownerId != action.playerId) return ActionResult.Invalid("No es tu propiedad")
        if (tileState.isMortgaged) return ActionResult.Invalid("Ya está hipotecada")
        if (tileState.upgradeLevel > 0) return ActionResult.Invalid("Debe vender mejoras primero")

        val cashReceived = EconomyManager.mortgageValue(state, action.tileIndex)
        val events = mutableListOf<GameEvent>()
        val newState = EconomyManager.receiveFromBank(state, action.playerId, cashReceived)
            .updateTile(action.tileIndex) { it.copy(isMortgaged = true) }

        events.add(GameEvent.PropertyMortgaged(
            state.turnNumber, action.playerId, action.tileIndex, cashReceived
        ))
        return ActionResult.Success(newState, events)
    }

    // ── UnmortgageProperty ──

    private fun handleUnmortgageProperty(
        state: GameState,
        action: GameAction.UnmortgageProperty
    ): ActionResult {
        if (state.phase !in listOf(GamePhase.PRE_ROLL, GamePhase.POST_ROLL)) {
            return ActionResult.Invalid("No se puede deshypotecar en fase ${state.phase}")
        }

        val tileState = state.tileStateAt(action.tileIndex)
        if (tileState.ownerId != action.playerId) return ActionResult.Invalid("No es tu propiedad")
        if (!tileState.isMortgaged) return ActionResult.Invalid("No está hipotecada")

        val cost = EconomyManager.unmortgageCost(state, action.tileIndex)
        val player = state.playerById(action.playerId)
            ?: return ActionResult.Invalid("Jugador no encontrado")
        if (player.balance < cost) return ActionResult.Invalid("Fondos insuficientes")

        val events = mutableListOf<GameEvent>()
        val newState = EconomyManager.payToBank(state, action.playerId, cost)
            .updateTile(action.tileIndex) { it.copy(isMortgaged = false) }

        events.add(GameEvent.PropertyUnmortgaged(
            state.turnNumber, action.playerId, action.tileIndex, cost
        ))
        return ActionResult.Success(newState, events)
    }

    // ── PayJailFine ──

    private fun handlePayJailFine(state: GameState, @Suppress("UNUSED_PARAMETER") action: GameAction.PayJailFine): ActionResult {
        if (state.phase != GamePhase.PRE_ROLL) {
            return ActionResult.Invalid("No está en fase PRE_ROLL")
        }

        val player = state.currentPlayer
        if (!player.isInJail) return ActionResult.Invalid("El jugador no está en la cárcel")
        if (player.balance < state.config.jailFine) {
            return ActionResult.Invalid("Fondos insuficientes para la multa")
        }

        val events = mutableListOf<GameEvent>()
        val newState = EconomyManager.payToBank(state, player.id, state.config.jailFine)
            .updatePlayer(player.id) { it.copy(isInJail = false, turnsInJail = 0) }

        events.add(GameEvent.ReleasedFromJail(state.turnNumber, player.id, "Pago de multa"))
        return ActionResult.Success(newState, events)
    }

    // ── EndTurn ──

    private fun handleEndTurn(state: GameState, @Suppress("UNUSED_PARAMETER") action: GameAction.EndTurn): ActionResult {
        if (state.phase == GamePhase.PAYING_RENT) {
            // Resolver pago de renta pendiente antes de terminar turno
            val player = state.currentPlayer
            val tileState = state.tileStateAt(player.position)
            val tileDef = state.tileDefAt(player.position)
            val events = mutableListOf<GameEvent>()

            val amount = when (tileDef.type) {
                TileType.PROPERTY -> state.calculateRent(player.position)
                TileType.TAX -> tileDef.taxAmount
                else -> 0
            }

            if (amount > 0 && player.balance >= amount) {
                val newState = if (tileDef.type == TileType.PROPERTY && tileState.ownerId != null) {
                    events.add(GameEvent.RentPaid(
                        state.turnNumber, player.id, tileState.ownerId,
                        amount, player.position
                    ))
                    EconomyManager.transferMoney(state, player.id, tileState.ownerId, amount)
                } else if (tileDef.type == TileType.TAX) {
                    events.add(GameEvent.TaxPaid(state.turnNumber, player.id, amount))
                    EconomyManager.payToBank(state, player.id, amount)
                } else {
                    state
                }
                return advanceTurn(newState.copy(phase = GamePhase.POST_ROLL), events)
            }
        }

        if (state.phase != GamePhase.POST_ROLL) {
            return ActionResult.Invalid("No se puede terminar turno en fase ${state.phase}")
        }

        return advanceTurn(state, mutableListOf())
    }

    private fun advanceTurn(state: GameState, events: MutableList<GameEvent>): ActionResult {
        val nextIndex = findNextActivePlayer(state)
        val newTurn = if (nextIndex <= state.currentPlayerIndex) {
            state.turnNumber + 1
        } else {
            state.turnNumber
        }

        // Verificar límite de turnos
        if (newTurn > state.config.maxTurns) {
            return endGameByTurnLimit(state, events)
        }

        val newState = state.copy(
            currentPlayerIndex = nextIndex,
            phase = GamePhase.PRE_ROLL,
            turnNumber = newTurn,
            consecutiveDoubles = 0,
            lastDiceResult = null
        )
        return ActionResult.Success(newState, events)
    }

    // ── DeclareBankruptcy ──

    private fun handleDeclareBankruptcy(
        state: GameState,
        @Suppress("UNUSED_PARAMETER") action: GameAction.DeclareBankruptcy
    ): ActionResult {
        val events = mutableListOf<GameEvent>()
        val player = state.currentPlayer
        if (player.id != action.playerId) return ActionResult.Invalid("No es el turno de este jugador")

        events.add(GameEvent.PlayerBankrupt(
            state.turnNumber, player.id,
            player.balance, state.calculateNetWorth(player.id)
        ))

        // Liberar todas las propiedades
        var newState = state.updatePlayer(player.id) { it.copy(isBankrupt = true, balance = 0) }
        for (tileIdx in state.propertiesOwnedBy(player.id)) {
            newState = newState.updateTile(tileIdx) {
                it.copy(ownerId = null, upgradeLevel = 0, isMortgaged = false)
            }
        }

        // Verificar si el juego debe terminar (solo 1 jugador activo)
        val active = newState.activePlayers
        if (active.size <= 1) {
            val winnerId = active.firstOrNull()?.id
            events.add(GameEvent.GameEnded(
                state.turnNumber, player.id, winnerId,
                "Bancarrota del oponente", state.turnNumber
            ))
            return ActionResult.Success(newState.copy(phase = GamePhase.GAME_OVER), events)
        }

        // Continuar juego desde el siguiente jugador
        val nextIndex = findNextActivePlayer(newState)
        return ActionResult.Success(
            newState.copy(currentPlayerIndex = nextIndex, phase = GamePhase.PRE_ROLL),
            events
        )
    }

    // ═══════════════════════════════════════════════════════
    // FUNCIONES AUXILIARES
    // ═══════════════════════════════════════════════════════

    private fun findNextActivePlayer(state: GameState): Int {
        var idx = (state.currentPlayerIndex + 1) % state.players.size
        var safety = 0
        while (state.players[idx].isBankrupt && safety < state.players.size) {
            idx = (idx + 1) % state.players.size
            safety++
        }
        return idx
    }

    private fun endGameByTurnLimit(state: GameState, events: MutableList<GameEvent>): ActionResult {
        val winner = state.activePlayers.maxByOrNull { state.calculateNetWorth(it.id) }
        events.add(GameEvent.GameEnded(
            state.turnNumber, state.currentPlayer.id, winner?.id,
            "Límite de turnos alcanzado", state.turnNumber
        ))
        return ActionResult.Success(state.copy(phase = GamePhase.GAME_OVER), events)
    }

    private fun addPropertyManagementActions(
        state: GameState,
        playerId: PlayerId,
        actions: MutableList<GameAction>
    ) {
        addBuildActions(state, playerId, actions)
        addMortgageActions(state, playerId, actions)
        addUnmortgageActions(state, playerId, actions)
    }

    private fun addBuildActions(
        state: GameState,
        playerId: PlayerId,
        actions: MutableList<GameAction>
    ) {
        val player = state.playerById(playerId) ?: return
        for (tileIdx in state.propertiesOwnedBy(playerId)) {
            val tileDef = state.tileDefAt(tileIdx)
            val tileState = state.tileStateAt(tileIdx)
            if (tileDef.group != null &&
                state.ownsFullGroup(playerId, tileDef.group) &&
                !tileState.isMortgaged &&
                tileState.upgradeLevel < tileDef.rentByLevel.size &&
                player.balance >= tileDef.upgradeCost
            ) {
                actions.add(GameAction.BuildUpgrade(playerId, tileIdx))
            }
        }
    }

    private fun addMortgageActions(
        state: GameState,
        playerId: PlayerId,
        actions: MutableList<GameAction>
    ) {
        for (tileIdx in state.propertiesOwnedBy(playerId)) {
            val tileState = state.tileStateAt(tileIdx)
            if (!tileState.isMortgaged && tileState.upgradeLevel == 0) {
                actions.add(GameAction.MortgageProperty(playerId, tileIdx))
            }
        }
    }

    private fun addUnmortgageActions(
        state: GameState,
        playerId: PlayerId,
        actions: MutableList<GameAction>
    ) {
        val player = state.playerById(playerId) ?: return
        for (tileIdx in state.propertiesOwnedBy(playerId)) {
            val tileState = state.tileStateAt(tileIdx)
            if (tileState.isMortgaged &&
                player.balance >= EconomyManager.unmortgageCost(state, tileIdx)
            ) {
                actions.add(GameAction.UnmortgageProperty(playerId, tileIdx))
            }
        }
    }
}
