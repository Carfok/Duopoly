package com.duopoly.app.di;

import com.duopoly.core.domain.ports.DiceProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class GameModule_ProvideDiceProviderFactory implements Factory<DiceProvider> {
  @Override
  public DiceProvider get() {
    return provideDiceProvider();
  }

  public static GameModule_ProvideDiceProviderFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DiceProvider provideDiceProvider() {
    return Preconditions.checkNotNullFromProvides(GameModule.INSTANCE.provideDiceProvider());
  }

  private static final class InstanceHolder {
    private static final GameModule_ProvideDiceProviderFactory INSTANCE = new GameModule_ProvideDiceProviderFactory();
  }
}
