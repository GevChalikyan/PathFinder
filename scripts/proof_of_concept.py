import tensorflow as tf
import numpy as np
from matplotlib import pyplot as plt
import cv2

def draw_keypoints(frame, keypoints, confidence_threshold):
	frame_height, frame_width, _ = frame.shape
	input_height = input_image.shape[1]
	input_width = input_image.shape[2]

	# Calculate the scale and offset
	scale = min(input_height / frame_height, input_width / frame_width)
	offset_y = (input_height - scale * frame_height) / 2.0
	offset_x = (input_width - scale * frame_width) / 2.0

	keypoints = keypoints[0, 0, :, :]

	for kp in keypoints:
		ky, kx, kp_conf = kp
		if kp_conf > confidence_threshold:
			# Adjust keypoints
			kx = (kx * input_width - offset_x) / scale
			ky = (ky * input_height - offset_y) / scale
			cv2.circle(frame, (int(kx), int(ky)), 4, (0, 255, 0), -1)

def draw_connections(frame, keypoints, edges, confidence_threshold):
	frame_height, frame_width, _ = frame.shape
	input_height = input_image.shape[1]
	input_width = input_image.shape[2]

	# Calculate the scale and offset
	scale = min(input_height / frame_height, input_width / frame_width)
	offset_y = (input_height - scale * frame_height) / 2.0
	offset_x = (input_width - scale * frame_width) / 2.0

	keypoints = keypoints[0, 0, :, :]

	for edge, color in edges.items():
		p1, p2 = edge
		y1, x1, c1 = keypoints[p1]
		y2, x2, c2 = keypoints[p2]
		if (c1 > confidence_threshold) and (c2 > confidence_threshold):
			# Adjust keypoints
			x1 = (x1 * input_width - offset_x) / scale
			y1 = (y1 * input_height - offset_y) / scale
			x2 = (x2 * input_width - offset_x) / scale
			y2 = (y2 * input_height - offset_y) / scale
			cv2.line(frame, (int(x1), int(y1)), (int(x2), int(y2)), (0, 0, 255), 2)
			
EDGES = {
	(0, 1): 'm',
	(0, 2): 'c',
	(1, 3): 'm',
	(2, 4): 'c',
	(0, 5): 'm',
	(0, 6): 'c',
	(5, 7): 'm',
	(7, 9): 'm',
	(6, 8): 'c',
	(8, 10): 'c',
	(5, 6): 'y',
	(5, 11): 'm',
	(6, 12): 'c',
	(11, 12): 'y',
	(11, 13): 'm',
	(13, 15): 'm',
	(12, 14): 'c',
	(14, 16): 'c'
}

interpreter = tf.lite.Interpreter(model_path='../models/MoveNet_Lightning.tflite')
interpreter.allocate_tensors()

# Open camera (May need to be played with)
cap = cv2.VideoCapture("sample3.mp4")

# Get properties of the video
fps = cap.get(cv2.CAP_PROP_FPS)
frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

# Define the codec and create VideoWriter object
fourcc = cv2.VideoWriter_fourcc(*'mp4v')  # You can also use 'XVID' or 'MJPG'
out = cv2.VideoWriter('output.mp4', fourcc, fps, (frame_width, frame_height))

while cap.isOpened():
	ret, frame = cap.read()
	if not ret:
		break
	
	# Reshape image
	img = frame.copy()
	img = tf.image.resize_with_pad(np.expand_dims(img, axis=0), 192,192)
	input_image = tf.cast(img, dtype=tf.float32)
	
	# Setup input and output 
	input_details = interpreter.get_input_details()
	output_details = interpreter.get_output_details()
	
	# Make predictions 
	interpreter.set_tensor(input_details[0]['index'], np.array(input_image))
	interpreter.invoke()
	keypoints_with_scores = interpreter.get_tensor(output_details[0]['index'])
	
	# Rendering 
	___CONFIDENCE_THRESHOLD = 0.4
	draw_connections(frame, keypoints_with_scores, EDGES, ___CONFIDENCE_THRESHOLD)
	draw_keypoints(frame, keypoints_with_scores, ___CONFIDENCE_THRESHOLD)
	
	# Save frame
	out.write(frame)
	
	cv2.imshow('MoveNet Lightning', frame)
	
	if cv2.waitKey(10) & 0xFF==ord('q'):
		break

# Close open streams
cap.release()
out.release()
cv2.destroyAllWindows()

# Needed for windows to close properly (for some reason)
cv2.waitKey(1)