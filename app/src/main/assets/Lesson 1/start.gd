extends Control

# ✅ App plugin check
var appPlugin = Engine.get_singleton("AppPlugin")

func _ready() -> void:
	if appPlugin:
		print("✅ App plugin is available")
		appPlugin.connect("open_game", Callable(self, "_on_open_game"))
	else:
		print("❌ App plugin not found")

func _on_start_pressed() -> void:
	get_tree().change_scene_to_file("res://Lesson 1/Lesson1.tscn")

func _on_open_game(scene_name: String) -> void:
	print("📌 Switching to: ", scene_name)
	match scene_name:
		"Lesson 1":
			get_tree().change_scene_to_file("res://Lesson 1/Lesson1Start.tscn")
		"Lesson 2":
			get_tree().change_scene_to_file("res://lesson 2/Lesson2Start.tscn")
		_:
			push_error("⚠ Unknown scene requested: " + scene_name)
