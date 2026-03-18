# PLAN MAESTRO DE EJECUCIÓN — DUOPOLY

## Trabajo de Fin de Máster: IA y Big Data

> **Proyecto:** Juego estratégico por turnos (inspirado en Monopoly) con IA multinivel  
> **Plataforma:** Android (Kotlin) + Godot 2D  
> **Arquitectura:** Clean Architecture Hexagonal con módulos Gradle desacoplados  
> **Fecha de inicio estimada:** Marzo 2026

---

## 1. RESUMEN EJECUTIVO

Duopoly es un juego estratégico de tablero para 2 jugadores (Humano vs IA), donde la IA
opera en tres niveles de sofisticación progresiva (Easy, Medium, Hard). El objetivo académico
es demostrar empíricamente la mejora de rendimiento entre niveles de IA mediante análisis
estadístico de datos masivos de partidas simuladas.

El proyecto se ejecuta en 10 fases estrictamente secuenciales, donde cada fase produce
artefactos verificables que alimentan la siguiente.

---

## 2. MAPA DE DEPENDENCIAS ENTRE FASES

```
F1 ──→ F2 ──→ F3 ──┬──→ F4 ──→ F5
                    │              │
                    │              ▼
                    └──→ F6 ──→ F7 ──→ F8 ──→ F9
                                                │
                                                ▼
                                               F10
```

| Fase | Depende de | Bloquea a |
| ---- | ---------- | --------- |
| F1   | —          | F2        |
| F2   | F1         | F3, F6    |
| F3   | F2         | F4, F5    |
| F4   | F2, F3     | F5, F8    |
| F5   | F3, F4     | F10       |
| F6   | F2         | F7        |
| F7   | F6         | F8, F9    |
| F8   | F4, F7     | F9        |
| F9   | F7, F8     | F10       |
| F10  | Todas      | —         |

**Ruta Crítica:** F1 → F2 → F3 → F4 → F5 → F10  
**Ruta Paralela (UI):** F6 → F7 → F8 → F9 (puede avanzar en paralelo desde F6 una vez F2 esté completa)

---

## 3. FASE 1 — DOMINIO Y MODELO FORMAL

### Objetivo

Definir matemáticamente el sistema de juego antes de escribir una línea de lógica de negocio.

### Modelo Formal

**Estado del Juego S:**

```
S = (P, B, τ, φ, δ, C)
donde:
  P = {p₁, p₂}         — estados de jugadores
  pᵢ = (id, pos, bal, props, jail, bankrupt, isAI, difficulty)
  B = {b₀, ..., b₂₃}   — estados de casillas
  bⱼ = (owner, level, mortgaged)
  τ ∈ ℕ                 — número de turno
  φ ∈ {PRE_ROLL, POST_ROLL, BUYING, AUCTION, PAYING, GAME_OVER} — fase
  δ = (d₁, d₂)          — último resultado de dados
  C = configuración      — parámetros tunables del juego
```

**Espacio de Acciones A:**

```
A = { RollDice, BuyProperty, DeclineProperty, PlaceBid, WithdrawBid,
      BuildUpgrade(tile), Mortgage(tile), Unmortgage(tile),
      PayJailFine, EndTurn, DeclareBankruptcy }
```

**Función de Transición T:**

```
T: S × A × Ω → S'
donde Ω es el espacio muestral de los dados
T es determinista dado (S, A, ω)
```

**Condiciones de Victoria V:**

```
V(S) = true ⟺ (∃i: pᵢ.bankrupt = true) ∨ (τ > τ_max ∧ winner = argmax_i(netWorth(pᵢ)))
```

**Invariantes del Sistema I:**

```
I₁: ∀i: pᵢ.balance ≥ 0 ∨ pᵢ.bankrupt = true        (solvencia)
I₂: Σ dinero_en_sistema ≤ constante                    (conservación monetaria)
I₃: ∀i: 0 ≤ pᵢ.position < |Board|                     (posición válida)
I₄: ∀j: bⱼ.owner ∈ {null, p₁.id, p₂.id}              (propiedad válida)
I₅: ∀j: 0 ≤ bⱼ.level ≤ max_level                      (nivel de mejora acotado)
I₆: ∀j: bⱼ.level > 0 → bⱼ.mortgaged = false           (no hipotecar con mejoras)
I₇: Turno solo avanza cuando fase = POST_ROLL + EndTurn (flujo determinista)
```

