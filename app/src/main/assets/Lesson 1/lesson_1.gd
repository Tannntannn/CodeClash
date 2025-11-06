extends Control

@onready var game_timer = $Timer
@onready var time_label = $Panel2/Time
@onready var mistakes_label = $Panel2/MistakesLabel
@onready var score_label = $Panel2/ScoreLabel
@onready var hint_button = $Panel2/HintButton
@onready var hint_label = $Panel2/HintLabel

var time_left = 300	
var correctly_placed_blocks = {}
var total_blocks = 5
var mistakes_made = 0
var max_mistakes = 2

# SCORING SYSTEM VARIABLES
var current_score = 0
var points_per_correct = 5
var max_possible_score = 0

# HINT SYSTEM VARIABLES
var hints_remaining = 1
var hint_penalty = 2
var current_hint_zone = null

func _ready():
	add_to_group("GameController")
	SoundManager.bgm.play()
	
	setup_timer()
	start_countdown()
	setup_mistakes_display()
	setup_scoring_system()
	setup_hint_system()
	
	var drag_drop_state = get_node("/root/DragDropState")
	if drag_drop_state:
		drag_drop_state.reset_score()
		drag_drop_state.set_lesson("Lesson 1")
	
	correctly_placed_blocks["SystembuttonL1"] = false
	correctly_placed_blocks["HelloworldbuttonL1"] = false
	correctly_placed_blocks["ClassMainbuttonL1"] = false
	correctly_placed_blocks["publicstaticbuttonL1"] = false
	correctly_placed_blocks["StringbuttonL1"] = false

func setup_hint_system():
	if hint_label:
		print("✅ Hint label found!")
	else:
		print("⚠️ HintLabel not found at Panel2/HintLabel")
	
	update_hints_display()
	
	if hint_button:
		# Check if already connected (via editor)
		if not hint_button.pressed.is_connected(_on_hint_button_pressed):
			hint_button.pressed.connect(_on_hint_button_pressed)
			print("✅ Hint button connected via code!")
		else:
			print("✅ Hint button already connected via editor!")
	else:
		print("⚠️ HintButton not found at Panel2/HintButton")

func update_hints_display():
	if hint_label:
		hint_label.text = "Hints: %d" % hints_remaining
		print("💡 Updated hint label to: ", hint_label.text)
	else:
		print("⚠️ Hint label is null!")
	
	if hint_button:
		hint_button.disabled = (hints_remaining <= 0)
		# TextureButton doesn't have a text property, so we just disable it
		# The hint_label shows the count instead

func _on_hint_button_pressed():
	print("🔔 Hint button pressed!")
	
	if hints_remaining <= 0:
		print("❌ No hints remaining!")
		return
	
	# Find the first incorrectly placed block
	var incorrect_blocks = []
	for block_name in correctly_placed_blocks:
		if not correctly_placed_blocks[block_name]:
			incorrect_blocks.append(block_name)
	
	if incorrect_blocks.is_empty():
		print("✅ All blocks are correctly placed!")
		return
	
	var target_block = incorrect_blocks[0]
	
	# Map blocks to their correct drop zones and display names
	var block_to_zone = {
		"SystembuttonL1": {"zone": "SystemoutprintlnL1", "text": "System.out.println()"},
		"HelloworldbuttonL1": {"zone": "HelloWorldL1", "text": "\"Hello World\""},
		"ClassMainbuttonL1": {"zone": "ClassmmainL1", "text": "class Main"},
		"publicstaticbuttonL1": {"zone": "Public staticL1", "text": "public static void main"},
		"StringbuttonL1": {"zone": "String[]L1", "text": "String[] args"}
	}
	
	var target_data = block_to_zone.get(target_block, {})
	var target_zone = target_data.get("zone", "")
	var hint_text = target_data.get("text", "Place block here")
	
	if target_zone == "":
		print("❌ No drop zone mapped for: ", target_block)
		return
	
	# Show hint on the drop zone
	show_hint_on_zone(target_zone, hint_text)
	
	# Deduct hints but don't affect score
	hints_remaining -= 1
	
	update_hints_display()
	update_score_display()
	
	print("💡 Hint used! ", target_block, " should go to → ", target_zone)
	print("Hints remaining: ", hints_remaining, " | Score: ", current_score)
	
	# Auto-clear hint after 3 seconds
	get_tree().create_timer(3.0).timeout.connect(clear_hint_highlights)

