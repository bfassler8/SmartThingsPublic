/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.transform.Field

// enummaps
@Field final Map      MODE = [
    OFF:   "off",
    HEAT:  "heat",
    AUTO:  "auto",
    COOL:  "cool",
    EHEAT: "emergency heat"
]

@Field final Map      FAN_MODE = [
    OFF:       "off",
    AUTO:      "auto",
    CIRCULATE: "circulate",
    ON:        "on"
]

@Field final Map      OP_STATE = [
    COOLING:   "cooling",
    HEATING:   "heating",
    FAN:       "fan only",
    PEND_COOL: "pending cool",
    PEND_HEAT: "pending heat",
    VENT_ECO:  "vent economizer",
    IDLE:      "idle"
]

@Field final Map SETPOINT_TYPE = [
    COOLING: "cooling",
    HEATING: "heating"
]

@Field final List HEAT_ONLY_MODES = [MODE.HEAT, MODE.EHEAT]
@Field final List COOL_ONLY_MODES = [MODE.COOL]
@Field final List DUAL_SETPOINT_MODES = [MODE.AUTO]
@Field final List RUNNING_OP_STATES = [OP_STATE.HEATING, OP_STATE.COOLING]

// config - TODO: move these to a pref page
@Field List SUPPORTED_MODES = [MODE.OFF, MODE.HEAT, MODE.AUTO, MODE.COOL, MODE.EHEAT]
@Field List SUPPORTED_FAN_MODES = [FAN_MODE.OFF, FAN_MODE.AUTO, FAN_MODE.ON]

@Field final Float    THRESHOLD_DEGREES = 1.0
@Field final Integer  SIM_HVAC_CYCLE_SECONDS = 15
@Field final Integer  DELAY_EVAL_ON_MODE_CHANGE_SECONDS = 3

@Field final Integer  MIN_SETPOINT = 35
@Field final Integer  MAX_SETPOINT = 95
@Field final Integer  AUTO_MODE_SETPOINT_SPREAD = 4 // In auto mode, heat & cool setpoints must be this far apart
// end config

// derivatives
@Field final IntRange FULL_SETPOINT_RANGE = (MIN_SETPOINT..MAX_SETPOINT)
@Field final IntRange HEATING_SETPOINT_RANGE = (MIN_SETPOINT..(MAX_SETPOINT - AUTO_MODE_SETPOINT_SPREAD))
@Field final IntRange COOLING_SETPOINT_RANGE = ((MIN_SETPOINT + AUTO_MODE_SETPOINT_SPREAD)..MAX_SETPOINT)

// defaults
@Field final String   DEFAULT_MODE = MODE.OFF
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.AUTO
@Field final String   DEFAULT_OP_STATE = OP_STATE.IDLE
@Field final String   DEFAULT_PREVIOUS_STATE = OP_STATE.HEATING
@Field final String   DEFAULT_SETPOINT_TYPE = SETPOINT_TYPE.HEATING
@Field final Integer  DEFAULT_TEMPERATURE = 72
@Field final Integer  DEFAULT_HEATING_SETPOINT = 68
@Field final Integer  DEFAULT_COOLING_SETPOINT = 80
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = DEFAULT_HEATING_SETPOINT
@Field final Integer  DEFAULT_HUMIDITY = 52


