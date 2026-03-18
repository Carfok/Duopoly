# Arquitectura Técnica — Duopoly

## 1. Arquitectura Hexagonal (Clean Architecture)

El proyecto se divide en módulos Gradle desacoplados para garantizar la testabilidad del dominio sin dependencias de la UI o el sistema operativo.

- **:core**: Contiene el motor de reglas (`RuleEngine`) y el estado inmutable (`GameState`). Es Java/Kotlin puro.
- **:ai**: Implementa los puertos de estrategia. Contiene algoritmos de Reglas, Heurísticas y Monte Carlo Tree Search (MCTS).
- **:godot-bridge**: Adaptador que expone la lógica de Kotlin a Godot mediante JNI y el sistema de plugins de Android.
- **:app**: Punto de entrada de Android, gestión de ciclos de vida y DI con Hilt.

## 2. Puente de Comunicación (Android-Godot)

La comunicación es bidireccional y asíncrona:

- **Android -> Godot**: Uso de `emitSignal` para notificar cambios de estado (`player_moved`, `balance_changed`).
- **Godot -> Android**: Llamadas a métodos `@UsedByGodot` para ejecutar acciones (`rollDice`, `buyProperty`).

## 3. Optimizaciones de Fase 8

Se implementó un "Time-Boxing" en el algoritmo MCTS para asegurar que la experiencia en dispositivos de gama media no se vea degradada, limitando el tiempo de computación a 150ms por decisión.
