extends Panel

@export var allowed_button_name: String = ""  # Set this in inspector for each panel

func _ready():
	add_to_group("dropzone")
	print("Panel ", name, " ready. Expects: ", allowed_button_name)

func _process(_delta):
	# FIXED: Only check is_inside_tree() for critical operations, not every frame
	for node in get_tree().get_nodes_in_group("draggable"):
		if node.dragging and is_inside_tree():
			var rect = Rect2(global_position, size)
			# ✅ Only set valid drop if no panel has claimed AND mouse is inside this panel
			if rect.has_point(get_global_mouse_position()):
				if node.drop_target == NodePath(""):
					node.set_valid_drop(true, get_path())
			elif node.drop_target == get_path():
				# Clear this panel if the mouse leaves
				node.set_valid_drop(false, NodePath(""))

func accept_drop(draggable: Control):
	# Check if this is the correct placement
	var is_correct = (draggable.name == allowed_button_name)
	
	# Always notify the game controller first (this counts mistakes/score)
	var game_controller = get_tree().get_first_node_in_group("GameController")
	if game_controller and game_controller.has_method("on_block_placed"):
		game_controller.on_block_placed(draggable.name, is_correct)
		print("Panel ", name, " received ", draggable.name, " - Correct: ", is_correct)
	
	if is_correct:
		# ✅ Correct placement → snap into panel
		SoundManager.play_drag()
		if draggable.get_parent():
			# FIXED: Use proper parent removal and child addition
			var parent = draggable.get_parent()
			parent.remove_child(draggable)
			add_child(draggable)
		else:
			add_child(draggable)
		draggable.position = (size - draggable.size) / 2
		print("Button placed correctly in panel")
	else:
		# ❌ Wrong placement → return to original position
		if draggable.has_method("return_to_original_position"):
			draggable.return_to_original_position()
		print("Button returned to original position - mistake counted")