metadata {
    // Automatically generated. Make future change here.
    definition (name: "Simulated Thermostat", namespace: "smartthings/testing", author: "SmartThings") {
        capability "Sensor"
        capability "Actuator"
        capability "Health Check"

        capability "Thermostat"
        capability "Relative Humidity Measurement"
        capability "Configuration"
        capability "Refresh"

        command "tempUp"
        command "tempDown"
        command "heatUp"
        command "heatDown"
        command "coolUp"
        command "coolDown"
        command "setpointUp"
        command "setpointDown"

        command "cycleMode"
        command "cycleFanMode"

        command "setTemperature", ["number"]
        command "setHumidityPercent", ["number"]
        command "delayedEvaluate"
        command "runSimHvacCycle"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("default", label:'${currentValue} °F', unit:"°F")
            }
            tileAttribute("device.temperature", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "setpointUp")
                attributeState("VALUE_DOWN", action: "setpointDown")
            }
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
                attributeState("default", label: '${currentValue}% Hum', unit: "%", icon: "st.Weather.weather12")
            }
            tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
                attributeState("idle", backgroundColor: "#FFFFFF")
                attributeState("heating", backgroundColor: "#E86D13")
                attributeState("cooling", backgroundColor: "#00A0DC")
            }
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
                attributeState("off",  label: '${name}')
                attributeState("heat", label: '${name}')
                attributeState("cool", label: '${name}')
                attributeState("auto", label: '${name}')
                attributeState("emergency heat", label: 'e-heat')
            }
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "°F")
            }
            tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
                attributeState("default", label: '${currentValue}', unit: "°F")
            }
        }

        standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
            state "off",            action: "cycleMode", nextState: "updating", icon: "st.thermostat.heating-cooling-off", backgroundColor: "#CCCCCC", defaultState: true
            state "heat",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.heat"
            state "cool",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.cool"
            state "auto",           action: "cycleMode", nextState: "updating", icon: "st.thermostat.auto"
            state "emergency heat", action: "cycleMode", nextState: "updating", icon: "st.thermostat.emergency-heat"
            state "updating", label: "Working", icon: "st.secondary.secondary"
        }

        standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
            state "off",       action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-off", backgroundColor: "#CCCCCC", defaultState: true
            state "auto",      action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-auto"
            state "on",        action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-on"
            state "circulate", action: "cycleFanMode", nextState: "updating", icon: "st.thermostat.fan-circulate" 
            state "updating", label: "Working", icon: "st.secondary.secondary"
        }

        valueTile("heatingSetpoint", "device.heatingSetpoint", width: 2, height: 2, decoration: "flat") {
            state "heat", label:'Heat\n${currentValue} °F', unit: "°F", backgroundColor:"#E86D13"
        }
        standardTile("heatDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "heat", action: "heatDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("heatUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "heat", action: "heatUp", icon: "st.thermostat.thermostat-up"
        }

        valueTile("coolingSetpoint", "device.coolingSetpoint", width: 2, height: 2, decoration: "flat") {
            state "cool", label: 'Cool\n${currentValue} °F', unit: "°F", backgroundColor: "#00A0DC"
        }
        standardTile("coolDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "cool", action: "coolDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("coolUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "cool", action: "coolUp", icon: "st.thermostat.thermostat-up"
        }

        valueTile("roomTemp", "device.temperature", width: 2, height: 2, decoration: "flat") {
            state "default", label:'${currentValue} °F', unit: "°F", backgroundColors: [
                // Celsius Color Range
                [value:  0, color: "#153591"],
                [value:  7, color: "#1E9CBB"],
                [value: 15, color: "#90D2A7"],
                [value: 23, color: "#44B621"],
                [value: 29, color: "#F1D801"],
                [value: 33, color: "#D04E00"],
                [value: 36, color: "#BC2323"],
                // Fahrenheit Color Range
                [value: 40, color: "#153591"],
                [value: 44, color: "#1E9CBB"],
                [value: 59, color: "#90D2A7"],
                [value: 74, color: "#44B621"],
                [value: 84, color: "#F1D801"],
                [value: 92, color: "#D04E00"],
                [value: 96, color: "#BC2323"]
            ]
        }
        standardTile("tempDown", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "temp", action: "tempDown", icon: "st.thermostat.thermostat-down"
        }
        standardTile("tempUp", "device.temperature", width: 1, height: 1, decoration: "flat") {
            state "default", label: "temp", action: "tempUp", icon: "st.thermostat.thermostat-up"
        }

        // To modify the simulation environment
        valueTile("simControlLabel", "device.switch", width: 4, height: 1, decoration: "flat") {
            state "default", label: "Simulated Environment Control"
        }

        valueTile("blank1x1", "device.switch", width: 1, height: 1, decoration: "flat") {
            state "default", label: ""
        }
        valueTile("blank2x1", "device.switch", width: 2, height: 1, decoration: "flat") {
            state "default", label: ""
        }

        valueTile("reset", "device.switch", width: 4, height: 1, decoration: "flat") {
            state "default", label: "Reset to Defaults", action: "configure"
        }

        valueTile("humiditySliderLabel", "device.humidity", width: 4, height: 1, decoration: "flat") {
            state "default", label: 'Simulated Humidity: ${currentValue}%'
        }

        controlTile("humiditySlider", "device.humidity", "slider", width: 4, height: 1, range: "(0..100)") {
            state "humidity", action: "setHumidityPercent"
        }

        main("thermostatMulti")
        details(["thermostatMulti",
            "heatDown", "heatUp",
            "mode", 
            "coolDown", "coolUp",
            "heatingSetpoint",
            "coolingSetpoint",
            "fanMode",
            "blank2x1", "blank2x1",
            "blank1x1", "simControlLabel", "blank1x1",
            "tempDown", "tempUp", "humiditySliderLabel",
            "roomTemp", "humiditySlider",
            "reset"
        ])
    }
}

