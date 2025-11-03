extends Area2D

signal hit_monster

var target_position: Vector2
var speed: float = 1000
var target_node: Node2D
var has_hit: bool = false  # Prevent multiple hits

func _ready():
	# Only connect if not already connected to prevent the error
	if not body_entered.is_connected(_on_body_entered):
		body_entered.connect(_on_body_entered)
	
	# Also check for area_entered signal if you're using Area2D detection
	if has_signal("area_entered") and not area_entered.is_connected(_on_area_entered):
		area_entered.connect(_on_area_entered)

func setup_target(target: Node2D):
	target_node = target
	target_position = target.global_position

func _physics_process(delta):
	# Stop processing if tree is being quit to prevent freezing
	if not is_inside_tree():
		return
		
	if has_hit:
		return
		
	if target_node:
		var direction = (target_position - global_position).normalized()
		global_position += direction * speed * delta
		
		# Check if we're close enough to the target
		if global_position.distance_to(target_position) < 10:
			hit_target()

func _on_body_entered(body):
	if has_hit:
		return
	if body.name == "Monster":
		hit_target()

func _on_area_entered(area):
	if has_hit:
		return
	# Alternative detection method
	if area.get_parent() and area.get_parent().name == "Monster":
		hit_target()

func hit_target():
	if has_hit:
		return
	has_hit = true
	emit_signal("hit_monster")
	queue_free()
