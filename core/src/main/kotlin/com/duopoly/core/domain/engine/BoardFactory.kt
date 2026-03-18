package com.duopoly.core.domain.engine

import com.duopoly.core.domain.model.*

/**
 * Fábrica del tablero estándar de Duopoly.
 *
 * Tablero de 24 casillas dispuestas en un cuadrado:
 *   4 esquinas: SALIDA (0), CÁRCEL (6), DESCANSO (12), IR_A_CÁRCEL (18)
 *   4 grupos de propiedades × 3 propiedades = 12 propiedades
 *   4 casillas de impuesto
 *   4 casillas de suerte/arca comunal
 *
 * Diseño intencionalmente más pequeño que Monopoly clásico
 * para análisis de IA tratable y simulación rápida.
 */
object BoardFactory {

    // ── Grupos de Propiedades ──

    private val TECH = PropertyGroup(1, "Tecnología", 3)
    private val ENERGY = PropertyGroup(2, "Energía", 3)
    private val FINANCE = PropertyGroup(3, "Finanzas", 3)
    private val INDUSTRY = PropertyGroup(4, "Industria", 3)

    /**
     * Crea el tablero estándar de 24 casillas.
     * Los precios y rentas están balanceados para partidas de ~100-200 turnos.
     */
    fun createStandardBoard(): List<TileDefinition> = listOf(
        // ═══ Lado 1 (índices 0-5) ═══
        TileDefinition(0, "Salida", TileType.START),
        TileDefinition(1, "DataNet", TileType.PROPERTY, TECH,
            purchasePrice = 100, baseRent = 8,
            rentByLevel = listOf(40, 120, 360), upgradeCost = 100),
        TileDefinition(2, "Suerte", TileType.CHANCE),
        TileDefinition(3, "CloudHub", TileType.PROPERTY, TECH,
            purchasePrice = 150, baseRent = 12,
            rentByLevel = listOf(60, 180, 500), upgradeCost = 100),
        TileDefinition(4, "Impuesto Digital", TileType.TAX, taxAmount = 100),
        TileDefinition(5, "CyberCore", TileType.PROPERTY, TECH,
            purchasePrice = 200, baseRent = 16,
            rentByLevel = listOf(80, 240, 600), upgradeCost = 100),

        // ═══ Lado 2 (índices 6-11) ═══
        TileDefinition(6, "Cárcel", TileType.JAIL),
        TileDefinition(7, "SolarGrid", TileType.PROPERTY, ENERGY,
            purchasePrice = 300, baseRent = 24,
            rentByLevel = listOf(120, 360, 850), upgradeCost = 150),
        TileDefinition(8, "Arca Comunal", TileType.COMMUNITY_CHEST),
        TileDefinition(9, "WindForge", TileType.PROPERTY, ENERGY,
            purchasePrice = 350, baseRent = 30,
            rentByLevel = listOf(150, 450, 1000), upgradeCost = 150),
        TileDefinition(10, "FusionPlant", TileType.PROPERTY, ENERGY,
            purchasePrice = 400, baseRent = 36,
            rentByLevel = listOf(180, 540, 1200), upgradeCost = 150),
        TileDefinition(11, "Suerte", TileType.CHANCE),

        // ═══ Lado 3 (índices 12-17) ═══
        TileDefinition(12, "Descanso", TileType.FREE_PARKING),
        TileDefinition(13, "CryptoVault", TileType.PROPERTY, FINANCE,
            purchasePrice = 500, baseRent = 45,
            rentByLevel = listOf(220, 660, 1500), upgradeCost = 200),
        TileDefinition(14, "Arca Comunal", TileType.COMMUNITY_CHEST),
        TileDefinition(15, "TradeFlow", TileType.PROPERTY, FINANCE,
            purchasePrice = 550, baseRent = 50,
            rentByLevel = listOf(250, 750, 1700), upgradeCost = 200),
        TileDefinition(16, "Impuesto Corporativo", TileType.TAX, taxAmount = 200),
        TileDefinition(17, "EquityPrime", TileType.PROPERTY, FINANCE,
            purchasePrice = 650, baseRent = 60,
            rentByLevel = listOf(300, 900, 2000), upgradeCost = 200),

        // ═══ Lado 4 (índices 18-23) ═══
        TileDefinition(18, "Ir a Cárcel", TileType.GO_TO_JAIL),
        TileDefinition(19, "SteelWorks", TileType.PROPERTY, INDUSTRY,
            purchasePrice = 800, baseRent = 80,
            rentByLevel = listOf(400, 1200, 2500), upgradeCost = 300),
        TileDefinition(20, "Suerte", TileType.CHANCE),
        TileDefinition(21, "MegaForge", TileType.PROPERTY, INDUSTRY,
            purchasePrice = 900, baseRent = 90,
            rentByLevel = listOf(450, 1350, 2800), upgradeCost = 300),
        TileDefinition(22, "Arca Comunal", TileType.COMMUNITY_CHEST),
        TileDefinition(23, "OmniFactory", TileType.PROPERTY, INDUSTRY,
            purchasePrice = 1200, baseRent = 120,
            rentByLevel = listOf(600, 1800, 3500), upgradeCost = 300)
    )

