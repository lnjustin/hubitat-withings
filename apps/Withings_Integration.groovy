/**
 *
 *  Withings Integration
 *
 *  Copyright 2020 Dominick Meglio
 *  Copyright 2022 @lnjustin
 *
 *  Licensed under the BSD 3-Clause License
 *
 *  Change History:
 *  All Changes documented in Withings_User.groovy
 */
 
import groovy.transform.Field

definition(
    name: "Withings Integration",
    namespace: "lnjustin",
    author: "Dominick Meglio, lnjustin",
    description: "Integrate various Withings into Hubitat.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	documentationLink: "https://github.com/dcmeglio/hubitat-withings/blob/master/README.md")

preferences {
     page(name: "mainPage", title: "", install: true, uninstall: true)
} 

def installed() {
    app.updateSetting("showInstructions", false)
}

def updated() {
    app.updateSetting("showInstructions", false)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	isInstalled()
		
            // Use the temperature scale to guess the default value for the measurement system
            def defaultMeasurementSystem = 2
            if (getTemperatureScale() == "F")
                defaultMeasurementSystem = 1
			section("API Access"){
                paragraph "To connect to the Withings API you will need to obtain Client Id and Consumer Secret from Withings."
                paragraph "Check the box below to view instructions on how to obtain API access."
				input "showInstructions", "bool", title: "Show API Instructions", submitOnChange: true

                if (showInstructions) {
                    paragraph """<ul><li>Go to <a href="https://account.withings.com/partner/add_oauth2" target="_blank">https://account.withings.com/partner/add_oauth2</a></li>
                    <li>Enter a Description, Contact Email, and Company name</li>
                    <li>Enter <b>https://cloud.hubitat.com/oauth/stateredirect</b> for the Callback URL</li>
                    <li>Choose <b>Prod</b> for the environment</li>
                    <li>For the logo you can use <b>https://github.com/dcmeglio/hubitat-withings/raw/master/hubitat-logo.PNG</b></li>
                    <li>Copy both the <b>Client Id</b> and <b>Consumer Secret</b> into the boxes below</li></ul>"""
                }
			}
			section("General") {
                input "clientID", "text", title: "API Client ID", required: true
			    input "clientSecret", "text", title: "API Client Secret", required: true
			    input "measurementSystem", "enum", title: "Measurement System", options: [1: "Imperial", 2: "Metric", 3: "Imperial (UK)"], required: true, defaultValue: defaultMeasurementSystem
			    input "logToFile", "bool", title: "Enable logging of specified data types to local file?", defaultValue: false, submitOnChange: true
                paragraph "For each selected data type and Withings device, local file logging will log (only) the latest pull of data to a file specific to that data type and Withings device."
                if (logToFile) {
                    input "fileManager", "device.FileManagerDevice", title: "Select File Manager Device", required: true
			        input "dataTypesToLogToFile", "enum", title: "Select Data Type(s) to Log to File", options: ["Sleep State", "Sleep Snoring", "Sleep Heart Rate", "Sleep Respiratory Rate"], multiple: true, required: true   
                    input "logFormat", "enum", title: "Select Log Format", options: ["Withings Format", "Quick Chart Format"], multiple: false, required: true   
               }
               input "restrictSleepUpdates", "bool", title: "Restrict Sleep Data Updates to Time Window?", width: 12, submitOnChange: true
               if (restrictSleepUpdates == true) {
                    input "sleepUpdateWindowStart", "time", title: "Start of Window During Which to Update Sleep Data", required: true
                    input "sleepUpdateWindowEnd", "time", title: "End of Window During Which to Update Sleep Data", required: true
               }
                input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true
       			label title: "Enter a name for parent app (optional)", required: false
 			}
        if(state.appInstalled == 'COMPLETE'){
            section("Withings Users") {
				app(name: "withingsUsers", appName: "Withings User", namespace: "dcm.withings", title: "Add a new Withings User", multiple: true)
			}
			displayFooter()
		}
	}
}

def isInstalled() {
	state.appInstalled = app.getInstallationState() 
	if (state.appInstalled != 'COMPLETE') {
		section
		{
			paragraph "Please click <b>Done</b> to install the parent app. Afterwards reopen the app to add your Withings Users."
		}
  	}
}

def getOAuthDetails() {
    return [clientID: clientID, clientSecret: clientSecret]
}

