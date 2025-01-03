/**
 *
 *  Withings User
 *
 *  Copyright 2020 Dominick Meglio
 *  Copyright 2022 @lnjustin
 *
 *  Licensed under the BSD 3-Clause License
 *
 *  Change History:
 * v1.7.4 - Bug fix
 *  v1.7.3 - Added virtual activity tracker
 *  v1.7.2 - Fixed device label
*  v1.7.1 - Updated namespace
 *  v1.7.0 - Added support for File Manager Device and data logging; Added option to restrict sleep data update to time window
 *  v1.6.0 - Released under BSD 3-Clause License
 */

 import groovy.transform.Field
 import groovy.json.JsonOutput 
 import java.security.MessageDigest
 
definition(
    name: "Withings User",
    namespace: "lnjustin",
    author: "Dominick Meglio, lnjustin",
    description: "Integrate Withings smart devices with Hubitat.",
    category: "My Apps",
	parent: "lnjustin:Withings Integration",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	documentationLink: "https://github.com/lnjustin/hubitat-withings/blob/master/README.md")

preferences {
    page(name: "prefMain")
	page(name: "prefOAuth")
	page(name: "prefDevices")
}

@Field static def measurementSystems = [
	imperial: 1,
	metric: 2,
	ukimperial: 3
]

@Field static def applids = [
	weight: 1,
	temperature: 2,
	heartrate: 4,
	activity: 16,
	sleep: 44,
	user: 46,
	bedIn: 50,
	bedOut: 51,
	inflateDone: 52
]

@Field def measures = [
	1: [attribute: "weight", displayAttribute: "weightDisplay", converter: this.&massConverter],
	4: [attribute: "height", displayAttribute: "heightDisplay", converter: this.&heightConverter],
	5: [attribute: "fatFreeMass", displayAttribute: "fatFreeMassDisplay", converter: this.&massConverter],
	6: [attribute: "fatRatio", converter: this.&fatRatioConverter],
	8: [attribute: "fatMassWeight", displayAttribute: "fatMassWeightDisplay", converter: this.&massConverter],
	9: [attribute: "diastolicBloodPressure", converter: this.&bloodPressureConverter],
	10: [attribute: "systolicBloodPressure", converter: this.&bloodPressureConverter],
	11: [attribute: "pulse", converter: this.&pulseConverter],
	12: [attribute: "temperature", converter: this.&temperatureConverter],
	54: [attribute: "oxygenSaturation", converter: this.&oxygenSaturationConverter],
	71: [attribute: "bodyTemperature", converter: this.&temperatureConverter],
	73: [attribute: "skinTemperature", converter: this.&temperatureConverter],
	76: [attribute: "muscleMass", displayAttribute: "muscleMassDisplay", converter: this.&massConverter],
	77: [attribute: "hydration", converter: this.&massConverter],
	88: [attribute: "boneMass", displayAttribute: "boneMassDisplay", converter: this.&massConverter],
	91: [attribute: "pulseWaveVelocity", converter: this.&pulseWaveVelocityConverter]
]

String.metaClass.toSHA1 = { salt = "" ->
   def messageDigest = MessageDigest.getInstance("SHA1")

   messageDigest.update(salt.getBytes())
   messageDigest.update(delegate.getBytes())

   /*
* Why pad up to 40 characters? Because SHA-1 has an output
* size of 160 bits. Each hexadecimal character is 4-bits.
* 160 / 4 = 40
*/
   new BigInteger(1, messageDigest.digest()).toString(16).padLeft(40, '0')
}

// Converters
def massConverter(weight, unit) {
	def metricValue = weight*(10**unit)

	if (parent.getMeasurementSystem() == measurementSystems.metric)
		return [value: metricValue, unit: "kg", displayValue: "${metricValue}kg"]
	else if (parent.getMeasurementSystem() == measurementSystems.imperial) {
		def lbs = (metricValue * 2.20462262)
		def oz = (int)(((lbs-(int)lbs)*16.0).round(0))
		return [value: lbs, unit: "lbs", displayValue: "${(int)lbs}lbs ${oz}oz"]
	}
	else if (parent.getMeasurementSystem() == measurementSystems.ukimperial) {
		def stones = (metricValue * 0.15747304)
		def lbs = (stones-(int)stones)*14
		def oz = (int)(((lbs-(int)lbs)*16.0).round(0))
		return [value: stones, unit: "st", displayValue: "${(int)stones}st ${(int)lbs}lbs ${oz}oz"]
	}
}