def installed() {
    log.trace "Executing 'installed'"
    initialize()
}

def configure() {
    log.trace "Executing 'configure'"
    initialize()
}

private initialize() {
    log.trace "Executing 'initialize'"

    // for HealthCheck
    sendEvent(name: "checkInterval", value: 12 * 60, displayed: false, data: [protocol: "cloud", scheme: "untracked"])

    sendEvent(name: "temperature", value: DEFAULT_TEMPERATURE, unit: "°F")
    sendEvent(name: "humidity", value: DEFAULT_HUMIDITY, unit: "%")
    sendEvent(name: "heatingSetpoint", value: DEFAULT_HEATING_SETPOINT, unit: "°F")
    sendEvent(name: "heatingSetpointMin", value: HEATING_SETPOINT_RANGE.getFrom(), unit: "°F")
    sendEvent(name: "heatingSetpointMax", value: HEATING_SETPOINT_RANGE.getTo(), unit: "°F")
    sendEvent(name: "thermostatSetpoint", value: DEFAULT_THERMOSTAT_SETPOINT, unit: "°F")
    sendEvent(name: "coolingSetpoint", value: DEFAULT_COOLING_SETPOINT, unit: "°F")
    sendEvent(name: "coolingSetpointMin", value: COOLING_SETPOINT_RANGE.getFrom(), unit: "°F")
    sendEvent(name: "coolingSetpointMax", value: COOLING_SETPOINT_RANGE.getTo(), unit: "°F")
    sendEvent(name: "thermostatMode", value: DEFAULT_MODE)
    sendEvent(name: "thermostatFanMode", value: DEFAULT_FAN_MODE)
    sendEvent(name: "thermostatOperatingState", value: DEFAULT_OP_STATE)

    state.isHvacRunning = false
    state.lastOperatingState = DEFAULT_OP_STATE
    state.lastUserSetpointMode = DEFAULT_PREVIOUS_STATE
    unschedule()
}


// parse events into attributes
def parse(String description) {
    log.trace "parse $description"
    def parsedEvents
    def pair = description?.split(":")
    if (!pair || pair.length < 2) {
        log.warn "parse() could not extract an event name and value from '$description'"
    } else {
        String name = pair[0]?.trim()
        if (name) {
            name = name.replaceAll(~/\W/, "_").replaceAll(~/_{2,}?/, "_")
        }
        parsedEvents = createEvent(name: name, value: pair[1]?.trim())
    }
    return parsedEvents
}


def ping() {
    refresh()
}

