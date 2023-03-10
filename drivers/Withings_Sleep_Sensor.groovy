/**
 *  Withings Sleep Sensor
 *
 *  Copyright 2020 Dominick Meglio
 *  Copyright 2022 @lnjustin
 *
 *  Licensed under the BSD 3-Clause License
 *  Change History:
 *  All Changes documented in Withings_User.groovy
 */

metadata {
    definition(name: "Withings Sleep Sensor", namespace: "lnjustin", author: "dmeglio@gmail.com, lnjustin") {
        capability "Sensor"
        capability "SleepSensor"
        capability "Switch"
        capability "PresenceSensor"
        
        attribute "wakeupDuration", "number"
        attribute "wakeupDurationDisplay", "string"
        attribute "lightSleepDuration", "number"
        attribute "lightSleepDurationDisplay", "string"
        attribute "deepSleepDuration", "number"
        attribute "deepSleepDurationDisplay", "string"
        attribute "wakeupCount", "number"
        attribute "durationToSleep", "number"
        attribute "durationToSleepDisplay", "number"
        attribute "remSleepDuration", "number"
        attribute "remSleepDurationDisplay", "string"
        attribute "durationToWakeup", "number"
        attribute "durationToWakeupDisplay", "string"
        attribute "heartRateAverage", "number"
        attribute "heartRateAverageQuality", "string"
        attribute "heartRateMin", "number"
        attribute "heartRateMax", "number"
        attribute "respirationRateAverage", "number"
        attribute "respirationRateMin", "number"
        attribute "respirationRateMax", "number"
        attribute "breathingDisturbancesIntensity", "number"
        attribute "snoring", "number"
        attribute "snoringDisplay", "string"
        attribute "snoringEpisodeCount", "number"
        attribute "sleepScore", "number"

        attribute "sleepQuality", "string"
        attribute "depthQuality", "string"
        attribute "durationQuality", "string"
        
        attribute "sleepEfficiency", "number"
        attribute "sleepLatency", "number"
        attribute "sleepLatencyQuality", "string"
        attribute "totalSleepDuration", "number"
        attribute "totalTimeInBed", "number"
        attribute "wakeupLatency", "number"
        attribute "wakeupLatencyQuality", "string"
        attribute "awakeAfterSleepDuration", "number"
        attribute "awakeAfterSleepQuality", "string"
        attribute "outOfBedCount", "number"
    }
}

def asleep() {
	on()
}

def awake() {
	off()
}

def on() {
    sendEvent(name: "sleeping", value: "sleeping")
    sendEvent(name: "switch", value: "on")
}

def off() {
    sendEvent(name: "sleeping", value: "not sleeping")
    sendEvent(name: "switch", value: "off")
}
