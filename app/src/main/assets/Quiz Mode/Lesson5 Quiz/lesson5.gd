extends Control

@onready var correct_sound = $CorrectSound
@onready var wrong_sound = $WrongSound
@onready var warning_sound = $WarningSound
@onready var warning_overlay = $WarningOverlay
@onready var player_health_bar = $PlayerHealthBar
@onready var player_sprite = $Player/AnimatedSprite2D
@onready var monster_sprite = $Monster/AnimatedSprite2D
@onready var virus_health_bar = $VirusHealthBar
@onready var question_label = $QuestionLabel
@onready var buttons = [$A1, $B1, $C1, $D1]
@onready var labels = [$A1/Label, $B1/Label, $C1/Label, $D1/Label]
@onready var bg_music = $BGMusic
@onready var hint_button = $HintButton
@onready var hint_label = $HintLabel

var lose_scene = preload("res://Quiz Mode/popup scene/lose_popup5.tscn")
var win_scene = preload("res://Quiz Mode/popup scene/win_popup5.tscn")
var fireball_scene = preload("res://Quiz Mode/Lesson5 Quiz/fireball5.tscn")
var virusfireball_scene = preload("res://Quiz Mode/Lesson5 Quiz/virusfireball5.tscn")
var popup_scene = preload("res://Quiz Mode/popup scene/Popup scene Start.tscn")

var current_q = 0
var score = 0
var answered = false
var timer: Timer
var time_left = 15
var warning_active = false
var quiz_finished = false
var attack_in_progress = false

# Hint system variables
var hints_remaining = 3
var hint_blink_tween: Tween
var is_hint_available = true
var correct_answer_blink_tween: Tween

var questions := [
	{"text": "Which loop is best when the number of repetitions is known?", "answers": ["while", "do-while", "for", "switch"], "correct": 2},
	{"text": "Which loop checks condition before running?", "answers": ["for", "do-while", "while", "repeat"], "correct": 2},
	{"text": "Which loop runs at least once?", "answers": ["for", "do-while", "while", "if"], "correct": 1},
	{"text": "What keyword exits a loop early?", "answers": ["end", "stop", "break", "exit"], "correct": 2},
	{"text": "What keyword skips to the next iteration?", "answers": ["next", "skip", "continue", "break"], "correct": 2},
	{"text": "Which loop is most compact for counters?", "answers": ["do-while", "for", "while", "repeat"], "correct": 1},
	{"text": "What must be true for a while loop to continue?", "answers": ["False", "True", "Equal", "Null"], "correct": 1},
	{"text": "What happens if a loop never ends?", "answers": ["Error", "Infinite loop", "Stops early", "Skips code"], "correct": 1},
	{"text": "What can you use inside a for loop?", "answers": ["Condition only", "Init, cond, update", "Loop only", "None"], "correct": 1},
	{"text": "Which part of for loop runs after each iteration?", "answers": ["Condition", "Update", "Init", "Body"], "correct": 1},
	{"text": "What does nested loop mean?", "answers": ["Loop in loop", "Loop in if", "Loop after if", "Loop with break"], "correct": 0},
	{"text": "What is an infinite while loop example?", "answers": ["while(true)", "while(false)", "while(0)", "while(null)"], "correct": 0},
	{"text": "What happens when condition is false?", "answers": ["Runs again", "Ends loop", "Skips condition", "Freezes"], "correct": 1},
	{"text": "Which keyword ends all loops and returns?", "answers": ["continue", "stop", "return", "break"], "correct": 2},
	{"text": "What can cause logic error in loops?", "answers": ["Wrong semicolon", "Missing braces", "Wrong condition", "Comment"], "correct": 2}
]

