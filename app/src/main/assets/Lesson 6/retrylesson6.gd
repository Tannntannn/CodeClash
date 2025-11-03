extends Control

@onready var score_label: Label = $PanelContainer/VBoxContainer/Label

func _ready():
	# Display the drag and drop progress
	display_progress()

func display_progress():
	var drag_drop_state = get_node("/root/DragDropState")
	
	# Get the actual game score from the game controller
	var game_score = 0
	var game_controllers = get_tree().get_nodes_in_group("GameController")
	if game_controllers.size() > 0:
		var controller = game_controllers[0]
		if controller.has_method("get_current_score"):
			game_score = controller.get_current_score()
		elif "current_score" in controller:
			game_score = controller.current_score
	
	if drag_drop_state and score_label:
		score_label.text = "Score: %d" % game_score
		print("📊 Displaying score only:", game_score)
	else:
		if score_label:
			score_label.text = "Score: %d" % game_score
		print("❌ Could not get DragDropState or ScoreLabel")

func _on_button_2_pressed() -> void:  # Fixed: _on_ not *on*
	print("Retry button pressed!")
	# FIXED: Add error handling for scene changes
	if not is_inside_tree():
		return
	var err = get_tree().change_scene_to_file("res://Lesson 6/Lesson6Start.tscn")
	if err != OK:
		print("❌ Failed to change scene: ", err)

func _on_button_3_pressed() -> void:  # Fixed: _on_ not *on*
	print("🔙 Quit button pressed - Using AppPlugin to quit")
	# FIXED: Add error handling for quit operations
	if not is_inside_tree():
		return
	var app = Engine.get_singleton("AppPlugin")
	if app:
		app.navigateBack()
	else:
		print("❌ AppPlugin not found, using fallback")
		get_tree().quit()
