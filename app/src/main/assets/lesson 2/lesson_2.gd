extends Control

@onready var game_timer = $Timer
@onready var time_label = $Panel2/TimeLabel
@onready var mistakes_label = $Panel2/MistakesLabel
@onready var score_label = $Panel2/ScoreLabel  # Add this UI element to show score
@onready var hint_button = $Panel2/HintButton
@onready var hint_label = $Panel2/HintLabel

var time_left = 300	
var correctly_placed_blocks = {}
var total_blocks = 5
var mistakes_made = 0
var max_mistakes = 3

# SCORING SYSTEM VARIABLES
var current_score = 0
var points_per_correct = 5  # Simple: 5 points per correct placement
var max_possible_score = 0  # Calculate this in _ready()

# HINT SYSTEM VARIABLES
var hints_remaining = 1
var current_hint_zone = null

func _ready():
	add_to_group("GameController")
	SoundManager.bgm.play()
	
	setup_timer()
	start_countdown()
	setup_mistakes_display()
	setup_scoring_system()
	setup_hint_system()
	
	# Set current lesson in DragDropState
	var drag_drop_state = get_node("/root/DragDropState")
	if drag_drop_state:
		drag_drop_state.reset_score()  # Reset score for new lesson
		drag_drop_state.set_lesson("Lesson 2")
	
	# Initialize tracking for Lesson 2 buttons
	correctly_placed_blocks["intL2button"] = false
	correctly_placed_blocks["StringL2button"] = false
	correctly_placed_blocks["charL2button"] = false
	correctly_placed_blocks["booleanL2button"] = false
	correctly_placed_blocks["nameL2button"] = false

func setup_hint_system():
	update_hints_display()
	if hint_button:
		if not hint_button.pressed.is_connected(_on_hint_button_pressed):
			hint_button.pressed.connect(_on_hint_button_pressed)
	
func update_hints_display():
	if hint_label:
		hint_label.text = "Hints: %d" % hints_remaining
	if hint_button:
		hint_button.disabled = (hints_remaining <= 0)

func _on_hint_button_pressed():
	if hints_remaining <= 0:
		return
	
	var incorrect_blocks = []
	for block_name in correctly_placed_blocks:
		if not correctly_placed_blocks[block_name]:
			incorrect_blocks.append(block_name)
	if incorrect_blocks.is_empty():
		return
	
	var target_block = incorrect_blocks[0]
	
	var block_to_zone = {
		"intL2button": {"zone": "intL2", "text": "int"},
		"StringL2button": {"zone": "StringL2", "text": "String"},
		"charL2button": {"zone": "charL2", "text": "char"},
		"booleanL2button": {"zone": "booleanL2", "text": "boolean"},
		"nameL2button": {"zone": "nameL2", "text": "variable name"}
	}
	
	var target_data = block_to_zone.get(target_block, {})
	var target_zone = target_data.get("zone", "")
	var hint_text = target_data.get("text", "Place here")
	if target_zone == "":
		return
	
	show_hint_on_zone(target_zone, hint_text)
	hints_remaining -= 1
	update_hints_display()
	get_tree().create_timer(3.0).timeout.connect(clear_hint_highlights)

func show_hint_on_zone(zone_name: String, hint_text: String):
	var zone = find_node_by_name(zone_name)
	if zone:
		var hint_label_temp = Label.new()
		hint_label_temp.text = hint_text
		hint_label_temp.name = "HintLabelTemp"
		hint_label_temp.add_theme_color_override("font_color", Color.GREEN)
		hint_label_temp.add_theme_font_size_override("font_size", 20)
		hint_label_temp.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		hint_label_temp.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
		hint_label_temp.position = Vector2(0, 0)
		hint_label_temp.size = zone.size
		hint_label_temp.z_index = 100
		zone.add_child(hint_label_temp)
		current_hint_zone = hint_label_temp
		zone.modulate = Color(0.5, 1.0, 0.5)
		var tween = create_tween()
		tween.set_loops(6)
		tween.tween_property(zone, "scale", Vector2(1.1, 1.1), 0.3)
		tween.tween_property(zone, "scale", Vector2(1.0, 1.0), 0.3)

