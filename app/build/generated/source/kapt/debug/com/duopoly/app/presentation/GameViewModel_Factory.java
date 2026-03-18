package com.duopoly.app.presentation;

import com.duopoly.core.domain.engine.GameEngine;
import com.duopoly.core.domain.ports.AIStrategy;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class GameViewModel_Factory implements Factory<GameViewModel> {
  private final Provider<GameEngine> gameEngineProvider;

  private final Provider<AIStrategy> easyAIProvider;

  private final Provider<AIStrategy> mediumAIProvider;

  private final Provider<AIStrategy> hardAIProvider;

  public GameViewModel_Factory(Provider<GameEngine> gameEngineProvider,
      Provider<AIStrategy> easyAIProvider, Provider<AIStrategy> mediumAIProvider,
      Provider<AIStrategy> hardAIProvider) {
    this.gameEngineProvider = gameEngineProvider;
    this.easyAIProvider = easyAIProvider;
    this.mediumAIProvider = mediumAIProvider;
    this.hardAIProvider = hardAIProvider;
  }

  @Override
  public GameViewModel get() {
    return newInstance(gameEngineProvider.get(), easyAIProvider.get(), mediumAIProvider.get(), hardAIProvider.get());
  }

  public static GameViewModel_Factory create(Provider<GameEngine> gameEngineProvider,
      Provider<AIStrategy> easyAIProvider, Provider<AIStrategy> mediumAIProvider,
      Provider<AIStrategy> hardAIProvider) {
    return new GameViewModel_Factory(gameEngineProvider, easyAIProvider, mediumAIProvider, hardAIProvider);
  }

  public static GameViewModel newInstance(GameEngine gameEngine, AIStrategy easyAI,
      AIStrategy mediumAI, AIStrategy hardAI) {
    return new GameViewModel(gameEngine, easyAI, mediumAI, hardAI);
  }
}
