📱 SmartSpeak: Eye Gaze Communication App with IoT Integration
SmartSpeak is an Android application that empowers individuals with speech and motor impairments to communicate using eye gaze. The app also integrates with smart home devices to enable greater independence.

📌 Features
👁️ Eye Gaze Control using MediaPipe

🔊 Text-to-Speech Communication

💡 Smart Home Integration (e.g., control lights)

📦 Firebase Realtime Database support

📶 Raspberry Pi IoT simulation with sensors and actuators

⚙️ Adaptive gaze sensitivity and feedback system


🛠️ Technologies Used
Kotlin (Jetpack Compose) – Android development

MediaPipe – Eye gaze detection

Firebase – Realtime Database

Raspberry Pi 4 – IoT integration

TTS (Text-to-Speech) – Voice feedback



🚀 Getting Started
Prerequisites
Android Studio installed

Raspberry Pi set up and connected to Firebase

Firebase project with Realtime Database configured


Open the project in Android Studio.

Set up Firebase:

Add google-services.json to app/ folder.

Enable Realtime Database in Firebase console.

Run the app on a physical device (camera required).


🧰 Firebase & Raspberry Pi Integration
To enable communication between your SmartSpeak app and IoT devices via Firebase, use the firebase_listener.py script on your Raspberry Pi.

1. 📄 serviceAccountKey.json
Download your Firebase service account key from:


Firebase Console > Project Settings > Service Accounts > Generate New Private Key
Save the key as serviceAccountKey.json in the same directory as your Python script.

⚠️ Important: Never upload your service account key to public repositories.

2. 🐍 firebase_listener.py
This Python script runs on the Raspberry Pi and listens for updates in the Firebase Realtime Database. When specific changes occur (e.g., temperature or button press signals), it triggers actions like lighting an LED, activating a buzzer, or reading sensor values.

Sample Usage:

python3 firebase_listener.py
Make sure you have installed the required Python libraries:


pip3 install firebase-admin
You can expand it to interact with:

Sense HAT for temperature/humidity readings

PIR sensor for motion detection

Buzzer and LED for alerts

🧪 Testing
Use the gaze selection system to navigate the UI.

Trigger phrases and IoT events using eye gaze.

Monitor data updates in Firebase and device responses (e.g., LED on Raspberry Pi).


🧠 Project Purpose
This app is part of a final year project to address communication challenges for individuals with severe disabilities, such as motor neuron disease. It demonstrates the use of accessible, low-cost technologies for interaction and independence.


🙋‍♀️ Author
Gabriela Pansini
Technological University of the Shannon