func show_hint_on_zone(zone_name: String, hint_text: String):
	print("🔍 Searching for zone: ", zone_name)
	var zone = find_node_by_name(zone_name)
	
	if zone:
		# Create a temporary label to show just the name
		var hint_label_temp = Label.new()
		hint_label_temp.text = hint_text
		hint_label_temp.name = "HintLabelTemp"
		
		# Style the label with GREEN color
		hint_label_temp.add_theme_color_override("font_color", Color.GREEN)
		hint_label_temp.add_theme_font_size_override("font_size", 20)
		hint_label_temp.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		hint_label_temp.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
		
		# Position it on the zone
		hint_label_temp.position = Vector2(0, 0)
		hint_label_temp.size = zone.size
		hint_label_temp.z_index = 100
		
		# Add to the zone
		zone.add_child(hint_label_temp)
		current_hint_zone = hint_label_temp
		
		# Pulse the zone background with green tint
		zone.modulate = Color(0.5, 1.0, 0.5)  # Greenish tint
		var tween = create_tween()
		tween.set_loops(6)
		tween.tween_property(zone, "scale", Vector2(1.1, 1.1), 0.3)
		tween.tween_property(zone, "scale", Vector2(1.0, 1.0), 0.3)
		
		print("✨ Showed hint on zone: ", zone_name, " with text: ", hint_text)
	else:
		print("❌ Could not find zone: ", zone_name)

func clear_hint_highlights():
	if current_hint_zone:
		# Remove the temporary label
		if current_hint_zone.get_parent():
			current_hint_zone.get_parent().modulate = Color.WHITE
			current_hint_zone.get_parent().scale = Vector2(1.0, 1.0)
		current_hint_zone.queue_free()
		current_hint_zone = null
		print("🔄 Hint highlights cleared")

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
			print("✅ Found at path: ", path)
			return get_node(path)
	
	# Recursive search as last resort
	var found = find_child(node_name, true, false)
	if found:
		print("✅ Found via recursive search: ", node_name)
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
	print("Lesson started! Timer counting down...")

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

func add_correct_placement_points(button_name: String):
	current_score += points_per_correct
	print("Correct placement! +", points_per_correct, " points (Total: ", current_score, ")")
	update_score_display()

func subtract_mistake_points():
	pass

func calculate_final_score():
	var percentage = 0.0
	if max_possible_score > 0:
		percentage = (float(current_score) / float(max_possible_score)) * 100.0
	
	print("=== FINAL SCORE ===")
	var percent_str = "%.1f" % percentage
	print("Final Score: ", current_score, "/", max_possible_score, " (", percent_str, "%)")
	
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
	reset_button_color("SystembuttonL1")
	reset_button_color("HelloworldbuttonL1")
	reset_button_color("ClassMainbuttonL1")
	reset_button_color("publicstaticbuttonL1")
	reset_button_color("StringbuttonL1")

func on_block_placed(button_name: String, is_correct_position: bool):
	print("=== GAME CONTROLLER CALLED ===")
	print("Button: ", button_name, " | Correct: ", is_correct_position)
	
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
	print("Puzzle completed!")
	SoundManager.bgm.stop()
	
	var score_data = calculate_final_score()
	
	var success_scene = preload("res://Lesson 1/ProblemSolved.tscn")
	var success_instance = success_scene.instantiate()
	
	if success_instance.has_method("set_score_data"):
		success_instance.set_score_data(score_data)
	
	submit_score_to_android()
	
	get_tree().current_scene.add_child(success_instance)

func mistakes_limit_reached():
	game_timer.stop()
	print("Maximum mistakes reached! Game over.")
	
	var score_data = calculate_final_score()
	submit_score_to_android()
	
	var retry_scene = preload("res://Lesson 1/Retry.tscn")
	var retry_instance = retry_scene.instantiate()
	
	if retry_instance.has_method("set_score_data"):
		retry_instance.set_score_data(score_data)
	
	get_tree().current_scene.add_child(retry_instance)

func time_up():
	game_timer.stop()
	print("Time's up! Lesson ended.")
	
	var score_data = calculate_final_score()
	submit_score_to_android()
	
	var retry_scene = preload("res://Lesson 1/Retry.tscn")
	var retry_instance = retry_scene.instantiate()
	
	if retry_instance.has_method("set_score_data"):
		retry_instance.set_score_data(score_data)
	
	get_tree().current_scene.add_child(retry_instance)

func reset_game():
	mistakes_made = 0
	current_score = 0
	time_left = 300
	hints_remaining = 1
	
	for block_name in correctly_placed_blocks:
		correctly_placed_blocks[block_name] = false
	
	clear_hint_highlights()
	reset_all_button_colors()
	update_mistakes_display()
	update_score_display()
	update_timer_display()
	update_hints_display()
	
	print("Game state reset!")

func submit_score_to_android(  ):
	print("🎯 Lesson1: Submitting score to Android - Score:", current_score)
	
	var app_plugin = Engine.get_singleton("AppPlugin")
	if app_plugin:
		print("🎯 Lesson1: Calling AppPlugin.submitScore()")
		app_plugin.submitScore(current_score, 1)
	else:
		print("❌ Lesson1: AppPlugin not found!")
