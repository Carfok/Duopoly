package com.duopoly.app.di;

import com.duopoly.core.domain.engine.EconomyManager;
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
public final class GameModule_ProvideEconomyManagerFactory implements Factory<EconomyManager> {
  @Override
  public EconomyManager get() {
    return provideEconomyManager();
  }

  public static GameModule_ProvideEconomyManagerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EconomyManager provideEconomyManager() {
    return Preconditions.checkNotNullFromProvides(GameModule.INSTANCE.provideEconomyManager());
  }

  private static final class InstanceHolder {
    private static final GameModule_ProvideEconomyManagerFactory INSTANCE = new GameModule_ProvideEconomyManagerFactory();
  }
}
