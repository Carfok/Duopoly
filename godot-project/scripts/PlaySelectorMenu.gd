extends Control

@onready var new_game_button = $MarginContainer/VBoxContainer/NewGameButton
@onready var load_game_button = $MarginContainer/VBoxContainer/LoadGameButton

func _ready():
	# Verificar si hay partida guardada para habilitar el botón de cargar
	if not _has_save_game():
		load_game_button.disabled = true

func _on_new_game_button_pressed():
	get_tree().change_scene_to_file("res://scenes/NewGameSettings.tscn")

func _on_load_game_button_pressed():
	# El bridge cargará el estado y notificará a la escena de juego
	var bridge = Engine.get_singleton("DuopolyBridge")
	if bridge:
		bridge.loadSavedGame()
		get_tree().change_scene_to_file("res://scenes/MainBoard.tscn")

func _has_save_game() -> bool:
	return FileAccess.file_exists("user://savegame.json")
