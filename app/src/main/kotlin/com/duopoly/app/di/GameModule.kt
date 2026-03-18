package com.duopoly.app.di

import com.duopoly.ai.easy.RuleBasedStrategy
import com.duopoly.ai.hard.HardStrategy
import com.duopoly.ai.medium.HeuristicStrategy
import com.duopoly.core.domain.engine.*
import com.duopoly.core.domain.model.AIDifficulty
import com.duopoly.core.domain.ports.AIStrategy
import com.duopoly.core.domain.ports.DiceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    @Provides
    @Singleton
    fun provideDiceProvider(): DiceProvider = DefaultDiceProvider()

    @Provides
    @Singleton
    fun provideRuleEngine(): RuleEngine = RuleEngine()

    @Provides
    @Singleton
    fun provideEconomyManager(): EconomyManager = EconomyManager

    @Provides
    @Singleton
    fun provideBoardFactory(): BoardFactory = BoardFactory

    @Provides
    @Named("Easy")
    fun provideEasyStrategy(): AIStrategy = RuleBasedStrategy()

    @Named("Medium")
    @Provides
    fun provideMediumStrategy(): AIStrategy = HeuristicStrategy()

    @Named("Hard")
    @Provides
    fun provideHardStrategy(ruleEngine: RuleEngine): AIStrategy = HardStrategy(ruleEngine)

    @Provides
    fun provideGameEngine(
        diceProvider: DiceProvider,
        ruleEngine: RuleEngine,
        @Named("Easy") easyAI: AIStrategy,
        @Named("Medium") mediumAI: AIStrategy,
        @Named("Hard") hardAI: AIStrategy
    ): GameEngine {
        val initialState = BoardFactory.createInitialGameState(
            player1Name = "Human",
            player2Name = "AI-Easy",
            player2IsAI = true,
            player2Difficulty = AIDifficulty.EASY
        )
        val strategies = mapOf(
            AIDifficulty.EASY to easyAI,
            AIDifficulty.MEDIUM to mediumAI,
            AIDifficulty.HARD to hardAI
        )
        return GameEngine(
            initialState = initialState,
            ruleEngine = ruleEngine,
            diceProvider = diceProvider,
            aiStrategies = strategies
        )
    }
}