func clear_hint_highlights():
	if current_hint_zone:
		if current_hint_zone.get_parent():
			current_hint_zone.get_parent().modulate = Color.WHITE
			current_hint_zone.get_parent().scale = Vector2(1.0, 1.0)
		current_hint_zone.queue_free()
		current_hint_zone = null

func find_node_by_name(node_name: String):
	var possible_paths = [
		node_name,
		"Panel/" + node_name,
		"Panel2/" + node_name,
		"DraggableButtons/" + node_name,
		"DropZones/" + node_name,
		"VBoxContainer/" + node_name,
		"HBoxContainer/" + node_name,
		"ScrollContainer/" + node_name,
		"MarginContainer/" + node_name,
		"GridContainer/" + node_name
	]
	for path in possible_paths:
		if has_node(path):
			return get_node(path)
	var found = find_child(node_name, true, false)
	return found

func setup_scoring_system():
	max_possible_score = total_blocks * points_per_correct
	update_score_display()
	print("Scoring system initialized. Max possible score: ", max_possible_score)

func setup_timer():
	game_timer.wait_time = 1.0
	game_timer.timeout.connect(_on_timer_timeout)
	update_timer_display()

func setup_mistakes_display():
	update_mistakes_display()

func start_countdown():
	game_timer.start()
	print("Lesson 2 started! Timer counting down...")

func _on_timer_timeout():
	time_left -= 1
	update_timer_display()
	
	if time_left <= 0:
		time_up()

func update_timer_display():
	var minutes = time_left / 60.0
	var seconds = time_left % 60
	time_label.text = "%d:%02d" % [minutes, seconds]

func update_mistakes_display():
	if mistakes_label:
		mistakes_label.text = "Mistakes: %d/%d" % [mistakes_made, max_mistakes]

func update_score_display():
	if score_label:
		score_label.text = "Score: %d" % current_score

# SCORING FUNCTIONS
func add_correct_placement_points(button_name: String):
	current_score += points_per_correct
	print("Correct placement! +", points_per_correct, " points (Total: ", current_score, ")")
	update_score_display()

func subtract_mistake_points():
	# No penalty for mistakes in this simple scoring system
	pass

func calculate_final_score():
	var percentage = 0.0
	if max_possible_score > 0:
		percentage = (float(current_score) / float(max_possible_score)) * 100.0
	
	print("=== FINAL SCORE ===")
	print("Final Score: ", current_score, "/", max_possible_score, " (", "%.1f" % percentage, "%)")
	
	return {
		"final_score": current_score,
		"max_score": max_possible_score,
		"percentage": percentage,
		"grade": get_letter_grade(percentage)
	}

func get_letter_grade(percentage: float) -> String:
	if percentage >= 90: return "A+"
	elif percentage >= 85: return "A"
	elif percentage >= 80: return "A-"
	elif percentage >= 75: return "B+"
	elif percentage >= 70: return "B"
	elif percentage >= 65: return "B-"
	elif percentage >= 60: return "C+"
	elif percentage >= 55: return "C"
	elif percentage >= 50: return "C-"
	else: return "F"

# Function to make a button red when there's an error
func show_button_error(button_name: String):
	print("Trying to make button red: ", button_name)
	
	var button = null
	var possible_paths = [
		button_name,
		"Panel/" + button_name,
		"Panel2/" + button_name,
		"$" + button_name
	]
	
	for path in possible_paths:
		if has_node(path):
			button = get_node(path)
			print("Found button at path: ", path)
			break
	
	if button:
		button.modulate = Color.RED
		print("Button turned red successfully!")
	else:
		print("ERROR: Could not find button: ", button_name)

func reset_button_color(button_name: String):
	print("Trying to reset button color: ", button_name)
	
	var button = null
	var possible_paths = [
		button_name,
		"Panel/" + button_name,
		"Panel2/" + button_name,
		"$" + button_name
	]
	
	for path in possible_paths:
		if has_node(path):
			button = get_node(path)
			break
	
	if button:
		button.modulate = Color.WHITE
		print("Button color reset successfully!")

