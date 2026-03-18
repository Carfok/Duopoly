package com.duopoly.app.di;

import com.duopoly.core.domain.engine.GameEngine;
import com.duopoly.core.domain.engine.RuleEngine;
import com.duopoly.core.domain.ports.AIStrategy;
import com.duopoly.core.domain.ports.DiceProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class GameModule_ProvideGameEngineFactory implements Factory<GameEngine> {
  private final Provider<DiceProvider> diceProvider;

  private final Provider<RuleEngine> ruleEngineProvider;

  private final Provider<AIStrategy> easyAIProvider;

  private final Provider<AIStrategy> mediumAIProvider;

  private final Provider<AIStrategy> hardAIProvider;

  public GameModule_ProvideGameEngineFactory(Provider<DiceProvider> diceProvider,
      Provider<RuleEngine> ruleEngineProvider, Provider<AIStrategy> easyAIProvider,
      Provider<AIStrategy> mediumAIProvider, Provider<AIStrategy> hardAIProvider) {
    this.diceProvider = diceProvider;
    this.ruleEngineProvider = ruleEngineProvider;
    this.easyAIProvider = easyAIProvider;
    this.mediumAIProvider = mediumAIProvider;
    this.hardAIProvider = hardAIProvider;
  }

  @Override
  public GameEngine get() {
    return provideGameEngine(diceProvider.get(), ruleEngineProvider.get(), easyAIProvider.get(), mediumAIProvider.get(), hardAIProvider.get());
  }

  public static GameModule_ProvideGameEngineFactory create(Provider<DiceProvider> diceProvider,
      Provider<RuleEngine> ruleEngineProvider, Provider<AIStrategy> easyAIProvider,
      Provider<AIStrategy> mediumAIProvider, Provider<AIStrategy> hardAIProvider) {
    return new GameModule_ProvideGameEngineFactory(diceProvider, ruleEngineProvider, easyAIProvider, mediumAIProvider, hardAIProvider);
  }

  public static GameEngine provideGameEngine(DiceProvider diceProvider, RuleEngine ruleEngine,
      AIStrategy easyAI, AIStrategy mediumAI, AIStrategy hardAI) {
    return Preconditions.checkNotNullFromProvides(GameModule.INSTANCE.provideGameEngine(diceProvider, ruleEngine, easyAI, mediumAI, hardAI));
  }
}
