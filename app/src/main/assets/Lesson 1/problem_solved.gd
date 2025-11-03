extends Control

@onready var score_label: Label = null
var passed_score: int = 0

func _ready():
	# Resolve label and display the score
	score_label = _resolve_score_label()
	display_progress()

func set_score_data(score_data: Dictionary):
	passed_score = score_data.get("final_score", 0)
	print("📊 Success popup received score:", passed_score)

func _resolve_score_label() -> Label:
	var candidate_paths = [
		"PanelContainer/VBoxContainer/Label",
		"VBoxContainer/Label",
		"MarginContainer/VBoxContainer/Label",
		"Label"
	]
	for p in candidate_paths:
		if has_node(p):
			var n = get_node(p)
			if n is Label:
				return n
	# Fallback: recursive search for any Label
	var found = find_child("Label", true, false)
	if found and found is Label:
		return found
	return null

func display_progress():
	var game_score = passed_score
	if game_score == 0:
		var game_controllers = get_tree().get_nodes_in_group("GameController")
		if game_controllers.size() > 0:
			var controller = game_controllers[0]
			if controller.has_method("get_current_score"):
				game_score = controller.get_current_score()
			elif "current_score" in controller:
				game_score = controller.current_score

	if score_label:
		score_label.text = "Congratulations!\nScore: %d" % game_score
		print("📊 Displaying score only:", game_score)
	else:
		print("❌ ScoreLabel is null in success popup - check scene hierarchy")

func _on_button_2_pressed() -> void:
	get_tree().change_scene_to_file("res://Lesson 1/Output.tscn")
