extends Control

@onready var difficulty_selector = $MarginContainer/VBoxContainer/DifficultyOption
@onready var start_balance_input = $MarginContainer/VBoxContainer/BalanceInput
@onready var max_turns_input = $MarginContainer/VBoxContainer/TurnsInput
@onready var start_button = $MarginContainer/VBoxContainer/StartButton

func _ready():
	# Configurar valores por defecto
	start_balance_input.value = 1500
	max_turns_input.value = 200

func _on_start_button_pressed():
	var difficulty = difficulty_selector.get_selected_id()
	var balance = start_balance_input.value
	var turns = max_turns_input.value
	
	var bridge = Engine.get_singleton("DuopolyBridge")
	if bridge:
		bridge.initializeGame(balance, turns, difficulty)
		get_tree().change_scene_to_file("res://scenes/MainBoard.tscn")
