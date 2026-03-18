package com.duopoly.app

import com.duopoly.core.domain.engine.GameEngine

/**
 * Puente estático para conectar el GameEngine inyectado por Hilt
 * con el DuopolyBridge instanciado por el motor Godot.
 */
object GameEngineProvider {
    var instance: GameEngine? = null
}

