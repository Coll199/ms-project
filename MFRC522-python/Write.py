#!/usr/bin/env python
# -*- coding: utf8 -*-

import RPi.GPIO as GPIO
import MFRC522
import signal
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

# Fetch the service account key JSON file contents
cred = credentials.Certificate('/home/pi/Desktop/MFRC522-python/passengeranalytics.json')

# Initialize the app with a service account, granting admin privileges
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://passengeranalytics-5640e.firebaseio.com/'
})

# As an admin, the app has access to read and write all data, regradless of Security Rules
ref = db.reference('users')
#print(ref.get().get('x9xJmK3mb1cJTsTYeFudDdm1LMb2').get('username'))
snapshot = ref.get()
username = raw_input('Type the username you want to edit:')
def getUID(username):
    for key, val in snapshot.iteritems():
        if(val.get('username') == username):
            return key
    #print('uid:{0} username:{1} perm:{2}'.format(key, val.get('username'), val.get('permission')))

continue_reading = True

# Capture SIGINT for cleanup when the script is aborted
def end_read(signal,frame):
    global continue_reading
    print "Ctrl+C captured, ending read."
    continue_reading = False
    GPIO.cleanup()

# Hook the SIGINT
signal.signal(signal.SIGINT, end_read)

# Create an object of the class MFRC522
MIFAREReader = MFRC522.MFRC522()

# This loop keeps checking for chips. If one is near it will get the UID and authenticate
while continue_reading:
    
    # Scan for cards    
    (status,TagType) = MIFAREReader.MFRC522_Request(MIFAREReader.PICC_REQIDL)

    # If a card is found
    if status == MIFAREReader.MI_OK:
        print "Card detected"
    
    # Get the UID of the card
    (status,uid) = MIFAREReader.MFRC522_Anticoll()

    # If we have the UID, continue
    if status == MIFAREReader.MI_OK:

        # Print UID
        print "Card read UID: "+str(uid[0])+","+str(uid[1])+","+str(uid[2])+","+str(uid[3])
    
        # This is the default key for authentication
        key = [0xFF,0xFF,0xFF,0xFF,0xFF,0xFF]
        
        # Select the scanned tag
        MIFAREReader.MFRC522_SelectTag(uid)

        # Authenticate
        status = MIFAREReader.MFRC522_Auth(MIFAREReader.PICC_AUTHENT1A, 8, key, uid)
        print "\n"

        # Check if authenticated
        if status == MIFAREReader.MI_OK:
            userid = getUID(username)
            useridASCII = [ord(c) for c in userid]
            primusector = []
            aldoileasector = []
            for x in range(0,16):
                primusector.append(useridASCII[x])
            for x in range(16,len(useridASCII)):
                aldoileasector.append(useridASCII[x])
            for x in range(len(useridASCII),32):
                aldoileasector.append(0x00)
            MIFAREReader.MFRC522_Read(8)
            MIFAREReader.MFRC522_Read(9)
            
            #print "Sector 8 :"
            MIFAREReader.MFRC522_Write(8, primusector)
            #print "Sector 9 :"
            MIFAREReader.MFRC522_Write(9, aldoileasector)
            
            MIFAREReader.MFRC522_StopCrypto1()

            continue_reading = False
        else:
            print "Authentication error"
