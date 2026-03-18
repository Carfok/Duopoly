package com.duopoly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duopoly.core.domain.model.GameState
import com.duopoly.core.domain.model.PlayerId
import com.duopoly.core.domain.model.TileType

@Composable
fun BoardView(state: GameState, modifier: Modifier = Modifier) {
    val boardSize = 24
    val sideSize = 7 // 0-6, 6-12, 12-18, 18-0
    
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val tileSize = maxWidth / sideSize
        
        // Dibujar casillas
        state.board.forEachIndexed { index, tile ->
            val position = getTileCoordinates(index, sideSize)
            
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .offset(x = tileSize * position.first, y = tileSize * position.second)
                    .border(0.5.dp, Color.Black)
                    .background(getTileColor(tile.type))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = tile.name,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                    
                    // Mostrar dueños si es propiedad
                    val tileState = state.tileStates.find { it.tileIndex == index }
                    if (tileState?.ownerId != null) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (tileState.ownerId == PlayerId("p1")) Color.Blue else Color.Red,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
        
        // Dibujar jugadores
        state.players.forEachIndexed { index, player ->
            val pos = getTileCoordinates(player.position, sideSize)
            val offset = if (index == 0) 0.dp else 4.dp
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(
                        x = tileSize * pos.first + tileSize / 2 - 6.dp + offset,
                        y = tileSize * pos.second + tileSize / 2 - 6.dp + offset
                    )
                    .background(if (index == 0) Color.Blue else Color.Red, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
        
        // Centro del tablero
        Box(
            modifier = Modifier
                .size(tileSize * 5)
                .offset(x = tileSize, y = tileSize),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DUOPOLY", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                if (state.lastDiceResult != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dados: ${state.lastDiceResult?.die1} + ${state.lastDiceResult?.die2}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun getTileCoordinates(index: Int, sideSize: Int): Pair<Int, Int> {
    val lastIdx = sideSize - 1
    return when {
        index <= lastIdx -> (lastIdx - index) to lastIdx // Lado inferior (derecha a izquierda)
        index <= lastIdx * 2 -> 0 to (lastIdx * 2 - index) // Lado izquierdo (abajo a arriba)
        index <= lastIdx * 3 -> (index - lastIdx * 2) to 0 // Lado superior (izquierda a derecha)
        else -> lastIdx to (index - lastIdx * 3) // Lado derecho (arriba a abajo)
    }
}

private fun getTileColor(type: TileType): Color = when (type) {
    TileType.PROPERTY -> Color(0xFFF0F0F0)
    TileType.START -> Color(0xFFC8E6C9)
    TileType.JAIL -> Color(0xFFFFCCBC)
    TileType.FREE_PARKING -> Color(0xFFFFF9C4)
    TileType.GO_TO_JAIL -> Color(0xFFFFAB91)
    TileType.TAX -> Color(0xFFB3E5FC)
    TileType.CHANCE, TileType.COMMUNITY_CHEST -> Color(0xFFE1BEE7)
}
