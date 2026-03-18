extends Control

@onready var play_button = $MarginContainer/VBoxContainer/PlayButton
@onready var exit_button = $MarginContainer/VBoxContainer/ExitButton

func _ready():
	_apply_colorful_theme()

func _apply_colorful_theme():
	# Aplicar colores tipo "billetes" a los botones
	_style_button(play_button, Color(0.1, 0.7, 0.1)) # Verde (50€)
	_style_button(exit_button, Color(0.7, 0.1, 0.1)) # Rojo (10€)

func _style_button(btn: Button, color: Color):
	if not btn: return
	
	# Crear un StyleBoxFlat dinámico
	var sb = StyleBoxFlat.new()
	sb.bg_color = color
	sb.border_width_left = 4
	sb.border_width_top = 4
	sb.border_width_right = 4
	sb.border_width_bottom = 4
	sb.border_color = Color.BLACK
	sb.corner_radius_top_left = 8
	sb.corner_radius_top_right = 8
	sb.corner_radius_bottom_left = 8
	sb.corner_radius_bottom_right = 8
	
	btn.add_theme_stylebox_override("normal", sb)
	
	# Hover un poco más claro
	var sb_hover = sb.duplicate()
	sb_hover.bg_color = color.lightened(0.2)
	btn.add_theme_stylebox_override("hover", sb_hover)
	
	# Pressed más oscuro
	var sb_pressed = sb.duplicate()
	sb_pressed.bg_color = color.darkened(0.2)
	btn.add_theme_stylebox_override("pressed", sb_pressed)

func _on_play_button_pressed():
	get_tree().change_scene_to_file("res://scenes/PlaySelectorMenu.tscn")

func _on_exit_button_pressed():
	get_tree().quit()