### Subtareas Técnicas

| #   | Subtarea                                     | Entregable                             |
| --- | -------------------------------------------- | -------------------------------------- |
| 1.1 | Formalizar S, A, T en documento              | Sección "Modelo Formal" en memoria TFM |
| 1.2 | Diseñar `GameState` inmutable (data class)   | `Models.kt`                            |
| 1.3 | Diseñar `GameAction` (sealed class)          | `Actions.kt`                           |
| 1.4 | Diseñar `GameEvent` (sealed class)           | `Events.kt`                            |
| 1.5 | Definir interfaces hexagonales (ports)       | `Ports.kt`                             |
| 1.6 | Diseñar tablero de 24 casillas               | `BoardFactory.kt`                      |
| 1.7 | Validar invariantes como assertions en tests | `InvariantTest.kt`                     |
| 1.8 | Crear diagrama de clases UML                 | Diagrama exportable                    |

### Riesgos

| Riesgo                       | Probabilidad | Impacto | Mitigación                                     |
| ---------------------------- | ------------ | ------- | ---------------------------------------------- |
| Scope creep en formalización | Media        | Alto    | Limitar a 2 páginas de LaTeX                   |
| Modelo demasiado abstracto   | Baja         | Medio   | Validar con implementación real desde F1       |
| Omitir mecánicas necesarias  | Media        | Alto    | Mapear todas las mecánicas antes de formalizar |

### Señales de Sobreingeniería

- ⚠️ Escribir más de 3 páginas de formalización matemática
- ⚠️ Crear diagramas UML con >20 clases antes de implementar
- ⚠️ Debatir patrones de diseño sin haber escrito un solo test

### Complejidad: 2/5 | Duración Estimada: 1-2 semanas

---

## 4. FASE 2 — IMPLEMENTACIÓN DEL MÓDULO :core

### Objetivo

Construir un motor de juego completamente funcional que pueda ejecutar una partida completa
por consola, sin dependencias de Android ni Godot.

### Subtareas Técnicas

| #    | Subtarea                             | Descripción                                         | Fichero                  |
| ---- | ------------------------------------ | --------------------------------------------------- | ------------------------ |
| 2.1  | Implementar modelos inmutables       | `PlayerId`, `PlayerState`, `TileState`, `GameState` | `Models.kt`              |
| 2.2  | Implementar acciones tipadas         | Sealed class con todas las acciones                 | `Actions.kt`             |
| 2.3  | Implementar sistema de eventos       | Sealed class para logging/UI                        | `Events.kt`              |
| 2.4  | Implementar `RuleEngine`             | Función de transición T(S,A) → S' pura              | `RuleEngine.kt`          |
| 2.5  | Implementar `EconomyManager`         | Transferencias, rentas, hipotecas                   | `EconomyManager.kt`      |
| 2.6  | Implementar `BoardFactory`           | Tablero de 24 casillas parametrizable               | `BoardFactory.kt`        |
| 2.7  | Implementar `DiceProvider`           | Interfaz + impl. random + determinista              | `DefaultDiceProvider.kt` |
| 2.8  | Implementar `GameEngine`             | Orquestador principal del juego                     | `GameEngine.kt`          |
| 2.9  | Implementar `TurnManager`            | Utilidades de gestión de turnos                     | `TurnManager.kt`         |
| 2.10 | Battery de tests unitarios           | Cobertura ≥ 80% de RuleEngine                       | `GameEngineTest.kt`      |
| 2.11 | Test de partida completa por consola | Ejecución end-to-end sin UI                         | `FullGameTest.kt`        |

### Decisiones de Diseño Clave

1. **GameState inmutable**: Cada acción genera un NUEVO estado. Esto permite:
   - Búsqueda de árbol en MCTS sin clonación
   - Snapshot para Big Data
   - Replay determinista
   - Testing sin side effects

2. **RuleEngine puro**: Sin estado interno. `applyAction(state, action, dice) → (newState, events)`.

3. **Dados inyectados**: El `DiceProvider` se pasa al engine, permitiendo:
   - Tests deterministas con `SequentialDiceProvider`
   - Simulación con semilla fija para reproducibilidad
   - Dados reales para juego en vivo

### Riesgos