def refresh() {
    sendEvent(name: "thermostatMode", value: getThermostatMode())
    sendEvent(name: "thermostatFanMode", value: getFanMode())
    sendEvent(name: "thermostatOperatingState", value: getOperatingState())
    sendEvent(name: "thermostatSetpoint", value: getThermostatSetpoint(), unit: "°F")
    sendEvent(name: "coolingSetpoint", value: getCoolingSetpoint(), unit: "°F")
    sendEvent(name: "heatingSetpoint", value: getHeatingSetpoint(), unit: "°F")
    sendEvent(name: "temperature", value: getTemperature(), unit: "°F")
    sendEvent(name: "humidity", value: getHumidityPercent(), unit: "%")
}

// Thermostat mode
private String getThermostatMode() {
    return device.currentValue("thermostatMode") ?: DEFAULT_MODE
}

def setThermostatMode(String value) {
    if (value in SUPPORTED_MODES) {
        proposeSetpoints(getHeatingSetpoint(), getCoolingSetpoint(), state.lastUserSetpointMode)
        sendEvent(name: "thermostatMode", value: value)
        evaluateOperatingState()
    } else {
        log.warn "'$value' is not a supported mode. Please set one of ${SUPPORTED_MODES.join(', ')}"
    }
}

private String cycleMode() {
    String nextMode = nextListElement(SUPPORTED_MODES, getThermostatMode())
    setThermostatMode(nextMode)
    return nextMode
}

private Boolean isThermostatOff() {
    return getThermostatMode() == MODE.OFF
}

// Fan mode
private String getFanMode() {
    return device.currentValue("thermostatFanMode") ?: DEFAULT_FAN_MODE
}

def setThermostatFanMode(String value) {
    if (value in SUPPORTED_FAN_MODES) {
        sendEvent(name: "thermostatFanMode", value: value)
    } else {
        log.warn "'$value' is not a supported fan mode. Please set one of ${SUPPORTED_FAN_MODES.join(', ')}"
    }
}

private String cycleFanMode() {
    String nextMode = nextListElement(SUPPORTED_FAN_MODES, getFanMode())
    setThermostatFanMode(nextMode)
    return nextMode
}

private String nextListElement(List uniqueList, currentElt) {
    if (uniqueList != uniqueList.unique().asList()) {
        throw InvalidPararmeterException("Each element of the List argument must be unique.")
    } else if (!(currentElt in uniqueList)) {
        throw InvalidParameterException("currentElt '$currentElt' must be a member element in List uniqueList, but was not found.")
    }
    Integer listIdxMax = uniqueList.size() -1
    Integer currentEltIdx = uniqueList.indexOf(currentElt)
    Integer nextEltIdx = currentEltIdx < listIdxMax ? ++currentEltIdx : 0
    String nextElt = uniqueList[nextEltIdx] as String
    return nextElt
}

// operating state
private String getOperatingState() {
    String operatingState = device.currentValue("thermostatOperatingState")?:OP_STATE.IDLE
    return operatingState
}

private setOperatingState(String operatingState) {
    if (operatingState in OP_STATE.values()) {
        sendEvent(name: "thermostatOperatingState", value: operatingState)
        if (operatingState != OP_STATE.IDLE) {
            state.lastOperatingState = operatingState
        }
    } else {
        log.warn "'$operatingState' is not a supported operating state. Please set one of ${OP_STATE.values().join(', ')}"
    }
}

// setpoint
private Integer getThermostatSetpoint() {
    def ts = device.currentState("thermostatSetpoint")
    return ts ? ts.getIntegerValue() : DEFAULT_THERMOSTAT_SETPOINT
}

private Integer getHeatingSetpoint() {
    def hs = device.currentState("heatingSetpoint")
    return hs ? hs.getIntegerValue() : DEFAULT_HEATING_SETPOINT
}

def setHeatingSetpoint(Double degreesF) {
    log.debug "setHeatingSetpoint($degreesF)"
    state.lastUserSetpointMode = SETPOINT_TYPE.HEATING
    setHeatingSetpointInternal(degreesF)
}