def heightConverter(height, unit) {
	def metricValue = weight*(10**unit)
	if (parent.getMeasurementSystem() == measurementSystems.metric)
		return [value: metricValue, unit: "m"]
	else {
		def ft = metricValue * 3.2808399
		def inches = (int)(((ft-(int)ft)*12.0).round(0))
		return [value: ft, unit: "ft", displayValue: "${(int)ft}ft ${inches}in"]
	}
}

def fatRatioConverter(ratio, unit) {
	return [value: ratio*(10**unit), unit: "%"]
}

def bloodPressureConverter(bp, unit) {
	return [value: bp*(10**unit), unit: "mmHg"]
}

def pulseConverter(pulse, unit) {
	return [value: pulse, unit: "bpm"]
}

def temperatureConverter(temp, unit) {
	if (parent.getMeasurementSystem() == measurementSystems.metric)
		return [value: temp*(10**unit), unit: "C"]
	else {
		return [value: ((double)celsiusToFahrenheit(temp*(10**unit))).round(1), unit: "F"]
	}
}

def oxygenSaturationConverter(o2sat, unit) {
	return [value: o2sat*(10**unit), unit: "%"]
}

def pulseWaveVelocityConverter(pulsewavevelocity, unit) {
	return [value: pulsewavevelocity*(10**unit), unit: "m/s"]
}

def durationConverter(duration) {
	def hours = (duration * 0.00027778)
	def minutes = (hours-(int)hours)*60.0
	def seconds = (int)((minutes-(int)minutes)*60.0)

	def durationStr = ""
	if (duration/3600 >= 1)
		durationStr += "${(int)hours}hours "
	durationStr += "${(int)minutes}minutes "
	if (seconds > 0)
		durationStr += "${(int)seconds}seconds"
	return durationStr
}

def prefMain() {
	return dynamicPage(name: "prefMain", title: "Withings Integration", nextPage: "prefOAuth", uninstall:false, install: false) {
		section {
			input "userName", "text", title: "The Withings Username associated with this app", required: true
		}
	}
}
def prefOAuth() {
	return dynamicPage(name: "prefOAuth", title: "Withings OAuth", nextPage: "prefDevices", uninstall:false, install: false) {
		section {	
			def desc = ""
			if (!state.authToken) {
				showHideNextButton(false)
				desc = "To continue you will need to connect your Withings and Hubitat accounts"
			}
			else {
				showHideNextButton(true)
				desc = "Your Hubitat and Withings accounts are connected"
			}
			href url: oauthInitialize(), style: "external", required: true, title: "Withings Account Authorization", description: desc
		}
	}
}

def prefDevices() {
	app.updateLabel("${userName}")
	state.devices = getWithingsDevices() 
	return dynamicPage(name: "prefDevices", title: "Withings Devices", uninstall:true, install: true) {
		section {
			if (state.devices?.scales?.size() > 0)
				input "scales", "enum", title: "Scales", options: state.devices.scales, multiple: true
			if (state.devices?.sleepMonitors?.size() > 0)
				input "sleepMonitors", "enum", title: "Sleep Monitors", options: state.devices.sleepMonitors, multiple: true, submitOnChange: true
			if (state.devices?.activityTrackers?.size() > 0)
				input "activityTrackers", "enum", title: "Activity Trackers", options: state.devices.activityTrackers, multiple: true
			input "useVirtualActivityTracker", "bool", title: "Create Virtual Activity Tracker?", description: "Create a virtual activity tracker with activity data to which you give Withings access from a non-Withings device", submitOnChange: true
			if (useVirtualActivityTracker == true) {
				def trackerString = 'WithingsVirtualActivityTracker'
				state.virtualActivityTracker = trackerString.toSHA1('salty')
			}
			else state.virtualActivityTracker = null
			if (state.devices?.bloodPressure?.size() > 0)
				input "bloodPressure", "enum", title: "Blood Pressure Monitors", options: state.devices.bloodPressure, multiple: true
			if (state.devices?.thermometers?.size() > 0)
				input "thermometers", "enum", title: "Thermometers", options: state.devices.thermometers, multiple: true
		}
		if (sleepMonitors?.size() > 0) {
			section {
				input "sleepSwitches", "capability.switch", title: "Switches to turn on when someone gets into bed and off when they get out", multiple: true
			}
		}
	}
}

