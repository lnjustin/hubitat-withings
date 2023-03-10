/**
 *  Withings Thermometer
 *
 *  Copyright 2020 Dominick Meglio
 *  Copyright 2022 @lnjustin
 *
 *  Licensed under the BSD 3-Clause License
 *
 *  Change History:
 *  All Changes documented in Withings_User.groovy
 */

metadata {
    definition(name: "Withings Thermometer", namespace: "lnjustin", author: "dmeglio@gmail.com, lnjustin") {
        capability "Sensor"
        capability "TemperatureMeasurement"
		capability "Battery"

        
        attribute "bodyTemperature", "number"
        attribute "skinTemperature", "number"
    }
}
