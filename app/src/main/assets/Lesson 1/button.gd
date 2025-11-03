extends Button

var dragging = false
var drop_target: NodePath = NodePath("")
var drag_offset = Vector2.ZERO
var original_parent: Node
var original_position: Vector2

func _ready():
	mouse_filter = Control.MOUSE_FILTER_PASS
	add_to_group("draggable")
	original_parent = get_parent()
	original_position = position

func _gui_input(event):  # ✅ FIXED: was *gui*input, should be _gui_input
	if event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT:
			if event.pressed:
				# Play click sound on pick up
				SoundManager.play_click()
				dragging = true
				drag_offset = event.position
				z_index = 100  # Bring to front
			else:
				dragging = false
				z_index = 0
				if drop_target != NodePath("") and has_node(drop_target):
					# Dropped on a valid panel
					var panel = get_node(drop_target)
					panel.accept_drop(self)
				else:
					# Dropped outside - return to original position (no mistake)
					return_to_original_position()
						
				drop_target = NodePath("")
	elif event is InputEventMouseMotion and dragging:
		global_position = event.global_position - drag_offset

func return_to_original_position():
	if get_parent() != original_parent:
		get_parent().remove_child(self)
		original_parent.add_child(self)
	position = original_position
	print(name, " returned to original position")

func set_valid_drop(valid: bool, target_path: NodePath):
	if valid:
		drop_target = target_path
	else:
		drop_target = NodePath("")
