extends Control

func _ready() -> void:
	var lesson_name := _read_text("user://scene_name.txt")
	var activity_type := _read_text("user://activity_type.txt")
	if lesson_name == "":
		lesson_name = "Lesson 1"
	if activity_type == "":
		activity_type = "quiz"

	print("📌 Launcher read:", " lesson=", lesson_name, " mode=", activity_type)

	var scene_path := _get_scene_path(lesson_name, activity_type)
	print("🎯 Resolved scene path:", scene_path)
	if scene_path != "":
		# FIXED: Add comprehensive error handling for scene loading
		if not is_inside_tree():
			print("❌ Launcher: Not inside tree, cannot change scene")
			_show_fallback_ui("Launcher not properly initialized")
			return
			
		var err := get_tree().change_scene_to_file(scene_path)
		if err != OK:
			push_error("Failed to load scene: " + scene_path + " err=" + str(err))
			_show_fallback_ui("Failed to load: " + scene_path + " (Error: " + str(err) + ")")
		else:
			print("✅ Launcher: Successfully loaded scene: " + scene_path)
	else:
		push_error("Unknown lesson/mode; showing fallback")
		_show_fallback_ui("Unknown selection: " + lesson_name + ", " + activity_type)

func _read_text(path: String) -> String:
	# FIXED: Add error handling for file operations
	var f := FileAccess.open(path, FileAccess.READ)
	if f:
		var t := f.get_as_text().strip_edges()
		f.close()
		return t
	else:
		print("⚠️ Launcher: Could not read file: " + path + " (Error: " + str(FileAccess.get_open_error()) + ")")
		return ""

func _get_scene_path(lesson_name: String, activity_type: String) -> String:
	var lower := lesson_name.to_lower().strip_edges()
	# Normalize common punctuation/aliases
	lower = lower.replace("&", "and")
	lower = lower.replace("_", " ")

	# Normalize direct lesson number variants first
	if lower.contains("lesson 1") or lower.contains("lesson1"):
		lower = "lesson 1"
	elif lower.contains("lesson 2") or lower.contains("lesson2"):
		lower = "lesson 2"
	elif lower.contains("lesson 3") or lower.contains("lesson3"):
		lower = "lesson 3"
	elif lower.contains("lesson 4") or lower.contains("lesson4"):
		lower = "lesson 4"
	elif lower.contains("lesson 5") or lower.contains("lesson5"):
		lower = "lesson 5"
	elif lower.contains("lesson 6") or lower.contains("lesson6"):
		lower = "lesson 6"
	else:
		# Allow lesson titles as provided by Android (map to lesson numbers)
		var title_to_lesson := {
			"introduction to java": "lesson 1",
			"variables and data": "lesson 2",
			# Lesson 3 aliases
			"operations and expressions": "lesson 3",
			"operators and expressions": "lesson 3",
			"operation and expressions": "lesson 3",
			"operator and expressions": "lesson 3",
			"operations expressions": "lesson 3",
			# Lesson 4-6
			"conditional statements": "lesson 4",
			"conditional loops": "lesson 5",
			"arrays": "lesson 6",
		}
		if title_to_lesson.has(lower):
			lower = title_to_lesson[lower]

	var mode := activity_type.to_lower().strip_edges()
	if mode == "code builder" or mode == "codebuilder":
		mode = "code_builder"

	match mode:
		"quiz":
			match lower:
				"lesson 1": return "res://Quiz Mode/Lesson1 Quiz/Lesson1.tscn"
				"lesson 2": return "res://Quiz Mode/Lesson2 Quiz/Lesson2.tscn"
				"lesson 3": return "res://Quiz Mode/Lesson3 Quiz/Lesson3.tscn"
				"lesson 4": return "res://Quiz Mode/Lesson4 Quiz/Lesson4.tscn"
				"lesson 5": return "res://Quiz Mode/Lesson5 Quiz/Lesson5.tscn"
				"lesson 6": return "res://Quiz Mode/Lesson6 quiz/Lesson6.tscn"
		"code_builder":
			match lower:
				"lesson 1": return "res://Lesson 1/Lesson1Start.tscn"
				"lesson 2": return "res://lesson 2/Lesson2Start.tscn"
				"lesson 3": return "res://Lesson 3/Lesson3Start.tscn"
				"lesson 4": return "res://Lesson 4/Lesson4Start.tscn"
				"lesson 5": return "res://Lesson 5/Lesson5Start.tscn"
				"lesson 6": return "res://Lesson 6/Lesson6Start.tscn"
	return ""

func _show_fallback_ui(msg: String) -> void:
	var lbl := Label.new()
	lbl.text = "Launcher fallback\n" + msg
	lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	lbl.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	lbl.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	lbl.size_flags_vertical = Control.SIZE_EXPAND_FILL
	add_child(lbl)
