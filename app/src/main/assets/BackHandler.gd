extends Node

func _ready():
	print("🔙 BackHandler: Initialized with IMMEDIATE EXIT approach")
	# Enable proper quit behavior
	get_tree().set_quit_on_go_back(true)

func _input(event):
	# IMMEDIATE EXIT: Handle Android back button (ui_cancel) directly
	if event.is_action_pressed("ui_cancel"):
		print("🔙 BackHandler: ui_cancel pressed - IMMEDIATE EXIT")
		_handle_back_button()

func _handle_back_button():
	print("🔙 BackHandler: Back button pressed - IMMEDIATE EXIT")
	
	# IMMEDIATE EXIT: Use AppPlugin to kill process immediately
	var app = Engine.get_singleton("AppPlugin")
	if app:
		print("🔙 BackHandler: Calling AppPlugin.navigateBack() - IMMEDIATE EXIT")
		app.navigateBack()
	else:
		print("❌ BackHandler: AppPlugin not found, using get_tree().quit()")
		get_tree().quit()

func _notification(what):
	# Handle Android back button notification
	if what == NOTIFICATION_WM_GO_BACK_REQUEST:
		print("🔙 BackHandler: Android back button notification received - IMMEDIATE EXIT")
		_handle_back_button()
	elif what == NOTIFICATION_WM_CLOSE_REQUEST:
		print("🔙 BackHandler: Close request received - IMMEDIATE EXIT")
		_handle_back_button()
