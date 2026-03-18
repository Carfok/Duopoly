extends Node2D

# Configuración del tablero
const BOARD_SIZE = 24
const TILE_SIZE = 120
const BOARD_WIDTH = 7 # 7 casillas por lado (incluyendo esquinas)
const BOARD_HEIGHT = 7

# Colores para los grupos (Billetes/Monedas)
const GROUP_COLORS = {
	0: Color(0.1, 0.4, 0.1), # Brown / Green (1€)
	1: Color(0.1, 0.1, 0.7), # LightBlue / DarkBlue (5€)
	2: Color(0.7, 0.1, 0.1), # Pink / Red (10€)
	3: Color(0.7, 0.6, 0.1), # Orange / Gold (20€)
	4: Color(0.1, 0.7, 0.1), # Red / Green (50€)
	5: Color(0.7, 0.7, 0.1), # Yellow (100€)
	6: Color(0.1, 0.7, 0.7), # Green / Cyan (200€)
	7: Color(0.4, 0.1, 0.4)  # Blue / Purple (500€)
}

@onready var dice_label = $CanvasLayer/DiceUI/DiceResult
@onready var p1_token = $Tokens/Player1
@onready var p2_token = $Tokens/Player2
@onready var board_container = $Board

var tile_nodes = []

func _ready():
	_create_board()

func _create_board():
	# Generar el tablero visual de forma dinámica si no hay tiles
	for i in range(BOARD_SIZE):
		var tile = ColorRect.new()
		tile.custom_minimum_size = Vector2(TILE_SIZE, TILE_SIZE)
		tile.name = "Tile_" + str(i)
		
		# Calcular posición en O
		var pos = _get_tile_position(i)
		tile.position = pos
		
		# Estética base (Borde fino)
		var border = ReferenceRect.new()
		border.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		border.border_color = Color.BLACK
		border.editor_only = false
		tile.add_child(border)
		
		# Label para el nombre
		var label = Label.new()
		label.text = "Tile " + str(i)
		label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
		label.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		tile.add_child(label)
		
		board_container.add_child(tile)
		tile_nodes.append(tile)

func _get_tile_position(index: Int) -> Vector2:
	# Lado inferior (0-6)
	if index <= 6:
		return Vector2((6 - index) * TILE_SIZE, 6 * TILE_SIZE)
	# Lado izquierdo (7-12)
	elif index <= 12:
		return Vector2(0, (12 - index) * TILE_SIZE)
	# Lado superior (13-18)
	elif index <= 18:
		return Vector2((index - 12) * TILE_SIZE, 0)
	# Lado derecho (19-23)
	else:
		return Vector2(6 * TILE_SIZE, (index - 18) * TILE_SIZE)

func update_dice(d1: int, d2: int):
	dice_label.text = "Dados: %d + %d = %d" % [d1, d2, d1+d2]
	# Animación simple de vibración
	var tween = create_tween()
	tween.tween_property(dice_label, "scale", Vector2(1.5, 1.5), 0.1)
	tween.tween_property(dice_label, "scale", Vector2(1.0, 1.0), 0.1)

func move_player(player_id: String, to_index: int):
	var token = p1_token if player_id == "p1" else p2_token
	var target_pos = _get_tile_position(to_index) + Vector2(TILE_SIZE/2, TILE_SIZE/2)
	
	var tween = create_tween()
	tween.set_trans(Tween.TRANS_SINE)
	tween.set_ease(Tween.EASE_IN_OUT)
	tween.tween_property(token, "position", target_pos, 0.5)
