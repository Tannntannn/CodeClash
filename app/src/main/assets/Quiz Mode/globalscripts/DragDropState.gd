extends Node

var score: int = 0
var correct_placements: int = 0
var total_attempts: int = 0
var current_lesson: String = ""

func add_points(points: int) -> void:
	score += points
	print("🎯 DragDrop Score updated:", score)

func reset_score() -> void:
	score = 0
	correct_placements = 0
	total_attempts = 0
	print("🔄 DragDrop Score reset")

func set_lesson(lesson_name: String) -> void:
	current_lesson = lesson_name
	print("📚 DragDrop Lesson set to:", current_lesson)

func on_correct_placement() -> void:
	correct_placements += 1
	total_attempts += 1
	print("✅ Correct placement! Progress:", correct_placements, "/", total_attempts)

func on_wrong_placement() -> void:
	total_attempts += 1
	print("❌ Wrong placement! Progress:", correct_placements, "/", total_attempts)

func get_success_rate() -> float:
	if total_attempts == 0:
		return 0.0
	return (correct_placements * 100.0) / total_attempts

func get_progress_text() -> String:
	return "Progress: %d/%d" % [correct_placements, total_attempts]