func _ready():
	print("🎮 LESSON 2 STARTING")
	GameState.force_reset_health()
	questions.shuffle()
	GameState.register_healthbars(player_health_bar, virus_health_bar)
	for i in 4:
		buttons[i].pressed.connect(func(): answer(i))
	
	# Setup hint button
	if hint_button:
		hint_button.pressed.connect(use_hint)
	
	timer = Timer.new()
	timer.wait_time = 1.0
	timer.timeout.connect(tick)
	add_child(timer)
	warning_overlay.modulate.a = 0.0
	
	# Initialize hint system
	update_hint_display()
	
	# Start background music
	if bg_music:
		bg_music.play()
	
	# Show instructions popup first
	show_instructions_popup()

func show_instructions_popup():
	var popup_instance = popup_scene.instantiate()
	add_child(popup_instance)
	popup_instance.instructions_closed.connect(_on_instructions_closed)

func _on_instructions_closed():
	print("✅ Instructions closed, starting quiz!")
	show_q()

func update_hint_display():
	if hint_label:
		hint_label.text = str(hints_remaining)
	
	if hint_button:
		if hints_remaining <= 0:
			# No hints left - make button gray
			hint_button.modulate = Color(0.5, 0.5, 0.5, 1.0)
			hint_button.disabled = true
			is_hint_available = false
		else:
			# Hints available - normal color and enable
			hint_button.modulate = Color.WHITE
			hint_button.disabled = false
			is_hint_available = true

func start_hint_blink():
	if not hint_button or not is_hint_available:
		return
	
	# Stop existing blink animation if any
	stop_hint_blink()
	
	# Create blinking animation
	hint_blink_tween = create_tween()
	hint_blink_tween.set_loops()
	hint_blink_tween.tween_property(hint_button, "modulate:a", 0.3, 0.5)
	hint_blink_tween.tween_property(hint_button, "modulate:a", 1.0, 0.5)

func stop_hint_blink():
	if hint_blink_tween and hint_blink_tween.is_valid():
		hint_blink_tween.kill()
		hint_blink_tween = null
	if hint_button:
		hint_button.modulate.a = 1.0

func use_hint():
	if hints_remaining <= 0 or answered:
		return
	
	hints_remaining -= 1
	update_hint_display()
	
	# Start blinking the correct answer
	start_correct_answer_blink()
	
	# Visual feedback for hint button
	animate_hint_button()

func start_correct_answer_blink():
	var correct_idx = questions[current_q].correct
	var correct_button = buttons[correct_idx]
	
	# Stop any existing blink animation
	stop_correct_answer_blink()
	
	# Create infinite looping blink animation in green
	correct_answer_blink_tween = create_tween()
	correct_answer_blink_tween.set_loops()
	correct_answer_blink_tween.tween_property(correct_button, "modulate", Color(0.3, 1.0, 0.3, 1.0), 0.4)  # Bright green
	correct_answer_blink_tween.tween_property(correct_button, "modulate", Color.WHITE, 0.4)  # Back to normal

func stop_correct_answer_blink():
	if correct_answer_blink_tween and correct_answer_blink_tween.is_valid():
		correct_answer_blink_tween.kill()
		correct_answer_blink_tween = null
	
	# Reset all button colors to white
	for btn in buttons:
		if not btn.disabled:
			btn.modulate = Color.WHITE

func animate_hint_button():
	if not hint_button:
		return
	
	var anim_tween = create_tween()
	anim_tween.tween_property(hint_button, "scale", Vector2(1.2, 1.2), 0.1)
	anim_tween.tween_property(hint_button, "scale", Vector2.ONE, 0.1)

func show_q():
	# Check if game should end due to health before showing new question
	if should_end_game():
		end_quiz()
		return
	
	# Stop any correct answer blinking from previous question
	stop_correct_answer_blink()
		
	var q = questions[current_q]
	question_label.text = q.text
	for i in 4:
		labels[i].text = q.answers[i]
		buttons[i].disabled = false
		buttons[i].modulate = Color.WHITE  # Reset button appearance
	answered = false
	time_left = 15
	warning_active = false
	warning_overlay.modulate.a = 0.0
	timer.start()

