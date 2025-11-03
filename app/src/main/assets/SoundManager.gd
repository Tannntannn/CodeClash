extends Node

@onready var bgm: AudioStreamPlayer = $BGM
@onready var click: AudioStreamPlayer = $Click
@onready var drop: AudioStreamPlayer = $Drop

func _ready() -> void:
	# Do not auto-play. Lessons will explicitly start/stop BGM.
	pass

func play_click() -> void:
	print("[SoundManager] play_click() called. Stream set:", click.stream != null)
	if click.stream:
		click.play()
	else:
		print("[SoundManager] Click stream is not assigned")

func play_drag() -> void:
	print("[SoundManager] play_drag() called. Stream set:", drop.stream != null)
	if drop.stream:
		drop.play()
	else:
		print("[SoundManager] Drop stream is not assigned")

# Optional: switch background track
func play_bgm(new_track: AudioStream) -> void:
	bgm.stream = new_track
	bgm.play()
