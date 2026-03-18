package com.duopoly.app.di;

import com.duopoly.core.domain.engine.RuleEngine;
import com.duopoly.core.domain.ports.AIStrategy;
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
public final class GameModule_ProvideHardStrategyFactory implements Factory<AIStrategy> {
  private final Provider<RuleEngine> ruleEngineProvider;

  public GameModule_ProvideHardStrategyFactory(Provider<RuleEngine> ruleEngineProvider) {
    this.ruleEngineProvider = ruleEngineProvider;
  }

  @Override
  public AIStrategy get() {
    return provideHardStrategy(ruleEngineProvider.get());
  }

  public static GameModule_ProvideHardStrategyFactory create(
      Provider<RuleEngine> ruleEngineProvider) {
    return new GameModule_ProvideHardStrategyFactory(ruleEngineProvider);
  }

  public static AIStrategy provideHardStrategy(RuleEngine ruleEngine) {
    return Preconditions.checkNotNullFromProvides(GameModule.INSTANCE.provideHardStrategy(ruleEngine));
  }
}