| Riesgo                          | Probabilidad | Impacto | Mitigación                                      |
| ------------------------------- | ------------ | ------- | ----------------------------------------------- |
| Reglas incompletas              | Alta         | Alto    | Checklist de mecánicas antes de implementar     |
| Estado mutable accidental       | Media        | Crítico | Usar `val` everywhere, `copy()` para mutaciones |
| RuleEngine demasiado monolítico | Media        | Medio   | Métodos privados granulares + tests por método  |
| Bugs en cálculo de renta        | Alta         | Alto    | Tests paramétricos con tablas de renta          |

### Señales de Sobreingeniería

- ⚠️ Implementar más de 2 tipos de subastas
- ⚠️ Añadir sistema de trading entre jugadores en esta fase
- ⚠️ Crear abstracciones para "futuros" tipos de tablero
- ⚠️ Implementar Chance/Community Chest completas (simplificar primero)

### Criterio de Validación

```bash
# Esto debe ejecutarse sin errores:
./gradlew :core:test
# Y un test de integración debe completar una partida completa IA vs IA
```

### Complejidad: 3/5 | Duración Estimada: 3-4 semanas

---

## 5. FASE 3 — MOTOR DE SIMULACIÓN MASIVA

### Objetivo

Convertir el motor de juego en un entorno experimental capaz de ejecutar miles de partidas
sin intervención humana y generar datos estructurados.

### Subtareas Técnicas

| #   | Subtarea                         | Descripción                                                 |
| --- | -------------------------------- | ----------------------------------------------------------- |
| 3.1 | Implementar `GameSimulator`      | Ejecuta una partida completa headless y retorna métricas    |
| 3.2 | Implementar `BatchRunner`        | Ejecuta N partidas secuenciales/paralelas con configuración |
| 3.3 | Definir `SimulationConfig`       | Parámetros: N partidas, semilla, dificultades, config juego |
| 3.4 | Implementar métricas por partida | Win/loss, duración, patrimonio, decisiones                  |
| 3.5 | Implementar `BatchMetrics`       | Agregados: win rate, media, desviación, mediana             |
| 3.6 | Implementar `CsvExporter`        | Exportar dataset a CSV (formato estándar)                   |
| 3.7 | Implementar `SqliteExporter`     | Exportar a SQLite (para consultas complejas)                |
| 3.8 | Test de 10,000 partidas          | Validar rendimiento y ausencia de errores                   |
| 3.9 | Benchmark de velocidad           | Medir partidas/segundo                                      |

### Métricas a Capturar por Partida

```
[game_id, seed, p1_difficulty, p2_difficulty, winner_id, total_turns,
 p1_final_balance, p2_final_balance, p1_final_net_worth, p2_final_net_worth,
 p1_properties_count, p2_properties_count, p1_bankrupted, p2_bankrupted,
 avg_decision_time_p1_ms, avg_decision_time_p2_ms, game_duration_ms]
```

### Métricas Agregadas (BatchMetrics)

```
- Win rate por nivel de dificultad
- Duración promedio de partida (turnos)
- Patrimonio final medio
- Tiempo medio de decisión de la IA
- Tasa de bancarrota por jugador
- Tasa de empate
- Mediana y desviación estándar de turnos
```

### Riesgos

| Riesgo                      | Probabilidad | Impacto | Mitigación                              |
| --------------------------- | ------------ | ------- | --------------------------------------- |
| Partidas infinitas (loops)  | Alta         | Crítico | `maxTurns` obligatorio en GameConfig    |
| OOM en 10K partidas         | Media        | Alto    | No acumular estados; procesar y liberar |
| Datos inconsistentes        | Media        | Medio   | Validar invariantes post-partida        |
| Simulación lenta (<100 p/s) | Baja         | Medio   | Profiling y optimización de RuleEngine  |

### Señales de Sobreingeniería

- ⚠️ Construir dashboard gráfico antes de tener datos
- ⚠️ Implementar simulación distribuida (basta con un hilo)
- ⚠️ Usar Apache Spark para procesar 10K partidas (overkill)

### Complejidad: 2/5 | Duración Estimada: 1-2 semanas

---

## 6. FASE 4 — IMPLEMENTACIÓN PROGRESIVA DE IA

### Objetivo

Implementar tres niveles de IA con complejidad creciente, donde cada nivel es empíricamente
superior al anterior.

### Nivel Easy: Rule-Based (Semana 1)