private setHeatingSetpointInternal(Double degreesF) {
    log.debug "setHeatingSetpointInternal($degreesF)"
    proposeHeatSetpoint(degreesF as Integer)
    evaluateOperatingState(heatingSetpoint: degreesF)
}

private heatUp() {
    def newHsp = getHeatingSetpoint() + 1
    if (getThermostatMode() in HEAT_ONLY_MODES + DUAL_SETPOINT_MODES) {
        setHeatingSetpoint(newHsp)
    }
}

private heatDown() {
    def newHsp = getHeatingSetpoint() - 1
    if (getThermostatMode() in HEAT_ONLY_MODES + DUAL_SETPOINT_MODES) {
        setHeatingSetpoint(newHsp)
    }
}

private Integer getCoolingSetpoint() {
    def cs = device.currentState("coolingSetpoint")
    return cs ? cs.getIntegerValue() : DEFAULT_COOLING_SETPOINT
}

def setCoolingSetpoint(Double degreesF) {
    log.debug "setCoolingSetpoint($degreesF)"
    state.lastUserSetpointMode = SETPOINT_TYPE.COOLING
    setCoolingSetpointInternal(degreesF)
}

private setCoolingSetpointInternal(Double degreesF) {
    log.debug "setCoolingSetpointInternal($degreesF)"
    proposeCoolSetpoint(degreesF as Integer)
    evaluateOperatingState(coolingSetpoint: degreesF)
}

private coolUp() {
    def newCsp = getCoolingSetpoint() + 1
    if (getThermostatMode() in COOL_ONLY_MODES + DUAL_SETPOINT_MODES) {
        setCoolingSetpoint(newCsp)
    }
}

private coolDown() {
    def newCsp = getCoolingSetpoint() - 1
    if (getThermostatMode() in COOL_ONLY_MODES + DUAL_SETPOINT_MODES) {
        setCoolingSetpoint(newCsp)
    }
}

// for the setpoint up/down buttons on the multi-attribute thermostat tile.
private setpointUp() {
    String mode = getThermostatMode()
    if (mode in COOL_ONLY_MODES) {
        coolUp()
    } else if (mode in HEAT_ONLY_MODES + DUAL_SETPOINT_MODES) {
        heatUp()
    }
}

private setpointDown() {
    String mode = getThermostatMode()
    if (mode in COOL_ONLY_MODES + DUAL_SETPOINT_MODES) {
        coolDown()
    } else if (mode in HEAT_ONLY_MODES) {
        heatDown()
    }
}

// simulated temperature
private Integer getTemperature() {
    def ts = device.currentState("temperature")
    Integer currentTemp = DEFAULT_TEMPERATURE
    try {
        currentTemp = ts.integerValue
    } catch (all) {
        log.warn "Encountered an error getting Integer value of temperature state. Value is '$ts.stringValue'. Reverting to default of $DEFAULT_TEMPERATURE"
        setTemperature(DEFAULT_TEMPERATURE)
    }
    return currentTemp
}

// changes the "room" temperature for the simulation
private setTemperature(newTemp) {
    sendEvent(name:"temperature", value: newTemp)
    evaluateOperatingState(temperature: newTemp)
} 

private tempUp() {
    def newTemp = getTemperature() ? getTemperature() + 1 : DEFAULT_TEMPERATURE
    setTemperature(newTemp)
}

private tempDown() {
    def newTemp = getTemperature() ? getTemperature() - 1 : DEFAULT_TEMPERATURE
    setTemperature(newTemp)
}

private setHumidityPercent(Integer humidityValue) {
    log.trace "Executing 'setHumidityPercent' to $humidityValue"
    Integer curHum = device.currentValue("humidity") as Integer
    if (humidityValue != null) { 
        Integer hum = boundInt(humidityValue, (0..100))
        if (hum != humidityValue) {
            log.warn "Corrrected humidity value to $hum"
            humidityValue = hum
        }
        sendEvent(name: "humidity", value: humidityValue, unit: "%")
    } else {
        log.warn "Could not set measured huimidity to $humidityValue%"
    }
}

