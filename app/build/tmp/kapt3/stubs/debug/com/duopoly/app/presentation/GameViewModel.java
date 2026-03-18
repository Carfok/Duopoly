package com.duopoly.app.presentation;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B-\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0001\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0001\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u001b\u001a\u00020\u001cJ\u000e\u0010\u001d\u001a\u00020\u001cH\u0082@\u00a2\u0006\u0002\u0010\u001eJ\u0006\u0010\u001f\u001a\u00020\u001cJ\u0006\u0010 \u001a\u00020\u001cJ\u0018\u0010!\u001a\u00020\u001c2\u0006\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020$H\u0002J\b\u0010%\u001a\u00020\u001cH\u0002J\u000e\u0010&\u001a\u00020\u001c2\u0006\u0010\'\u001a\u00020(J\u000e\u0010)\u001a\u00020\u001c2\u0006\u0010*\u001a\u00020\u000bJ\u0006\u0010+\u001a\u00020\u001cJ\b\u0010,\u001a\u00020\u001cH\u0002J\u0010\u0010-\u001a\u00020\u001c2\b\b\u0002\u0010#\u001a\u00020$J\u0006\u0010.\u001a\u00020\u001cJ\u0006\u0010/\u001a\u00020\u001cR\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0017\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0016R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0016R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0016R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2 = {"Lcom/duopoly/app/presentation/GameViewModel;", "Landroidx/lifecycle/ViewModel;", "gameEngine", "Lcom/duopoly/core/domain/engine/GameEngine;", "easyAI", "Lcom/duopoly/core/domain/ports/AIStrategy;", "mediumAI", "hardAI", "(Lcom/duopoly/core/domain/engine/GameEngine;Lcom/duopoly/core/domain/ports/AIStrategy;Lcom/duopoly/core/domain/ports/AIStrategy;Lcom/duopoly/core/domain/ports/AIStrategy;)V", "_auctionTimer", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_gameState", "Lcom/duopoly/core/domain/model/GameState;", "_isPaused", "", "_isThinking", "auctionJob", "Lkotlinx/coroutines/Job;", "auctionTimer", "Lkotlinx/coroutines/flow/StateFlow;", "getAuctionTimer", "()Lkotlinx/coroutines/flow/StateFlow;", "gameState", "getGameState", "isPaused", "isThinking", "buyProperty", "", "checkAITurn", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "declineProperty", "endTurn", "executeAILogic", "state", "difficulty", "Lcom/duopoly/core/domain/model/AIDifficulty;", "handleAuctionTimer", "performAction", "action", "Lcom/duopoly/core/domain/model/GameAction;", "placeBid", "amount", "rollDice", "startAuctionTimer", "startGame", "togglePause", "withdrawFromAuction", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class GameViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.duopoly.core.domain.engine.GameEngine gameEngine = null;
    @org.jetbrains.annotations.NotNull
    private final com.duopoly.core.domain.ports.AIStrategy easyAI = null;
    @org.jetbrains.annotations.NotNull
    private final com.duopoly.core.domain.ports.AIStrategy mediumAI = null;
    @org.jetbrains.annotations.NotNull
    private final com.duopoly.core.domain.ports.AIStrategy hardAI = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.duopoly.core.domain.model.GameState> _gameState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.duopoly.core.domain.model.GameState> gameState = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isThinking = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isThinking = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isPaused = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPaused = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Integer> _auctionTimer = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> auctionTimer = null;
    @org.jetbrains.annotations.Nullable
    private kotlinx.coroutines.Job auctionJob;
    
    @javax.inject.Inject
    public GameViewModel(@org.jetbrains.annotations.NotNull
    com.duopoly.core.domain.engine.GameEngine gameEngine, @javax.inject.Named(value = "Easy")
    @org.jetbrains.annotations.NotNull
    com.duopoly.core.domain.ports.AIStrategy easyAI, @javax.inject.Named(value = "Medium")
    @org.jetbrains.annotations.NotNull
    com.duopoly.core.domain.ports.AIStrategy mediumAI, @javax.inject.Named(value = "Hard")
    @org.jetbrains.annotations.NotNull
    com.duopoly.core.domain.ports.AIStrategy hardAI) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.duopoly.core.domain.model.GameState> getGameState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isThinking() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPaused() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getAuctionTimer() {
        return null;
    }
    
    public final void togglePause() {
    }
    
    public final void startGame(@org.jetbrains.annotations.NotNull
    com.duopoly.core.domain.model.AIDifficulty difficulty) {
    }
    
    public final void rollDice() {
    }
    
    public final void endTurn() {
    }
    
    public final void buyProperty() {
    }
    
    public final void declineProperty() {
    }
    
    public final void placeBid(int amount) {
    }
    
    public final void withdrawFromAuction() {
    }
    
    public final void performAction(@org.jetbrains.annotations.NotNull
    com.duopoly.core.domain.model.GameAction action) {
    }
    
    private final void handleAuctionTimer() {
    }
    
    private final void startAuctionTimer() {
    }
    
    private final java.lang.Object checkAITurn(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void executeAILogic(com.duopoly.core.domain.model.GameState state, com.duopoly.core.domain.model.AIDifficulty difficulty) {
    }
}