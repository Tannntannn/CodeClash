extends Node

# --- Health values ---
var max_player_health: int = 15
var max_virus_health: int = 15
var player_health: int = max_player_health
var virus_health: int = max_virus_health

# --- References to ProgressBars in the current scene ---
var player_health_bar: ProgressBar = null
var virus_health_bar: ProgressBar = null

# --- Scene isolation ---
var current_scene_path: String = ""
var lesson_sessions: Dictionary = {}

# Called by each scene (in _ready()) to link the scene's bars
func register_healthbars(player_bar: ProgressBar, virus_bar: ProgressBar) -> void:
	# Get current scene info
	var scene = get_tree().current_scene
	var new_scene_path = ""
	if scene and scene.scene_file_path:
		new_scene_path = scene.scene_file_path
	else:
		new_scene_path = str(scene.get_script().resource_path) if scene else "unknown"
	
	print("🎯 Registering health bars for: ", new_scene_path)
	
	# Check if we're switching to a different lesson
	if new_scene_path != current_scene_path:
		current_scene_path = new_scene_path
		print("🆕 New lesson detected: ", current_scene_path)
		
		# Always reset health for new lessons
		force_reset_health()
		
		# Track this lesson session
		if not lesson_sessions.has(current_scene_path):
			lesson_sessions[current_scene_path] = {"started": true, "completed": false}
	
	player_health_bar = player_bar
	virus_health_bar = virus_bar
	update_bars()

# Force reset health - can be called manually and automatically
func force_reset_health() -> void:
	player_health = max_player_health
	virus_health = max_virus_health
	print("🔄 FORCE HEALTH RESET - Player:", player_health, "/", max_player_health, " Virus:", virus_health, "/", max_virus_health)
	update_bars()

# Damage functions you call from your question logic
func damage_player(amount: int = 1) -> void:
	var old_health = player_health
	player_health = clamp(player_health - amount, 0, max_player_health)
	print("⚔️ Player damaged! ", old_health, " -> ", player_health, " (Scene: ", current_scene_path.get_file(), ")")
	update_bars()
	animate_player_damage()

func damage_virus(amount: int = 1) -> void:
	var old_health = virus_health
	virus_health = clamp(virus_health - amount, 0, max_virus_health)
	print("💥 Virus damaged! ", old_health, " -> ", virus_health, " (Scene: ", current_scene_path.get_file(), ")")
	update_bars()
	animate_virus_damage()

# Healing functions (optional)
func heal_player(amount: int = 1) -> void:
	player_health = clamp(player_health + amount, 0, max_player_health)
	update_bars()
	print("💚 Player healed! Health: ", player_health, "/", max_player_health)

func heal_virus(amount: int = 1) -> void:
	virus_health = clamp(virus_health + amount, 0, max_virus_health)
	update_bars()
	print("🩹 Virus healed! Health: ", virus_health, "/", max_virus_health)

# Internal: keep bars synced with health values
func update_bars() -> void:
	if player_health_bar:
		player_health_bar.max_value = max_player_health
		player_health_bar.value = player_health
	if virus_health_bar:
		virus_health_bar.max_value = max_virus_health
		virus_health_bar.value = virus_health

# Animated health bar updates with timing
func animate_player_damage() -> void:
	if not player_health_bar:
		print("⚠️ No player health bar to animate!")
		return
	
	# Create smooth animation for health bar decrease
	var tween = create_tween()
	tween.tween_property(player_health_bar, "value", player_health, 0.3)
	
	# Flash effect when taking damage
	var original_color = player_health_bar.modulate
	player_health_bar.modulate = Color.RED
	tween.parallel().tween_property(player_health_bar, "modulate", original_color, 0.3)

func animate_virus_damage() -> void:
	if not virus_health_bar:
		print("⚠️ No virus health bar to animate!")
		return
	
	# Create smooth animation for health bar decrease
	var tween = create_tween()
	tween.tween_property(virus_health_bar, "value", virus_health, 0.3)
	
	# Flash effect when taking damage
	var original_color = virus_health_bar.modulate
	virus_health_bar.modulate = Color.RED
	tween.parallel().tween_property(virus_health_bar, "modulate", original_color, 0.3)

# Legacy reset health function (for backwards compatibility)
func reset_health() -> void:
	force_reset_health()

# Check if game is over
func is_game_over() -> bool:
	return player_health <= 0 or virus_health <= 0

func get_winner() -> String:
	if player_health <= 0:
		return "virus"
	elif virus_health <= 0:
		return "player"
	else:
		return "none"

# Debug function to check current state
func debug_state() -> void:
	print("🔍 GAMESTATE DEBUG:")
	print("   Current Scene: ", current_scene_path.get_file() if current_scene_path else "none")
	print("   Player Health: ", player_health, "/", max_player_health)
	print("   Virus Health: ", virus_health, "/", max_virus_health)
	print("   Player Bar: ", "exists" if player_health_bar else "missing")
	print("   Virus Bar: ", "exists" if virus_health_bar else "missing")

# Clean up when scene changes
func _notification(what):
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		cleanup_references()

func cleanup_references() -> void:
	print("🧹 Cleaning up GameState references")
	
	# SIMPLIFIED: Just stop all timers without checking connections
	_stop_all_timers_simple()
	
	player_health_bar = null
	virus_health_bar = null
func _stop_all_timers_simple():
	print("🧹 GameState: Stopping all timers (simple)")
	var current_scene = get_tree().current_scene
	if current_scene:
		_find_and_stop_timers_simple(current_scene)

func _find_and_stop_timers_simple(node):
	if node is Timer:
		node.stop()
		print("🧹 GameState: Stopped timer: ", node.name)
	
	for child in node.get_children():
		_find_and_stop_timers_simple(child)
