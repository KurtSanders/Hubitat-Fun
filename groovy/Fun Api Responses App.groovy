/*
 *  Fun Api Responses App
 *
 *  Copyright 2025 Kurt Sanders
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

#include kurtsanders.SanderSoft-Library
#include kurtsanders.Fun-Api-Responses-Library

import groovy.transform.Field

@Field static final String APP_NAME      			= "Fun Api Responses App"
@Field static final String VERSION                 	= "0.1.2"


definition(
    name              : "Fun Api Responses App",
    namespace         : NAMESPACE,
    author            : AUTHOR_NAME,
    description       : "Installs 'Fun Api Responses' application, creates parent device and sets attributes & preferences.",
    category          : "",
    iconUrl           : "",
    iconX2Url         : "",
    installOnOpen	  : true,
    documentationLink : COMM_LINK,
    singleInstance    : true
) {
}

preferences {
    page(name: "mainPage")
}

def installed() {
    // Set logging to 'Debug for 30 minutes' during initial setup of the app
    log.info "Setting initial '${app.name}' level logging to 'Debug' for 30 Minutes..."
    setLogLevel("Debug", "1 Hour")
    def initialDefaultSite 		= 'Idioms'
    app.updateSetting("site"		,[value: initialDefaultSite, type:"enum"])
    app.updateSetting("activeSites"	,[value: initialDefaultSite, type:"enum"])
    state.response 					= ' '
    state.apiKeyMap 				= [:]
    state.activeSites 				= []
}

def updated() {
    // Makes ure we have all the child devices created.
    setChildrenDevices()
    // Update device attributes for responses
    updateDeviceQueryKeys()
    // Register/De-register Global Hub Variables
    removeAllInUseGlobalVar()
    if (whichVar) addInUseGlobalVar(whichVar)
}

void setChildrenDevices(createChildDevices=true) {
    logTrace "setChildrenDevices()"
    // Create child devices for each api response site
    if (state.activeSites != activeSites) {
        logTrace "==> activeSites= ${activeSites}"
        Map childrenDeviceMap = [:]
        convert2List(activeSites).each {
            logInfo "Creating child device 'Fun-${it}'"
            def devId 	= "${it}-${app.id}-${PARENT_DEVICE_TYPE_NAME}"
            if (createChildDevices) createDataChildDevice(NAMESPACE, PARENT_DEVICE_TYPE_NAME, devId, "Fun-${it}")
            childrenDeviceMap[it] = devId
        }
	    state.siteDevId = childrenDeviceMap
    	state.activeSites = convert2List(activeSites)
    }
}

List convert2List(var) {
    logTrace "convert2List(${var})"
    if (var instanceof String) return [var]
    return var
}

void deleteChildrenDevices() {
    logTrace "deleteChildrenDevices()"
    // Delete child devices for each unselected api response site
    Map childrenDeviceMap = [:]
    getAllChildDevices().each {
        def siteName = it.deviceNetworkId.minus("-${app.id}-${PARENT_DEVICE_TYPE_NAME}")
        logTrace "==> siteName= ${siteName}"
        logTrace "==> convert2List(activeSites)= ${convert2List(activeSites)}"
        if (convert2List(activeSites).contains(siteName)) {
            logInfo getFormat('text-green',"Selected site '${it.name}' is selected and will be kept")
            childrenDeviceMap[siteName] = it.deviceNetworkId            
        } else {
            // Delete this unselected site child device
            logInfo getFormat('text-red',"Unselected child device '${it.name}' was deleted, Select it again to re-create it.")
            deleteChildDevice(it.deviceNetworkId)
        }
    }
    state.siteDevId = childrenDeviceMap
    state.activeSites = convert2List(activeSites)
}

// Testing Area
def testGroovy() {
    logTrace "testGroovy()"
	return
}

List getSites() {
    List sites = [] 
    SERVICES['sites'].each {
        sites << "${it.key}"
    }
	return sites.sort()
}

List getChildDeviceSiteNames() {
    List sites = []
	getAllChildDevices().each {
       sites.add(it.deviceNetworkId.minus("-${app.id}-${PARENT_DEVICE_TYPE_NAME}"))
    }
    return sites.sort()
}

List orphanedChildren() {
    return getChildDeviceSiteNames() - convert2List(activeSites)
}
    
void updateDeviceQueryKeys() {
    // Update device attributes for sites with paramaters
    getSites().each {
        logTrace "Site = ${it}"
        if (SERVICES['sites'][it].parameters) {
            def d = getChildDevice(state.siteDevId[it])
            if (d) {
                SERVICES['sites'][it].parameters.each {
                    logTrace "Parameter = ${it}"        
                    switch (it) {
                    case "include-tags":
                        d.sendEvent(name: "includeTags", value: includeTags?includeTags.toString().replaceAll("[\\[\\](){}]",""):' ')
                        break
                    case "exclude-tags":
                        d.sendEvent(name: "excludeTags", value: excludeTags?excludeTags.toString().replaceAll("[\\[\\](){}]",""):' ')
                        break
                    case "difficulty":
                        d.sendEvent(name: "difficulty", value: difficultyTags?difficultyTags.toString().replaceAll("[\\[\\](){}]",""):' ')
                        break
                    case "language":
                        break
                    default:
                        logErr "updateDeviceQueryKeys(): Unknown parameter '${it}'"
                    break
                    }
                }
            }
        }
    }
}

def mainPage() {
    dynamicPage(name: "mainPage", uninstall: true, install: true ) {
        def parameters
        def orphanedChildren = orphanedChildren()
        //Community Help Link
        section () {
            input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Hubitat Community Support <u>WebLink</u> to ${app.name}")
            paragraph ("")
        }        
        section(sectionHeader("${PARENT_DEVICE_TYPE_NAME} Child Device Manager")) {
            input "activeSites", "enum", title: getFormat('text-blue',"Select Sites to Create/Delete Child Devices. "), required: true,
            	width: 6, multiple: true, submitOnChange: true, options: getSites()
            setChildrenDevices()
            if (orphanedChildren) {
                paragraph getFormat('text-red',"<strong>Delete</strong> ${orphanedChildren.size()} unused (${orphanedChildren.join(", ")}) child device(s)?")
                input "deleteButton", "button", title: "Delete"
            }
        }
        if (activeSites) {
            section(sectionHeader("${PARENT_DEVICE_TYPE_NAME} Device and Preferences Manager")) {
                def hubIP = location.hub.localIP
                def line = '<span><style>#menu ul{list-style: none;}#menu li{display: inline-block;margin-right: 20px;}</style><div id="menu"><ul>'
                convert2List(activeSites).each {
                    def d = getChildDevice(state.siteDevId[it])
                    if (d) {
                        def deviceLink = "'http://${hubIP}/device/edit/${d.id}'"
                        def hoverTitle = "title='View ${it} device in new browser tab'" 
                        def parentDeviceWebLink = "<a target='_blank' ${hoverTitle} rel='noopener noreferrer' href=${deviceLink}><strong>${d.label}</strong></a>"
                        def boxGraphic = "<a href=${deviceLink} target='_blank' ${hoverTitle} > ${BOX_ARROW} </a>"
                        line += "<li>${parentDeviceWebLink}${boxGraphic}</li>"
                    }
                }
                line += '</ul></div></span>'
                line += "The following ${convert2List(activeSites).size()} devices <strong>&#8593;</strong> have been automatically created for you that will contain the response. "
                line += "You will need to install the respective device's 'Switch' or 'Push' tiles on your HE dashboard and/or use Rules/WebCore to command refresh the respective device."
                paragraph line
                if (location.hub.name == 'C7-2 Hub') input "testGroovy", "button", title: "Test Groovy Code"
            }
            section(sectionHeader("REQUIRED Site Choice and Inputs")) {
                if (site) {
                    state.mysite = SERVICES['sites'][site]
                    if (state.mysite.description) paragraph getFormat('text-blue',"${state.mysite.description}")
                    parameters  = state.mysite?.parameters
                }
                input "site", "enum", title: getFormat('text-blue',"Select a Response Category"), defaultValue: convert2List(activeSites)[0], width: 2, required: true,
                    multiple: false, submitOnChange: true, options: convert2List(activeSites)
                if (state.mysite?.api) {
                    def apiWebLink = makeWebLink("${state.mysite.website.link}","${state.mysite.website.name}")
                    input "apiKey", "text"	, title: getFormat('text-blue', "Enter your API key from the ${apiWebLink}"), 
                        required: true, width: 4, submitOnChange: true
                }
                if (state.mysite.api) {
                    state.apiKeyMap = (state.apiKeyMap)?:[:]
                    logTrace "==> state.apiKeyMap[state.mysite.url]= ${state.apiKeyMap[state.mysite.url]}"
                    if (state.apiKeyMap[state.mysite.url] != apiKey) {
                        logInfo "Adding/Updating the '${site}' sites api key to 'state.apiKeyMap' storage: '${state.mysite.url}' : '${apiKey}'"
                        state.apiKeyMap << [(state.mysite.url) : apiKey]
                    }
                }
            }                      
            // Only display these optional input options once the getType is selected above
            if ((site && (state.mysite?.api && apiKey)) || (site && (state.mysite?.api==null))) {
                def title = (state.mysite?.parameters)?"OPTIONAL ${site} Filters & Test":"${site} Test"
                section(sectionHeader("${title}")) {
                    paragraph getFormat('text-blue', "You can 'test' your preferences and TTS settings using this button.  If successful, you will see the response below and be confident that the child devices will create a random response.")
                    input "testGet", "button", title: "Test ${site} Response", submitOnChange: true
                    if (state.response) {
                        def responseOutput = "<style>table, th, td {border:1px solid black;}</style><table><tr><th style='text-align:center'>${site} Response</th></tr><tr><td>${getFormat('text-green',state.response)}</td></tr></table>"
                        if (state.mysite?.api && state.quotaLeft) {
                            def apiWebLink = makeWebLink("${state.mysite.website.link}","${state.mysite.website.name}")
                            paragraph getFormat('text-blue', "You have <strong>${state.quotaLeft}</strong> api request tokens left in your ${apiWebLink} account plan as of ${nowFormatted('EEEEE, MMM-dd h:mm:ss a z')}.")
                        }
                        if (site == state.lastSite) paragraph "${responseOutput}"
                    }
                    if (state.mysite?.parameters) {
                        if (state.mysite?.parameters.contains("include-tags")) {
                            input "includeTags"		, "enum"	, title: getFormat('text-green', "The list of filters the ${site} should have."), width: 4, submitOnChange: true,
                                multiple: true, options: SERVICES['sites'][site]?.keywords
                        }
                        if (state.mysite?.parameters.contains("exclude-tags")) {
                            input "excludeTags"		, "enum"	, title: getFormat('text-red', "The list of filters the ${site} should <strong>NOT</strong> have."), width: 4,
                                submitOnChange: true, multiple: true, options: SERVICES['sites'][site]?.keywords
                        }
                        if (state.mysite?.parameters.contains("difficulty")) {
                            input "difficultyTags"		, "enum"	, title: getFormat('text-blue', "The difficulty level of the ${site}. Leave unselected to produce a variety."), width: 4,
                                submitOnChange: true, options: SERVICES['sites'][site]?.keywords
                        }
                    }
                }
            }
            section(sectionHeader("OPTIONAL: Text To Speach <strong>(TTS)</strong> Output")) {

                // Automatically Send response text to a TTS device option
                input "ttsDevice", "capability.speechSynthesis"	, title: getFormat('text-blue', "Select a speech capable device to automatically play the TTS response"), width: 6,
                submitOnChange: true, showFilter: false, multiple: false
                if (ttsDevice) {
                    List<String> deviceCommands = ttsDevice.supportedCommands.name
                    if ((ttsDevice.typeName == "Echo Speaks Device") && (ttsDevice.hasCommand('playAnnouncementAll'))) {
                        input "playAnnounceAllBool", "bool"	, title: getFormat('text-blue', "${(playAnnounceAllBool)?'Send to <strong>ALL</strong> Echo Speaks Alexa devices':'Send only to the <strong>ONE</strong> Echo Speaks Alexa device'}"), 
                            width: 4, submitOnChange: true
                    }
                    if (site == 'Riddles') {
                        input "pauseDuration", "number", title: "TTS pause duration in secs.  This is for sites that respond with a question & answer, like a riddle.", defaultValue: 4, required: true
                    }
                }

                // Select a Global Hub Variable Option for the response string
                def ghvWebLink		= makeWebLink("/installedapp/direct/hubVariables","Global Hub Variable","Link to configure a global hub variable")
                def ghvTitle 	= "Choose an existing ${ghvWebLink} to use for the response string.  The ${ghvWebLink} must already exist.  This is optional, as one can create a temporary variable in Hubitat Rules to capture the response from the child device."             
                List vars = []
                getAllGlobalVars().each{vars += it.key}            
                input "whichVar", "enum", multiple: false, title: getFormat('text-blue',ghvTitle), options: vars.sort(), submitOnChange: true
            }
        }        
        
        //App Logging Options
        def ll 	= (logLevel)?LOG_LEVELS[getLogLevelInfo()['level']]:'?'
        def ls	= (logLevelTime)?LOG_TIMES[getLogLevelInfo()['time']]:'?'
        section(sectionHeader("Logging Options: Current: ${ll} for ${ls}"), hideable: true, hidden: true) {
            input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
                description: fmtDesc("Logs selected level and above"), defaultValue: 3, submitOnChange: true, options: LOG_LEVELS
            input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"), submitOnChange: true,
                description: fmtDesc("Time to enable Debug/Trace logging"), defaultValue: 10, options: LOG_TIMES
        }
    }
}

void appButtonHandler(String buttonName) {
    logTrace "==> Button Name = ${buttonName}"
    switch (buttonName) {
        case 'testGroovy':
	        testGroovy()
        break
        case 'testGet':
           	refresh(site)
        break
        case 'deleteButton':
            logTrace "Cleanup/Deleting inactive children"
            deleteChildrenDevices()
//            app.updateSetting("deleteInactiveChildren",[value: false, type:"bool"])
        break
        default:
            logErr "Unknown '${buttonName}' button call"
            break
    }
}

void renameVariable(String oldName, String newName) {
    logInfo "Global Variable: Renaming ${oldName} to ${newName}"
    app.updateSetting("whichVar",[value: newName, type:"enum"])
}

void refresh(selectedSite=site) {
    logTrace "==> selectedSite= ${selectedSite}"
    def d = getChildDevice(state.siteDevId[selectedSite])
    state.mysite = SERVICES['sites'][selectedSite]
    logTrace "==> state.mysite= ${state.mysite}"
    state.lastSite = selectedSite
    // Get site response
    def uri 	= SERVICES['sites'][selectedSite]['url']
    if (state.mysite.api) {
	    if (!apiKey) {
            def errorMessage = "You need a free api key from ${state.mysite.website.link} ${state.mysite.website.name} for the '${selectedSite}' category'"
			log.error errorMessage
            d.sendEvent(name: 'error'	, value: errorMessage)
        	return
    	}
	}
    
    
    //Get uri path per selectedSite
    String path
    Integer rndNumIndex
    switch (selectedSite) {
        case 'Idioms':
		    Integer fileSuffixNum
            Integer rndNum = Math.abs(new Random().nextInt() % IDIOMS_MAX) + 1
        	rndNumIndex = (rndNum<100)?rndNum:rndNum.toString()[-2..-1].toInteger()
        	if (rndNum % 100 != 0) {
                int division = (rndNum / 100) + 1;
                fileSuffixNum = division * 100;
            } else {
                fileSuffixNum = Math.max(100, rndNum);
            }
        	logInfo "==> Idiom Random #: ${rndNum} of ${IDIOMS_MAX}.  This idiom, which is Idiom #${rndNumIndex}, is located in the Idioms-${fileSuffixNum}.json file."
	        path = String.format(state.mysite.path, fileSuffixNum)
        break
        default:
            path = state.mysite?.path
        break
    }
    
    d.sendEvent(name: 'error'	, value: ' ')
    state.response = ''
    def params = [
        uri			: uri,
        path		: path,
        contentType	: "application/json",
        headers		: [
            'Content-Type' : 'application/json'
            ]
    ]
    // Check for User-Agent Requirement
    if (state.mysite?.UserAgent) {
        logTrace "Adding User-Agent Header"
        params['headers']['User-Agent'] = "SystemId:${app.getHubUID()}"
    }
        
    // Place apiKey into either the header or the query into the params
    if (state.mysite?.api) {
        params[state.mysite?.api.where] = [
            "${state.mysite?.api.name}": apiKey
            ]
    }
    // Check for query tags for sites that allow and add to query list
    if (state.mysite?.parameters) {    
        def queryParams = [:]
        if (includeTags) 	{queryParams["include-tags"] 	= includeTags.join(",")}
        if (excludeTags) 	{queryParams["exclude-tags"]	= excludeTags.join(",")}
        if (difficultyTags) {queryParams["difficulty"]		= difficultyTags}
        if (language) 		{queryParams["language"]		= 'en'}
        if (queryParams) params['query'] = queryParams
    }
    params.each {
        logTrace "httpGet params → ${it}"
    }
    
	//  Break here for review of the params
    //	return
    def httpResponse
    try {
	    logInfo "==> Sending a httpGET request to '${uri}${path}' for '${selectedSite}' response"
        httpGet(params) { response ->
            httpResponse 	= response
            logTrace "==> response.data= ${response.data}"
        }
    } catch (Exception ex) {
    	logWarn "HttpGet call to ${uri} failed: ${ex.message}"
        d.sendEvent(name: "error", value: ex.toString())
        state.response = ''
        return
    }    
    logInfo "==> HttpGet response status '${httpResponse.status}' received"
    // Read Response Headers
    if (httpResponse.status == 200) {
        if (state.mysite?.api && httpResponse?.headers) {
            String newName
            Integer newValue
            d.sendEvent('name': 'quotaLastUpdated', 'value': nowFormatted())
            httpResponse.headers.each {
                logTrace "Header = ${it}"
                if (it.name.startsWith('X-Api')) {
                    newValue = Float.valueOf(it.value).toInteger()
                    logTrace "==> Working on it.name= ${it.name} = ${newValue}"
                    switch ("${it.name}") {
                        case 'X-Api-Quota-Request':
                            newName = 'quotaRequest'
                        break
                        case 'X-Api-Quota-Used':
                            newName = 'quotaUsed'
                        break
                        case 'X-Api-Quota-Left':
                            state.quotaLeft = "${newValue.toString()}"
                            logTrace "==> state.quotaLeft= ${state.quotaLeft}"
                            newName = 'quotaLeft'
                        break
                        default:
                            logWarn "Invald Header = ${it.name}"
                            newName = ''
                            break
                    }
                    if (newName) {
                        logTrace "(Quota: ${newName} = ${newValue})"
                        d.sendEvent('name': "${newName}", 'value': newValue)
                    } else {
                        logWarn  "newName is ${newName}: ${it.name}"  
                    }
                }
            }
        }

        // Get the vars into a list object from the data map        
        List responseVars	= []
        def dbResults
        if (state.mysite?.responseKey) {
            dbResults = (rndNumIndex)?httpResponse.data[state.mysite.responseKey][rndNumIndex]:httpResponse.data[state.mysite.responseKey]
        } else dbResults = (rndNumIndex)?httpResponse.data[rndNumIndex]:httpResponse.data
        logTrace "==> dbResults= ${dbResults}"
        state.mysite.responseVars.each {
            responseVars.add(dbResults[it])
            d.sendEvent(name: it, value: dbResults[it])
        }
        responseVars = responseVars.flatten()
        logTrace "==> responseVars= ${responseVars}"

        if (responseVars) {
            // create the response string for next steps
            String response = sprintf(state.mysite.strTemplate,responseVars)         
            logDebug "==> response= ${response}"
            
            //create an event for the response in the respective device
            d.sendEvent(name: "response", value: response)
            // Update state variable with response
            state.response = response
            
            // Add response to the global hub variable
            if (whichVar)  {
                def rc = setGlobalVar(whichVar, response.take(1024))
                if (!rc) logErr "Error: setGlobalVar failed for ${whichVar}, rc = ${rc}"
            }
            
        //Send to TTS Device if selected
            if (ttsDevice) {
                def ttsCommand = (playAnnounceAllBool)?'playAnnouncementAll':'speak'
                logDebug "Sending response to ${ttsDevice} as '${ttsCommand}'"
                switch(selectedSite) {
	                // Check for a question type response with a '?'. Split the response and pauseexecution for TTS output               
                    case 'Riddles':
	                    ttsDevice."${ttsCommand}"(response.split('\\?')[0] + '?')
                    	pauseExecution((pauseDuration)?:5)
	                    ttsDevice."${ttsCommand}"(response.split('\\?')[1])
                    	break
                    default:
                    	ttsDevice."${ttsCommand}"(response)
                    	break
                }
            }
        } else {
            responseError("No response data from ${selectedSite} to parse to dataVars!")
        }
    }
}

void responseError(message) {
    def d = getChildDevice(state.siteDevId[state.lastSite])
    response = "Opps, I couldn't find any witty ${selectedSite} for you today."  
    d.sendEvent(name: "error", value: message)
    if (state.mysite?.parameters) response += " Maybe try some new or fewer include or exclude tags/filters?"
    state.response = response    
}

Void errorMessage(message) {
    def d = getChildDevice(state.siteDevId[site])
    d.sendEvent(name: "error", value: message)
    logErr message
}