# 📱 SmartSpeak  
**An Eye Gaze Communication App with IoT Integration**

SmartSpeak is an Android app that empowers individuals with **speech and motor impairments** to communicate using **eye gaze**. It also enables control of **smart home devices** via IoT integration, fostering greater independence.




https://github.com/user-attachments/assets/0f5828ac-05b6-48e9-99de-5ee50c0c0003


---

## 🚀 Features

- 👁️ **Eye Gaze Control** – Hands-free interaction using MediaPipe  
- 🔊 **Text-to-Speech (TTS)** – Converts selected phrases to spoken words  
- 💡 **Smart Home Integration** – Control lights and other devices  
- 📦 **Firebase Realtime Database** – Sync communication and IoT data  
- 📶 **Raspberry Pi IoT Simulation** – Trigger sensors and actuators  
- ⚙️ **Adaptive Gaze Sensitivity** – Custom feedback system for improved UX

---

## 🛠️ Technologies Used

- **Kotlin + Jetpack Compose** – Android app development  
- **MediaPipe** – Eye gaze detection via face landmarks  
- **Firebase** – Realtime Database backend  
- **Raspberry Pi 4** – IoT integration for devices  
- **Text-to-Speech API** – For audio phrase output

---

## 📂 How to Locate Source Code

<p>To find the core implementation files in your project, follow this folder path:</p>

<pre>
SmartSpeakTest2/
└── app/
    └── src/
        └── main/
            └── java/
                └── com/
                    └── example/
                        └── smartspeaktest/
                            ├── MainActivity.kt
                            ├── MainViewModel.kt
                            ├── FaceLandMarkHelper.kt
                            └── ui/
                                ├── EyeGazeSelectionScreen.kt
                                ├── ControlDevicesScreen.kt
                                ├── SmartSpeakMainMenu.kt
                                └── TutorialScreen.kt
</pre>

<h3>🧭 Navigation Guide (Step-by-Step)</h3>

<ol>
  <li>Open the project folder: <code>SmartSpeakTest2</code></li>
  <li>Go to <code>app/</code></li>
  <li>Then navigate to <code>src/</code></li>
  <li>Then open <code>main/</code></li>
  <li>Continue to <code>java/com/example/smartspeaktest/</code></li>
  <li>Inside, you'll find:
    <ul>
      <li><code>MainActivity.kt</code>, <code>MainViewModel.kt</code>, <code>FaceLandMarkHelper.kt</code></li>
      <li><code>ui/</code> folder with:
        <ul>
          <li><code>EyeGazeSelectionScreen.kt</code></li>
          <li><code>ControlDevicesScreen.kt</code></li>
          <li><code>SmartSpeakMainMenu.kt</code></li>
          <li><code>TutorialScreen.kt</code></li>
        </ul>
      </li>
    </ul>
  </li>
</ol>

<p><strong>💡 Tip:</strong> In <em>Android Studio</em>, press <kbd>Shift</kbd> + <kbd>Shift</kbd> to open "Search Everywhere" and type the file name directly (e.g., <code>EyeGazeSelectionScreen.kt</code>).</p>


# 📱 Getting Started (Android App)

### ✅ Prerequisites

- Android Studio installed  
- Physical Android device with a camera  
- Firebase project with Realtime Database enabled  

### 🔧 Setup Steps

1. Clone or download the repository  
2. Open the project folder `SmartSpeakTest2` in Android Studio  
3. Add your `google-services.json` file to the `app/` directory  
4. Enable **Realtime Database** in your Firebase Console  
5. Connect a real Android device and run the app

---

## 🌐 IoT Integration with Raspberry Pi

SmartSpeak uses Firebase to communicate with a Raspberry Pi running Python. This allows triggering hardware components like LEDs and buzzers.

### 📸 Raspberry Pi Setup


> ![IMG_9398](https://github.com/user-attachments/assets/c75d5e48-f26a-4e79-8ceb-ea58ccfbe073)


### 🔧 Steps to Run IoT Listener Script

1. Generate a Firebase service account key:
   - Go to **Firebase Console → Project Settings → Service Accounts**
   - Click **Generate New Private Key**
   - Save the key as `serviceAccountKey.json` in your Raspberry Pi script folder  

⚠️ **Important:** Do **not** upload `serviceAccountKey.json` to GitHub.

2. Install required Python libraries and run the script:


<p>pip3 install firebase-admin</p>
<p>python3 firebase_listener.py</p>


--- 
##  🧪 Testing the System
Use eye gaze to navigate the UI and select phrases

  - Eye selections trigger TTS feedback and Firebase updates
  
  - Observe Raspberry Pi components (e.g., LED, buzzer) respond in real-time
  
  - Monitor Realtime Database activity via Firebase Console

--- 
## 🎯 Project Purpose
SmartSpeak was created as part of a Final Year Project to help people with severe disabilities (such as motor neuron disease) communicate more independently using low-cost, accessible technology.

--- 
### 👩‍💻 Author
<p> Gabriela Pansini</p>
<p> Technological University of the Shannon</p>
<p> Final Year Project – 2025</p>

