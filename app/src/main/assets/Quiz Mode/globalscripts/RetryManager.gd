extends Node

var retries_left: int = 3

func reset_retries() -> void:
	retries_left = 3

func use_retry() -> void:
	if retries_left > 0:
		retries_left -= 1

func can_retry() -> bool:
	return retries_left > 0
