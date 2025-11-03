extends Control


func _on_button_2_pressed() -> void:
	print("🔙 Return button pressed - Using AppPlugin to quit")
	var app = Engine.get_singleton("AppPlugin")
	if app:
		app.navigateBack()
	else:
		print("❌ AppPlugin not found, using fallback")
		get_tree().quit()