mappings {
	path("/oauth/initialize") {
		action: [
			GET: "oauthInitialize"
		]
	}
	path("/callback") {
		action: [
			GET: "oauthCallback"
		]
	}
	path("/oauth/callback") {
		action: [
			GET: "oauthCallback"
		]
	}
	path("/notification/:type") {
		action: [
			GET: "withingsNotification",
			POST: "withingsNotification"
		]
	}
}

// OAuth Routines
def oauthInitialize() {
	def oauthInfo = parent.getOAuthDetails()
	
	if (state.accessToken == null)
		createAccessToken()

	state.oauthState = "${getHubUID()}/apps/${app.id}/callback?access_token=${state.accessToken}"
		
	return "https://account.withings.com/oauth2_user/authorize2?response_type=code&client_id=${oauthInfo.clientID}&scope=${URLEncoder.encode("user.info,user.activity,user.sleepevents,user.metrics")}&redirect_uri=${URLEncoder.encode("https://cloud.hubitat.com/oauth/stateredirect")}&state=${URLEncoder.encode(state.oauthState)}"
}

def oauthCallback() {
	if (params.state == state.oauthState) {
		def oauthInfo = parent.getOAuthDetails()
        try { 
            httpPost([
				uri: "https://wbsapi.withings.net",
				path: "/v2/oauth2",
				body: [
					"action": "requesttoken",
					"grant_type": "authorization_code",
					code: params.code,
					client_id : oauthInfo.clientID,
					client_secret: oauthInfo.clientSecret,
					redirect_uri: "https://cloud.hubitat.com/oauth/stateredirect"
				]
			]) { resp ->
    			if (resp && resp.data && resp.success && resp.data.body.refresh_token && resp.data.body.access_token) {
                    state.refreshToken = resp.data.body.refresh_token
                    state.authToken = resp.data.body.access_token
					def tokenExpiresIn = resp.data.body.expires_in ?: 60
               		state.authTokenExpires = now() + (tokenExpiresIn * 1000) - 60000
					state.userid = resp.data.body.userid
                }
            }
		} 
		catch (e) {
            log.error "OAuth error: ${e}"
        }
	} 
	else {
		log.error "OAuth state does not match, possible spoofing?"
	}
	if (state.authToken) 
		oauthSuccess()
	else
		oauthFailure()
}

def oauthSuccess() {
	render contentType: 'text/html', data: """
	<p>Your Withings Account is now connected to Hubitat</p>
	<p>Close this window to continue setup.</p>
	"""
}

def oauthFailure() {
	render contentType: 'text/html', data: """
		<p>The connection could not be established!</p>
		<p>Close this window to try again.</p>
	"""
}

def refreshToken() {
	def result = false
	try {
		def oauthInfo = parent.getOAuthDetails()
		def params = [
			uri: "https://wbsapi.withings.net",
			path: "/v2/oauth2",
			body: [
				"action": "requesttoken",
				grant_type: "refresh_token",
				client_id: oauthInfo.clientID,
				client_secret: oauthInfo.clientSecret,
				refresh_token: state.refreshToken
			]
		]
		httpPost(params) { resp -> 
			if (resp && resp.data && resp.success && resp.data.body.refresh_token && resp.data.body.access_token) {
				state.refreshToken = resp.data.body.refresh_token
                state.authToken = resp.data.body.access_token
				logDebug("refreshToken response body: ${resp.data.body}")
				def tokenExpiresIn = resp.data.body.expires_in ?: 60
                state.authTokenExpires = now() + (tokenExpiresIn * 1000) - 60000
				result = true
			}
			else {
				state.authToken = null
				result = false
			}
		}
	}
	catch (e) {
		log.error "Failed to refresh token: ${e}"
		state.authToken = null
		result = false
	}
	return result
}

// Business logic
def getWithingsDevices() {
	def scales = [:]
	def sleepMonitors = [:]
	def activityTrackers = [:]
	def bloodPressure = [:]
	def thermometers = [:]
	def body = apiGet("v2/user", "getdevice")
	// log.debug "Withings Devices: ${body}"
	for (device in body.devices) {
		if (device.type == "Scale")
			scales[device.deviceid] = device.model
		else if (device.type == "Sleep Monitor")
			sleepMonitors[device.deviceid] = device.model
		else if (device.type == "Activity Tracker")
			activityTrackers[device.deviceid] = device.model ?: "Unnamed Activity Tracker"
		else if (device.type == "Blood Pressure Monitor")
			bloodPressure[device.deviceid] = device.model
		else if (device.type == "Smart Connected Thermometer")
			thermometers[device.deviceid] = device.model
	}
	return [scales: scales, sleepMonitors: sleepMonitors, activityTrackers: activityTrackers, bloodPressure: bloodPressure, thermometers: thermometers]
}

