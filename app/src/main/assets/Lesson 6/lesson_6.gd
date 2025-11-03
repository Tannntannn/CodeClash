extends Control

@onready var game_timer = $Timer
@onready var time_label = $Panel2/TimeLabel
@onready var mistakes_label = $Panel2/MistakesLabel  
@onready var score_label = $Panel2/ScoreLabel  # Make sure you add this label in your UI
@onready var hint_button = $Panel2/HintButton
@onready var hint_label = $Panel2/HintLabel

var time_left = 300	
var correctly_placed_blocks = {}  
var total_blocks = 5  # You have 5 buttons in Lesson 6

# Mistakes
var mistakes_made = 0
var max_mistakes = 3  

# Scoring
var current_score = 0
var points_per_correct = 5
var max_possible_score = 0

# Hints
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
		drag_drop_state.set_lesson("Lesson 6")
	
	# Initialize Lesson 5 blocks
	correctly_placed_blocks["int[]buttonL6"] = false
	correctly_placed_blocks["integermaxvaluebuttonl6"] = false
	correctly_placed_blocks["int[n]buttonL6"] = false
	correctly_placed_blocks["n;i++buttonL6"] = false
	correctly_placed_blocks["inti=0;buttonL6"] = false

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
		"int[]buttonL6": {"zone": "int[]L6", "text": "int[]"},
		"integermaxvaluebuttonl6": {"zone": "intergerMAX_VALUE;L6", "text": "Integer.MAX_VALUE"},
		"int[n]buttonL6": {"zone": "int[n]L6", "text": "int[n]"},
		"n;i++buttonL6": {"zone": "n;i++L6", "text": "for (...; i < n; i++)"},
		"inti=0;buttonL6": {"zone": "inti=0;L6", "text": "int i = 0;"}
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
		"ScrollContainer/" + node_name,
		"Panel/Panel/ScrollContainer/CodeArea/" + node_name,
	]
	for path in possible_paths:
		if has_node(path):
			return get_node(path)
	var found = find_child(node_name, true, false)
	return found

# --- Setup ---
func setup_scoring_system():
	max_possible_score = total_blocks * points_per_correct
	update_score_display()
	print("Scoring initialized. Max possible score: ", max_possible_score)

func setup_timer():
	game_timer.wait_time = 1.0
	game_timer.timeout.connect(_on_timer_timeout)
	update_timer_display()

func setup_mistakes_display():
	update_mistakes_display()

func start_countdown():
	game_timer.start()
	print("Lesson 6 started! Timer counting down...")

# --- Timer ---
func _on_timer_timeout():
	time_left -= 1
	update_timer_display()
	if time_left <= 0:
		time_up()

func update_timer_display():
	var minutes = time_left / 60.0
	var seconds = time_left % 60
	time_label.text = "%d:%02d" % [minutes, seconds]

# --- Mistakes / Score display ---
func update_mistakes_display():
	if mistakes_label:
		mistakes_label.text = "Mistakes: %d/%d" % [mistakes_made, max_mistakes]

func update_score_display():
	if score_label:
		score_label.text = "Score: %d" % current_score

# --- Scoring ---
func add_correct_placement_points(button_name: String):
	current_score += points_per_correct
	update_score_display()
	print("Correct placement! +", points_per_correct, " points (Total: ", current_score, ")")

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

# --- Button feedback ---
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

func reset_all_button_colors():
	reset_button_color("inti=0;buttonL6")
	reset_button_color("n;i++buttonL6") 
	reset_button_color("int[n]buttonL6")
	reset_button_color("integermaxvaluebuttonl6")
	reset_button_color("int[]buttonL6")

# --- Block placement ---
func on_block_placed(button_name: String, is_correct_position: bool):
	print("=== GAME CONTROLLER CALLED ===")
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
	else:
		correctly_placed_blocks[button_name] = false
		show_button_error(button_name)
		mistakes_made += 1
		update_mistakes_display()
		print("Mistake made! Total mistakes: ", mistakes_made, "/", max_mistakes)
		if mistakes_made >= max_mistakes:
			mistakes_limit_reached()
			return
	
	# Count correct placements
	var correct_count = 0
	for block_name in correctly_placed_blocks:
		if correctly_placed_blocks[block_name]:
			correct_count += 1
	
	print("Correctly placed blocks: ", correct_count, "/", total_blocks)
	print("=== END GAME CONTROLLER ===\n")
	
	if correct_count >= total_blocks:
		puzzle_completed()

# --- End conditions ---
func puzzle_completed():
	game_timer.stop()
	print("Puzzle completed!")
	var score_data = calculate_final_score()
	var success_scene = preload("res://Lesson 6/ProlemSolvedL6.tscn")
	var success_instance = success_scene.instantiate()
	if success_instance.has_method("set_score_data"):
		success_instance.set_score_data(score_data)
	
	# Submit score to Android leaderboard
	submit_score_to_android()
	
	get_tree().current_scene.add_child(success_instance)

func mistakes_limit_reached():
	game_timer.stop()
	print("Maximum mistakes reached! Game over.")
	var score_data = calculate_final_score()
	
	# Submit score to Android leaderboard even on loss
	submit_score_to_android()
	
	var retry_scene = preload("res://Lesson 6/RetryLesson6.tscn")
	var retry_instance = retry_scene.instantiate()
	if retry_instance.has_method("set_score_data"):
		retry_instance.set_score_data(score_data)
	get_tree().current_scene.add_child(retry_instance)

func time_up():
	game_timer.stop()
	print("Time's up! Lesson ended.")
	var score_data = calculate_final_score()
	
	# Submit score to Android leaderboard even on time up
	submit_score_to_android()
	
	var retry_scene = preload("res://Lesson 6/RetryLesson6.tscn")
	var retry_instance = retry_scene.instantiate()
	if retry_instance.has_method("set_score_data"):
		retry_instance.set_score_data(score_data)
	get_tree().current_scene.add_child(retry_instance)

# --- Utility ---
func set_max_mistakes(new_max: int):
	max_mistakes = new_max
	update_mistakes_display()

func reset_mistakes():
	mistakes_made = 0
	reset_all_button_colors()
	update_mistakes_display()
	update_score_display()
	current_score = 0
	print("Mistakes and score reset")

# Submit score to Android leaderboard
func submit_score_to_android():
	print("🎯 Lesson6: Submitting score to Android - Score:", current_score)
	
	# Get the AppPlugin instance
	var app_plugin = Engine.get_singleton("AppPlugin")
	if app_plugin:
		print("🎯 Lesson6: Calling AppPlugin.submitScore()")
		app_plugin.submitScore(current_score, 1)  # 1 attempt for drag and drop
	else:
		print("❌ Lesson6: AppPlugin not found!")
