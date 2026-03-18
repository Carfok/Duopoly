package com.duopoly.app.di;

import com.duopoly.core.domain.engine.BoardFactory;
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
public final class GameModule_ProvideBoardFactoryFactory implements Factory<BoardFactory> {
  @Override
  public BoardFactory get() {
    return provideBoardFactory();
  }

  public static GameModule_ProvideBoardFactoryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BoardFactory provideBoardFactory() {
    return Preconditions.checkNotNullFromProvides(GameModule.INSTANCE.provideBoardFactory());
  }

  private static final class InstanceHolder {
    private static final GameModule_ProvideBoardFactoryFactory INSTANCE = new GameModule_ProvideBoardFactoryFactory();
  }
}
