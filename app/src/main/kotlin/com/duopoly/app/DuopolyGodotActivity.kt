package com.duopoly.app

import org.godotengine.godot.GodotActivity

/**
 * Actividad concreta de Godot para la aplicación Duopoly.
 * Extiende GodotActivity para proporcionar una implementación instanciable.
 */
class DuopolyGodotActivity : GodotActivity() {
    override fun getCommandLine(): List<String> {
        return listOf("--main-pack", "res://Duopoly.pck")
    }
}
