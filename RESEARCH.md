# PathFinder 🧗
---
### Model
- For efficient real time detection, we should train a model using both YOLO and TensorFlow.
	- YOLO is incredibly efficient for real time object detection and can quickly identify the holds in a given scene.
	- TensorFlow comes built with models pre-trained to track the position of our climber as they traverse the path.
- ML models for mobile application can run real time with TensorFlow Lite.
	- First, a model is trained, then it is converted to TensorFlow Lite for efficiency.