def unsubscribeWithingsNotifications() {
	def subs = apiGet("notify", "list")?.profiles

	for (sub in subs) {
		apiGet("notify", "revoke", [callbackurl: sub.callbackurl, appli: sub.appli])
	}
}

def updateSubscriptions() {
	unsubscribeWithingsNotifications()
	
	if (scales?.size() > 0) {
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("weight"), appli: applids.weight])
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("heartrate"), appli: applids.heartrate])
	}
	if (activityTrackers?.size() > 0 || state.virtualActivityTracker != null) {
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("activity"), appli: applids.activity])
	}
	if (bloodPressure?.size() > 0) {
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("heartrate"), appli: applids.heartrate])
	}
	if (sleepMonitors?.size() > 0) {
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("sleep"), appli: applids.sleep])
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("bedIn"), appli: applids.bedIn])
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("bedOut"), appli: applids.bedOut])
	}
	if (thermometers?.size() > 0) {
		apiGet("notify", "subscribe", [callbackurl: callbackUrl("temperature"), appli: applids.temperature])
	}
}

def callbackUrl(type) {
	// This looks insecure but it's really not. The Withings API apparently requires HTTP (what???)
	// But, on the HE side HSTS is supported so it redirects right to HTTPS.
	return "${getFullApiServerUrl()}/notification/${type}?access_token=${state.accessToken}".replace("https://", "http://")
}

def withingsNotification() {
	logDebug "Notification Received: ${params}"
	// Withings requires that we respond within 2 seconds with a success message. So do this in the background so we
	// can return immediately.

	runInMillis(1,asyncWithingsNotificationHandler,[data:params, overwrite:false])
}

def asyncWithingsNotificationHandler(params) {
	switch (params.type) {
		case "weight":
			if (params.startdate != null)
				processWeight(params.startdate, params.enddate)
			break
		case "heartrate":
			if (params.startdate != null)
				processHeartrate(params.startdate, params.enddate)
			break
		case "activity":
			if (params.date != null)
				processActivity(params.date)
			break
		case "bedIn":
			if (params.deviceid != null)
				processBedPresence(true, params.deviceid)
			break
		case "bedOut":
			if (params.deviceid != null)
				processBedPresence(false, params.deviceid)
			break
		case "sleep":
			if (params.startdate != null) {
				def sleepUpdateRestriction = parent.getSleepDataUpdateRestriction()
				if (sleepUpdateRestriction && sleepUpdateRestriction.isRestricted && sleepUpdateRestriction.isRestricted == true) {
					def windowStart = toDateTime(sleepUpdateRestriction.windowStart)
					def windowEnd = toDateTime(sleepUpdateRestriction.windowEnd)
					if (timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)) {
						processSleep(params.startdate, params.enddate)
					}
					else logDebug("Sleep data received from Withings, but not updated in Hubitat because outside of sleep data update window.H")
				}
				else processSleep(params.startdate, params.enddate)
			}
			break
		case "temperature":
			if (params.startdate != null)
				processTemperature(params.startdate, params.enddate)
			break
	}
}

