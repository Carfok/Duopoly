package com.duopoly.bridge

import android.util.Log
import com.duopoly.core.domain.engine.GameEngine
import com.duopoly.core.domain.model.AIDifficulty
import com.duopoly.core.domain.model.GameAction
import com.duopoly.core.domain.model.PlayerId
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot

class DuopolyBridge(godot: Godot) : GodotPlugin(godot) {

    override fun getPluginName() = "DuopolyBridge"

    private var _gameEngine: GameEngine? = null
    val gameEngine: GameEngine? get() = _gameEngine

    @UsedByGodot
    fun initializeGame(initialBalance: Int, maxTurns: Int, difficultyId: Int) {
        Log.d("DuopolyBridge", "Initializing game: Balance=$initialBalance, Turns=$maxTurns, Difficulty=$difficultyId")
        val difficulty = when(difficultyId) {
            0 -> AIDifficulty.EASY
            1 -> AIDifficulty.MEDIUM
            else -> AIDifficulty.HARD
        }
        
        // El motor se inyectará o se creará aquí. 
        // Para simplificar, usaremos un setter desde MainActivity o Hilt
    }

    fun setGameEngine(engine: GameEngine) {
        this._gameEngine = engine
        // Registrar listeners para emitir señales a Godot
        // engine.registerListener(...)
    }

    @UsedByGodot
    fun getGameStateJson(): String {
        return "" // Serializar a JSON para Godot
    }

    @UsedByGodot
    fun rollDice() {
        Log.d("DuopolyBridge", "Rolling dice from Godot")
        _gameEngine?.let { engine ->
            val pId = engine.currentState.currentPlayer.id
            engine.executeAction(GameAction.RollDice(pId))
        }
    }

    @UsedByGodot
    fun buyProperty() {
        Log.d("DuopolyBridge", "Buying property from Godot")
        _gameEngine?.let { engine ->
            val pId = engine.currentState.currentPlayer.id
            engine.executeAction(GameAction.BuyProperty(pId))
        }
    }

    @UsedByGodot
    fun declineProperty() {
        _gameEngine?.let { engine ->
            val pId = engine.currentState.currentPlayer.id
            engine.executeAction(GameAction.DeclineProperty(pId))
        }
    }

    @UsedByGodot
    fun placeBid(amount: Int) {
        _gameEngine?.let { engine ->
            val pId = engine.currentState.currentPlayer.id
            engine.executeAction(GameAction.PlaceBid(pId, amount))
        }
    }

    @UsedByGodot
    fun withdrawFromAuction() {
        _gameEngine?.let { engine ->
            val pId = engine.currentState.currentPlayer.id
            engine.executeAction(GameAction.WithdrawFromAuction(pId))
        }
    }

    @UsedByGodot
    fun endTurn() {
        Log.d("DuopolyBridge", "Ending turn from Godot")
        _gameEngine?.let { engine ->
            val pId = engine.currentState.currentPlayer.id
            engine.executeAction(GameAction.EndTurn(pId))
        }
    }

    // Señales registradas para que Godot las escuche
    fun emitPlayerMoved(playerId: String, position: Int) {
        emitSignal("player_moved", playerId, position)
    }

    fun emitDiceRolled(die1: Int, die2: Int) {
        emitSignal("dice_rolled", die1, die2)
    }

    fun emitBalanceChanged(playerId: String, newBalance: Int) {
        emitSignal("balance_changed", playerId, newBalance)
    }

    fun emitGameEvent(eventJson: String) {
        emitSignal("game_event_received", eventJson)
    }

    fun emitAIThinkingStarted() {
        emitSignal("ai_thinking_started")
    }

    fun emitAIThinkingFinished() {
        emitSignal("ai_thinking_finished")
    }
}