| #   | Subtarea                                                          |
| --- | ----------------------------------------------------------------- |
| 4.1 | Implementar `RuleBasedStrategy`                                   |
| 4.2 | Regla: comprar si balance > 2× precio                             |
| 4.3 | Regla: nunca hipotecar                                            |
| 4.4 | Regla: mejorar propiedades si grupo completo y balance > 3× coste |
| 4.5 | Regla: pujar hasta precio base en subastas                        |
| 4.6 | Tests unitarios de cada regla                                     |

### Nivel Medium: Heurística (Semanas 2-3)

| #    | Subtarea                                                    |
| ---- | ----------------------------------------------------------- |
| 4.7  | Diseñar función de evaluación económica `V(S, p)`           |
| 4.8  | Implementar `HeuristicStrategy` con evaluación de acciones  |
| 4.9  | Evaluar: ROI esperado de cada propiedad                     |
| 4.10 | Evaluar: riesgo de bancarrota a N turnos                    |
| 4.11 | Evaluar: valor posicional (probabilidad de caer en casilla) |
| 4.12 | Implementar poda de acciones subóptimas                     |
| 4.13 | Validación comparativa: Easy vs Medium (1,000 partidas)     |
| 4.14 | Medium debe ganar ≥ 60% contra Easy                         |

### Nivel Hard: MCTS + RL (Semanas 4-6)

| #    | Subtarea                                                |
| ---- | ------------------------------------------------------- |
| 4.15 | Definir entorno RL formal: (estado, acción, recompensa) |
| 4.16 | Implementar MCTS básico con UCB1                        |
| 4.17 | Implementar evaluación de nodo con heurística           |
| 4.18 | Diseñar red neuronal para evaluación de estado (TFLite) |
| 4.19 | Entrenar modelo con datos de simulación F3              |
| 4.20 | Integrar modelo TFLite como evaluador de nodo MCTS      |
| 4.21 | Implementar `InferenceEngine` con límite temporal       |
| 4.22 | Comparativa: Easy vs Medium vs Hard (5,000 partidas)    |
| 4.23 | Hard debe ganar ≥ 55% contra Medium                     |
| 4.24 | Ajustar hiperparámetros MCTS (iteraciones, exploración) |

### Formación Entorno RL

```
Estado:   Vector de features derivado de GameState
          [balance_ratio, property_count, group_completion,
           opponent_balance_ratio, position_risk, upgrade_potential, ...]

Acción:   Índice discreto en el espacio de acciones válidas

Recompensa:
  r_turn  = Δ(netWorth) / startingBalance     (incremental)
  r_final = +1.0 si victoria, -1.0 si derrota, 0.0 si empate
```

### Riesgos

| Riesgo                               | Probabilidad | Impacto | Mitigación                                  |
| ------------------------------------ | ------------ | ------- | ------------------------------------------- |
| RL no converge                       | Alta         | Crítico | MCTS como fallback; RL es EXTRA             |
| Modelo TFLite no mejora a heurística | Alta         | Alto    | Documentar como resultado negativo (válido) |
| Tiempo de entrenamiento excesivo     | Media        | Medio   | Limitar a 100K episodios + early stopping   |
| MCTS demasiado lento en móvil        | Alta         | Alto    | Límite temporal estricto (150ms)            |
| Hard es invencible (mala UX)         | Media        | Medio   | Ajustar profundidad MCTS                    |

### Señales de Sobreingeniería

- ⚠️ Implementar PPO/A3C completo en lugar de MCTS+RL simple
- ⚠️ Crear framework de RL genérico en vez de implementación directa
- ⚠️ Más de 3 hiperparámetros tunables para MCTS
- ⚠️ Entrenar durante más de 48 horas

### Complejidad: 5/5 | Duración Estimada: 4-6 semanas

---

## 7. FASE 5 — EVALUACIÓN EXPERIMENTAL Y BIG DATA

### Objetivo

Convertir los resultados de simulación en evidencia académica rigurosa.

### Subtareas Técnicas