def processActivity(date) {
	def data = apiGet("v2/measure", "getactivity", [startdateymd: date, enddateymd: date, data_fields: "steps,distance,elevation,soft,moderate,intense,active,calories,totalcalories,hr_average,hr_min,hr_max,hr_zone_0,hr_zone_1,hr_zone_2,hr_zone_3"])?.activities

	for (item in data) {
		def dev = null

		if (item.deviceid != null)
			dev = getChildDevice(buildDNI(item.deviceid))
		else if (state.virtualActivityTracker != null) {
			dev = getChildDevice(buildDNI(state.virtualActivityTracker))
		}
		else if (item.is_tracker)
			dev = getChildByCapability("StepSensor")

		if (!dev)
			continue

		dev.sendEvent(name: "steps", value: item.steps, isStateChange: true)
		dev.sendEvent(name: "distance", value: item.distance, isStateChange: true)
		dev.sendEvent(name: "elevation", value: item.elevation, isStateChange: true)
		dev.sendEvent(name: "soft", value: item.soft, isStateChange: true)
		dev.sendEvent(name: "moderate", value: item.moderate, isStateChange: true)
		dev.sendEvent(name: "intense", value: item.intense, isStateChange: true)
		dev.sendEvent(name: "active", value: item.active, isStateChange: true)
		dev.sendEvent(name: "calories", value: item.calories, isStateChange: true)
		dev.sendEvent(name: "totalCalories", value: item.totalcalories, isStateChange: true)
		dev.sendEvent(name: "heartRateAverage", value: item.hr_average, isStateChange: true)
		dev.sendEvent(name: "heartRateMin", value: item.hr_min, isStateChange: true)
		dev.sendEvent(name: "heartRateMax", value: item.hr_max, isStateChange: true)
		dev.sendEvent(name: "heartRateZone0", value: item.hr_zone_0, isStateChange: true)
		dev.sendEvent(name: "heartRateZone1", value: item.hr_zone_1, isStateChange: true)
		dev.sendEvent(name: "heartRateZone2", value: item.hr_zone_2, isStateChange: true)
		dev.sendEvent(name: "heartRateZone3", value: item.hr_zone_3, isStateChange: true)
	}
}

def processBedPresence(inBed, deviceID) {
	def dev = getChildDevice(buildDNI(deviceID))

	if (!dev)
		return

	if (sleepSwitches?.size() > 0) {
		if (inBed)
			sleepSwitches*.on()
		else
			sleepSwitches*.off()
	}

	dev.sendEvent(name: "presence", value: inBed ? "present" : "not present")
}

def processWeight(startDate, endDate) {
	def data = apiGet("measure", "getmeas", [startdate: startDate, enddate: endDate, category: 1])?.measuregrps

	if (!data)
		return

	data = data.sort {it -> it.date}
	for (group in data) {
		def dev = getChildDevice(buildDNI(group.deviceid))
		// A device that the user didn't import
		if (!dev)
			continue

		// Heart related measurements
		sendEventsForMeasurements(dev, group.measures, [1,5,6,8,76,77,88,91])
	}
}

def processHeartrate(startDate, endDate) {
	def data = apiGet("measure", "getmeas", [startdate: startDate, enddate: endDate, category: 1])?.measuregrps

	if (!data)
		return

	data = data.sort {it -> it.date}
	for (group in data) {
		def dev = getChildDevice(buildDNI(group.deviceid))
		// A device that the user didn't import
		if (!dev)
			continue

		// Heart related measurements
		sendEventsForMeasurements(dev, group.measures, [9,10,11])
	}
}