private getHumidityPercent() {
    def hp = device.currentState("humidity")
    return hp ? hp.getIntegerValue() : DEFAULT_HUMIDITY
}

/**
 * Ensure an integer value is within the provided range, or set it to either extent if it is outside the range.
 * @param Number value         The integer to evaluate
 * @param IntRange theRange     The range within which the value must fall
 * @return Integer
 */
private Integer boundInt(Number value, IntRange theRange) {
    value = Math.max(theRange.getFrom(), Math.min(theRange.getTo(), value))
    return value.toInteger()
}

private proposeHeatSetpoint(Integer heatSetpoint) {
    proposeSetpoints(heatSetpoint, null)
}

private proposeCoolSetpoint(Integer coolSetpoint) {
    proposeSetpoints(null, coolSetpoint)
}

private proposeSetpoints(Integer heatSetpoint, Integer coolSetpoint, String prioritySetpointType=null) {
    Integer newHeatSetpoint;
    Integer newCoolSetpoint;

    String mode = getThermostatMode()
    Integer proposedHeatSetpoint = heatSetpoint?:getHeatingSetpoint()
    Integer proposedCoolSetpoint = coolSetpoint?:getCoolingSetpoint()
    if (coolSetpoint == null) { 
        prioritySetpointType = SETPOINT_TYPE.HEATING
    } else if (heatSetpoint == null) {
        prioritySetpointType = SETPOINT_TYPE.COOLING
    } else if (prioritySetpointType == null) {
        prioritySetpointType = DEFAULT_SETPOINT_TYPE
    } else {
        // we use what was passed as the arg. 
    }

    if (mode in HEAT_ONLY_MODES) {
        newHeatSetpoint = boundInt(proposedHeatSetpoint, FULL_SETPOINT_RANGE)
        if (newHeatSetpoint != proposedHeatSetpoint) {
            log.warn "proposed heat setpoint $proposedHeatSetpoint is out of bounds. Modifying..."
        }
    } else if (mode in COOL_ONLY_MODES) {
        newCoolSetpoint = boundInt(proposedCoolSetpoint, FULL_SETPOINT_RANGE)
        if (newCoolSetpoint != proposedCoolSetpoint) {
            log.warn "proposed cool setpoint $proposedCoolSetpoint is out of bounds. Modifying..."
        }
    } else if (mode in DUAL_SETPOINT_MODES) {
        if (prioritySetpointType == SETPOINT_TYPE.HEATING) {
            newHeatSetpoint = boundInt(proposedHeatSetpoint, HEATING_SETPOINT_RANGE)
            IntRange customCoolingSetpointRange = ((newHeatSetpoint + AUTO_MODE_SETPOINT_SPREAD)..COOLING_SETPOINT_RANGE.getTo())
            newCoolSetpoint = boundInt(proposedCoolSetpoint, customCoolingSetpointRange)
        } else if (prioritySetpointType == SETPOINT_TYPE.COOLING) {
            newCoolSetpoint = boundInt(proposedCoolSetpoint, COOLING_SETPOINT_RANGE)
            IntRange customHeatingSetpointRange = (HEATING_SETPOINT_RANGE.getFrom()..(newCoolSetpoint - AUTO_MODE_SETPOINT_SPREAD))
            newHeatSetpoint = boundInt(proposedHeatSetpoint, customHeatingSetpointRange)
        }
    } else if (mode == MODE.OFF) {
        log.debug "Thermostat is off - no setpoints will be modified"
    } else {
        log.warn "Unknown/unhandled Thermostat mode: $mode"
    }

    if (newHeatSetpoint != null) {
        log.info "set heating setpoint of $newHeatSetpoint"
        sendEvent(name: "heatingSetpoint", value: newHeatSetpoint, unit: "F")
    }
    if (newCoolSetpoint != null) {
        log.info "set cooling setpoint of $newCoolSetpoint"
        sendEvent(name: "coolingSetpoint", value: newCoolSetpoint, unit: "F")		
    }
}

