/**
 *  Withings Scale
 *
 *  Copyright 2020 Dominick Meglio
 *  Copyright 2022 @lnjustin
 *  Licensed under the BSD 3-Clause License
 *
 *  Change History:
 *  All Changes documented in Withings_User.groovy
 */

metadata {
    definition(name: "Withings Scale", namespace: "lnjustin", author: "dmeglio@gmail.com, lnjustin") {
        capability "Sensor"
		capability "Battery"

        attribute "weight", "number"
        attribute "weightDisplay", "string"
        attribute "pulse", "number"
        attribute "fatFreeMass", "number"
        attribute "fatFreeMassDisplay", "string"
        attribute "fatRatio", "number"
        attribute "fatMassWeight", "number"
        attribute "fatMassWeightDisplay", "string"
        attribute "muscleMass", "number"
        attribute "muscleMassDisplay", "string"
        attribute "boneMass", "number"
        attribute "boneMassDisplay", "string"
        attribute "pulseWaveVelocity", "number"
    }
}