def processSleep(startDate, endDate) {
	def startYMD = (new Date((long)startDate.toLong()*1000)).format("YYYY-MM-dd")
	def endYMD = (new Date((long)endDate.toLong()*1000)).format("YYYY-MM-dd")

	def data = apiGet("v2/sleep", "getsummary", [startdateymd: startYMD, enddateymd: endYMD, data_fields: "breathing_disturbances_intensity,deepsleepduration,durationtosleep,durationtowakeup,hr_average,hr_max,hr_min,lightsleepduration,remsleepduration,rr_average,rr_max,rr_min,sleep_score,snoring,snoringepisodecount,wakeupcount,wakeupduration,sleep_efficiency,sleep_latency,total_sleep_time,total_timeinbed,wakeup_latency,waso,out_of_bed_count"])?.series
    
    if (!data) {
        logDebug("Returning prematurely from processSleep since no data retrieved")
		return
    }

	 if (startDate.toLong() == state.lastSleepStart && endDate.toLong() == state.lastSleepEnd)
		return

	def item = data.last()
	def sleepData = item.data
	def dev = null

	// Sleep tracker
	if (item.model == 32) {
		dev = getChildByCapability("PresenceSensor")
		// These are not available from an activity tracker.
		dev.sendEvent(name: "remSleepDuration", value: sleepData.remsleepduration, isStateChange: true)
		dev.sendEvent(name: "remSleepDurationDisplay", value: durationConverter(sleepData.remsleepduration), isStateChange: true)
	}
	// Activity monitor
	else if (item.model == 16) {
		dev = getChildByCapability("StepSensor")
	}
    
	dev.sendEvent(name: "wakeupDuration", value: sleepData.wakeupduration, isStateChange: true)
	dev.sendEvent(name: "wakeupDurationDisplay", value: durationConverter(sleepData.wakeupduration), isStateChange: true)	
	dev.sendEvent(name: "lightSleepDuration", value: sleepData.lightsleepduration, isStateChange: true)
	dev.sendEvent(name: "lightSleepDurationDisplay", value: durationConverter(sleepData.lightsleepduration), isStateChange: true)
	dev.sendEvent(name: "deepSleepDuration", value: sleepData.deepsleepduration, isStateChange: true)
	dev.sendEvent(name: "deepSleepDurationDisplay", value: durationConverter(sleepData.deepsleepduration), isStateChange: true)
	dev.sendEvent(name: "wakeupCount", value: sleepData.wakeupcount, isStateChange: true)
	dev.sendEvent(name: "durationToSleep", value: sleepData.durationtosleep, isStateChange: true)
	dev.sendEvent(name: "durationToSleepDisplay", value: durationConverter(sleepData.durationtosleep), isStateChange: true)
	dev.sendEvent(name: "durationToWakeup", value: sleepData.durationtowakeup, isStateChange: true)
	dev.sendEvent(name: "durationToWakeupDisplay", value: durationConverter(sleepData.durationtowakeup), isStateChange: true)
	dev.sendEvent(name: "heartRateAverage", value: sleepData.hr_average, isStateChange: true)
	dev.sendEvent(name: "heartRateMin", value: sleepData.hr_min, isStateChange: true)
	dev.sendEvent(name: "heartRateMax", value: sleepData.hr_max, isStateChange: true)
	dev.sendEvent(name: "respirationRateAverage", value: sleepData.rr_average, isStateChange: true)
	dev.sendEvent(name: "respirationRateMin", value: sleepData.rr_min, isStateChange: true)
	dev.sendEvent(name: "respirationRateMax", value: sleepData.rr_max, isStateChange: true)
	dev.sendEvent(name: "breathingDisturbancesIntensity", value: sleepData.breathing_disturbances_intensity, isStateChange: true)
	dev.sendEvent(name: "snoring", value: sleepData.snoring, isStateChange: true)
	dev.sendEvent(name: "snoringDisplay", value: durationConverter(sleepData.snoring ?: 0), isStateChange: true)
	dev.sendEvent(name: "snoringEpisodeCount", value: sleepData.snoringepisodecount, isStateChange: true)
	dev.sendEvent(name: "sleepScore", value: sleepData.sleep_score, isStateChange: true)
    dev.sendEvent(name: "sleepEfficiency", value: sleepData.sleep_efficiency, isStateChange: true)
    dev.sendEvent(name: "sleepLatency", value: sleepData.sleep_latency, isStateChange: true)
    dev.sendEvent(name: "totalSleepDuration", value: sleepData.total_sleep_time, isStateChange: true)
    dev.sendEvent(name: "wakeupLatency", value: sleepData.wakeup_latency, isStateChange: true)
    dev.sendEvent(name: "awakeAfterSleepDuration", value: sleepData.waso, isStateChange: true)
    dev.sendEvent(name: "outOfBedCount", value: sleepData.out_of_bed_count, isStateChange: true)

	if (sleepData.sleep_score < 50)
		dev.sendEvent(name: "sleepQuality", value: "Restless", isStateChange: true)
	else if (sleepData.sleep_score < 75)
		dev.sendEvent(name: "sleepQuality", value: "Average", isStateChange: true)
	else
		dev.sendEvent(name: "sleepQuality", value: "Restful", isStateChange: true)

	def totalSleepTime = sleepData.lightsleepduration + sleepData.deepsleepduration + (sleepData.remsleepduration ?: 0)
	if (totalSleepTime < 21600)
		dev.sendEvent(name: "durationQuality", value: "Bad", isStateChange: true)
	else if (totalSleepTime < 25200)
		dev.sendEvent(name: "durationQuality", value: "Average", isStateChange: true)
	else
		dev.sendEvent(name: "durationQuality", value: "Good", isStateChange: true)

	def deepRemTime = sleepData.deepsleepduration + (sleepData.remsleepduration ?: 0)
	def deepRemPct = (deepRemTime*1.0)/(totalSleepTime*1.0)
	if (deepRemPct < 0.34)
		dev.sendEvent(name: "depthQuality", value: "Bad", isStateChange: true)
	else if (deepRemPct < 0.45)
		dev.sendEvent(name: "depthQuality", value: "Average", isStateChange: true)
	else
		dev.sendEvent(name: "depthQuality", value: "Good", isStateChange: true)

	if (sleepData.waso < 1080)
		dev.sendEvent(name: "awakeAfterSleepQuality", value: "Good", isStateChange: true)
	else if (sleepData.waso < 3300)
		dev.sendEvent(name: "awakeAfterSleepQuality", value: "Average", isStateChange: true)
	else if (sleepData.waso >= 3300)
    	dev.sendEvent(name: "awakeAfterSleepQuality", value: "Bad", isStateChange: true)

	if (sleepData.sleep_latency <= 1200)
		dev.sendEvent(name: "sleepLatencyQuality", value: "Good", isStateChange: true)
	else
		dev.sendEvent(name: "sleepLatencyQuality", value: "Not Good", isStateChange: true)

	if (sleepData.wakeup_latency <= 360)
		dev.sendEvent(name: "wakeupLatencyQuality", value: "Good", isStateChange: true)
	else
		dev.sendEvent(name: "wakeupLatencyQuality", value: "Not Good", isStateChange: true)
	
	if (sleepData.hr_average < 55)
		dev.sendEvent(name: "heartRateAverageQuality", value: "Optimal", isStateChange: true)
	else if (sleepData.hr_average < 75)
		dev.sendEvent(name: "heartRateAverageQuality", value: "Normal", isStateChange: true)
	else if (sleepData.hr_average >= 75)
		dev.sendEvent(name: "heartRateAverageQuality", value: "Not Optimal", isStateChange: true)
	

    if (parent.logToFile == true) {
        def typesToLog = parent.getDataTypesToLogToFile("Sleep")
        if ( typesToLog.any { it.contains("Sleep") } ) {
			def params = [startdate: startDate.toLong(), enddate: endDate.toLong()]
            typesToLog.removeElement("Sleep State") // sleep state always retrieved
			if (typeToLogs != [] && typesToLogs != null) {
				// data fields other than Sleep State must be included in the data_fields param as comma separated list
				typesToLog = typesToLog.collect { mapDataTypeToParam[it] }
            	typeString = typesToLog.join(",")
				params["data_fields"] = typeString
			}
            def detailedData = apiGet("v2/sleep", "get", params)
            if (detailedData) parent.writeToFile(detailedData.series, "Sleep", dev.getId(), dev.getName())
        }
        else logDebug("Logging to file enabled, but not for Sleep category")
    }
    
	state.lastSleepStart = startDate.toLong()
	state.lastSleepEnd = endDate.toLong()
}

