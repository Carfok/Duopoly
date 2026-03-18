extends Node2D

# Referencia al plugin de Android
var bridge = null

# UI Nodes (Suponiendo que existen en la escena MainBoard.tscn)
@onready var ui_ai_thinking = $CanvasLayer/UI/AIThinkingLabel
@onready var ui_thinking_dots = $CanvasLayer/UI/AIThinkingDots
@onready var board_ui = $BoardLayer/BoardUI # Nueva referencia al UI del tablero

var _thinking_timer = 0.0

func _ready():
	if ui_ai_thinking:
		ui_ai_thinking.visible = false
	
	if Engine.has_singleton("DuopolyBridge"):
		bridge = Engine.get_singleton("DuopolyBridge")
		_connect_signals()
		bridge.initializeGame(1500)
		print("Duopoly: Bridge conectado correctamente")
	else:
		print("Duopoly: Bridge no encontrado (¿Estás en Android?)")

func _process(delta):
	# ... (lógica de thinking dots)

func _connect_signals():
	bridge.connect("player_moved", _on_player_moved)
	bridge.connect("dice_rolled", _on_dice_rolled) # Nuevo señal
	bridge.connect("balance_changed", _on_balance_changed)
	bridge.connect("game_event_received", _on_game_event)
	bridge.connect("ai_thinking_started", _on_ai_thinking_started)
	bridge.connect("ai_thinking_finished", _on_ai_thinking_finished)

func _on_dice_rolled(d1, d2):
	if board_ui:
		board_ui.update_dice(d1, d2)

func _on_player_moved(player_id, new_position):
	print("Animando jugador ", player_id, " a posición ", new_position)
	if board_ui:
		board_ui.move_player(player_id, new_position)

func _on_balance_changed(player_id, new_balance):
	print("Actualizando UI balance ", player_id, ": ", new_balance)

func _on_game_event(json_data):
	var event = JSON.parse_string(json_data)
	print("Evento de juego recibido: ", event)