| #    | Subtarea                                                                  |
| ---- | ------------------------------------------------------------------------- |
| 5.1  | Ejecutar ≥ 50,000 partidas (Easy vs Medium, Medium vs Hard, Easy vs Hard) |
| 5.2  | Generar dataset estructurado completo                                     |
| 5.3  | Análisis de win rate con intervalos de confianza (95%)                    |
| 5.4  | Test de hipótesis: H₀="no hay diferencia entre niveles"                   |
| 5.5  | Analizar duración media por enfrentamiento                                |
| 5.6  | Evaluar robustez ante azar (varianza de resultados)                       |
| 5.7  | Evaluar impacto de hiperparámetros (ablation study)                       |
| 5.8  | Medir latencia media de decisión por nivel                                |
| 5.9  | Generar tablas comparativas LaTeX                                         |
| 5.10 | Generar gráficos (matplotlib/R): win rate, latencia, evolución            |

### Estructura del Dataset Principal

```csv
game_id,seed,p1_diff,p2_diff,winner,turns,p1_nw,p2_nw,p1_dt_ms,p2_dt_ms,duration_ms
```

### Tests Estadísticos Requeridos

1. **Test Chi-cuadrado**: Independencia win rate vs dificultad
2. **Test t de Student** (o Mann-Whitney U): Diferencia de medias en patrimonio
3. **ANOVA**: Diferencia entre los tres niveles simultáneamente
4. **Bootstrap**: Intervalos de confianza para win rate

### Riesgos

| Riesgo                                        | Probabilidad | Impacto | Mitigación                                                  |
| --------------------------------------------- | ------------ | ------- | ----------------------------------------------------------- |
| No hay diferencia significativa entre niveles | Media        | Crítico | Documentar y analizar por qué (válido académicamente)       |
| Dataset corrupto                              | Baja         | Alto    | Checksums + validación post-generación                      |
| Dominancia del azar sobre la estrategia       | Alta         | Medio   | Medir varianza; argumento: "la IA maximiza dentro del azar" |

### Señales de Sobreingeniería

- ⚠️ Construir pipeline de datos distribuido
- ⚠️ Usar más de 5 tests estadísticos diferentes
- ⚠️ Generar más de 15 gráficos para la memoria

### Complejidad: 3/5 | Duración Estimada: 2-3 semanas

---

## 8. FASE 6 — INTEGRACIÓN ANDROID

### Objetivo

Convertir el motor de juego en una aplicación Android funcional.

### Subtareas Técnicas

| #    | Subtarea                                                           |
| ---- | ------------------------------------------------------------------ |
| 6.1  | Configurar proyecto Android Studio con módulos Gradle              |
| 6.2  | Configurar Hilt para inyección de dependencias                     |
| 6.3  | Crear `GameModule` (provee RuleEngine, DiceProvider, AIStrategies) |
| 6.4  | Implementar `GameViewModel` con StateFlow                          |
| 6.5  | Implementar ejecución asíncrona con Coroutines                     |
| 6.6  | Dispatcher personalizado para IA (Dispatchers.Default)             |
| 6.7  | Medir latencia de IA en dispositivo real                           |
| 6.8  | Asegurar que IA no bloquea hilo principal                          |
| 6.9  | Implementar persistencia local (Room)                              |
| 6.10 | Test de integración en dispositivo                                 |

### Riesgos

| Riesgo                         | Probabilidad | Impacto | Mitigación                            |
| ------------------------------ | ------------ | ------- | ------------------------------------- |
| ANR por IA en hilo principal   | Alta         | Crítico | Lanzar siempre en Dispatchers.Default |
| Incompatibilidad Godot-Android | Media        | Alto    | Probar integración básica primero     |
| Memory leaks                   | Media        | Medio   | LeakCanary + lifecycle awareness      |

### Señales de Sobreingeniería

- ⚠️ Implementar Clean Architecture completa con UseCase classes triviales
- ⚠️ Añadir Repository pattern para datos que solo se leen/escriben localmente
- ⚠️ Múltiples Activity/Fragment antes de tener un flujo básico

### Complejidad: 3/5 | Duración Estimada: 2-3 semanas

---

## 9. FASE 7 — INTEGRACIÓN GODOT

### Objetivo

Añadir la capa visual profesional mediante Godot como motor de renderizado.

### Subtareas Técnicas