def processTemperature(startDate, endDate) {
	def data = apiGet("measure", "getmeas", [startdate: startDate, enddate: endDate, category: 1])?.measuregrps

	if (!data)
		return

	data = data.sort {it -> it.date}
	for (group in data) {
		def dev = getChildDevice(buildDNI(group.deviceid))
		// A device that the user didn't import
		if (!dev)
			continue

		// Temperature related measurements
		sendEventsForMeasurements(dev, group.measures, [12,71,73])
	}
}

def sendEventsForMeasurements(dev, measurements, types) {
	for (measurement in measurements) {
		if (types.contains(measurement.type)) {
			def attrib = measures[measurement.type].attribute
			def displayAttrib = measures[measurement.type].displayAttribute
			def result = measures[measurement.type].converter.call(measurement.value, measurement.unit)
			dev.sendEvent(name: attrib, value: result.value, unit: result.unit, isStateChange: true)
			if (displayAttrib != null)
				dev.sendEvent(name: displayAttrib, value: result.displayValue, isStateChange: true)
		}
	}
}

def getChildByCapability(capability) {
	def childDevices = getChildDevices()
	for (dev in childDevices) {
		if (dev.hasCapability(capability))
			return dev
	}
	return null
}
// API call methods

def apiGet(endpoint, action, query = null) {
	logDebug "${endpoint}?action=${action} -- ${query}"
	if (state.authTokenExpires <= now()) {
		if (!refreshToken())
			return null
	}
	def result = null
	try {
		def params = [
			uri: "https://wbsapi.withings.net",
			path: "/${endpoint}",
			query: [
				action: action
			],
			contentType: "application/json",
			headers: [
				"Authorization": "Bearer " + state.authToken
			]
		]
		if (query != null)
			params.query << query
		httpGet(params) { resp ->
            if (resp.data.status == 0) {
				result = resp.data.body
            }
			else if (resp.data.status == 401) {
				refreshToken()
			}
		}
	}
	catch (e) {
		log.error "Error getting API data ${endpoint}?action=${action}: ${e}"
		result = null
	}
	return result
}

