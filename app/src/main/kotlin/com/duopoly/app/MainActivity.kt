package com.duopoly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var gameEngine: com.duopoly.core.domain.engine.GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivity", "Starting MainActivity")
        
        try {
            // Verificar si nuestra clase DuopolyGodotActivity es accesible
            Class.forName("com.duopoly.app.DuopolyGodotActivity")
            android.util.Log.d("MainActivity", "DuopolyGodotActivity class found")

            val intent = Intent(this, DuopolyGodotActivity::class.java)
            intent.putExtra("game_engine_ready", true)
            
            GameEngineProvider.instance = gameEngine
            android.util.Log.d("MainActivity", "GameEngine set in provider")
            
            startActivity(intent)
            android.util.Log.d("MainActivity", "Godot activity started")
            
            finish()
        } catch (e: ClassNotFoundException) {
            android.util.Log.e("MainActivity", "DuopolyGodotActivity not found!")
            e.printStackTrace()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error launching Godot: ${e.message}")
            e.printStackTrace()
        }
    }
}
