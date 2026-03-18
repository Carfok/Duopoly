package com.duopoly.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DuopolyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Cargar librerías nativas de Godot manualmente para asegurar su disponibilidad
        try {
            System.loadLibrary("godot_android")
            android.util.Log.d("DuopolyApp", "libgodot_android loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("DuopolyApp", "Failed to load libgodot_android: ${e.message}")
        }
    }
}
