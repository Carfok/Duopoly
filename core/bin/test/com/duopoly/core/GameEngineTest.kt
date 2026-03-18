package com.duopoly.core

import com.duopoly.core.domain.engine.*
import com.duopoly.core.domain.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName

@DisplayName("Duopoly Core - Tests del Motor de Juego")
class GameEngineTest {

    private val engine = RuleEngine()

    // ═══════════════════════════════════════════
    // FASE 1: Tests del Modelo Formal
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Inicialización del Estado")
    inner class InitializationTests {

        @Test
        fun `estado inicial tiene 2 jugadores`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            assertEquals(2, state.players.size)
        }

        @Test
        fun `tablero tiene 24 casillas`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            assertEquals(24, state.board.size)
            assertEquals(24, state.tileStates.size)
        }

        @Test
        fun `estado inicial en fase PRE_ROLL turno 1`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            assertEquals(GamePhase.PRE_ROLL, state.phase)
            assertEquals(0, state.currentPlayerIndex)
            assertEquals(1, state.turnNumber)
            assertFalse(state.isGameOver)
        }

        @Test
        fun `jugadores comienzan en posición 0 con balance correcto`() {
            val config = GameConfig(startingBalance = 2000)
            val state = BoardFactory.createInitialGameState("P1", "P2", config = config)
            state.players.forEach { player ->
                assertEquals(0, player.position)
                assertEquals(2000, player.balance)
                assertFalse(player.isBankrupt)
                assertFalse(player.isInJail)
            }
        }

        @Test
        fun `todas las propiedades comienzan sin dueño`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
            state.tileStates.forEach { tile ->
                assertNull(tile.ownerId)
                assertEquals(0, tile.upgradeLevel)
                assertFalse(tile.isMortgaged)
            }
        }

        @Test
        fun `tablero tiene 12 propiedades en 4 grupos`() {
            val board = BoardFactory.createStandardBoard()
            val properties = board.filter { it.type == TileType.PROPERTY }
            assertEquals(12, properties.size)

            val groups = properties.mapNotNull { it.group }.map { it.id }.distinct()
            assertEquals(4, groups.size)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Inmutabilidad
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Inmutabilidad del Estado")
    inner class ImmutabilityTests {

        @Test
        fun `updatePlayer no modifica el estado original`() {
            val state1 = BoardFactory.createInitialGameState("P1", "P2")
            val state2 = state1.updatePlayer(PlayerId("p1")) { it.copy(balance = 999) }

            assertEquals(1500, state1.playerById(PlayerId("p1"))!!.balance)
            assertEquals(999, state2.playerById(PlayerId("p1"))!!.balance)
        }

        @Test
        fun `updateTile no modifica el estado original`() {
            val state1 = BoardFactory.createInitialGameState("P1", "P2")
            val state2 = state1.updateTile(1) { it.copy(ownerId = PlayerId("p1")) }

            assertNull(state1.tileStateAt(1).ownerId)
            assertEquals(PlayerId("p1"), state2.tileStateAt(1).ownerId)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Movimiento
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Movimiento de Jugadores")
    inner class MovementTests {

        @Test
        fun `lanzar dados mueve al jugador a posición correcta`() {
            val state = BoardFactory.createInitialGameState("P1", "IA", player2IsAI = true)
            // DiceResult(2, 4) = total 6, aterriza en casilla 6 (Cárcel - solo visitando)
            val dice = SequentialDiceProvider(listOf(DiceResult(2, 4)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)

            assertTrue(result is ActionResult.Success)
            val newState = (result as ActionResult.Success).newState
            assertEquals(6, newState.playerById(PlayerId("p1"))!!.position)
            assertEquals(GamePhase.POST_ROLL, newState.phase)
        }

        @Test
        fun `aterrizar en propiedad sin dueño activa BUYING_DECISION`() {
            val state = BoardFactory.createInitialGameState("P1", "IA", player2IsAI = true)
            // DiceResult(1, 2) = 3, aterriza en casilla 3 (CloudHub, PROPERTY, precio 80)
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)

            assertTrue(result is ActionResult.Success)
            val newState = (result as ActionResult.Success).newState
            assertEquals(3, newState.playerById(PlayerId("p1"))!!.position)
            assertEquals(GamePhase.BUYING_DECISION, newState.phase)
        }

        @Test
        fun `aterrizar en casilla de impuesto descuenta el monto`() {
            val state = BoardFactory.createInitialGameState("P1", "IA", player2IsAI = true)
            // DiceResult(2, 2) = 4, aterriza en casilla 4 (Impuesto Digital, 100)
            val dice = SequentialDiceProvider(listOf(DiceResult(2, 2)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)

            assertTrue(result is ActionResult.Success)
            val newState = (result as ActionResult.Success).newState
            assertEquals(1400, newState.playerById(PlayerId("p1"))!!.balance)
            assertEquals(GamePhase.POST_ROLL, newState.phase)
        }

        @Test
        fun `pasar por Salida otorga salario`() {
            // Mover al jugador a posición 22, luego lanzar dados para pasar por Salida
            val state = BoardFactory.createInitialGameState("P1", "IA")
                .updatePlayer(PlayerId("p1")) { it.copy(position = 22) }

            // DiceResult(1, 2) = 3, 22 + 3 = 25 % 24 = 1, pasa por GO
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)

            assertTrue(result is ActionResult.Success)
            val newState = (result as ActionResult.Success).newState
            val player = newState.playerById(PlayerId("p1"))!!

            assertEquals(1, player.position)
            // 1500 + 200 (salario) = 1700. Casilla 1 es DataNet (propiedad), fase BUYING
            assertEquals(1700, player.balance)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Compra de Propiedades
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Compra de Propiedades")
    inner class PurchaseTests {

        @Test
        fun `comprar propiedad descuenta precio y asigna dueño`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            // Mover a casilla 3 (CloudHub, precio 80)
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val rollResult = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            val buyResult = engine.applyAction(rollResult.newState, GameAction.BuyProperty(PlayerId("p1")))
                as ActionResult.Success

            val finalState = buyResult.newState
            assertEquals(1500 - 80, finalState.playerById(PlayerId("p1"))!!.balance)
            assertEquals(PlayerId("p1"), finalState.tileStateAt(3).ownerId)
            assertEquals(GamePhase.POST_ROLL, finalState.phase)
        }

        @Test
        fun `no puede comprar si fondos insuficientes`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
                .updatePlayer(PlayerId("p1")) { it.copy(balance = 50) }

            // Mover a casilla 3 (CloudHub, precio 80) — no puede comprar
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val rollResult = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            // Solo DeclineProperty debería estar disponible
            val validActions = engine.getValidActions(rollResult.newState)
            assertTrue(validActions.any { it is GameAction.DeclineProperty })
            assertFalse(validActions.any { it is GameAction.BuyProperty })
        }

        @Test
        fun `declinar compra inicia subasta`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val rollResult = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            val declineResult = engine.applyAction(
                rollResult.newState,
                GameAction.DeclineProperty(PlayerId("p1"))
            ) as ActionResult.Success

            assertEquals(GamePhase.AUCTION, declineResult.newState.phase)
            assertNotNull(declineResult.newState.auctionState)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Renta
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Pago de Renta")
    inner class RentTests {

        @Test
        fun `pagar renta transfiere dinero al dueño`() {
            // P2 posee casilla 3 (CloudHub, renta base 6)
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(3) { it.copy(ownerId = PlayerId("p2")) }

            // P1 aterriza en casilla 3
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            val newState = result.newState
            assertEquals(1500 - 6, newState.playerById(PlayerId("p1"))!!.balance)
            assertEquals(1500 + 6, newState.playerById(PlayerId("p2"))!!.balance)
        }

        @Test
        fun `propiedad hipotecada no cobra renta`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(3) { it.copy(ownerId = PlayerId("p2"), isMortgaged = true) }

            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            // Balance no cambia (renta = 0 por hipoteca)
            assertEquals(1500, result.newState.playerById(PlayerId("p1"))!!.balance)
            assertEquals(1500, result.newState.playerById(PlayerId("p2"))!!.balance)
        }

        @Test
        fun `grupo completo duplica renta base`() {
            // P2 posee las 3 propiedades del grupo Tech (casillas 1, 3, 5)
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(1) { it.copy(ownerId = PlayerId("p2")) }
                .updateTile(3) { it.copy(ownerId = PlayerId("p2")) }
                .updateTile(5) { it.copy(ownerId = PlayerId("p2")) }

            // cloudHub base rent = 6, con grupo completo = 6 * 2.0 = 12
            val rent = state.calculateRent(3)
            assertEquals(12, rent)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Cárcel
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Sistema de Cárcel")
    inner class JailTests {

        @Test
        fun `aterrizar en 'Ir a Cárcel' envía al jugador a la cárcel`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
                .updatePlayer(PlayerId("p1")) { it.copy(position = 16) }

            // DiceResult(1, 1) = 2, 16 + 2 = 18 (Ir a Cárcel)
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 1)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            val player = result.newState.playerById(PlayerId("p1"))!!
            assertEquals(6, player.position) // posición de la cárcel
            assertTrue(player.isInJail)
        }

        @Test
        fun `pagar multa libera de la cárcel`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
                .updatePlayer(PlayerId("p1")) { it.copy(position = 6, isInJail = true) }

            val result = engine.applyAction(state, GameAction.PayJailFine(PlayerId("p1")))
                as ActionResult.Success

            val player = result.newState.playerById(PlayerId("p1"))!!
            assertFalse(player.isInJail)
            assertEquals(1500 - 50, player.balance) // multa = 50
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Economía
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Sistema Económico")
    inner class EconomyTests {

        @Test
        fun `patrimonio neto incluye propiedades`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(1) { it.copy(ownerId = PlayerId("p1")) }

            // Net worth = 1500 (balance) + 60 (DataNet price)
            val netWorth = state.calculateNetWorth(PlayerId("p1"))
            assertEquals(1560, netWorth)
        }

        @Test
        fun `hipotecar propiedad otorga 50% del precio`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(1) { it.copy(ownerId = PlayerId("p1")) }

            // DataNet precio = 60, hipoteca = 60 * 0.5 = 30
            val mortgageValue = EconomyManager.mortgageValue(state, 1)
            assertEquals(30, mortgageValue)
        }

        @Test
        fun `deshypotecar cuesta principal + penalización`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(1) { it.copy(ownerId = PlayerId("p1"), isMortgaged = true) }

            // DataNet: 60 * 0.5 (principal) + 60 * 0.1 (penalización) = 30 + 6 = 36
            val cost = EconomyManager.unmortgageCost(state, 1)
            assertEquals(36, cost)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Flujo de Turno
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Flujo de Turnos")
    inner class TurnFlowTests {

        @Test
        fun `terminar turno avanza al siguiente jugador`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .copy(phase = GamePhase.POST_ROLL) // simular que ya lanzó dados

            val result = engine.applyAction(state, GameAction.EndTurn(PlayerId("p1")))
                as ActionResult.Success

            assertEquals(1, result.newState.currentPlayerIndex) // turno de P2
            assertEquals(GamePhase.PRE_ROLL, result.newState.phase)
        }

        @Test
        fun `no se puede lanzar dados en fase POST_ROLL`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .copy(phase = GamePhase.POST_ROLL)

            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")))
            assertTrue(result is ActionResult.Invalid)
        }

        @Test
        fun `no se puede terminar turno en fase PRE_ROLL`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")

            val result = engine.applyAction(state, GameAction.EndTurn(PlayerId("p1")))
            assertTrue(result is ActionResult.Invalid)
        }

        @Test
        fun `acciones válidas en PRE_ROLL incluyen RollDice`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
            val actions = engine.getValidActions(state)
            assertTrue(actions.any { it is GameAction.RollDice })
        }

        @Test
        fun `acciones válidas en POST_ROLL incluyen EndTurn`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .copy(phase = GamePhase.POST_ROLL)
            val actions = engine.getValidActions(state)
            assertTrue(actions.any { it is GameAction.EndTurn })
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Bancarrota y Fin de Juego
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Bancarrota y Fin de Juego")
    inner class BankruptcyTests {

        @Test
        fun `bancarrota de un jugador termina el juego`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")

            val result = engine.applyAction(state, GameAction.DeclareBankruptcy(PlayerId("p1")))
                as ActionResult.Success

            assertTrue(result.newState.isGameOver)
            assertTrue(result.newState.playerById(PlayerId("p1"))!!.isBankrupt)
        }

        @Test
        fun `propiedades del jugador en bancarrota se liberan`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
                .updateTile(1) { it.copy(ownerId = PlayerId("p1")) }
                .updateTile(3) { it.copy(ownerId = PlayerId("p1")) }

            val result = engine.applyAction(state, GameAction.DeclareBankruptcy(PlayerId("p1")))
                as ActionResult.Success

            assertNull(result.newState.tileStateAt(1).ownerId)
            assertNull(result.newState.tileStateAt(3).ownerId)
        }

        @Test
        fun `límite de turnos termina el juego`() {
            val config = GameConfig(maxTurns = 1)
            val state = BoardFactory.createInitialGameState("P1", "P2", config = config)
                .copy(turnNumber = 2, phase = GamePhase.POST_ROLL) // ya superó maxTurns

            val result = engine.applyAction(state, GameAction.EndTurn(PlayerId("p1")))
                as ActionResult.Success

            assertTrue(result.newState.isGameOver)
        }
    }

    // ═══════════════════════════════════════════
    // FASE 2: Tests de Eventos
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Generación de Eventos")
    inner class EventTests {

        @Test
        fun `lanzar dados genera evento DiceRolled y PlayerMoved`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            val dice = SequentialDiceProvider(listOf(DiceResult(2, 4)))
            val result = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            assertTrue(result.events.any { it is GameEvent.DiceRolled })
            assertTrue(result.events.any { it is GameEvent.PlayerMoved })
        }

        @Test
        fun `comprar propiedad genera evento PropertyPurchased`() {
            val state = BoardFactory.createInitialGameState("P1", "IA")
            val dice = SequentialDiceProvider(listOf(DiceResult(1, 2)))
            val rollResult = engine.applyAction(state, GameAction.RollDice(PlayerId("p1")), dice)
                as ActionResult.Success

            val buyResult = engine.applyAction(rollResult.newState, GameAction.BuyProperty(PlayerId("p1")))
                as ActionResult.Success

            assertTrue(buyResult.events.any { it is GameEvent.PropertyPurchased })
            val event = buyResult.events.filterIsInstance<GameEvent.PropertyPurchased>().first()
            assertEquals(3, event.tileIndex)
            assertEquals(80, event.price)
        }

        @Test
        fun `bancarrota genera eventos PlayerBankrupt y GameEnded`() {
            val state = BoardFactory.createInitialGameState("P1", "P2")
            val result = engine.applyAction(state, GameAction.DeclareBankruptcy(PlayerId("p1")))
                as ActionResult.Success

            assertTrue(result.events.any { it is GameEvent.PlayerBankrupt })
            assertTrue(result.events.any { it is GameEvent.GameEnded })
        }
    }

    // ═══════════════════════════════════════════
    // Tests de DiceResult
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("Validación de Dados")
    inner class DiceTests {

        @Test
        fun `dados válidos entre 1 y 6`() {
            val dice = DiceResult(1, 6)
            assertEquals(7, dice.total)
            assertFalse(dice.isDoubles)
        }

        @Test
        fun `dobles detectados correctamente`() {
            val dice = DiceResult(3, 3)
            assertTrue(dice.isDoubles)
            assertEquals(6, dice.total)
        }

        @Test
        fun `dados inválidos lanzan excepción`() {
            assertThrows(IllegalArgumentException::class.java) {
                DiceResult(0, 3)
            }
            assertThrows(IllegalArgumentException::class.java) {
                DiceResult(3, 7)
            }
        }
    }
}
