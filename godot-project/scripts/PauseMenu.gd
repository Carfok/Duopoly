extends Control

@onready var pause_panel = $PausePanel
@onready var overwrite_dialog = $OverwriteConfirmDialog

func _on_pause_button_pressed():
	pause_panel.visible = true
	get_tree().paused = true

func _on_save_game_pressed():
	# Primero verificamos si hay una partida previa
	if _has_previous_save():
		overwrite_dialog.popup_centered()
	else:
		_save_game_data()

func _on_confirm_overwrite_accepted():
	# Proceder con la eliminación de la anterior y guardar la nueva
	_clear_previous_save()
	_save_game_data()
	overwrite_dialog.hide()

func _on_confirm_overwrite_denied():
	overwrite_dialog.hide()

func _save_game_data():
	var bridge = Engine.get_singleton("DuopolyBridge")
	if bridge:
		bridge.saveCurrentGame()
	
	# Opcional: Notificar éxito al usuario
	print("Partida guardada correctamente")

func _on_exit_to_menu_pressed():
	get_tree().paused = false
	get_tree().change_scene_to_file("res://scenes/MainMenu.tscn")

func _has_previous_save() -> bool:
	return FileAccess.file_exists("user://savegame.json")

func _clear_previous_save():
	DirAccess.remove_absolute("user://savegame.json")