def getMeasurementSystem() {
    return measurementSystem.toInteger()
}

def getSleepDataUpdateRestriction() {
    return [isRestricted: restrictSleepUpdates, windowStart: sleepUpdateWindowStart, windowEnd: sleepUpdateWindowEnd]
}

def writeToFile(data, category, deviceId, deviceName) {
    if (fileManager != null) {        
        if (logFormat == "Withings Format") {
            if (category == "Sleep") {
                fileManager.writeFile("WithingsSleepDataForDevice${deviceId}.txt", data.toString())
            }            
        }
        else if (logFormat == "Quick Chart Format") {
            
            if (getDebugLogging()) {
                // check quick chart formatting against raw data
                fileManager.writeFile("WithingsSleepDataForDevice${deviceId}.txt", data.toString())    
            }
            
            def quickChartDateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
            if (category == "Sleep") {
                def typesToLog = getDataTypesToLogToFile("Sleep")
                typesToLog.each { typeToLog ->
                    def param = mapDataTypeToParam[typeToLog]
                    def parsedData = []
                    if (typeToLog == "Sleep State") {
                        def rawData = []
                        data.each { datapoint ->
                            def startDate = datapoint.startdate.toLong()*1000
                            def endDate = datapoint.enddate.toLong()*1000
                            def value = mapEncodedSleepStateToString[datapoint.state]
                            rawData << [startDate: startDate, endDate: endDate, value: value]   
                        }
                        rawData.sort { a, b -> a.startDate <=> b.startDate }    
                        
                        // reduce dataset to simplest form without extra intervals
                        def workingData = []
                        workingData << [date: rawData[0].startDate, value: rawData[0].value]
                        def previous = rawData[0]
                        for (int i = 1; i < rawData.size(); i++) {
                            def current = rawData[i]
                            if (current.value != previous.value) workingData << [date: current.startDate, value: current.value]
                            if (i == rawData.size() - 1) workingData << [date: current.endDate, value: current.value] // define end of dataset as last datapoint
                            if (current.value != previous.value) previous = current
                            
                        }            
                        
                        // put in format for QuickChart
                        for (int j = 0; j < workingData.size(); j++) {
                            def date = new Date(workingData[j].date).format(quickChartDateFormat)
                            parsedData << "${deviceName};${typeToLog};${date};${workingData[j].value};Final"
                        }
                        
                    }
                    else if (typeToLog == "Sleep Snoring" || typeToLog == "Sleep Heart Rate" || typeToLog == "Sleep Respiratory Rate") {
                        data.each { datapoint ->
                            datapoint?."$param".each { timestamp, value ->
                                def date = new Date(timestamp.toLong()*1000).format(quickChartDateFormat)
                                parsedData << "${deviceName};${typeToLog};${date};${value};Final"       
                            }
                        }                        
                    }
                   fileManager.writeFile("WithingsSleep${param.capitalize()}DataForDevice${deviceId}.txt", "${parsedData}")
                }
            }
            
        }
                    
    }
    else logDebug("writeToFile called but nothing written. fileManager is null")
}

def isLogDataTypeToFile(category) { // category is a substring common to all types of data that can be logged to file
    return dataTypesToLogToFile.any { it.contains(category) }
}

def getDataTypesToLogToFile(category) { // category is a substring common to all types of data that can be logged to file
    def typesToLog = []
    if (category == "Sleep") typesToLog = dataTypesToLogToFile.findAll { it.contains(category) }
    return typesToLog
}

def getDebugLogging() {
    return debugOutput ?: false
}

def displayFooter(){
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>Withings Integration<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7LBRPJRLJSDDN&source=url' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Please consider donating. This app took a lot of work to make.<br>If you find it valuable, I'd certainly appreciate it!</div>"
	}       
}

def getFormat(type, myText=""){			// Modified from @Stephack Code   
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def logDebug(msg) {
    if (getDebugLogging()) {
		log.debug msg
	}
}

@Field static mapEncodedSleepStateToString = [
	0: 'Awake',
	1: 'Light',
    2: 'Deep',
    3: 'REM',
    4: 'Manual',
    5: 'Unspecified',
]

@Field static mapDataTypeToParam = [
	'Sleep State': 'state',
    'Sleep Snoring': 'snoring',
	'Sleep Heart Rate': 'hr',
    'Sleep Respiratory Rate': 'rr',
]
