👁️ SmartSpeak
SmartSpeak is an Android application designed to help individuals with speech and motor impairments communicate and control smart devices like lights using eye gaze detection. Powered by MediaPipe, the app enables hands-free interaction via text-to-speech, smart home control, and video tutorials for enhanced accessibility.

✨ Features
👁️ Eye gaze-based interface for hands-free interaction

🗣️ Text-to-Speech output for real-time communication

💡 Control smart lights (on/off) via Firebase Realtime Database + Raspberry Pi

🎥 Onboarding video tutorials with automatic screen transitions

🧠 Powered by MediaPipe Face Landmarker for real-time eye gaze detection

🚀 How to Open & Run This Project
✅ Prerequisites
Android Studio (latest version)

Kotlin & Jetpack Compose support

Physical Android device (for camera input)

Firebase project with Realtime Database enabled

Raspberry Pi with GPIO access and internet

🛠️ Setup Steps
1. Clone the Repository
  
   git clone https://github.com/your-username/smartspeak.git
   cd smartspeak

2. Open in Android Studio
   Open Android Studio

Select “Open an existing project”

Navigate to the cloned smartspeak folder

Let Gradle finish syncing



3. Connect Your Firebase Project
   Go to Firebase Console

Create a new project (or use an existing one)

Enable Realtime Database in test mode

Copy your database URL (e.g., https://your-project-id.firebaseio.com/)



4. Add Firebase to Your App
   In Firebase, click Add App → Android

Use your app’s package name (e.g., com.example.smartspeaktest)

Download the google-services.json file

Place it in your project:


/app/google-services.json
Update the database URL in MainActivity.kt:


val database = Firebase.database("https://your-project-id.firebaseio.com/")


5. Run the App
   Plug in your physical Android device

Click Run ▶️ in Android Studio

💡 Raspberry Pi Integration (Smart Light Control)
Control an LED light through eye gaze and Firebase updates using a Raspberry Pi.

🔌 Wiring the LED
Connect the LED to GPIO pin 18 (or your chosen GPIO pin) with a resistor

📦 Install Firebase Admin SDK

pip install firebase-admin
🐍 Python Script for LED Control
python

import firebase_admin
from firebase_admin import credentials, db
import RPi.GPIO as GPIO
import time

# Setup GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(18, GPIO.OUT)

# Initialize Firebase
cred = credentials.Certificate("serviceAccountKey.json")  # Download from Firebase Console
firebase_admin.initialize_app(cred, {
'databaseURL': 'https://your-project-id.firebaseio.com/'
})

# Listen for changes
def listener(event):
print("Data changed:", event.data)
if event.data == "on":
GPIO.output(18, GPIO.HIGH)
elif event.data == "off":
GPIO.output(18, GPIO.LOW)

ref = db.reference("light")
ref.listen(listener)
▶️ Run the Script on Raspberry Pi

python3 your_script.py
Now, when a user looks left/right in the app and triggers a command, the Pi will turn the light on or off using the Firebase light node.

📁 Project Structure

smartspeak/
├── app/
│   ├── ui/                   # Composable screens (MainMenu, ControlDevices, etc.)
│   ├── looktospeakimp/      # MediaPipe + eye gaze logic
│   └── MainActivity.kt      # Navigation + Firebase setup
├── google-services.json     # Firebase config (NOT committed)
└── README.md                # Project documentation


📜 License
License © 2025 Gabriela Pansini