def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	unsubscribeWithingsNotifications()
	removeChildDevices(getChildDevices())
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
	cleanupChildDevices()
	createChildDevices()
	updateSubscriptions()
	schedule("0 */30 * * * ? *", refreshDevices)
}

def buildDNI(deviceid) {
	return "withings:${state.userid}:${deviceid}"
}

def createChildDevices() {
	for (scale in scales)
	{
		if (!getChildDevice(buildDNI(scale)))
            addChildDevice("lnjustin", "Withings Scale", buildDNI(scale), ["name": "${userName} ${state.devices.scales[scale]}", "label": "${userName} ${state.devices.scales[scale]}", isComponent: false])
	}
	for (sleepMonitor in sleepMonitors)
	{
		if (!getChildDevice(buildDNI(sleepMonitor)))
            addChildDevice("lnjustin", "Withings Sleep Sensor", buildDNI(sleepMonitor), ["name": "${userName} ${state.devices.sleepMonitors[sleepMonitor]}", "label": "${userName} ${state.devices.sleepMonitors[sleepMonitor]}", isComponent: false])
	}
	for (activityTracker in activityTrackers)
	{
		if (!getChildDevice(buildDNI(activityTracker)))
            addChildDevice("lnjustin", "Withings Activity Tracker", buildDNI(activityTracker), ["name": "${userName} ${state.devices.activityTrackers[activityTracker]}", "label": "${userName} ${state.devices.activityTrackers[activityTracker]}", isComponent: false])
	}
	if (state.virtualActivityTracker != null) {
		if (!getChildDevice(buildDNI(state.virtualActivityTracker)))
            addChildDevice("lnjustin", "Withings Activity Tracker", buildDNI(state.virtualActivityTracker), ["name": "${userName} Virtual Activity Tracker", "label": "${userName} Virtual Activity Tracker", isComponent: false])
	}
	for (bp in bloodPressure)
	{
		if (!getChildDevice(buildDNI(bp)))
            addChildDevice("lnjustin", "Withings Blood Pressure Monitor", buildDNI(bp), ["name": "${userName} ${state.devices.bloodPressure[bp]}", "label": "${userName} ${state.devices.bloodPressure[bp]}", isComponent: false])
	}
	for (thermometer in thermometers)
	{
		if (!getChildDevice(buildDNI(thermometer)))
            addChildDevice("lnjustin", "Withings Thermometer", buildDNI(thermometer), ["name": "${userName} ${state.devices.thermometers[thermometer]}", "label": "${userName} ${state.devices.thermometers[thermometer]}", isComponent: false])
	}
}

def cleanupChildDevices()
{
	for (device in getChildDevices())
	{
		def deviceId = device.deviceNetworkId.replace("withings:","")
		def allDevices = (scales ?: []) + (sleepMonitors ?: []) + (activityTrackers ?: []) + (bloodPressure ?: []) + (thermometers ?: [])
		def deviceFound = false
		for (dev in allDevices)
		{
			if (state.userid + ":" + dev == deviceId)
			{
				deviceFound = true
				break
			}
		}
		if (state.virtualActivityTracker != null) {
			if (state.userid + ":" + state.virtualActivityTracker == deviceId)
			{
				deviceFound = true
				break
			}			
		}

		if (deviceFound == true)
			continue
			
		deleteChildDevice(device.deviceNetworkId)
	}
}

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

def refreshDevices() {
	def body = apiGet("v2/user", "getdevice")
	for (device in body?.devices) {
		def dev = getChildDevice(buildDNI(device.deviceid))
		if (dev != null) {
			if (device.type != "Sleep Monitor") {
				def intBattery = 30
				if (device.battery == "high")
					intBattery = 80
				else if (device.battery == "medium")
					intBattery = 50
				else if (device.battery == "low")
					intBattery = 20
				dev.sendEvent(name: "battery", value: intBattery)
			}
		}
	}
}

def showHideNextButton(show) {
	if(show) paragraph "<script>\$('button[name=\"_action_next\"]').show()</script>"  
	else paragraph "<script>\$('button[name=\"_action_next\"]').hide()</script>"
}

def logDebug(msg) {
    if (parent.getDebugLogging()) {
		log.debug msg
	}
}

@Field static mapDataTypeToParam = [
	'Sleep Snoring': 'snoring',
	'Sleep Heart Rate': 'hr',
    'Sleep Respiratory Rate': 'rr',
]
