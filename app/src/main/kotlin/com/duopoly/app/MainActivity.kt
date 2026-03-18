package com.duopoly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Iniciar directamente la actividad de Godot
        // Los menús ahora se manejan dentro del proyecto de Godot
        try {
            val intent = Intent(this, Class.forName("org.godotengine.godot.Godot"))
            startActivity(intent)
            finish() // Cerramos la activity de Android para que no quede debajo
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


