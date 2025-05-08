import firebase_admin
from firebase_admin import credentials, db
import RPi.GPIO as GPIO
import time

# ------------------ GPIO Setup ------------------
GPIO.setmode(GPIO.BOARD)  # Use physical pin numbering

# LED setup
LED_PIN = 11  # Physical pin 11 (adjust if needed)
GPIO.setup(LED_PIN, GPIO.OUT)
GPIO.output(LED_PIN, GPIO.HIGH)  # LED off (assuming active low)

# Buzzer setup
BUZZER_PIN = 13  # Choose an appropriate pin for the buzzer
GPIO.setup(BUZZER_PIN, GPIO.OUT)
GPIO.output(BUZZER_PIN, GPIO.HIGH)  # Buzzer off (assuming active low)

# ------------------ Firebase Setup ------------------
# Replace with the actual path to your service account key JSON file
cred = credentials.Certificate('/home/gabipansini/Downloads/serviceAccountKey.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://smartspeak-d1d14-default-rtdb.europe-west1.firebasedatabase.app/'
})
# Separate references for the LED light and buzzer
light_ref = db.reference('light')
buzzer_ref = db.reference('buzzer')

def get_light_state():
    """Fetch the current state of the light from Firebase."""
    return light_ref.get()

def get_buzzer_state():
    """Fetch the current state of the buzzer from Firebase."""
    return buzzer_ref.get()

last_light_state = None
last_buzzer_state = None

try:
    while True:
        current_light_state = get_light_state()
        current_buzzer_state = get_buzzer_state()

        # Handle LED state
        if current_light_state != last_light_state:
            print(f"Light state changed to: {current_light_state}")
            if current_light_state == "on":
                # For an active-low LED, LOW turns it on.
                GPIO.output(LED_PIN, GPIO.LOW)
                print("Turning LED ON (steady)")
            elif current_light_state == "off":
                GPIO.output(LED_PIN, GPIO.HIGH)
                print("Turning LED OFF")
            elif current_light_state == "blink":
                print("Blinking LED...")
                # Continue blinking until the state changes
                while get_light_state() == "blink":
                    GPIO.output(LED_PIN, GPIO.LOW)  # LED ON
                    time.sleep(0.5)
                    GPIO.output(LED_PIN, GPIO.HIGH)  # LED OFF
                    time.sleep(0.5)
            else:
                print("Unknown light state received:", current_light_state)
            last_light_state = current_light_state

        # Handle buzzer state
        if current_buzzer_state != last_buzzer_state:
            print(f"Buzzer state changed to: {current_buzzer_state}")
            if current_buzzer_state == "on":
                # For an active-low buzzer, LOW turns it on.
                GPIO.output(BUZZER_PIN, GPIO.LOW)
                print("Turning buzzer ON")
            elif current_buzzer_state == "off":
                GPIO.output(BUZZER_PIN, GPIO.HIGH)
                print("Turning buzzer OFF")
            else:
                print("Unknown buzzer state received:", current_buzzer_state)
            last_buzzer_state = current_buzzer_state

        time.sleep(1)  # Poll every second
except KeyboardInterrupt:
    print("Exiting program...")
finally:
    GPIO.cleanup()