| #    | Subtarea                                                                 |
| ---- | ------------------------------------------------------------------------ |
| 7.1  | Compilar Godot como Android Library (.aar)                               |
| 7.2  | Crear plugin Godot Android (`godot-bridge`)                              |
| 7.3  | Implementar puente bidireccional                                         |
| 7.4  | Godot → Android: `game_plugin.roll_dice()`, `game_plugin.buy_property()` |
| 7.5  | Android → Godot: Signals/callbacks para actualizar UI                    |
| 7.6  | Diseñar escena del tablero en Godot 2D                                   |
| 7.7  | Implementar animaciones (movimiento de fichas, compra, mejora)           |
| 7.8  | Implementar feedback visual de "IA pensando"                             |
| 7.9  | Validar sincronización estado-lógica-render                              |
| 7.10 | Pruebas de consistencia visual                                           |

### Protocolo de Comunicación

```
Godot (GDScript)                    Android (Kotlin)
─────────────────                   ─────────────────
game_plugin.end_turn()     ──→     Bridge.onEndTurn()
                                        ↓
                                   GameEngine.executeAction()
                                        ↓
                                   Bridge.emitSignal("player_moved", data)
                           ←──
update_board(data)
```

### Riesgos

| Riesgo                                           | Probabilidad | Impacto | Mitigación                                           |
| ------------------------------------------------ | ------------ | ------- | ---------------------------------------------------- |
| Versión de Godot incompatible con Android plugin | Alta         | Crítico | Fijar Godot 4.x y probar desde día 1                 |
| Estado desincronizado UI/lógica                  | Alta         | Alto    | UI solo refleja GameState; nunca tiene estado propio |
| Latencia perceptible en animaciones              | Media        | Medio   | Cola de animaciones asíncrona                        |

### Señales de Sobreingeniería

- ⚠️ Shaders custom para el tablero
- ⚠️ Sistema de partículas elaborado
- ⚠️ Más de 3 tipos de animación por acción

### Complejidad: 4/5 | Duración Estimada: 3-4 semanas

---

## 10. FASE 8 — OPTIMIZACIÓN Y RENDIMIENTO

### Subtareas Técnicas

| #   | Subtarea                            | Métrica Objetivo             |
| --- | ----------------------------------- | ---------------------------- |
| 8.1 | Profiling de CPU en partida típica  | < 5% uso continuado          |
| 8.2 | Profiling de memoria                | < 100MB heap en juego        |
| 8.3 | Benchmark de latencia IA Easy       | < 10ms                       |
| 8.4 | Benchmark de latencia IA Medium     | < 50ms                       |
| 8.5 | Benchmark de latencia IA Hard       | < 200ms                      |
| 8.6 | Optimizar MCTS (iteraciones por ms) | ≥ 100 iter en 150ms          |
| 8.7 | Optimizar inferencia TFLite         | Delegado GPU si disponible   |
| 8.8 | Test en Redmi Note 12 (gama media)  | Todas las métricas           |
| 8.9 | Test en dispositivo gama baja       | Modo "Light AI" si necesario |

### Complejidad: 2/5 | Duración Estimada: 1-2 semanas

---

## 11. FASE 9 — VALIDACIÓN DE EXPERIENCIA DE USUARIO

### Subtareas Técnicas

| #   | Subtarea                                                   |
| --- | ---------------------------------------------------------- |
| 9.1 | Reclutar 5-10 testers                                      |
| 9.2 | Sesiones de juego controladas (15 min cada una)            |
| 9.3 | Cuestionario post-sesión (SUS + preguntas custom)          |
| 9.4 | Evaluar percepción de dificultad por nivel                 |
| 9.5 | Ajustar parámetros de IA según feedback                    |
| 9.6 | Implementar pausa artificial de "pensamiento" (500-1500ms) |
| 9.7 | Verificar ausencia de bloqueos perceptibles                |
| 9.8 | Documentar hallazgos cualitativos                          |

### Complejidad: 2/5 | Duración Estimada: 1-2 semanas

---

## 12. FASE 10 — PREPARACIÓN PARA DEFENSA ACADÉMICA

### Subtareas

| #    | Subtarea                                                    |
| ---- | ----------------------------------------------------------- |
| 10.1 | Documentar arquitectura final con diagramas actualizados    |
| 10.2 | Escribir sección de "Resultados Experimentales"             |
| 10.3 | Generar tablas comparativas (LaTeX)                         |
| 10.4 | Generar gráficos finales                                    |
| 10.5 | Escribir sección "Discusión y Limitaciones"                 |
| 10.6 | Escribir sección "Trabajo Futuro"                           |
| 10.7 | Verificar reproducibilidad (semillas, configs documentadas) |
| 10.8 | Preparar presentación (20 slides)                           |
| 10.9 | Ensayo de defensa (30 min)                                  |

