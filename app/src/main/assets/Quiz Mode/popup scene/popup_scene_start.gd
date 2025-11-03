extends Control

signal instructions_closed

@onready var start_button = $StartButton

func _ready():
	print("🎮 Popup scene starting...")
	# Allow this popup to work even while the game is paused
	process_mode = Node.PROCESS_MODE_ALWAYS
	
	# Ensure the popup is visible and on top
	visible = true
	modulate = Color.WHITE
	z_index = 1000  # Make sure it's on top
	
	print("✅ Popup visibility set, z_index:", z_index)
	
	if start_button:
		print("✅ Start button found, connecting signal")
		start_button.pressed.connect(_on_start_button_pressed)
	else:
		print("❌ Start button not found!")
	
	# Pause the game while showing instructions
	get_tree().paused = true
	print("✅ Game paused, popup should be visible")
	
	# Force update
	call_deferred("_ensure_visible")

func _ensure_visible():
	print("🔍 Ensuring popup is visible...")
	visible = true
	modulate = Color.WHITE
	print("✅ Popup should now be visible")


func _on_start_button_pressed() -> void:
	print("✅ Start button pressed!")  # Debugging message (optional)
	
	# Unpause the game and close the popup
	get_tree().paused = false
	instructions_closed.emit()
	queue_free()
