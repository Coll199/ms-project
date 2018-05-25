#!/usr/bin/env python
# -*- coding: utf8 -*-

import RPi.GPIO as GPIO
import MFRC522
import signal
import time
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
snapshot = ref.get()
log_ref = db.reference('log')
station_ref = db.reference('stations')

def getExpTime(uid):
    for key, val in snapshot.iteritems():
        if(key  ==  uid):
            return val.get('expirationTime')
    #print('uid:{0} username:{1} perm:{2}'.format(key, val.get('username'), val.get('permission')))
continue_reading = True

def getAboard(uid):
    for key, val in snapshot.iteritems():
       # print(val)
        if(key  ==  uid):
            return val.get('isAboard')

def getStation():
        return (station_ref.get().get('currentLocation'))#.get('currentLocation')
     #   return val.get('currentLocation')
    
def getUserName(uid):
    for key, val in snapshot.iteritems():
        if(key  ==  uid):
            return val.get('username')
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

print "Press Ctrl-C to stop."

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

        # Check if authenticated
        if status == MIFAREReader.MI_OK:
            primulsector = MIFAREReader.MFRC522_Read_Return(8)
            aldoileasector = MIFAREReader.MFRC522_Read_Return(9)
            primulsectorRev = [chr(c) for c in primulsector]
            aldoileasectorRev = [chr(c) for c in aldoileasector if c != 0]
            part1 = ''.join(primulsectorRev)
            part2 = ''.join(aldoileasectorRev)
            final = part1 + part2
            MIFAREReader.MFRC522_Read(8)
            MIFAREReader.MFRC522_Read(9)
            print "Expiration time:"+(str(getExpTime(final)))
            users_ref = ref.child(final)
            currentTime = int(time.time())
            print(getStation())
            if ( getExpTime(final) - currentTime > 0 ):
                print "Card is valid "
            else:
                print "Card is invalid"
            if ( getAboard(final) == False ):
                log_ref = log_ref.push()
                log_ref.set({
                    'username' : getUserName(final),
                    'time' : currentTime,
                    'currentLocation' : getStation(),
                    'gotAboard' : False
                    })
                users_ref.update({
                    'isAboard' : True
                    })
                print " HE GOT ON "
            elif( getAboard(final) == True ):
                log_ref = log_ref.push()
                log_ref.set({
                    'username' : getUserName(final),
                    'time' : currentTime,
                    'currentLocation' : getStation(),
                    'gotAboard' : True
                    })
                users_ref.update({
                    'isAboard' : False
                    })
                print " HE GOT OFF "
            MIFAREReader.MFRC522_StopCrypto1()
            ref = db.reference('users')
            snapshot = ref.get()
            station_ref = db.reference('stations')
            log_ref = db.reference('log')
            continue_reading = True
            
        else:
            print "Authentication error"
