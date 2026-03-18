package com.duopoly.app.di;

import com.duopoly.core.domain.ports.AIStrategy;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class GameModule_ProvideMediumStrategyFactory implements Factory<AIStrategy> {
  @Override
  public AIStrategy get() {
    return provideMediumStrategy();
  }

  public static GameModule_ProvideMediumStrategyFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AIStrategy provideMediumStrategy() {
    return Preconditions.checkNotNullFromProvides(GameModule.INSTANCE.provideMediumStrategy());
  }

  private static final class InstanceHolder {
    private static final GameModule_ProvideMediumStrategyFactory INSTANCE = new GameModule_ProvideMediumStrategyFactory();
  }
}
