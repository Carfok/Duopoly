package com.duopoly.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duopoly.core.domain.model.*
import com.duopoly.core.domain.engine.GameEngine
import com.duopoly.core.domain.ports.AIStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameEngine: GameEngine,
    @Named("Easy") private val easyAI: AIStrategy,
    @Named("Medium") private val mediumAI: AIStrategy,
    @Named("Hard") private val hardAI: AIStrategy
) : ViewModel() {

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState = _gameState.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking = _isThinking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val _auctionTimer = MutableStateFlow(0)
    val auctionTimer = _auctionTimer.asStateFlow()

    private var auctionJob: Job? = null

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun startGame(difficulty: AIDifficulty = AIDifficulty.EASY) {
        viewModelScope.launch {
            // Re-inicializar el motor con la dificultad seleccionada
            gameEngine.startNewGame(
                player1Name = "Jugador",
                player2Name = "IA",
                player2Difficulty = difficulty
            )
            _gameState.value = gameEngine.currentState
            
            // Si le toca a la IA nada más empezar (no habitual pero posible)
            checkAITurn()
        }
    }

    fun rollDice() {
        _gameState.value?.let { state ->
            performAction(GameAction.RollDice(state.currentPlayer.id))
        }
    }

    fun endTurn() {
        _gameState.value?.let { state ->
            performAction(GameAction.EndTurn(state.currentPlayer.id))
        }
    }

    fun buyProperty() {
        _gameState.value?.let { state ->
            performAction(GameAction.BuyProperty(state.currentPlayer.id))
        }
    }

    fun declineProperty() {
        _gameState.value?.let { state ->
            performAction(GameAction.DeclineProperty(state.currentPlayer.id))
        }
    }

    fun placeBid(amount: Int) {
        _gameState.value?.let { state ->
            performAction(GameAction.PlaceBid(state.currentPlayer.id, amount))
        }
    }

    fun withdrawFromAuction() {
        _gameState.value?.let { state ->
            performAction(GameAction.WithdrawFromAuction(state.currentPlayer.id))
        }
    }

    fun performAction(action: GameAction) {
        if (gameEngine.isGameOver) return

        viewModelScope.launch {
            gameEngine.executeAction(action)
            _gameState.value = gameEngine.currentState
            
            handleAuctionTimer()

            if (!gameEngine.isGameOver) {
                checkAITurn()
            }
        }
    }

    private fun handleAuctionTimer() {
        val state = _gameState.value ?: return
        if (state.phase == GamePhase.AUCTION) {
            startAuctionTimer()
        } else {
            auctionJob?.cancel()
            _auctionTimer.value = 0
        }
    }

    private fun startAuctionTimer() {
        auctionJob?.cancel()
        auctionJob = viewModelScope.launch {
            _auctionTimer.value = 15
            while (_auctionTimer.value > 0) {
                delay(1000)
                _auctionTimer.value -= 1
            }
            // Al terminar el tiempo, forzar retiro si es turno del humano para avanzar
            val state = _gameState.value
            if (state?.phase == GamePhase.AUCTION && state.currentPlayerIndex == 0) {
                withdrawFromAuction()
            }
        }
    }

    private suspend fun checkAITurn() {
        val state = _gameState.value ?: return
        val activePlayer = state.currentPlayer
        
        if (activePlayer.isAI && !state.isGameOver) {
            executeAILogic(state, activePlayer.aiDifficulty ?: AIDifficulty.EASY)
        }
    }

    private fun executeAILogic(state: GameState, difficulty: AIDifficulty) {
        viewModelScope.launch {
            _isThinking.value = true
            
            // Validar que el estado actual sigue siendo el esperado antes de ejecutar
            if (_gameState.value?.gameId != state.gameId) {
                _isThinking.value = false
                return@launch
            }

            // Pausa artificial dinámica
            val thinkDelay = when (difficulty) {
                AIDifficulty.EASY -> 600L
                AIDifficulty.MEDIUM -> 1000L
                AIDifficulty.HARD -> 1500L
            }
            
            val startTime = System.currentTimeMillis()

            // Esperar el delay artificial
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingDelay = thinkDelay - elapsedTime
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }

            withContext(Dispatchers.Default) {
                gameEngine.executeAITurn()
            }

            _gameState.value = gameEngine.currentState
            _isThinking.value = false

            if (!gameEngine.isGameOver) {
                checkAITurn()
            }
        }
    }
}
