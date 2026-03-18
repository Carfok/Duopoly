# Guion de Defensa — TFM Duopoly

## Diapositiva 1-3: Introducción y Objetivos

- Presentación del problema: Juegos de estrategia como entorno de prueba para IA.
- Objetivo: Progresión medible Easy -> Medium -> Hard.

## Diapositiva 4-6: Arquitectura Hexagonal

- Explicación de por qué separar el dominio `:core` de Godot.
- Inmutabilidad del `GameState` y su importancia para el árbol de búsqueda de MCTS.

## Diapositiva 7-10: Implementación de IA

- Explicación de la heurística de valor económico.
- MCTS: Selección (UCB1), Expansión, Simulación y Retropropagación.

## Diapositiva 11-13: Resultados Big Data

- Muestra de la tabla de win rates (Fase 5).
- Validación de la hipótesis: Hard gana consistentemente a Medium.

## Diapositiva 14-15: Integración y UX

- Demostración del puente Godot.
- Importancia de los retardos artificiales para la percepción del usuario.

## Diapositiva 16: Conclusiones

- Escalabilidad del motor.
- Trabajo futuro: Integración de TFLite para evaluación de estados complejos.
