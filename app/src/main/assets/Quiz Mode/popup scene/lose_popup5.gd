extends Control

signal retry_requested
signal game_over_final

@onready var score_label: Label = $ScoreLabel


func set_score(score: int, total: int) -> void:
	if score_label:
		score_label.text = "%d/%d" % [score, total]

func _on_quit_button_pressed() -> void:
	print("🔙 Quit button pressed - Using AppPlugin to quit")
	var app = Engine.get_singleton("AppPlugin")
	if app:
		app.navigateBack()
	else:
		print("❌ AppPlugin not found, using fallback")
		get_tree().quit()