func reset_all_button_colors():
	reset_button_color("intL2button")
	reset_button_color("StringL2button")
	reset_button_color("charL2button")
	reset_button_color("booleanL2button")
	reset_button_color("nameL2button")

func on_block_placed(button_name: String, is_correct_position: bool):
	print("=== GAME CONTROLLER CALLED (Lesson 2) ===")
	print("Button: ", button_name, " | Correct: ", is_correct_position)
	
	# Update DragDropState for score tracking
	var drag_drop_state = get_node("/root/DragDropState")
	if drag_drop_state:
		if is_correct_position:
			drag_drop_state.on_correct_placement()
		else:
			drag_drop_state.on_wrong_placement()
	
	if is_correct_position:
		correctly_placed_blocks[button_name] = true
		reset_button_color(button_name)
		add_correct_placement_points(button_name)
		print("Correct placement for: ", button_name)
	else:
		correctly_placed_blocks[button_name] = false
		show_button_error(button_name)
		mistakes_made += 1
		update_mistakes_display()
		print("Mistake made! Total mistakes: ", mistakes_made, "/", max_mistakes)
		
		if mistakes_made >= max_mistakes:
			mistakes_limit_reached()
			return
	
	# Count correctly placed blocks
	var correct_count = 0
	for block_name in correctly_placed_blocks:
		if correctly_placed_blocks[block_name]:
			correct_count += 1
	
	print("Correctly placed blocks: ", correct_count, "/", total_blocks)
	print("Current score: ", current_score)
	print("=== END GAME CONTROLLER ===")
	print("")
	
	if correct_count >= total_blocks:
		puzzle_completed()

func puzzle_completed():
	game_timer.stop()
	SoundManager.bgm.stop()
	print("Puzzle completed (Lesson 2)!")
	
	var score_data = calculate_final_score()
	
	var success_scene = preload("res://lesson 2/ProlemSolvedL2.tscn")
	var success_instance = success_scene.instantiate()
	
	if success_instance.has_method("set_score_data"):
		success_instance.set_score_data(score_data)
	
	# Submit score to Android leaderboard
	submit_score_to_android()
	
	get_tree().current_scene.add_child(success_instance)

func mistakes_limit_reached():
	game_timer.stop()
	SoundManager.bgm.stop()
	print("Maximum mistakes reached! Game over (Lesson 2).")
	
	var score_data = calculate_final_score()
	
	# Submit score to Android leaderboard even on loss
	submit_score_to_android()
	
	var retry_scene = preload("res://lesson 2/RetryLesson2.tscn")
	var retry_instance = retry_scene.instantiate()
	
	if retry_instance.has_method("set_score_data"):
		retry_instance.set_score_data(score_data)
	
	get_tree().current_scene.add_child(retry_instance)

func time_up():
	game_timer.stop()
	SoundManager.bgm.stop()
	print("Time's up! Lesson 2 ended.")
	
	var score_data = calculate_final_score()
	
	# Submit score to Android leaderboard even on time up
	submit_score_to_android()
	
	var retry_scene = preload("res://lesson 2/RetryLesson2.tscn")
	var retry_instance = retry_scene.instantiate()
	
	if retry_instance.has_method("set_score_data"):
		retry_instance.set_score_data(score_data)
	
	get_tree().current_scene.add_child(retry_instance)

# Reset game state
func reset_game():
	mistakes_made = 0
	current_score = 0
	time_left = 300
	
	for block_name in correctly_placed_blocks:
		correctly_placed_blocks[block_name] = false
	
	reset_all_button_colors()
	update_mistakes_display()
	update_score_display()
	update_timer_display()
	
	print("Game state reset for Lesson 2!")

# Submit score to Android leaderboard
func submit_score_to_android():
	print("🎯 Lesson2: Submitting score to Android - Score:", current_score)
	
	# Get the AppPlugin instance
	var app_plugin = Engine.get_singleton("AppPlugin")
	if app_plugin:
		print("🎯 Lesson2: Calling AppPlugin.submitScore()")
		app_plugin.submitScore(current_score, 1)  # 1 attempt for drag and drop
	else:
		print("❌ Lesson2: AppPlugin not found!") 