    /** Crea estados iniciales de casilla (todas sin propietario). */
    fun createInitialTileStates(board: List<TileDefinition>): List<TileState> =
        board.map { TileState(tileIndex = it.index) }

    /** Crea estado inicial de un jugador. */
    fun createInitialPlayerState(
        id: PlayerId,
        name: String,
        config: GameConfig,
        isAI: Boolean = false,
        aiDifficulty: AIDifficulty? = null
    ): PlayerState = PlayerState(
        id = id,
        name = name,
        balance = config.startingBalance,
        position = 0,
        isAI = isAI,
        aiDifficulty = aiDifficulty
    )

    /**
     * Crea un estado de juego inicial completo para una partida 1v1.
     *
     * @param player1Name Nombre del jugador 1 (humano por defecto)
     * @param player2Name Nombre del jugador 2
     * @param player2IsAI Si el jugador 2 es controlado por IA
     * @param player2Difficulty Nivel de dificultad de la IA
     * @param config Configuración del juego (parámetros tunables)
     */
    fun createInitialGameState(
        player1Name: String,
        player2Name: String,
        player2IsAI: Boolean = true,
        player2Difficulty: AIDifficulty = AIDifficulty.EASY,
        config: GameConfig = GameConfig()
    ): GameState {
        val board = createStandardBoard()
        val p1 = createInitialPlayerState(PlayerId("p1"), player1Name, config)
        val p2 = createInitialPlayerState(
            PlayerId("p2"), player2Name, config,
            isAI = player2IsAI,
            aiDifficulty = if (player2IsAI) player2Difficulty else null
        )

        return GameState(
            players = listOf(p1, p2),
            tileStates = createInitialTileStates(board),
            board = board,
            config = config,
            currentPlayerIndex = 0,
            phase = GamePhase.PRE_ROLL,
            turnNumber = 1
        )
    }

    /**
     * Crea un estado para simulación IA vs IA.
     */
    fun createAIvsAIState(
        difficulty1: AIDifficulty,
        difficulty2: AIDifficulty,
        config: GameConfig = GameConfig()
    ): GameState {
        val board = createStandardBoard()
        val p1 = createInitialPlayerState(
            PlayerId("p1"), "IA-${difficulty1.name}", config,
            isAI = true, aiDifficulty = difficulty1
        )
        val p2 = createInitialPlayerState(
            PlayerId("p2"), "IA-${difficulty2.name}", config,
            isAI = true, aiDifficulty = difficulty2
        )

        return GameState(
            players = listOf(p1, p2),
            tileStates = createInitialTileStates(board),
            board = board,
            config = config,
            currentPlayerIndex = 0,
            phase = GamePhase.PRE_ROLL,
            turnNumber = 1
        )
    }
}