### Complejidad: 1/5 | Duración Estimada: 2-3 semanas

---

## 13. CRONOGRAMA CONSOLIDADO

| Fase                   | Semanas | Inicio      | Fin         |
| ---------------------- | ------- | ----------- | ----------- |
| F1 — Dominio Formal    | 1-2     | Semana 1    | Semana 2    |
| F2 — Módulo :core      | 3-4     | Semana 3    | Semana 6    |
| F3 — Simulación Masiva | 1-2     | Semana 7    | Semana 8    |
| F4 — IA Progresiva     | 4-6     | Semana 9    | Semana 14   |
| F5 — Big Data          | 2-3     | Semana 15   | Semana 17   |
| F6 — Android           | 2-3     | Semana 7\*  | Semana 9\*  |
| F7 — Godot             | 3-4     | Semana 10\* | Semana 13\* |
| F8 — Optimización      | 1-2     | Semana 15   | Semana 16   |
| F9 — UX Validation     | 1-2     | Semana 17   | Semana 18   |
| F10 — Defensa          | 2-3     | Semana 19   | Semana 21   |

\*F6-F7 pueden iniciar en paralelo con F3-F4 una vez que F2 esté completa.

**Duración total estimada: 20-24 semanas (5-6 meses)**

---

## 14. RECOMENDACIONES METODOLÓGICAS

### Control de Versiones

- **Git Flow** simplificado: `main` + `develop` + feature branches por fase
- Tag cada entregable de fase: `v0.1-phase1`, `v0.2-phase2`, etc.
- Commits semánticos: `feat:`, `fix:`, `test:`, `docs:`

### Calidad de Código

- **Tests primero** para RuleEngine (TDD parcial donde tenga sentido)
- Cobertura mínima del módulo `:core`: 80%
- Cobertura mínima del módulo `:ai`: 70%
- Linter: ktlint configurado desde el inicio

### Documentación Continua

- Cada fase genera un "mini-reporte" de 1 página
- Los mini-reportes se integran en la memoria final
- Los datos experimentales se guardan con timestamp y config

### Gestión del Tiempo

- **Timeboxing estricto**: Si una tarea supera 2× su estimación, replantear
- Reunión de "retrospectiva personal" cada 2 semanas
- Priorizar profundidad en :core y :ai sobre pulido visual

### Principio YAGNI (You Aren't Gonna Need It)

- No implementar features que no entren en la evaluación
- No abstraer hasta tener 3 casos concretos de duplicación
- Preferir código simple y legible sobre código "elegante"

---

## 15. SEÑALES DE ALERTA GLOBALES

| Señal                                         | Acción Correctiva                          |
| --------------------------------------------- | ------------------------------------------ |
| Más de 2 semanas sin un test verde nuevo      | Parar; escribir tests del código existente |
| Módulo :core depende de Android               | Refactorizar inmediatamente                |
| GameState tiene `var` en vez de `val`         | Reescribir a inmutable                     |
| La IA tarda >500ms en decidir en modo Hard    | Reducir profundidad MCTS                   |
| Más de 5 patrones de diseño en un módulo      | Simplificar                                |
| El juego tiene bugs visibles en modo headless | No avanzar a siguiente fase                |
| Datos de simulación no reproducibles          | Auditar semillas y configuración           |

---

## 16. CRITERIOS DE CALIDAD ACADÉMICA

Para que el TFM sea **defendible**, debe demostrar:

1. **Separación de intereses**: Motor testeable sin UI ✓ (Hexagonal Architecture)
2. **Reproducibilidad**: Simulaciones con semilla fija producen resultados idénticos
3. **Evidencia empírica**: ≥ 50,000 partidas con análisis estadístico
4. **Progresión medible**: Win rate Easy < Medium < Hard (con significancia)
5. **Trazabilidad**: Log completo de cada decisión de IA
6. **Rendimiento**: Latencia documentada y dentro de umbrales
7. **Código limpio**: Módulos desacoplados, tests, documentación inline

---

_Documento generado como hoja de ruta técnica para el TFM "Duopoly"._
_Arquitectura: Clean Architecture Hexagonal con módulos Gradle independientes._
