extends Control

signal retry_requested
signal game_over_final

@onready var score_label: Label = $ScoreLabel
@onready var retry_button: TextureButton = $RetryButton
@onready var retries_left_label: Label = $RetriesLeftLabel

func _ready() -> void:
	print("🎮 LosePopup: _ready() called")
	update_retries_left()
	retry_button.pressed.connect(_on_retry_pressed)

func set_score(score: int, total: int) -> void:
	if score_label:
		score_label.text = "%d/%d" % [score, total]

func update_retries_left() -> void:
	var left = RetryManager.retries_left
	print("🎮 LosePopup: Updating retries display - RetryManager.retries_left =", left)
	retries_left_label.text = "%d retries left" % left
	retry_button.disabled = left <= 0
	
	if left <= 0:
		print("⚠️ LosePopup: Retry button disabled - no retries left")

func _on_retry_pressed() -> void:
	print("🎮 LosePopup: Retry button pressed!")
	
	# Check if we can retry BEFORE using the retry
	if RetryManager.can_retry():
		print("🎮 LosePopup: Can retry - using retry now...")
		
		# Use the retry (decrements the counter)
		RetryManager.use_retry()
		
		# Button click animation (quick)
		var tween = create_tween()
		tween.tween_property(retry_button, "scale", Vector2(0.9, 0.9), 0.05)
		tween.tween_property(retry_button, "scale", Vector2(1.0, 1.0), 0.05)
		
		# Hide the popup immediately
		self.visible = false
		
		await tween.finished
		
		print("🎮 LosePopup: Emitting retry_requested signal")
		retry_requested.emit()
	else:
		print("❌ LosePopup: Cannot retry - no retries left")
		retry_button.disabled = true
		game_over_final.emit()
