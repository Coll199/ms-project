import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import time


# Fetch the service account key JSON file contents
cred = credentials.Certificate('/home/pi/Desktop/MFRC522-python/passengeranalytics.json')

# Initialize the app with a service account, granting admin privileges
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://passengeranalytics-5640e.firebaseio.com/'
})

ref = db.reference('stations')
while(True):
    for x in range(1,10):
        ref.set({
            'currentLocation' : x
            })
        time.sleep(60)