func answer(i):
	if answered: return
	answered = true
	timer.stop()
	stop_warning()
	stop_correct_answer_blink()  # Stop blinking when answer is clicked
	for b in buttons: b.disabled = true
	animate_btn(buttons[i])
	
	if i == questions[current_q].correct:
		score += 1
		await correct()
	else:
		await wrong()

func stop_warning():
	warning_active = false
	if warning_sound and warning_sound.playing:
		warning_sound.stop()
	warning_overlay.modulate.a = 0.0
	if quiz_finished:
		warning_overlay.visible = false

func correct():
	if correct_sound: correct_sound.play()
	player_sprite.play("ATTACK5")
	await get_tree().create_timer(0.3).timeout
	shoot_fireball()
	await get_tree().create_timer(1.5).timeout
	if not quiz_finished:
		player_sprite.play("IDLE5")

func wrong():
	if wrong_sound: wrong_sound.play()
	monster_sprite.play("ATTACKm5")
	await get_tree().create_timer(0.25).timeout
	shoot_virus()
	await get_tree().create_timer(1.5).timeout
	if not quiz_finished:
		monster_sprite.play("IDLEm5")

func should_end_game() -> bool:
	return GameState.player_health <= 0 or GameState.virus_health <= 0

func next_q():
	current_q += 1
	
	# Check if all questions are done
	if current_q >= questions.size():
		end_quiz()
		return
	
	# Check if game should end due to health
	if should_end_game():
		end_quiz()
		return
		
	# Continue to next question
	show_q()

func end_quiz():
	quiz_finished = true
	timer.stop()
	stop_warning()
	stop_hint_blink()  # Stop hint blinking when quiz ends
	stop_correct_answer_blink()  # Stop correct answer blinking when quiz ends
	
	# Stop background music when quiz ends
	if bg_music and bg_music.playing:
		bg_music.stop()
	
	# Submit score to Android
	submit_score_to_android()
	
	# Determine winner based on health
	if GameState.player_health > GameState.virus_health:
		player_sprite.play("WIN5")
		# Don't change monster animation if it's already dead
		if GameState.virus_health > 0:
			monster_sprite.play("IDLEm5")
		show_popup(win_scene.instantiate())
	elif GameState.virus_health > GameState.player_health:
		monster_sprite.play("WINm5")
		# Don't change player animation if it's already dead
		if GameState.player_health > 0:
			player_sprite.play("DEAD5")
		show_popup(lose_scene.instantiate())
	else:
		# Tie - could go either way, let's say player wins on tie
		player_sprite.play("WIN5")
		# Don't change monster animation if it's already dead
		if GameState.virus_health > 0:
			monster_sprite.play("IDLEm5")
		show_popup(win_scene.instantiate())

func shoot_fireball():
	var fb = fireball_scene.instantiate()
	fb.global_position = $Player/ShootPoint.global_position
	add_child(fb)
	fb.setup_target($Monster/ShootPoint)
	fb.hit_monster.connect(hit_monster)

func shoot_virus():
	var vb = virusfireball_scene.instantiate()
	vb.global_position = $Monster/ShootPoint.global_position
	add_child(vb)
	vb.setup_target($Player/ShootPoint)
	vb.hit_player.connect(hit_player)

func hit_monster():
	GameState.damage_virus(1)
	monster_sprite.play("HITm5")
	flash(monster_sprite)
	await get_tree().create_timer(0.5).timeout
	
	if GameState.virus_health <= 0:
		monster_sprite.play("DEADm5")
		await get_tree().create_timer(1.0).timeout
		end_quiz()
	elif not quiz_finished:
		monster_sprite.play("IDLEm5")
		next_q()

func hit_player():
	GameState.damage_player(1)
	player_sprite.play("HIT5")
	flash(player_sprite)
	await get_tree().create_timer(0.5).timeout
	
	if GameState.player_health <= 0:
		player_sprite.play("DEAD5")
		await get_tree().create_timer(1.0).timeout
		end_quiz()
	elif not quiz_finished:
		player_sprite.play("IDLE5")
		next_q()

