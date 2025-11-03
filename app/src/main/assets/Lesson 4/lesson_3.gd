extends Control
@onready var game_timer = $Timer
@onready var time_label = $Panel2/TimeLabel
@onready var mistakes_label = $Panel2/MistakesLabel  # Add this UI element to show mistakes
var time_left = 300	
var correctly_placed_blocks = {}  # Track which blocks are correctly placed
var total_blocks = 5   # You have 4 buttons
# Mistake tracking variables
var mistakes_made = 0
var max_mistakes = 3  # Change this value based on the lesson difficulty

func _ready():
	# Add this controller to a group so buttons can find it
	add_to_group("GameController")
	
	setup_timer()
	start_countdown()
	setup_mistakes_display()
	# Initialize tracking - no blocks are correctly placed yet
	correctly_placed_blocks["intL2button"] = false
	correctly_placed_blocks["StringL2button"] = false
	correctly_placed_blocks["charL2button"] = false
	correctly_placed_blocks["booleanL2button"] = false

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

# Function to make a button red when there's an error
func show_button_error(button_name: String):
	print("Trying to make button red: ", button_name)
	
	# Try different possible paths
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
		print("Available children: ")
		for child in get_children():
			print("  - ", child.name)

# Function to reset button to normal color
func reset_button_color(button_name: String):
	print("Trying to reset button color: ", button_name)
	
	# Try different possible paths
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
	else:
		print("ERROR: Could not find button for reset: ", button_name)

# Function to reset all buttons to normal color
func reset_all_button_colors():
	reset_button_color("intL2button")
	reset_button_color("StringL2button") 
	reset_button_color("charL2button")
	reset_button_color("booleanL2button")

# Call this when a block is placed (correctly or incorrectly)
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
		# Correct placement
		correctly_placed_blocks[button_name] = true
		reset_button_color(button_name)  # Reset to normal color
		print("Correct placement for: ", button_name)
	else:
		# Incorrect placement - count as mistake
		correctly_placed_blocks[button_name] = false
		show_button_error(button_name)  # Make button red
		mistakes_made += 1
		update_mistakes_display()
		print("Mistake made! Total mistakes: ", mistakes_made, "/", max_mistakes)
		
		# Check if maximum mistakes reached
		if mistakes_made >= max_mistakes:
			mistakes_limit_reached()
			return  # Exit early to prevent further processing
	
	# Count how many are correctly placed
	var correct_count = 0
	for block_name in correctly_placed_blocks:
		if correctly_placed_blocks[block_name]:
			correct_count += 1
	
	print("Correctly placed blocks: ", correct_count, "/", total_blocks)
	print("=== END GAME CONTROLLER ===")
	print("")
	
	if correct_count >= total_blocks:
		puzzle_completed()

func puzzle_completed():
	game_timer.stop()
	print("Puzzle completed!")
	
	# Show success popup
	var success_scene = preload("res://Lesson 3/ProlemSolvedL3.tscn")
	var success_instance = success_scene.instantiate()
	get_tree().current_scene.add_child(success_instance)

func mistakes_limit_reached():
	game_timer.stop()
	print("Maximum mistakes reached! Game over.")
	
	# Show retry popup
	var retry_scene = preload("res://Lesson 3/RetryLesson3.tscn")
	var retry_instance = retry_scene.instantiate()
	get_tree().current_scene.add_child(retry_instance)

func time_up():
	game_timer.stop()
	print("Time's up! Lesson ended.")
	
	var retry_scene = preload("res://Lesson 3/RetryLesson3.tscn")
	var retry_instance = retry_scene.instantiate()
	get_tree().current_scene.add_child(retry_instance)

# Optional: Function to set max mistakes for different lessons
func set_max_mistakes(new_max: int):
	max_mistakes = new_max
	update_mistakes_display()
	print("Max mistakes set to: ", max_mistakes)

# Optional: Function to reset mistakes (useful for retry functionality)
func reset_mistakes():
	mistakes_made = 0
	reset_all_button_colors()  # Reset button colors too
	update_mistakes_display()
	print("Mistakes reset")
