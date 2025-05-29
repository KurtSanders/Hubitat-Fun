/**
 * Fun Api Responses Driver Integration by Kurt Sanders 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include kurtsanders.SanderSoft-Library
#include kurtsanders.Fun-Api-Responses-Library

@Field static final String  VERSION 			= "0.1.0"

metadata {
    definition(name: "Fun Api Responses Driver", namespace: "kurtsanders", author: "Kurt Sanders") {
        capability "Actuator"
		capability "Momentary"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"

        // Define the full response and response attributes parts for each site category
        attribute "answer"				, "string"
        attribute "author"				, "string"
        attribute "definition"			, "string"
        attribute "joke"				, "string"
        attribute "phrase"				, "string"
        attribute "poem"				, "string"
        attribute "question"			, "string"
        attribute "quote"				, "string"
        attribute "response"			, "string"
        attribute "riddle"				, "string"
        attribute "title"				, "string"
        attribute "trivia"				, "string"
        
		// Site metadata attributes
        attribute "error"				, "string"
        attribute "difficultyLevel"		, "string"
        attribute "excludeTags"			, "string"
        attribute "includeTags"			, "string"
        attribute "quotaRequest"		, "number"
        attribute "quotaUsed"			, "number"
        attribute "quotaLeft"			, "number"
        attribute "quotaLastUpdated"	, "date"
    }
}

preferences {    
        //	Logging Levels & Help
		input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
    		description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
		input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
    		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
    	//  Display Help Link
		input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

def push() {
    refresh()
}

def on() {
 	refresh()
    runIn(1,'off')
}

def off() {
 	sendEvent(name:'switch', value:'off')   
}

def installed() {
    setLogLevel("Debug", "30 Minutes")
    checkLogLevel()  // Set Logging Objects    
	log.info "Setting Inital logging level to 'Debug' for 30 minutes"
}

def updated() {
    logInfo "updated..."
    checkLogLevel()  // Set Logging Objects    
    logDebug "Debug logging is: ${logEnable == true}"    
    sendEvent(name: 'error', value: ' ')
}

def refresh() {
    parent.refresh(device.deviceNetworkId.split('-')[0])   
}