func flash(sprite):
	sprite.modulate = Color.RED
	await get_tree().create_timer(0.45).timeout
	sprite.modulate = Color.WHITE

func tick():
	if quiz_finished:
		return
	time_left -= 1
	print("⏰ TIME LEFT:", time_left, "WARNING_ACTIVE:", warning_active)
	if time_left == 5 and not warning_active:
		print("🚨 TRIGGERING WARNING!")
		warning_active = true
		if warning_sound:
			warning_sound.play()
		await flash_warning()
	if time_left <= 0:
		timer.stop()
		timeout()

func flash_warning():
	print("🚨 FLASH WARNING STARTED")
	warning_overlay.visible = true
	warning_overlay.modulate.a = 0.0
	
	for i in 5:
		if answered or not warning_active or quiz_finished:
			print("🚨 FLASH WARNING STOPPED - answered:", answered, "warning_active:", warning_active, "quiz_finished:", quiz_finished)
			break
		print("🚨 FLASH WARNING CYCLE:", i+1)
		var t = create_tween()
		t.tween_property(warning_overlay, "modulate:a", 0.8, 0.1)
		await t.finished
		await get_tree().create_timer(0.2).timeout
		t = create_tween()
		t.tween_property(warning_overlay, "modulate:a", 0.0, 0.7)
		await t.finished
		await get_tree().create_timer(0.1).timeout
	print("🚨 FLASH WARNING COMPLETED")

func timeout():
	if answered: return
	answered = true
	stop_warning()
	for b in buttons: b.disabled = true
	
	wrong()

func animate_btn(btn):
	var t = create_tween()
	t.tween_property(btn, "scale", Vector2(0.9, 0.9), 0.05)
	t.tween_property(btn, "scale", Vector2.ONE, 0.1)

func show_popup(popup):
	if not is_inside_tree():
		return
	add_child(popup)
	if not is_inside_tree():
		return
	if popup.has_method("set_score"):
		popup.set_score(score, questions.size())
	if popup.has_signal("retry_requested"):
		popup.retry_requested.connect(retry)
	if popup.has_signal("game_over_final"):
		popup.game_over_final.connect(game_over)
	hide_ui()
	popup.position = get_viewport().get_visible_rect().size / 2 - popup.size / 2

func retry():
	if not is_inside_tree():
		return
	for child in get_children():
		if child.has_signal("retry_requested"):
			child.queue_free()
	if not is_inside_tree():
		return
	current_q = 0
	score = 0
	answered = false
	quiz_finished = false
	questions.shuffle()
	GameState.force_reset_health()
	player_sprite.play("IDLE5")
	monster_sprite.play("IDLEm5")
	warning_overlay.visible = true
	warning_overlay.modulate.a = 0.0
	warning_active = false
	
	# Reset hints
	hints_remaining = 3
	is_hint_available = true
	update_hint_display()
	
	# Restart background music
	if bg_music:
		bg_music.play()
	
	show_ui()
	show_q()

func game_over():
	get_tree().reload_current_scene()

func hide_ui():
	for n in ["QuestionBar", "QuestionLabel", "A1", "B1", "C1", "D1"]:
		if has_node(n):
			get_node(n).visible = false

func show_ui():
	for n in ["QuestionBar", "QuestionLabel", "A1", "B1", "C1", "D1"]:
		if has_node(n):
			get_node(n).visible = true

func _notification(what):
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		cleanup_timers()
		stop_hint_blink()
		stop_correct_answer_blink()

func cleanup_timers():
	if timer and is_instance_valid(timer):
		timer.stop()
		timer.queue_free()
		timer = null

func submit_score_to_android():
	print("🎯 Lesson1: Submitting score to Android - Score: " + str(score) + ", Questions: " + str(questions.size()))
	
	var attempts_used = 1
	
	var app = Engine.get_singleton("AppPlugin")
	if app:
		print("🎯 Lesson1: Calling AppPlugin.submitScore()")
		app.submitScore(score, attempts_used)
	else:
		print("❌ Lesson1: AppPlugin not found, cannot submit score")