// sets the thermostat setpoint and operating state and starts the "HVAC" or lets it end.
private evaluateOperatingState(Map overrides) {
    // check for override values, otherwise use current state values
    Integer currentTemp = overrides.find { key, value -> 
            "$key".toLowerCase().startsWith("curr")|"$key".toLowerCase().startsWith("temp")
        }?.value?:getTemperature() as Integer
    Integer heatingSetpoint = overrides.find { key, value -> "$key".toLowerCase().startsWith("heat") }?.value?:getHeatingSetpoint() as Integer
    Integer coolingSetpoint = overrides.find { key, value -> "$key".toLowerCase().startsWith("cool") }?.value?:getCoolingSetpoint() as Integer

    String tsMode = getThermostatMode()
    String currentOperatingState = getOperatingState()

    log.debug "evaluate current temp: $currentTemp, heating setpoint: $heatingSetpoint, cooling setpoint: $coolingSetpoint"
    log.debug "mode: $tsMode, operating state: $currentOperatingState"

    Boolean isHeating = false
    Boolean isCooling = false
    Boolean isIdle = false
    if (tsMode in HEAT_ONLY_MODES + DUAL_SETPOINT_MODES) {
        if (heatingSetpoint - currentTemp >= THRESHOLD_DEGREES) {
            isHeating = true
            setOperatingState(OP_STATE.HEATING)
        } 
        sendEvent(name: "thermostatSetpoint", value: heatingSetpoint)
    }
    if (tsMode in COOL_ONLY_MODES + DUAL_SETPOINT_MODES && !isHeating) {
        if (currentTemp - coolingSetpoint >= THRESHOLD_DEGREES) {
            isCooling = true
            setOperatingState(OP_STATE.COOLING)
        }
        sendEvent(name: "thermostatSetpoint", value: coolingSetpoint)
    }
    else {
        sendEvent(name: "thermostatSetpoint", value: heatingSetpoint)
    }
    if (isHeating || isCooling) {
        startSimHvac() // we need to run the HVAC
    } else {
        setOperatingState(OP_STATE.IDLE)
    }
}

//
// Methods to "run" the heating/air conditioning. This baby heats or  cools at about a degree every 15 seconds.
//

private startSimHvac() {
    String operatingState = getOperatingState()
    Boolean isRunning = state?.isHvacRunning?:false
    Boolean shouldBeRunning = (operatingState in RUNNING_OP_STATES)
    log.trace "Executing 'startSimHvac' isRunning: $isRunning, shouldBeRunning: $shouldBeRunning, op: $operatingState"
    if (!isRunning && shouldBeRunning) {
        log.info "starting simulated hvac"
        state.isHvacRunning = true
        runIn(SIM_HVAC_CYCLE_SECONDS, "runSimHvacCycle")
    } else if (isRunning) {
        log.trace "simulated hvac is already running"
    } else if (!shouldBeRunning) {
        log.trace "simulated hvac does not need to run now"		
    }
}

private runSimHvacCycle() {
    def operatingState = getOperatingState()
    def currentTemp = getTemperature()
    def heatSet = getHeatingSetpoint()
    def coolSet = getCoolingSetpoint()
    log.trace "Executing 'runSimHvacCycle' - op: $operatingState, current: $currentTemp, heat set: $heatSet, cool set: $coolSet"

    if (operatingState == OP_STATE.HEATING && heatSet - currentTemp >= THRESHOLD_DEGREES) {
        tempUp()
        runIn(SIM_HVAC_CYCLE_SECONDS, "runSimHvacCycle")
    } else if (operatingState == OP_STATE.COOLING && currentTemp - coolSet >= THRESHOLD_DEGREES) {
        tempDown()
        runIn(SIM_HVAC_CYCLE_SECONDS, "runSimHvacCycle")
    } else {
        // end the job
        evaluateOperatingState()
        state.isHvacRunning = false
        log.info "simulated hvac cycle has concluded"
    }
}

