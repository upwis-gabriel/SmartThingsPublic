metadata {
	definition (name: "Smart Home", namespace: "eventstreet19532", author: "gabriel", runLocally: true, minHubCoreVersion: '000.017.0012', mnmn: "SmartThingsCommunity", vid: "58edeaf1-047e-3754-badb-aa8f023d1468") {
		capability "Refresh"
        capability "Battery"
		capability "Health Check"
 
 		capability "eventstreet19532.chmode"
 		capability "eventstreet19532.chocp"
        
        attribute "OCP", "number"
        attribute "mode", "STRING"
		
        command "setbleach"
        command "setcolor"
        command "setautonomous"
        command "setOCP"
        command "setMode"
        command "nextmode"
        command "ping"
        
		fingerprint deviceId: "0x"
		fingerprint deviceId: "0x3101"  // for z-wave certification, can remove these when sub-meters/window-coverings are supported
		fingerprint deviceId: "0x3101", inClusters: "0x86,0x32"
		fingerprint deviceId: "0x09", inClusters: "0x86,0x72,0x26"
		fingerprint deviceId: "0x0805", inClusters: "0x47,0x86,0x72"
	}
}

def setautonomous(String s) {
	log.debug "setautonomous"
	delayBetween([
		zwave.basicV1.basicSet(value: 0x32).format(),
		zwave.basicV1.basicGet().format()
	], 1000)
}

def installed() {
    log.debug "running installed"
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 600, displayed: true, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	sendEvent(name: "OCP", value: 0)
	sendEvent(name: "battery", value: 0)
	sendEvent(name: "mode", value: "autonomous")
    unschedule(refresh)
    runEvery10Minutes(refresh)
}

def updated(){
	log.debug "running updated"
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 600, displayed: true, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	response(refresh())
    unschedule(refresh)
    runEvery10Minutes(refresh)
}

def getCommandClassVersions() {
	[
		0x20: 1,  // Basic
        0x56: 1,  // Crc16Encap
		0x70: 1,  // Configuration
        0x31: 1, //multilevel sensor
        0x80: 1, //battery
	]
}

def parse(String description) {
	log.debug "started parse"
	def result = null
	def cmd = zwave.parse(description, commandClassVersions)
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
        log.debug "created zw event"
	}
		log.debug "Parse returned ${result?.descriptionText}"
        log.debug "Parse description is ${description}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
 	log.debug "basic report value is ${cmd?.value}"
    
    if(cmd.value < 10) {
    	sendEvent(name: "mode", value: "bleach")
//		[name: "switch", value: "off"]
    } else if (cmd.value > 90) {
    	//[name: "switch", value: "on"]
        sendEvent(name: "mode", value: "color")
    } else {
 //   	[name: "switch", value: "test"]
		sendEvent(name: "mode", value: "autonomous")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv1.SensorMultilevelReport cmd) {
	log.debug "sensor multilevel report"
    log.debug (cmd?.sensorValue[0])
    log.debug(cmd)
	sendEvent(name: "OCP", value: cmd?.sensorValue[0])
}





def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def setcolor(String s) {
	log.debug "setcolor"
	delayBetween([
		zwave.basicV1.basicSet(value: 0x64).format(),
		zwave.basicV1.basicGet().format()
	], 1000)
}

def setbleach(String s) {
	log.debug "setbleach"
	delayBetween([
		zwave.basicV1.basicSet(value: 0x01).format(),
		zwave.basicV1.basicGet().format()
	], 1000)
}

def setMode(String newmode) {
	log.debug "setMode"
	log.debug(newmode)
    switch (newmode) {
    	case "color":
        	setcolor("a")
        	break
        case "bleach":
        	setbleach("a")
            break
        case "autonomous":
        	setautonomous("a")
            break
        default:
        	log.debug "Unknown window mode"
            log.debug(newmode)
    }
}

def setOCP(Number a) {
	log.debug "setOCPval num"
    log.debug(a)
}

def setOCP(int a) {
	log.debug "setOCPval int"
    log.debug(a)
}

def setOCP(something) {
	log.debug "setOCPval ??"
    log.debug(something)
}

def nextmode() {
	log.debug "nextmode!"
	def currentmode = device.currentValue("mode")
	log.debug (currentmode)
    switch (currentmode) {
    	case "color":
        	setautonomous("a")
        	break
        case "bleach":
        	setcolor("a")
            break
        case "autonomous":
        	setbleach("a")
            break
        default:
        	log.debug "Unknown window mode"
            log.debug(currentmode)
    }
}
/**
  * PING is used by Device-Watch in attempt to reach the Device
**/
def ping() {
	log.debug "ping"
//	delayBetween([
//		zwave.basicV1.basicGet().format(),
//		zwave.batteryV1.batteryGet().format(),
 //       zwave.sensorMultilevelV1.sensorMultilevelGet().format()
//	], 1000)
//	refresh()

	if (!state.pingLastRanAt || now() >= state.pingLastRanAt + 5000) {
		state.pingLastRanAt = now()
		log.debug "Executing 'ping'"
    	refresh()
	}
	else {
		log.trace "ping(): Ran within last 5 seconds so aborting."
	}

}

def refresh() {
	log.debug "refresh"
	delayBetween([zwave.basicV1.basicGet().format(), delayBetween([zwave.sensorMultilevelV1.sensorMultilevelGet().format(), zwave.batteryV1.batteryGet().format()], 500)], 1000);
    //return zwave.batteryV1.batteryGet().format()
    
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	sendEvent(name: "battery", value: cmd?.batteryLevel)
}