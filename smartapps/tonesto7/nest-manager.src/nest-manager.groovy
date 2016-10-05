/********************************************************************************************
|    Application Name: Nest Manager and Automations                                         |
|    Author: Anthony S. (@tonesto7), Eric S. (@E_sch)                                       |
|    Contributors: Ben W. (@desertblade)                                                    |
|                                                                                           |
|*******************************************************************************************|
|    There maybe portions of the code that may resemble code from other apps in the         |
|    community. I may have used some of it as a point of reference.                         |
|    Thanks go out to those Authors!!!                                                      |
|    I apologize if i've missed anyone.  Please let me know and I will add your credits     |
|                                                                                           |
|    ### I really hope that we don't have a ton or forks being released to the community,   |
|    ### I hope that we can collaborate and make app and device type that will accommodate  |
|    ### every use case                                                                     |
*********************************************************************************************/

import groovy.json.*
import groovy.time.*
import java.text.SimpleDateFormat
import java.security.MessageDigest

definition(
	name: "${textAppName()}",
	namespace: "${textNamespace()}",
	author: "${textAuthor()}",
	description: "${textDesc()}",
	category: "Convenience",
	iconUrl: "https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_manager.png",
	iconX2Url: "https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_manager%402x.png",
	iconX3Url: "https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_manager%403x.png",
	singleInstance: true,
	oauth: true )

{
	appSetting "clientId"
	appSetting "clientSecret"
}

include 'asynchttp_v1'

def appVersion() { "3.5.4" }
def appVerDate() { "10-5-2016" }
def appVerInfo() {
	def str = ""

	str += "V3.5.2 (October 4th, 2016):"
	str += "\n▔▔▔▔▔▔▔▔▔▔▔"
	str += "\n • UPDATED: Lot's more UI polish automations..."
	str += "\n • UPDATED: Lot's of little bugfixes...."
	str += "\n • UPDATED: Beta Test of async http polling...."

	str += "\n\nV3.4.6 (October 1st, 2016):"
	str += "\n▔▔▔▔▔▔▔▔▔▔▔"
	str += "\n • UPDATED: Lot's of UI reworks for automations..."
	str += "\n • UPDATED: Lot's of little bugfixes...."
	str += "\n • UPDATED: Merged in eric's latest patch..."
	str += "\n • UPDATED: Lot's of modifications to the thermostat UI design..."
	str += "\n • UPDATED: Cleanup of Old Code and bugfixes..."
	str += "\n • UPDATED: More bug fixes and cleanups..."

	str += "\n\nV3.3.0 (September 19th, 2016):"
	str += "\n▔▔▔▔▔▔▔▔▔▔▔"
	str += "\n • UPDATED: Automation Refactor with Schedules (ALPHA)..."

	return str
}

preferences {
	//startPage
	page(name: "startPage")

	//Manager Pages
	page(name: "authPage")
	page(name: "mainPage")
	page(name: "deviceSelectPage")
	page(name: "reviewSetupPage")
	page(name: "changeLogPage")
	page(name: "prefsPage")
	page(name: "infoPage")
	page(name: "nestInfoPage")
	page(name: "structInfoPage")
	page(name: "tstatInfoPage")
	page(name: "protInfoPage")
	page(name: "camInfoPage")
	page(name: "pollPrefPage")
	page(name: "debugPrefPage")
	page(name: "notifPrefPage")
	page(name: "diagPage")
	page(name: "appParamsDataPage")
	page(name: "devNamePage")
	page(name: "childAppDataPage")
	page(name: "childDevDataPage")
	page(name: "managAppDataPage")
	page(name: "alarmTestPage")
	page(name: "simulateTestEventPage")
	page(name: "devNameResetPage")
	page(name: "resetDiagQueuePage")
	page(name: "devPrefPage")
	page(name: "nestLoginPrefPage")
	page(name: "nestTokenResetPage")
	page(name: "uninstallPage")
	page(name: "custWeatherPage")
	page(name: "automationsPage")
	page(name: "automationKickStartPage")
	page(name: "automationGlobalPrefsPage")
	page(name: "automationStatisticsPage")
	page(name: "automationSchedulePage")

	//Automation Pages
	page(name: "selectAutoPage" )
	page(name: "mainAutoPage")
	page(name: "remSenTstatFanSwitchPage")
	page(name: "remSenShowTempsPage")
	page(name: "nestModePresPage")
	page(name: "tstatModePage")
	page(name: "schMotModePage")
	page(name: "setDayModeTimePage")
	page(name: "watchDogPage")
	page(name: "schMotSchedulePage")
	page(name: "scheduleConfigPage")
	page(name: "tstatConfigAutoPage")

	//shared pages
	page(name: "setNotificationPage")
	page(name: "setNotificationTimePage")
}


mappings {
	if(!parent) {
		//used during Oauth Authentication
		path("/oauth/initialize") 	{action: [GET: "oauthInitUrl"]}
		path("/oauth/callback") 	{action: [GET: "callback"]}
		//Renders Json Data
		path("/renderInstallId")    {action: [GET: "renderInstallId"]}
		path("/renderInstallData")  {action: [GET: "renderInstallData"]}
		//path("/receiveEventData") {action: [POST: "receiveEventData"]}
	}
}

//This Page is used to load either parent or child app interface code
def startPage() {
	if(parent) {
		atomicState?.isParent = false
		selectAutoPage()
	} else {
		atomicState?.isParent = true
		authPage()
	}
}

def authPage() {
	//log.trace "authPage()"
	getAccessToken()
	def preReqOk = preReqCheck()
	deviceHandlerTest()

	if(!atomicState?.accessToken || (!atomicState?.isInstalled && (!atomicState?.devHandlersTested || !preReqOk))) {
		return dynamicPage(name: "authPage", title: "Status Page", nextPage: "", install: false, uninstall: false) {
			section ("Status Page:") {
				def desc
				if(!atomicState?.accessToken) {
					desc = "OAuth is not Enabled for the Nest Manager application.  Please click remove and review the installation directions again..."
				}
				else if(!atomicState?.devHandlersTested) {
					desc = "Device Handlers are likely Missing or Not Published.  Please read the installation instructions and verify all device handlers are present before continuing."
				}
				else if(!preReqOk) {
					desc = "SmartThings Location is not returning (TimeZone: ${location?.timeZone}) or (ZipCode: ${location?.zipCode}) Please edit these settings under the IDE or Mobile App..."
				}
				else {
					desc = "Application Status has not received any messages to display"
				}
				LogAction("Status Message: $desc", "warn", true)
				paragraph "$desc", required: true, state: null
			}
		}
	}
	updateWebStuff(true)
	setStateVar(true)
	if(atomicState?.newSetupComplete) {
		def result = ((atomicState?.appData?.updater?.setupVersion && !atomicState?.setupVersion) || (atomicState?.setupVersion?.toInteger() < atomicState?.appData?.updater?.setupVersion?.toInteger())) ? true : false
		if(result) { atomicState?.newSetupComplete = null }
	}

	def description
	def oauthTokenProvided = false

	if(atomicState?.authToken) {
		description = "You are connected."
		oauthTokenProvided = true
	} else { description = "Click to enter Nest Credentials" }

	def redirectUrl = buildRedirectUrl
	//log.debug "RedirectUrl = ${redirectUrl}"

	if(!oauthTokenProvided && atomicState?.accessToken) {
		LogAction("AuthToken not found: Directing to Login Page...", "info", true)
		return dynamicPage(name: "authPage", title: "Login Page", nextPage: "mainPage", install: false, uninstall: false) {
			section("") {
				paragraph appInfoDesc(), image: getAppImg("nest_manager%402x.png", true)
			}
			section(""){
				paragraph "Tap 'Login to Nest' below to authorize SmartThings to access your Nest Account.\n\nAfter login you will be taken to the 'Works with Nest' page. Read the info and if you 'Agree' press the 'Accept' button."
				paragraph "❖ FYI: If you are using a Nest Family account please signin with the parent Nest account, family member accounts will not work correctly...", state: "complete"
				href url: redirectUrl, style:"embedded", required: true, title: "Login to Nest", description: description
			}
		}
	}
	else {
		return mainPage()
	}
}

def mainPage() {
	//log.trace "mainPage"
	def setupComplete = (!atomicState?.newSetupComplete || !atomicState.isInstalled) ? false : true
	def rfrshDash = atomicState?.dashSetup == true ? 5 : null
	return dynamicPage(name: "mainPage", title: "", nextPage: (!setupComplete ? "reviewSetupPage" : null), refreshInterval: rfrshDash, install: setupComplete, uninstall: false) {
		section("") {
			href "changeLogPage", title: "", description: "${appInfoDesc()}", image: getAppImg("nest_manager%402x.png", true)
			if (settings?.enableDashboard && atomicState?.dashboardInstalled && atomicState?.dashboardUrl) {
				href "", title: "Nest Manager Dashboard", style: "external", url: "${atomicState?.dashboardUrl}dashboard", image: getAppImg("dashboard_icon.png"), required: false
			}
			if(atomicState?.appData && !appDevType() && isAppUpdateAvail()) {
				href url: stIdeLink(), style:"external", required: false, title:"An Update is Available for ${appName()}!!!",
						description:"Current: v${appVersion()} | New: ${atomicState?.appData?.updater?.versions?.app?.ver}\n\nTap to Open the IDE in your Mobile Browser...", state: "complete", image: getAppImg("update_icon.png")
			}
		}
		if(atomicState?.isInstalled) {
			section("Manage your Devices & Location:") {
				def devDesc = getDevicesDesc() ? "Nest Location: (${locationPresence().toString().capitalize()})\n\nCurrent Devices: ${getDevicesDesc()}\n\nTap to Modify..." : "Tap to Configure..."
				href "deviceSelectPage", title: "Devices & Location", description: devDesc, state: "complete", image: getAppImg("thermostat_icon.png")
			}
		}
		if(!atomicState?.isInstalled) {
			devicesPage()
		}
		if(atomicState?.isInstalled && atomicState?.structures && (atomicState?.thermostats || atomicState?.protects || atomicState?.cameras)) {
			def autoDesc = getInstAutoTypesDesc() ? "${getInstAutoTypesDesc()}\n\nTap to Modify..." : null
			section("Manage your Automations:") {
				href "automationsPage", title: "Automations...", description: (autoDesc ? autoDesc : "Tap to Configure..."), state: (autoDesc ? "complete" : null), image: getAppImg("automation_icon.png")
			}
		}
		if(atomicState?.isInstalled) {
			section("Manage Your Login, Notification, and Polling Preferences:") {
				def descStr = ""
				def sz = descStr.size()
				descStr += getAppNotifConfDesc() ?: ""
				if(descStr.size() != sz) { descStr += "\n\n"; sz = descStr.size() }
				descStr += getAppDebugDesc() ?: ""
				if(descStr.size() != sz) { descStr += "\n\n"; sz = descStr.size() }
				descStr += getPollingConfDesc() ?: ""
				if(descStr.size() != sz) { descStr += "\n\n"; sz = descStr.size() }
				def prefDesc = (descStr != "") ? "Tap to Modify..." : "Tap to Configure..."
				href "prefsPage", title: "Preferences", description: prefDesc, state: (descStr ? "complete" : ""), image: getAppImg("settings_icon.png")
			}
			section("View Change Logs, Donation, and License Info:") {
				href "infoPage", title: "Help, Info and Instructions", description: "Tap to view...", image: getAppImg("info.png")
			}
			if(atomicState?.isInstalled && atomicState?.structures && (atomicState?.thermostats || atomicState?.protects || atomicState?.weatherDevice)) {
				section("View App and Device Data, and Perform Device Tests:") {
					href "nestInfoPage", title: "API | Diagnostics | Testing...", description: "Tap to view info...", image: getAppImg("api_diag_icon.png")
				}
			}
			webDashConfig()
			section("Remove All Apps, Automations, and Devices:") {
				href "uninstallPage", title: "Uninstall this App", description: "Tap to Remove...", image: getAppImg("uninstall_icon.png")
			}
		}
	}
}

def devicesPage() {
	def structs = getNestStructures()
	def structDesc = !structs?.size() ? "No Locations Found" : "Found (${structs?.size()}) Locations..."
	LogAction("${structDesc} (${structs})", "info", false)
	if (atomicState?.thermostats || atomicState?.protects || atomicState?.vThermostats || atomicState?.cameras || atomicState?.presDevice || atomicState?.weatherDevice ) {  // if devices are configured, you cannot change the structure until they are removed
		section("Your Location:") {
			 paragraph "Location: ${structs[atomicState?.structures]}\n\n(Remove All Devices to Change!)", image: getAppImg("nest_structure_icon.png")
		}
	} else {
		section("Select your Location:") {
			input(name: "structures", title:"Nest Locations", type: "enum", required: true, multiple: false, submitOnChange: true, metadata: [values:structs],
					image: getAppImg("nest_structure_icon.png"))
		}
	}
	if (settings?.structures) {
		atomicState.structures = settings?.structures ?: null

		def stats = getNestThermostats()
		def statDesc = stats.size() ? "Found (${stats.size()}) Thermostats..." : "No Thermostats"
		LogAction("${statDesc} (${stats})", "info", false)

		def coSmokes = getNestProtects()
		def coDesc = coSmokes.size() ? "Found (${coSmokes.size()}) Protects..." : "No Protects"
		LogAction("${coDesc} (${coSmokes})", "info", false)

		def cams = getNestCameras()
		def camDesc = cams.size() ? "Found (${cams.size()}) Cameras..." : "No Cameras"
		LogAction("${camDesc} (${cams})", "info", false)

		section("Select your Devices:") {
			if(!stats?.size() && !coSmokes.size() && !cams?.size()) { paragraph "No Devices were found..." }
			if(stats?.size() > 0) {
				input(name: "thermostats", title:"Nest Thermostats", type: "enum", required: false, multiple: true, submitOnChange: true, metadata: [values:stats],
						image: getAppImg("thermostat_icon.png"))
			}
			atomicState.thermostats =  settings?.thermostats ? statState(settings?.thermostats) : null
			if(coSmokes.size() > 0) {
				input(name: "protects", title:"Nest Protects", type: "enum", required: false, multiple: true, submitOnChange: true, metadata: [values:coSmokes],
						image: getAppImg("protect_icon.png"))
			}
			atomicState.protects = settings?.protects ? coState(settings?.protects) : null
			if(cams.size() > 0) {
				input(name: "cameras", title:"Nest Cameras", type: "enum", required: false, multiple: true, submitOnChange: true, metadata: [values:cams],
						image: getAppImg("camera_icon.png"))
			}
			atomicState.cameras = settings?.cameras ? camState(settings?.cameras) : null
			input(name: "presDevice", title:"Add Presence Device?\n", type: "bool", default: false, required: false, submitOnChange: true, image: getAppImg("presence_icon.png"))
			atomicState.presDevice = settings?.presDevice ?: null
			input(name: "weatherDevice", title:"Add Weather Device?\n", type: "bool", default: false, required: false, submitOnChange: true, image: getAppImg("weather_icon.png"))
			atomicState.weatherDevice = settings?.weatherDevice ?: null
		}
	}
}

def deviceSelectPage() {
	return dynamicPage(name: "deviceSelectPage", title: "Device Selection", nextPage: "startPage", install: false, uninstall: false) {
		devicesPage()
	}
}

def reviewSetupPage() {
	return dynamicPage(name: "reviewSetupPage", title: "Setup Review", install: true, uninstall: atomicState?.isInstalled) {
		if(!atomicState?.newSetupComplete) { atomicState.newSetupComplete = true }
		atomicState?.setupVersion = atomicState?.appData?.updater?.setupVersion?.toInteger() ?: 0
		section("Device Summary:") {
			def str = ""
			str += !atomicState?.isInstalled ? "Devices to Install:" : "Installed Devices:"
			str += getDevicesDesc() ?: ""
			paragraph "${str}"
			if(atomicState?.weatherDevice) {
				def wmsg = ""
				if(!getStZipCode() || getStZipCode() != getNestZipCode()) {
					wmsg = "Please configure as zip codes do not match..."
				}
				href "custWeatherPage", title: "Customize Weather Location?", description: (getWeatherConfDesc() ? "${getWeatherConfDesc()}\n\nTap to Modify..." : "${wmsg}"), state: ((getWeatherConfDesc() || wmsg) ? "complete":""), image: getAppImg("weather_icon_grey.png")

				input ("weathAlertNotif", "bool", title: "Notify on Weather Alerts?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("weather_icon.png"))
			}
			if(!atomicState?.isInstalled && (settings?.thermostats || settings?.protects || settings?.cameras || settings?.presDevice || settings?.weatherDevice)) {
				href "devNamePage", title: "Customize Device Names?", description: (atomicState?.custLabelUsed || atomicState?.useAltNames) ? "Tap to Modify..." : "Tap to configure...", state: ((atomicState?.custLabelUsed || atomicState?.useAltNames) ? "complete" : null), image: getAppImg("device_name_icon.png")
			}
		}
		section("Notifications:") {
			href "notifPrefPage", title: "Notifications", description: (getAppNotifConfDesc() ? "${getAppNotifConfDesc()}\n\nTap to modify..." : "Tap to configure..."), state: (getAppNotifConfDesc() ? "complete" : null), image: getAppImg("notification_icon.png")
		}
		section("Polling:") {
			href "pollPrefPage", title: "Polling Preferences", description: "${getPollingConfDesc()}\n\nTap to modify...", state: (getPollingConfDesc() != "" ? "complete" : null), image: getAppImg("timer_icon.png")
		}
		doShareDev()
		if(atomicState?.showHelp) {
			section(" ") {
				href "infoPage", title: "Help, Info and Instructions", description: "Tap to view...", image: getAppImg("info.png")
			}
		}
		if(!atomicState?.isInstalled) {
			section("  ") {
				href "uninstallPage", title: "Uninstall this App", description: "Tap to Remove...", image: getAppImg("uninstall_icon.png")
			}
		}
	}
}

def doShareDev() {
	section("Share Data with Developer:") {
		paragraph "These options will send the developer non-identifiable app information as well as error data to help diagnose issues quicker and catch trending issues."
		input ("optInAppAnalytics", "bool", title: "Send Install Data?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("app_analytics_icon.png"))
		input ("optInSendExceptions", "bool", title: "Send Error Data?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("diag_icon.png"))
		if(settings?.optInAppAnalytics != false) {
			input(name: "mobileClientType", title:"Primary Mobile Device?", type: "enum", required: true, submitOnChange: true, metadata: [values:["android":"Android", "ios":"iOS", "winphone":"Windows Phone"]],
							image: getAppImg("${(settings?.mobileClientType && settings?.mobileClientType != "decline") ? "${settings?.mobileClientType}_icon" : "mobile_device_icon"}.png"))
			href url: getAppEndpointUrl("renderInstallData"), style:"embedded", title:"View the Data that will be Shared with the Developer", description: "Tap to view Data...", required:false, image: getAppImg("view_icon.png")
		}
	}
}

//Defines the Preference Page
def prefsPage() {
	def devSelected = (atomicState?.structures && (atomicState?.thermostats || atomicState?.protects || atomicState?.cameras || atomicState?.presDevice || atomicState?.weatherDevice))
	dynamicPage(name: "prefsPage", title: "Application Preferences", nextPage: "", install: false, uninstall: false ) {
		section("Polling:") {
			href "pollPrefPage", title: "Polling Preferences", description: "${getPollingConfDesc()}\n\nTap to modify...", state: (getPollingConfDesc() != "" ? "complete" : null), image: getAppImg("timer_icon.png")
		}
		if(devSelected) {
			section("Devices:") {
				href "devPrefPage", title: "Device Customization", description: (devCustomizePageDesc() ? "${devCustomizePageDesc()}\n\nTap to Modify..." : "Tap to configure..."),
						state: (devCustomizePageDesc() ? "complete" : null), image: getAppImg("device_pref_icon.png")
			}
		}
		section("Notifications Options:") {
			href "notifPrefPage", title: "Notifications", description: (getAppNotifConfDesc() ? "${getAppNotifConfDesc()}\n\nTap to modify..." : "Tap to configure..."), state: (getAppNotifConfDesc() ? "complete" : null),
					image: getAppImg("notification_icon.png")
		}
		section("App and Device Logging:") {
			href "debugPrefPage", title: "Logging", description: (getAppDebugDesc() ? "${getAppDebugDesc() ?: ""}\n\nTap to modify..." : "Tap to configure..."), state: ((isAppDebug() || isChildDebug()) ? "complete" : null),
					image: getAppImg("log.png")
		}
		doShareDev()
		section ("Misc. Options:") {
			input ("useMilitaryTime", "bool", title: "Use Military Time (HH:mm)?", defaultValue: false, submitOnChange: true, required: false, image: getAppImg("military_time_icon.png"))
			input ("disAppIcons", "bool", title: "Disable App Icons?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("no_icon.png"))
			input ("debugAppendAppName", "bool", title: "Append App Name to Log Entries?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("log.png"))
			atomicState.needChildUpd = true
		}
		section("Manage Your Nest Login:") {
			href "nestLoginPrefPage", title: "Nest Login Preferences", description: "Tap to configure...", image: getAppImg("login_icon.png")
		}
		section("Customize the Label of the App:") {
			label title:"Application Label (optional)", required:false
		}
	}
}

def automationsPage() {
	return dynamicPage(name: "automationsPage", title: "Installed Automations", nextPage: !parent ? "" : "automationsPage", install: false) {
		def autoApp = findChildAppByName( appName() )

		if(autoApp) {
			//Nothing to add here yet...
		}
		else {
			section("") {
				paragraph "You haven't created any Automations yet!!!\nTap Create New Automation to get Started..."
			}
		}
		section("") {
			app(name: "autoApp", appName: appName(), namespace: "tonesto7", multiple: true, title: "Create New Automation...", image: getAppImg("automation_icon.png"))
			def rText = "NOTICE:\nAutomations is still in BETA!!!\n" +
						"We are not responsible for any damages caused by using this SmartApp.\n\n	       USE AT YOUR OWN RISK!!!"
			paragraph "${rText}"//, required: true, state: null
		}
		if(isAutoAppInst()) {
			section("") {
				def schEn = getChildApps()?.findAll { it?.getActiveScheduleState() != null }
				if(schEn.size()) {
					href "automationSchedulePage", title: "View Automation Schedule(s)", description: "", image: getAppImg("schedule_icon.png")
				}
				href "automationStatisticsPage", title: "View Automation Statistics", description: "", image: getAppImg("app_analytics_icon.png")
			}
			section("Global Options:                                                                         ", hideable: true, hidden: false) {
				def descStr = ""
				descStr += (settings?.locDesiredCoolTemp || settings?.locDesiredHeatTemp) ? "Comfort Settings:" : ""
				descStr += settings?.locDesiredHeatTemp ? "\n • Desired Heat Temp: (${settings?.locDesiredHeatTemp}°${getTemperatureScale()})" : ""
				descStr += settings?.locDesiredCoolTemp ? "\n • Desired Cool Temp: (${settings?.locDesiredCoolTemp}°${getTemperatureScale()})" : ""
				descStr += (settings?.locDesiredComfortDewpointMax) ? "${(settings?.locDesiredCoolTemp || settings?.locDesiredHeatTemp) ? "\n\n" : ""}Dew Point:" : ""
				descStr += settings?.locDesiredComfortDewpointMax ? "\n • Max Dew Point: (${settings?.locDesiredComfortDewpointMax}${getTemperatureScale()})" : ""
				descStr += "${(settings?.locDesiredCoolTemp || settings?.locDesiredHeatTemp) ? "\n\n" : ""}${getSafetyValuesDesc()}" ?: ""
				def prefDesc = (descStr != "") ? "${descStr}\n\nTap to Modify..." : "Tap to Configure..."
				href "automationGlobalPrefsPage", title: "Global Automation Preferences", description: prefDesc, state: (descStr != "" ? "complete" : null), image: getAppImg("global_prefs_icon.png")
			}
			section("Advanced Options: (Tap + to Show)                                                          ", hideable: true, hidden: true) {
				href "automationKickStartPage", title: "Re-Initialize All Automations", description: "Tap to call the Update() action on each automation.\nTap to Begin...", image: getAppImg("reset_icon.png")
			}
		}
	}
}

def automationSchedulePage() {
	dynamicPage(name: "automationSchedulePage", title: "View Schedule Data..", uninstall: false) {
		def schMap = []
		getChildApps()?.each {
			def actSch = it?.getScheduleDesc() ?: null
			if (actSch?.size()) {
				def schInfo = it?.getScheduleDesc()
				def curSch = it?.getCurrentSchedule()
				if (schInfo?.size()) {
					schInfo?.each { schItem ->
						def schNum = schItem?.key
						def schDesc = schItem?.value
						def schInUse = (curSch?.toInteger() == schNum?.toInteger()) ? true : false
						if(schNum && schDesc) {
							section("${it?.label}") {
								paragraph "${schDesc}", state: schInUse ? "complete" : ""
							}
							//href "schMotSchedulePage", title: "", description: "${schDesc}\n\nTap to Modify Schedule...", params: ["sNum":schNum], state: (schInUse ? "complete" : "")
						}
					}
				}
			}
		}
	}
}

def automationStatisticsPage() {
	dynamicPage(name: "automationStatisticsPage", title: "Installed Automations Stats\n(Auto-Refresh Every 20 sec.)", refreshInterval: 20, uninstall: false) {
		def cApps = getChildApps()
		if(cApps) {
			cApps?.sort()?.each { chld ->
				def autoType = chld?.getAutomationType()
				section(" ") {
					paragraph "${chld?.label}", state: "complete", image: getAutoIcon(autoType)
					def data = chld?.getAutomationStats()
					def tf = new SimpleDateFormat("M/d/yyyy - h:mm a")
						tf.setTimeZone(getTimeZone())
					def lastModDt = data?.lastUpdatedDt ? tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", data?.lastUpdatedDt.toString())) : null
					def lastEvtDt = data?.lastEvent?.date ? tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", data?.lastEvent?.date.toString())) : null
					def lastActionDt = data?.lastActionData?.dt ? tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", data?.lastActionData?.dt.toString())) : null
					def lastEvalDt = data?.lastEvalDt ? tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", data?.lastEvalDt.toString())) : null
					def lastSchedDt = data?.lastSchedDt ? tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", data?.lastSchedDt.toString())) : null
					def lastExecVal = data?.lastExecVal ?: null
					def execAvgVal = data?.execAvgVal ?: null

					def str = ""
					str += lastModDt ? " • Last Modified:\n  └ (${lastModDt})" : "\n • Last Modified: (Not Available)"
					str += lastEvtDt ? "\n\n • Last Event:" : ""
					str += lastEvtDt ? "${(data?.lastEvent?.displayName.length() > 20) ? "\n  │ Device:\n  │└ " : "\n  ├ Device: "}${data?.lastEvent?.displayName}" : ""
					str += lastEvtDt ? "\n  ├ Type: (${data?.lastEvent?.name.toString().capitalize()})" : ""
					str += lastEvtDt ? "\n  ├ Value: (${data?.lastEvent?.value}${data?.lastEvent?.unit ? "${data?.lastEvent?.unit}" : ""})" : ""
					str += lastEvtDt ? "\n  └ DateTime: (${lastEvtDt})" : "\n\n • Last Event: (Not Available)"
					str += lastEvalDt ? "\n\n • Last Evaluation:\n  └ (${lastEvalDt})" : "\n\n • Last Evaluation: (Not Available)"
					str += lastSchedDt ? "\n\n • Last Schedule:\n  └ (${lastSchedDt})" : "\n\n • Last Schedule: (Not Available)"
					str += lastActionDt ? "\n\n • Last Action:\n  ├ DateTime: (${lastActionDt})\n  └ Action: ${data?.lastActionData?.actionDesc}" : "\n\n • Last Action: (Not Available)"
					str += lastExecVal ? "\n\n • Execution Info:\n  ${execAvgVal ? "├" : "└"} Last Time: (${lastExecVal} ms)${execAvgVal ? "\n  └ Avg. Time: (${execAvgVal} ms)" : ""}" : "\n\n • Execution Info: (Not Available)"
					paragraph "${str}", state: "complete"
				}
			}
		}
	}
}

def automationKickStartPage() {
	dynamicPage(name: "automationKickStartPage", title: "This Page is running Update() on all of your installed Automations", nextPage: "automationsPage", install: false, uninstall: false) {
		def cApps = getChildApps()
		section("Running Update All Automations:") {
			if(cApps) {
				cApps?.sort()?.each { chld ->
					chld?.update()
					paragraph "${chld?.label}\n\nUpdate() Completed Successfully!!!", state: "complete"
				}
			} else {
				paragraph "No Automations Found..."
			}
		}
	}
}

def automationGlobalPrefsPage() {
	dynamicPage(name: "automationGlobalPrefsPage", title: "", nextPage: "", install: false) {
		if(atomicState?.thermostats) {
			section {
				paragraph "These settings are applied if individual thermostat settings are not present"
			}
			section(title: "Comfort Preferences 									", hideable: true, hidden: false) {
				input "locDesiredHeatTemp", "decimal", title: "Desired Global Heat Temp (°${getTemperatureScale()})", description: "Range within ${tempRangeValues()}", range: tempRangeValues(),
						required: false, image: getAppImg("heat_icon.png")
				input "locDesiredCoolTemp", "decimal", title: "Desired Global Cool Temp (°${getTemperatureScale()})", description: "Range within ${tempRangeValues()}", range: tempRangeValues(),
						required: false, image: getAppImg("cool_icon.png")
				def tRange = (getTemperatureScale() == "C") ? "15..19" : "60..66"
				def wDev = getChildDevice(getNestWeatherId())
				def curDewPnt = wDev ? "${wDev?.currentDewpoint}°${getTemperatureScale()}" : 0
				input "locDesiredComfortDewpointMax", "decimal", title: "Max. Dewpoint Desired (${tRange} °${getTemperatureScale()})", required: false,  range: trange,
						image: getAppImg("dewpoint_icon.png")
				href url: "https://en.wikipedia.org/wiki/Dew_point#Relationship_to_human_comfort", style:"embedded", title: "What is Dew Point?",
						description:"Tap to View Info", image: getAppImg("instruct_icon.png")
			}
			section(title: "Safety Preferences 									", hideable:true, hidden: false) {
				if(atomicState?.thermostats) {
					atomicState?.thermostats?.each { ts ->
						def dev = getChildDevice(ts?.key)
						def canHeat = dev?.currentState("canHeat")?.stringValue == "false" ? false : true
						def canCool = dev?.currentState("canCool")?.stringValue == "false" ? false : true

						def defmin
						def defmax
						def safeTemp = getSafetyTemps(dev)
						if(safeTemp) {
							defmin = safeTemp.min
							defmax = safeTemp.max
						}
						def dew_max = getComfortDewpoint(dev)

		/*
		 TODO
		      need to check / default to current setting in dth
		      should have method in dth to set safety temps (today they are sent from nest manager polls..)
		      should have method in dth to clear safety temps
		      add global default
		*/
						def str = ""
						str +=  "Safety Values:"
						str +=  safeTemp ? "\n• Safefy Temps:\n	  └ Min: ${safeTemp.min}°${getTemperatureScale()}/Max: ${safeTemp.max}°${getTemperatureScale()}" : "\n• Safefy Temps: (Not Set)"
						str +=  dew_max ?  "\n• Comfort Max Dewpoint:\n  └Max: ${dew_max}°${getTemperatureScale()}" : "\n• Comfort Max Dewpoint: (Not Set)"
						paragraph "${str}", title:"${dev?.displayName}", state: "complete", image: getAppImg("instruct_icon.png")
						if(canHeat) {
							input "${dev?.deviceNetworkId}_safety_temp_min", "decimal", title: "Min. Temp Desired °(${getTemperatureScale()})", description: "Range within ${tempRangeValues()}",
									range: "0..90", submitOnChange: true, required: false, image: getAppImg("heat_icon.png")
						}
						if(canCool) {
							input "${dev?.deviceNetworkId}_safety_temp_max", "decimal", title: "Max. Temp Desired (°${getTemperatureScale()})", description: "Range within ${tempRangeValues()}",
									range: tempRangeValues(), submitOnChange: true, required: false,  image: getAppImg("cool_icon.png")
						}
						def tmin = settings?."${dev?.deviceNetworkId}_safety_temp_min"
						def tmax = settings?."${dev?.deviceNetworkId}_safety_temp_max"

						def comparelow = getTemperatureScale() == "C" ? 10 : 50
						def comparehigh = getTemperatureScale() == "C" ? 32 : 90
						tmin = (tmin == null || tmin == 0 || (tmin >= comparelow && tmin <= comparehigh)) ? tmin : 0
						tmax = (tmax == null || tmax == 0 || (tmax <= comparehigh && tmax >= comparelow)) ? tmax : 0
						if (tmin && tmin != 0 && tmax && tmax != 0) {
							tmin = tmin < tmax ? tmin : 0
							tmax = tmax > tmin ? tmax : 0
						}
						atomicState?."${dev?.deviceNetworkId}_safety_temp_min" = tmin
						atomicState?."${dev?.deviceNetworkId}_safety_temp_max" = tmax

						def tRange = (getTemperatureScale() == "C") ? "15..19" : "60..66"
						input "${dev?.deviceNetworkId}_comfort_dewpoint_max", "decimal", title: "Max. Dewpoint Desired (${tRange} °${getTemperatureScale()})", required: false,  range: trange,
									image: getAppImg("dewpoint_icon.png")
	/*
						def hrange = "10..80"
						input "${dev?.deviceNetworkId}_comfort_humidity_max", "number", title: "Max. Humidity Desired (%)", description: "Range within ${hrange}", range: hrange,
									required: false, image: getAppImg("humidity_icon.png")
	*/
					}
				}
			}
		}
	}
}

def getSafetyValuesDesc() {
	def str = ""
	def tstats = atomicState?.thermostats
	if(tstats) {
		tstats?.each { ts ->
			def dev = getChildDevice(ts?.key)
			def defmin
			def defmax
			def safeTemp = getSafetyTemps(dev)
			if(safeTemp) {
				defmin = safeTemp.min
				defmax = safeTemp.max
			}
			def dew_max = getComfortDewpoint(dev)
			def minTemp = defmin
			def maxTemp = defmax
			def maxDew = dew_max ?: (getTemperatureScale() == "C") ? 19 : 66

			if(minTemp == 0) { minTemp = null }
			if(maxTemp == 0) { maxTemp = null }
			if(maxDew == 0) { maxDew = null }

			str += (ts && (minTemp || maxTemp)) ? "${dev?.displayName}\nSafety Values:" : ""
			str += minTemp ? "\n• Min. Temp: ${minTemp}°${getTemperatureScale()}" : ""
			str += maxTemp ? "\n• Max. Temp: ${maxTemp}°${getTemperatureScale()}" : ""
			//str += maxHum ? "\n• Max. Humidity: ${maxHum}%" : ""
			str += (ts && (minTemp || maxTemp) && (maxDew)) ? "\n\n" : ""
			str += (ts && (maxDew)) ? "${dev?.displayName}\nComfort Values:" : ""
			str += maxDew ? "\n• Max. Dewpnt: ${maxDew}°${getTemperatureScale()}" : ""
			str += tstats?.size() > 1 ? "\n\n" : ""
		}
	}
	return (str != "") ? "${str}" : null
}

def webDashConfig() {
	section("Web Dashboard Preferences:") {
		def dashAct = (settings?.enableDashboard && atomicState?.dashboardInstalled && atomicState?.dashboardUrl) ? true : false
		def dashDesc = dashAct ? "Dashboard is (Active)\nTurn off to Remove" : "Toggle to Install.."
		input "enableDashboard", "bool", title: "Enable Web Dashboard", submitOnChange: true, defaultValue: false, required: false, description: dashDesc, state: dashAct ? "complete" : null,
				image: getAppImg("dashboard_icon.png")
		if(settings?.enableDashboard) {
			if(!dashAct) {
				atomicState?.dashSetup = true
				initDashboardApp()
			}
		} else {
			removeDashboardApp()
			atomicState?.dashSetup = false
		}
	}
}

def setMyLockId(val) {
	if(atomicState?.myID == null && parent && val) {
		atomicState.myID = val
	}
}

def getMyLockId() {
	if(parent) { return atomicState?.myID } else { return null }
}

def addRemoveVthermostat(tstatdni, tval, myID) {
	def odevId = tstatdni
	LogAction("addRemoveVthermostat() tstat: ${tstatdni}   devid: ${odevId}   tval: ${tval}   myID: ${myID} atomicState.vThermostats: ${atomicState?.vThermostats} ", "trace", true)

	if(parent || !myID || tval == null) { return false }
	def tstat = tstatdni

	def d1 = getChildDevice(odevId.toString())
	if(!d1) {
		LogAction("addRemoveVthermostat Error: Cannot find thermostat device child", "error", true)
		if(tval) { return false }  // if deleting (false), let it try to proceed
	} else {
		tstat = d1
	}

	def devId = "v${odevId}"

	if(atomicState?."vThermostat${devId}" && myID != atomicState?."vThermostatChildAppId${devId}") {
		LogAction("addRemoveVthermostat() not ours to play with ${myID} ${atomicState?."vThermostat${devId}"} ${atomicState?."vThermostatChildAppId${devId}"}", "trace", true)
		//atomicState?."vThermostat${devId}" = false
		//atomicState?."vThermostatChildAppId${devId}" = null
		//atomicState?."vThermostatMirrorId${devId}" = null
		//atomicState?.vThermostats = null
		return false

	} else if(tval && atomicState?."vThermostat${devId}" && myID == atomicState?."vThermostatChildAppId${devId}") {
		LogAction("addRemoveVthermostat() already created with ${myID} ${atomicState?."vThermostat${devId}"} ${atomicState?."vThermostatChildAppId${devId}"}", "trace", true)
		return true

	} else if(!tval && !atomicState?."vThermostat${devId}") {
		LogAction("addRemoveVthermostat() already removed with ${myID} ${atomicState?."vThermostat${devId}"} ${atomicState?."vThermostatChildAppId${devId}"}", "trace", true)
		return true

	} else {
		atomicState."vThermostat${devId}" = tval
		if(tval && !atomicState?."vThermostatChildAppId${devId}") {
			LogAction("addRemoveVthermostat() creating virtual thermostat tracking ${tstat}", "trace", true)
			atomicState."vThermostatChildAppId${devId}" = myID
			atomicState?."vThermostatMirrorId${devId}" = odevId
			def vtlist = atomicState?.vThermostats ?: [:]
			vtlist[devId] = "v${tstat.toString()}"
			atomicState.vThermostats = vtlist
			runIn(10, "updated", [overwrite: true])  // create what is needed

		} else if(!tval && atomicState?."vThermostatChildAppId${devId}") {
			LogAction("addRemoveVthermostat() removing virtual thermostat tracking ${tstat}", "trace", true)
			atomicState."vThermostatChildAppId${devId}" = null
			atomicState?."vThermostatMirrorId${devId}" = null
			def vtlist = atomicState?.vThermostats
			def newlist = [:]
			def vtstat
			vtstat = vtlist.collect { dni ->
				//LogAction("atomicState.vThermostats: ${atomicState.vThermostats}  dni: ${dni}  dni.key: ${dni.key.toString()}  dni.value: ${dni.value.toString()} devId: ${devId}", "debug", true)
				def ttkey = dni.key.toString()
				if(ttkey == devId) { ; /*log.trace "skipping $dni"*/ }
				else { newlist[ttkey] = dni.value }
				return true
			}
			vtlist = newlist
			atomicState.vThermostats = vtlist
			runIn(10, "updated", [overwrite: true])  // delete what is needed
		} else {
			LogAction("addRemoveVthermostat() unexpected operation state ${myID} ${atomicState?."vThermostat${devId}"} ${atomicState?."vThermostatChildAppId${devId}"}", "warn", true)
			return false
		}
		return true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
	sendNotificationEvent("${textAppName()} has been installed...")
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
	sendNotificationEvent("${textAppName()} has updated settings...")
	if(parent) {
		atomicState?.lastUpdatedDt = getDtNow()
	}
}

def uninstalled() {
	log.debug "uninstalled..."
	if(parent) {
		uninstAutomationApp()
	} else {
		uninstManagerApp()
	}
	sendNotificationEvent("${textAppName()} is uninstalled...")
}

def initialize() {
	//log.debug "initialize..."
	if(parent) {
		runIn(23, "initAutoApp", [overwrite: true])
	}
	else {
		initWatchdogApp()
		initManagerApp()
	}
}

def isDashAppInstalled() {
	def dashApp = getChildApps().findAll { it?.getSettingVal("webDashFlag") }
	return (dashApp.size() > 0) ? true : false
}

def dashboardInstalled(val) {
	log.trace "dashboardInstalled($val)"
	atomicState.dashboardInstalled = (val == true) ? true : false
}

def setDashboardUrl(val) {
	log.trace "setDashboardUrl($val)"
	atomicState?.dashboardUrl = val
	atomicState?.dashSetup = false
}

def initManagerApp() {
	setStateVar()
	unschedule()
	unsubscribe()
	atomicState.pollingOn = false
	atomicState.lastChildUpdDt = null // force child update on next poll
	atomicState.lastForcePoll = null
	atomicState.swVersion = appVersion()
	if(addRemoveDevices()) { // if we changed devices, reset queues and polling
		atomicState.cmdQlist = []
	}
	if(settings?.thermostats || settings?.protects || settings?.cameras || settings?.presDevice || settings?.weatherDevice) {
		atomicState?.isInstalled = true
	} else { atomicState.isInstalled = false }
	subscriber()
	setPollingState()
	if(optInAppAnalytics) { runIn(4, "sendInstallData", [overwrite: true]) } //If analytics are enabled this will send non-user identifiable data to firebase server
	runIn(20, "stateCleanup", [overwrite: true])
}

def uninstManagerApp() {
	log.trace "uninstManagerApp"
	try {
		if(addRemoveDevices(true)) {
			//removes analytic data from the server
			if(optInAppAnalytics) {
				if(removeInstallData()) {
					atomicState?.installationId = null
				}
			}
			//Revokes Smartthings endpoint token...
			revokeAccessToken()
			//Revokes Nest Auth Token
			if(atomicState?.authToken) { revokeNestToken() }
			//sends notification of uninstall
			sendNotificationEvent("${textAppName()} is uninstalled...")
		}
	} catch (ex) {
		log.error "uninstManagerApp Exception:", ex
		sendExceptionData(ex.message, "uninstManagerApp")
	}
}

def initWatchdogApp() {
	//log.trace "initWatchdogApp..."
	def watDogApp = getChildApps()?.findAll { it?.getAutomationType() == "watchDog" }
	if(watDogApp?.size() < 1) {
		LogAction("Installing Nest Watchdog App...", "info", true)
		addChildApp(textNamespace(), appName(), getWatchdogAppChildName(), [settings:[watchDogFlag: true]])
	} else if (watDogApp?.size() >= 1) {
		def cnt = 1
		watDogApp?.each { chld ->
			if(cnt == 1) {
				cnt = cnt+1
				//LogAction("Running Update Command on Watchdog...", "warn", true)
				chld.update()
			} else if (cnt > 1) {
				LogAction("Deleting Extra Watchdog Instance(${chld})...", "warn", true)
				deleteChildApp(chld)
			}
		}
	}
}

def initDashboardApp() {
	//log.trace "initDashboardApp"
	def dashApp = getChildApps().findAll { it?.getSettingVal("webDashFlag") }
	if(!dashApp) {
		LogAction("Installing Web Dashboard App...", "info", true)
		addChildApp("tonesto7", getWebDashAppChildName(), getWebDashAppChildName(), [settings:[webDashFlag: true]])
	}
}

def removeDashboardApp() {
	def dashApp = getChildApps().findAll { it?.getSettingVal("webDashFlag") }
	if(dashApp) {
		LogAction("Removing Web Dashboard App...", "warn", true)
		deleteChildApp(dashApp)
		atomicState?.dashboardUrl = null
		atomicState?.dashSetup = false
	}
}

def getChildAppVer(appName) { return appName?.appVersion() ? "v${appName?.appVersion()}" : "" }

def appBtnDesc(val) {
	return atomicState?.automationsActive ? (atomicState?.automationsActiveDesc ? "${atomicState?.automationsActiveDesc}\nTap to Modify..." : "Tap to Modify...") :  "Tap to Install..."
}

def isAutoAppInst() {
	def chldCnt = 0
	childApps?.each { cApp ->
		chldCnt = chldCnt + 1
	}
	return (chldCnt > 0) ? true : false
}

def autoAppInst(Boolean val) {
	log.debug "${getAutoAppChildName()} is Installed?: ${val}"
	atomicState.autoAppInstalled = val
}

def getInstAutoTypesDesc() {
	def nModeCnt = 0
	def schMotCnt = 0
	def watchDogCnt = 0
	def webDashCnt = 0
	def disCnt = 0
	def schedAutoInst
	def remSenCnt = 0
	def fanCtrlCnt = 0
	def fanCircCnt = 0
	def conWatCnt = 0
	def extTmpCnt = 0
	def leakWatCnt = 0
	def tSchedCnt = 0
	childApps?.each { a ->
		def type = a?.getAutomationType()
		if(a?.getIsAutomationDisabled()) { disCnt = disCnt+1 }
		else {
			//log.debug "automation type: $type"
			switch(type) {
				case "nMode":
					nModeCnt = nModeCnt+1
					break
				case "schMot":
					schMotCnt = schMotCnt+1
					def ai = a?.getAutomationsInstalled()
					if(ai) {
						ai?.each { aut ->
							aut?.each { it2 ->
								if(it2?.key == "schMot") {
									log.debug "aut data: ${aut}"
									if("tSched" in it2?.value) { tSchedCnt = tSchedCnt+1}
									if("remSen" in it2?.value) { remSenCnt = remSenCnt+1 }
									if("fanCtrl" in it2?.value) { fanCtrlCnt = fanCtrlCnt+1 }
									if("fanCirc" in it2?.value) { fanCircCnt = fanCircCnt+1 }
									if("conWat" in it2?.value) { conWatCnt = conWatCnt+1 }
									if("extTmp" in it2?.value) { extTmpCnt = extTmpCnt+1 }
									if("leakWat" in it2?.value) { leakWatCnt = leakWatCnt+1 }
								}
							}
						}
					}
					break
				case "watchDog":
					watchDogCnt = watchDogCnt+1
					break
				case "webDash":
					webDashCnt = webDashCnt+1
					break
			}
		}
	}
/*
	TODO I need to add the individual thermostat automation types installed to the analytics
*/
	def inAutoList = []
	inAutoList?.push("nestMode":nModeCnt)
	inAutoList?.push("watchDog":watchDogCnt)
	inAutoList?.push("webDash":webDashCnt)
	if(schMotCnt > 0) {
		inAutoList?.push("schMot":["tSched":tSchedCnt, "remSen":remSenCnt, "fanCtrl":fanCtrlCnt, "fanCirc":fanCircCnt, "conWat":conWatCnt, "extTmp":extTmpCnt, "leakWat":leakWatCnt])
	} else {
		inAutoList?.push("schMot":schMotCnt)
	}
	//log.debug "inAutoList: $inAutoList"
	atomicState?.installedAutomations = inAutoList

	def str = ""
	str += (watchDogCnt > 0 || nModeCnt > 0 || schMotCnt > 0 || disCnt > 0) ? "Installed Automations:" : ""
	str += (watchDogCnt > 0) ? "\n• Watchdog (Active)" : ""
	str += (nModeCnt > 0) ? ((nModeCnt > 1) ? "\n• Nest Home/Away ($nModeCnt)" : "\n• Nest Home/Away (Active)") : ""
	str += (schMotCnt > 0) ? "\n• Thermostat ($schMotCnt)" : ""
	str += (disCnt > 0) ? "\n\nDisabled Automations ($disCnt)" : ""
	return (str != "") ? str : null
}

def subscriber() {
	subscribe(app, onAppTouch)
}

def setPollingState() {
	if(!atomicState?.thermostats && !atomicState?.protects && !atomicState?.weatherDevice && !atomicState?.cameras) {
		LogAction("No Devices Selected...Polling is Off!!!", "info", true)
		unschedule()
		atomicState.pollingOn = false
	} else {
		if(!atomicState?.pollingOn) {
			LogAction("Polling is Now ACTIVE!!!", "info", true)
			atomicState.pollingOn = true
			def pollTime = !settings?.pollValue ? 180 : settings?.pollValue.toInteger()
			def pollStrTime = !settings?.pollStrValue ? 180 : settings?.pollStrValue.toInteger()
			def weatherTimer = pollTime
			if(atomicState?.weatherDevice) { weatherTimer = (settings?.pollWeatherValue ? settings?.pollWeatherValue.toInteger() : 900) }
			def timgcd = gcd([pollTime, pollStrTime, weatherTimer])
			def random = new Random()
			def random_int = random.nextInt(60)
			timgcd = (timgcd.toInteger() / 60) < 1 ? 1 : timgcd.toInteger()/60
			def random_dint = random.nextInt(timgcd.toInteger())
			LogAction("'Poll' scheduled using Cron (${random_int} ${random_dint}/${timgcd} * * * ?)", "info", true)
			schedule("${random_int} ${random_dint}/${timgcd} * * * ?", poll)  // this runs every timgcd minutes
			poll(true)
		}
	}
}

private gcd(a, b) {
	while (b > 0) {
		long temp = b;
		b = a % b;
		a = temp;
	}
	return a;
}

private gcd(input = []) {
	long result = input[0];
	for(int i = 1; i < input.size; i++) result = gcd(result, input[i]);
	return result;
}

def onAppTouch(event) {
	poll(true)
}

def refresh(child = null) {
	def devId = !child?.device?.deviceNetworkId ? child?.toString() : child?.device?.deviceNetworkId.toString()
	LogAction("Refresh Received from Device...${devId}", "debug", true)
	if(childDebug && child) { child?.log("refresh: ${devId}") }
	return sendNestApiCmd(atomicState?.structures, "poll", "poll", 0, devId)
}

/************************************************************************************************
|								API/Device Polling Methods										|
*************************************************************************************************/

def pollFollow() { if(isPollAllowed()) { poll() } }

def pollWatcher(evt) {
	if(isPollAllowed() && (ok2PollDevice() || ok2PollStruct())) { poll() }
}

def checkIfSwupdated() {
	if(atomicState?.swVersion != appVersion()) {
		def cApps = getChildApps()
		if(cApps) {
			cApps?.sort()?.each { chld ->
				chld?.update()
			}
		}
		updated()
	}
}

def poll(force = false, type = null) {
	if(isPollAllowed()) {
		//unschedule("postCmd")
		checkIfSwupdated()
		def dev = false
		def str = false
		if(force == true) { forcedPoll(type) }
		if( !force && !ok2PollDevice() && !ok2PollStruct() ) {
			LogAction("No Device or Structure poll - Devices Last Updated: ${getLastDevicePollSec()} seconds ago... | Structures Last Updated ${getLastStructPollSec()} seconds ago...", "info", true)
		}
		else if(!force) {
			if(ok2PollStruct()) {
				LogAction("Updating Structure Data...(Last Updated: ${getLastStructPollSec()} seconds ago)", "info", true)
				str = queueGetApiData("str")
				//str = getApiData("str")
			}
			if(ok2PollDevice()) {
				LogAction("Updating Device Data...(Last Updated: ${getLastDevicePollSec()} seconds ago)", "info", true)
				dev = queueGetApiData("dev")
				//dev = getApiData("dev")
			}
			return
		}
		finishPoll(str, dev)
	}
}

def finishPoll(str, dev) {
	LogAction("finishPoll($str, $dev) received...", "info", false)
	if(atomicState?.pollBlocked) { schedNextWorkQ(null); return }
	if(dev || str || atomicState?.needChildUpd ) { updateChildData() }
	updateWebStuff()
	notificationCheck() //Checks if a notification needs to be sent for a specific event
}

def forcedPoll(type = null) {
	LogAction("forcedPoll($type) received...", "warn", true)
	def lastFrcdPoll = getLastForcedPollSec()
	def pollWaitVal = !settings?.pollWaitVal ? 10 : settings?.pollWaitVal.toInteger()
	if(lastFrcdPoll > pollWaitVal) { // This limits manual forces to 10 seconds or more
		atomicState?.lastForcePoll = getDtNow()
		atomicState?.pollBlocked = false
		LogAction("Forcing Data Update... Last Forced Update was ${lastFrcdPoll} seconds ago.", "info", true)
		if(type == "dev" || !type) {
			LogAction("Forcing Update of Device Data...", "info", true)
			getApiData("dev")
		}
		if(type == "str" || !type) {
			LogAction("Forcing Update of Structure Data...", "info", true)
			getApiData("str")
		}
		atomicState?.lastWebUpdDt = null
		atomicState?.lastWeatherUpdDt = null
		atomicState?.lastForecastUpdDt = null
		schedNextWorkQ(null)
	} else {
		LogAction("Too Soon to Force Data Update!!!!  It's only been (${lastFrcdPoll}) seconds of the minimum (${settings?.pollWaitVal})...", "debug", true)
		atomicState.needStrPoll = true
		atomicState.needDevPoll = true
	}
	updateChildData(true)
}

def postCmd() {
	//log.trace "postCmd()"
	poll()
}

def getApiData(type = null) {
	//log.trace "getApiData($type)"
	LogAction("getApiData($type)", "info", false)
	def result = false
	if(!type) { return result }

	def tPath = (type == "str") ? "/structures" : "/devices"
	try {
		def params = [
			uri: getNestApiUrl(),
			path: "$tPath",
			headers: ["Content-Type": "text/json", "Authorization": "Bearer ${atomicState?.authToken}"]
		]
		if(type == "str") {
			httpGet(params) { resp ->
				if(resp?.status == 200) {
					atomicState?.lastStrucDataUpd = getDtNow()
					atomicState.needStrPoll = false
					LogTrace("API Structure Resp.Data: ${resp?.data}")
					apiIssueEvent(false)
					if(!resp?.data?.equals(atomicState?.structData) || !atomicState?.structData) {
						LogAction("API Structure Data HAS Changed... Updating State data...", "debug", true)
						atomicState?.structData = resp?.data
						atomicState.needChildUpd = true
						result = true
					}
				} else {
					LogAction("getApiStructureData - Received a diffent Response than expected: Resp (${resp?.status})", "error", true)
				}
			}
		}
		else if(type == "dev") {
			httpGet(params) { resp ->
				if(resp?.status == 200) {
					atomicState?.lastDevDataUpd = getDtNow()
					atomicState?.needDevPoll = false
					LogTrace("API Device Resp.Data: ${resp?.data}")
					apiIssueEvent(false)
					if(!resp?.data.equals(atomicState?.deviceData) || !atomicState?.deviceData) {
						LogAction("API Device Data HAS Changed... Updating State data...", "debug", true)
						atomicState?.deviceData = resp?.data
						result = true
					}
				} else {
					LogAction("getApiDeviceData - Received a diffent Response than expected: Resp (${resp?.status})", "error", true)
				}
			}
		}
	}
	catch(ex) {
		apiIssueEvent(true)
		atomicState.needChildUpd = true
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			if(ex.message.contains("Too Many Requests")) {
				log.warn "Received '${ex.message}' response code..."
			}
		} else {
			log.error "getApiData (type: $type) Exception:", ex
			if(type == "str") { atomicState.needStrPoll = true }
			else if(type == "dev") { atomicState?.needDevPoll = true }
			sendExceptionData(ex.message, "getApiData")
		}
	}
	return result
}

def queueGetApiData(type = null, newUrl = null) {
	//log.trace "queueGetApiData($type)"
	LogAction("queueGetApiData($type,$newUrl)", "info", false)
	def result = false
	if(!type) { return result }

	def tPath = (type == "str") ? "/structures" : "/devices"
	try {
		def theUrl = newUrl ?: getNestApiUrl()
		def params = [
			uri: theUrl,
			path: "$tPath",
			headers: ["Content-Type": "text/json", "Authorization": "Bearer ${atomicState?.authToken}"]
		]
		if(type == "str") {
			atomicState.qstrRequested = true
			asynchttp_v1.get(processResponse, params, [ type: "str"])
			result = true
		}
		else if(type == "dev") {
			atomicState.qdevRequested = true
			asynchttp_v1.get(processResponse, params, [ type: "dev"])
			result = true
		}
	} catch(ex) {
		log.error "queueGetApiData (type: $type) Exception:", ex
		sendExceptionData(ex.message, "queueGetApiData")
	}
	return result
}

def processResponse(resp, data) {
	LogAction("processResponse(${data?.type})", "info", false)
	def str = false
	def dev = false
	def type = data?.type

	try {
		if(!type) { return }

		if(resp?.status == 307) {
			//log.trace "resp: ${resp.headers}"
			def newUrl = resp?.headers?.Location?.split("\\?")
			//LogTrace("NewUrl: ${newUrl[0]}")
			queueGetApiData(type, newUrl[0])
			return
		}

		if(resp?.status == 200) {
			apiIssueEvent(false)
			if(type == "str") {
				atomicState?.lastStrucDataUpd = getDtNow()
				atomicState.needStrPoll = false
				LogTrace("API Structure Resp.Data: ${resp?.json}")
				//log.trace "API Structure Resp.Data: ${resp?.json}"
				if(!resp?.json?.equals(atomicState?.structData) || !atomicState?.structData) {
					LogAction("API Structure Data HAS Changed... Updating State data...", "debug", true)
					atomicState?.structData = resp?.json
					atomicState.needChildUpd = true
					str = true
				}
				atomicState.qstrRequested = false
			}
			if(type == "dev") {
				atomicState?.lastDevDataUpd = getDtNow()
				atomicState?.needDevPoll = false
				LogTrace("API Device Resp.Data: ${resp?.json}")
				//log.trace "API Device Resp.Data: ${resp?.json}"
				if(!resp?.json?.equals(atomicState?.deviceData) || !atomicState?.deviceData) {
					LogAction("API Device Data HAS Changed... Updating State data...", "debug", true)
					atomicState?.deviceData = resp?.json
					dev = true
				}
				atomicState.qdevRequested = false
			}
		} else {
			def tstr = (type == "str") ? "Structure" : "Device"
			LogAction("processResponse - Received a different Response than expected for $tstr poll: Resp (${resp?.status})", "error", true)
			if(resp.hasError()) {
				log.debug "raw response: $resp.errorData"
			}
			apiIssueEvent(true)
			atomicState.needChildUpd = true
			atomicState.qstrRequested = false
			atomicState.qdevRequested = false
		}
		if((atomicState?.qdevRequested == false && atomicState?.qstrRequested == false) && (dev || atomicState?.needChildUpd)) { finishPoll(true, true) }

	} catch (e) {
		apiIssueEvent(true)
		atomicState.needChildUpd = true
		atomicState.qstrRequested = false
		atomicState.qdevRequested = false
		log.error "processResponse (type: $type) Exception:", e
		if(type == "str") { atomicState.needStrPoll = true }
		else if(type == "dev") { atomicState?.needDevPoll = true }
		sendExceptionData(ex.message, "processResponse_${type}")
	}
}

def schedUpdateChild() {
	runIn(25, "updateChildData", [overwrite: true])
}

def generateMD5_A(String s) {
	MessageDigest digest = MessageDigest.getInstance("MD5")
	digest.update(s.bytes)
	return digest.digest().toString()
}

def minDevVer2Str(val) {
	def str = ""
	def pCnt = 0
	def list = []
	str += "v"
	val?.each {
		list.add(it)
		//str += "${it}"
	}
	log.debug "list: $list"
}

def updateChildData(force = false) {
	LogAction("updateChildData()", "info", true)
	if(atomicState?.pollBlocked) { return }
	def nforce = atomicState?.needChildUpd
	atomicState.needChildUpd = true
	//log.warn "force: $force   nforce: $nforce"
	//unschedule("schedUpdateChild")
	//runIn(40, "postCmd", [overwrite: true])
	try {
		atomicState?.lastChildUpdDt = getDtNow()
		def useMt = !useMilitaryTime ? false : true
		def dbg = !childDebug ? false : true
		def nestTz = getNestTimeZone()?.toString()
		def api = !apiIssues() ? false : true
		def htmlInfo = getHtmlInfo()
		def allowDbException = allowDbException()
		getAllChildDevices()?.each {
			def devId = it?.deviceNetworkId
			if(atomicState?.thermostats && atomicState?.deviceData?.thermostats[devId]) {
				def defmin = atomicState?."${physdevId}_safety_temp_min" ?: 0.0
				def defmax = atomicState?."${physdevId}_safety_temp_max" ?: 0.0
				def safetyTemps = [ "min":defmin, "max":defmax ]

				def comfortDewpoint = settings?."${devId}_comfort_dewpoint_max" ?: 0.0
				if(comfortDewpoint == 0) {
					comfortDewpoint = settings?.locDesiredComfortDewpointMax ?: 0.0
				}

				def comfortHumidity = settings?."${devId}_comfort_humidity_max" ?: 80
				def tData = ["data":atomicState?.deviceData?.thermostats[devId], "mt":useMt, "debug":dbg, "tz":nestTz, "apiIssues":api, "safetyTemps":safetyTemps, "comfortHumidity":comfortHumidity,
						"comfortDewpoint":comfortDewpoint, "pres":locationPresence(), "childWaitVal":getChildWaitVal().toInteger(), "htmlInfo":htmlInfo, "allowDbException":allowDbException,
						"latestVer":latestTstatVer()?.ver?.toString()]
				def oldTstatData = atomicState?."oldTstatData${devId}"
				def tDataChecksum = generateMD5_A(tData.toString())
				atomicState."oldTstatData${devId}" = tDataChecksum
				tDataChecksum = atomicState."oldTstatData${devId}"
				if(force || nforce || (oldTstatData != tDataChecksum)) {
					atomicState?.tDevVer = it?.devVer() ?: ""
					if(!atomicState?.tDevVer || (versionStr2Int(atomicState?.tDevVer) >= minDevVersions()?.thermostat?.val)) {
						LogTrace("UpdateChildData >> Thermostat id: ${devId} | data: ${tData}")
						//log.warn "oldTstatData: ${oldTstatData} tDataChecksum: ${tDataChecksum} force: $force  nforce: $nforce"
						it.generateEvent(tData)
					} else {
						LogAction("VERSION RESTRICTION: Your Thermostat Device Version (v${atomicState?.tDevVer}) is lower than the Minimum (v${minDevVersions()?.thermostat?.desc}) Required... Please Update the Device Code to latest version to resume operation!!!", "error", true)
						return false
					}
				}
				return true
			}
			else if(!atomicState?.pDevVer || (atomicState?.protects && atomicState?.deviceData?.smoke_co_alarms[devId])) {
				def pData = ["data":atomicState?.deviceData?.smoke_co_alarms[devId], "mt":useMt, "debug":dbg, "showProtActEvts":(!showProtActEvts ? false : true),
						"tz":nestTz, "htmlInfo":htmlInfo, "apiIssues":api, "allowDbException":allowDbException, "latestVer":latestProtVer()?.ver?.toString()]
				def oldProtData = atomicState?."oldProtData${devId}"
				def pDataChecksum = generateMD5_A(pData.toString())
				atomicState."oldProtData${devId}" = pDataChecksum
				pDataChecksum = atomicState."oldProtData${devId}"
				if(force || nforce || (oldProtData != pDataChecksum)) {
					atomicState?.pDevVer = it?.devVer() ?: ""
					if(!atomicState?.pDevVer || (versionStr2Int(atomicState?.pDevVer) >= minDevVersions()?.protect?.val)) {
						LogTrace("UpdateChildData >> Protect id: ${devId} | data: ${pData}")
						//log.warn "oldProtData: ${oldProtData} pDataChecksum: ${pDataChecksum} force: $force  nforce: $nforce"
						it.generateEvent(pData)
					} else {
						LogAction("VERSION RESTRICTION: Your Protect Device Version (v${atomicState?.pDevVer}) is lower than the Minimum of (v${minDevVersions()?.protect?.desc}) | Please Update the Device Code to latest version to resume operation!!!", "error", true)
						return false
					}
				}
				return true
			}
			else if(atomicState?.cameras && atomicState?.deviceData?.cameras[devId]) {
				def camData = ["data":atomicState?.deviceData?.cameras[devId], "mt":useMt, "debug":dbg,
						"tz":nestTz, "htmlInfo":htmlInfo, "apiIssues":api, "allowDbException":allowDbException, "latestVer":latestCamVer()?.ver?.toString()]
				def oldCamData = atomicState?."oldCamData${devId}"
				def cDataChecksum = generateMD5_A(camData.toString())
				if(force || nforce || (oldCamData != cDataChecksum)) {
					atomicState?.camDevVer = it?.devVer() ?: ""
					if(!atomicState?.camDevVer || (versionStr2Int(atomicState?.camDevVer) >= minDevVersions()?.camera?.val)) {
						LogTrace("UpdateChildData >> Camera id: ${devId} | data: ${camData}")
						it.generateEvent(camData)
						atomicState."oldCamData${devId}" = cDataChecksum
					} else {
						LogAction("VERSION RESTRICTION: Your Camera Device Version (v${atomicState?.camDevVer}) is lower than the Minimum of (v${minDevVersions()?.camera?.desc}) | Please Update the Device Code to latest version to resume operation!!!", "error", true)
						return false
					}
				}
				return true
			}
			else if(atomicState?.presDevice && devId == getNestPresId()) {
				def pData = ["debug":dbg, "tz":nestTz, "mt":useMt, "pres":locationPresence(), "apiIssues":api, "allowDbException":allowDbException, "latestVer":latestPresVer()?.ver?.toString()]
				def oldPresData = atomicState?."oldPresData${devId}"
				def pDataChecksum = generateMD5_A(pData.toString())
				atomicState."oldPresData${devId}" = pDataChecksum
				pDataChecksum = atomicState."oldPresData${devId}"
				if(force || nforce || (oldPresData != pDataChecksum)) {
					atomicState?.presDevVer = it?.devVer() ?: ""
					if(!atomicState?.presDevVer || (versionStr2Int(atomicState?.presDevVer) >= minDevVersions()?.presence?.val)) {
						LogTrace("UpdateChildData >> Presence id: ${devId}")
						//log.warn "oldPresData: ${oldPresData} pDataChecksum: ${pDataChecksum} force: $force  nforce: $nforce"
						it.generateEvent(pData)
					} else {
						LogAction("VERSION RESTRICTION: Your Presence Device Version (v${atomicState?.presDevVer}) is lower than the Minimum of (v${minDevVersions()?.presence?.desc}) | Please Update the Device Code to latest version to resume operation!!!", "error", true)
						return false
					}
				}
				return true
			}
			else if(atomicState?.weatherDevice && devId == getNestWeatherId()) {
				def wData = ["weatCond":getWData(), "weatForecast":getWForecastData(), "weatAstronomy":getWAstronomyData(), "weatAlerts":getWAlertsData()]
				def oldWeatherData = atomicState?."oldWeatherData${devId}"
				def wDataChecksum = generateMD5_A(wData.toString())
				atomicState."oldWeatherData${devId}" = wDataChecksum
				wDataChecksum = atomicState."oldWeatherData${devId}"
				if(force || nforce || (oldWeatherData != wDataChecksum)) {
					atomicState?.weatDevVer = it?.devVer() ?: ""
					if(!atomicState?.weatDevVer || (versionStr2Int(atomicState?.weatDevVer) >= minDevVersions()?.weather?.val)) {
						//log.warn "oldWeatherData: ${oldWeatherData} wDataChecksum: ${wDataChecksum} force: $force  nforce: $nforce"
						LogTrace("UpdateChildData >> Weather id: ${devId}")
						it.generateEvent(["data":wData, "tz":nestTz, "mt":useMt, "debug":dbg, "apiIssues":api, "htmlInfo":htmlInfo, "allowDbException":allowDbException, "weathAlertNotif":weathAlertNotif, "latestVer":latestWeathVer()?.ver?.toString()])
					} else {
						LogAction("VERSION RESTRICTION: Your Weather Device Version (v${atomicState?.weatDevVer}) is lower than the Required Minimum (v${minDevVersions()?.weather?.desc}) | Please Update the Device Code to latest version to resume operation!!!", "error", true)
						return false
					}
				}
				return true
			}

			else if(atomicState?.vThermostats && atomicState?."vThermostat${devId}") {
				def physdevId = atomicState?."vThermostatMirrorId${devId}"

				if(atomicState?.thermostats && atomicState?.deviceData?.thermostats[physdevId]) {
					def data = atomicState?.deviceData?.thermostats[physdevId]
					def defmin = atomicState?."${physdevId}_safety_temp_min" ?: 0.0
					def defmax = atomicState?."${physdevId}_safety_temp_max" ?: 0.0
					def safetyTemps = [ "min":defmin, "max":defmax ]

					def comfortDewpoint = settings?."${physdevId}_comfort_dewpoint_max" ?: 0.0
					if(comfortDewpoint == 0) {
						comfortDewpoint = settings?.locDesiredComfortDewpointMax ?: 0.0
					}
					def comfortHumidity = settings?."${physdevId}_comfort_humidity_max" ?: 80

					def automationChildApp = getChildApps().find{ it.id == atomicState?."vThermostatChildAppId${devId}" }

					if(automationChildApp.getRemoteSenAutomationEnabled()) {
						def tempC = 0.0
						def tempF = 0
						if(getTemperatureScale() == "C") {
							tempC = automationChildApp.getRemoteSenTemp()
							tempF = (tempC * 9/5 + 32) as Integer
						} else {
							tempF = automationChildApp.getRemoteSenTemp()
							tempC = (tempF - 32) * 5/9 as Double
						}
						data?.ambient_temperature_c = tempC
						data?.ambient_temperature_f = tempF

						def ctempC = 0.0
						def ctempF = 0
						if(getTemperatureScale() == "C") {
							ctempC = automationChildApp.getRemSenCoolSetTemp()
							ctempF = (ctempC * 9/5 + 32.0) as Integer
						} else {
							ctempF = automationChildApp.getRemSenCoolSetTemp()
							ctempC = (ctempF - 32.0) * 5/9 as Double
						}

						def htempC = 0.0
						def htempF = 0
						if(getTemperatureScale() == "C") {
							htempC = automationChildApp.getRemSenHeatSetTemp()
							htempF = (htempC * 9/5 + 32.0) as Integer
						} else {
							htempF = automationChildApp.getRemSenHeatSetTemp()
							htempC = (htempF - 32.0) * 5/9 as Double
						}

						if(data?.hvac_mode.toString() == "heat-cool") {
							data?.target_temperature_high_f = ctempF
							data?.target_temperature_low_f = htempF
							data?.target_temperature_high_c = ctempC
							data?.target_temperature_low_c = htempC
						} else if(data?.hvac_mode.toString() == "cool") {
							data?.target_temperature_f = ctempF
							data?.target_temperature_c = ctempC
						} else if(data?.hvac_mode.toString() == "heat") {
							data?.target_temperature_f = htempF
							data?.target_temperature_c = htempC
						}
					}

					def tData = ["data":data, "mt":useMt, "debug":dbg, "tz":nestTz, "apiIssues":api, "safetyTemps":safetyTemps, "comfortHumidity":comfortHumidity,
						"comfortDewpoint":comfortDewpoint, "pres":locationPresence(), "childWaitVal":getChildWaitVal().toInteger(), "htmlInfo":htmlInfo, "allowDbException":allowDbException,
						"latestVer":latestvStatVer()?.ver?.toString()]
					def oldTstatData = atomicState?."oldvStatData${devId}"
					def tDataChecksum = generateMD5_A(tData.toString())
					atomicState."oldvStatData${devId}" = tDataChecksum
					tDataChecksum = atomicState."oldvStatData${devId}"
					if(force || nforce || (oldTstatData != tDataChecksum)) {
						atomicState?.vtDevVer = it?.devVer() ?: ""
						if(!atomicState?.vtDevVer || (versionStr2Int(atomicState?.vtDevVer) >= minDevVersions()?.vthermostat?.val)) {
							LogTrace("UpdateChildData >> vThermostat id: ${devId} | data: ${tData}")
							//log.warn "oldvStatData: ${oldvStatData} tDataChecksum: ${tDataChecksum} force: $force  nforce: $nforce"
							it.generateEvent(tData)
						} else {
							LogAction("VERSION RESTRICTION: Your vThermostat Device Version (v${atomicState?.vtDevVer}) is lower than the Minimum of (v${minDevVersions()?.vthermostat?.desc}) | Please Update the Device Code to latest version to resume operation!!!", "error", true)
							return false
						}
					}
					return true
				}
			}

			else if(devId == getNestPresId()) {
				return true
			}
			else if(devId == getNestWeatherId()) {
				return true
			}
/*  This causes NP exceptions depending if child has not finished being deleted or if items are removed from Nest
			else if(!atomicState?.deviceData?.thermostats[devId] && !atomicState?.deviceData?.smoke_co_alarms[devId] && !atomicState?.deviceData?.cameras[devId]) {
				LogAction("Device found ${devId} and connection removed", "warn", true)
				return null
			}
*/
			else {
				LogAction("updateChildData() Device ${devId} found without claimed configuration", "warn", true)
				return true
			}
		}
		atomicState.needChildUpd = false
	}
	catch (ex) {
		log.error "updateChildData Exception:", ex
		sendExceptionData(ex.message, "updateChildData")
		atomicState?.lastChildUpdDt = null
		return
	}
	//unschedule("postCmd")
	atomicState.needChildUpd = false
}

def locationPresence() {
	if(atomicState?.structData[atomicState?.structures]) {
		def data = atomicState?.structData[atomicState?.structures]
		LogAction("Location Presence: ${data?.away}", "debug", false)
		return data?.away.toString()
	}
	else { return null }
}

def apiIssues() {
	def result = state?.apiIssuesList.toString().contains("true") ? true : false
	if(result) {
		LogAction("Nest API Issues Detected... (${getDtNow()})", "warn", true)
	}
	return result
}

def apiIssueEvent(issue, cmd = null) {
	def list = state?.apiIssuesList ?: []
	//log.debug "listIn: $list (${list?.size()})"
	def listSize = 3
	if(list?.size() < listSize) {
		list.push(issue)
	}
	else if(list?.size() > listSize) {
		def nSz = (list?.size()-listSize) + 1
		//log.debug ">listSize: ($nSz)"
		def nList = list?.drop(nSz)
		//log.debug "nListIn: $list"
		nList?.push(issue)
		//log.debug "nListOut: $nList"
		list = nList
	}
	else if(list?.size() == listSize) {
		def nList = list?.drop(1)
		nList?.push(issue)
		list = nList
	}

	if(list) { state?.apiIssuesList = list }
	//log.debug "listOut: $list"
}

def ok2PollDevice() {
	if(atomicState?.pollBlocked) { return false }
	if(atomicState?.needDevPoll) { return true }
	def pollTime = !settings?.pollValue ? 180 : settings?.pollValue.toInteger()
	def val = pollTime/3
	if(val > 60) { val = 50 }
	return ( ((getLastDevicePollSec() + val) > pollTime) ? true : false )
}

def ok2PollStruct() {
	if(atomicState?.pollBlocked) { return false }
	if(atomicState?.needStrPoll) { return true }
	def pollStrTime = !settings?.pollStrValue ? 180 : settings?.pollStrValue.toInteger()
	def val = pollStrTime/3
	if(val > 60) { val = 50 }
	return ( ((getLastStructPollSec() + val) > pollStrTime || !atomicState?.structData) ? true : false )
}


def isPollAllowed() { return (atomicState?.pollingOn && (atomicState?.thermostats || atomicState?.protects || atomicState?.weatherDevice || atomicState?.cameras)) ? true : false }
def getLastDevicePollSec() { return !atomicState?.lastDevDataUpd ? 840 : GetTimeDiffSeconds(atomicState?.lastDevDataUpd).toInteger() }
def getLastStructPollSec() { return !atomicState?.lastStrucDataUpd ? 1000 : GetTimeDiffSeconds(atomicState?.lastStrucDataUpd).toInteger() }
def getLastForcedPollSec() { return !atomicState?.lastForcePoll ? 1000 : GetTimeDiffSeconds(atomicState?.lastForcePoll).toInteger() }
def getLastChildUpdSec() { return !atomicState?.lastChildUpdDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastChildUpdDt).toInteger() }

/************************************************************************************************
|										Nest API Commands										|
*************************************************************************************************/

private cmdProcState(Boolean value) { atomicState?.cmdIsProc = value }
private cmdIsProc() { return !atomicState?.cmdIsProc ? false : true }
private getLastProcSeconds() { return atomicState?.cmdLastProcDt ? GetTimeDiffSeconds(atomicState?.cmdLastProcDt) : 0 }

def apiVar() {
	def api = [
		rootTypes:	[ struct:"structures", cos:"devices/smoke_co_alarms", tstat:"devices/thermostats", cam:"devices/cameras", meta:"metadata" ],
		cmdObjs:	[ targetF:"target_temperature_f", targetC:"target_temperature_c", targetLowF:"target_temperature_low_f", setLabel:"label",
					  targetLowC:"target_temperature_low_c", targetHighF:"target_temperature_high_f", targetHighC:"target_temperature_high_c",
					  fanActive:"fan_timer_active", fanTimer:"fan_timer_timeout", hvacMode:"hvac_mode", away:"away", streaming:"is_streaming" ],
		hvacModes: 	[ heat:"heat", cool:"cool", heatCool:"heat-cool", off:"off" ]
	]
	return api
}

def setCamStreaming(child, streamOn) {
	def devId = !child?.device?.deviceNetworkId ? child?.toString() : child?.device?.deviceNetworkId.toString()
	def val = streamOn.toBoolean() ? true : false
	LogAction("Nest Manager(setCamStreaming) - Setting Camera${!devId ? "" : " ${devId}"} Streaming to: (${val ? "On" : "Off"})", "debug", true)
	if(childDebug && child) { child?.log("setCamStreaming( devId: ${devId}, StreamOn: ${val})") }
	return sendNestApiCmd(devId, apiVar().rootTypes.cam, apiVar().cmdObjs.streaming, val, devId)
}

def setStructureAway(child, value, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def val = value?.toBoolean()
	def virt = virtual.toBoolean()

	if(virt && atomicState?.vThermostats && devId) {
		if(atomicState?."vThermostat${devId}") {
			def pdevId = atomicState?."vThermostatMirrorId${devId}"
			def pChild
			if(pdevId) { pChild = getChildDevice(pdevId) }

			if(pChild) {
				if(val) {
					pChild.away()
				} else {
					pChild.home()
				}
			} else { LogAction("setStructureAway - CANNOT Set Thermostat${pdevId} Presence to: (${val}) with child ${pChild}", "warn", true) }
		}
	} else {
		LogAction("setStructureAway - Setting Nest Location:${!devId ? "" : " ${devId}"} (${val ? "Away" : "Home"})", "debug", true)
		if(childDebug && child) { child?.log("setStructureAway: ${devId} | (${val})") }
		if(val) {
			return sendNestApiCmd(atomicState?.structures, apiVar().rootTypes.struct, apiVar().cmdObjs.away, "away", devId)
		}
		else {
			return sendNestApiCmd(atomicState?.structures, apiVar().rootTypes.struct, apiVar().cmdObjs.away, "home", devId)
		}
	}
}

def setTstatLabel(child, label, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def val = label
	def virt = virtual.toBoolean()

//  This is not used anywhere.  A command to set label is not available in the dth for a callback

	LogAction("Nest Manager(setTstatLabel) - Setting Thermostat${!devId ? "" : " ${devId}"} Label to: (${val ? "On" : "Auto"})", "debug", true)
	if(childDebug && child) { child?.log("setTstatLabel( devId: ${devId}, newLabel: ${val})") }
	return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.setLabel, val, devId)
}

def setFanMode(child, fanOn, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def val = fanOn.toBoolean()
	def virt = virtual.toBoolean()

	if(virt && atomicState?.vThermostats && devId) {
		if(atomicState?."vThermostat${devId}") {
			def pdevId = atomicState?."vThermostatMirrorId${devId}"
			def pChild
			if(pdevId) { pChild = getChildDevice(pdevId) }

			if(pChild) {
				if(val) {
					pChild.fanOn()
				} else {
					pChild.fanAuto()
				}
			} else { LogAction("setFanMode - CANNOT Set Thermostat${pdevId} FanMode to: (${fanOn}) with child ${pChild}", "warn", true) }
		}
	} else {
		LogAction("setFanMode - Setting Thermostat${!devId ? "" : " ${devId}"} Fan Mode to: (${val ? "On" : "Auto"})", "debug", true)
		if(childDebug && child) { child?.log("setFanMode( devId: ${devId}, fanOn: ${val})") }
		return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.fanActive, val, devId)
	}
}

def setHvacMode(child, mode, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def virt = virtual.toBoolean()

	if(virt && atomicState?.vThermostats && devId) {
		if(atomicState?."vThermostat${devId}") {
			def pdevId = atomicState?."vThermostatMirrorId${devId}"
			def pChild
			if(pdevId) { pChild = getChildDevice(pdevId) }

			if(pChild) {
				switch (mode) {
					case "auto":
						pChild.auto()
						break
					case "heat":
						pChild.heat()
						break
					case "cool":
						pChild.cool()
						break
					case "off":
						pChild.off()
						break
					case "emergency heat":
						pChild.emergencyHeat()
						break
					default:
						LogAction("setHvacMode Received an Invalid Request: ${mode}", "warn", true)
						break
				}
			} else { LogAction("setHvacMode - CANNOT Set Thermostat${pdevId} Mode to: (${mode}) with child ${pChild}", "warn", true) }
		}
	} else {
		LogAction("setHvacMode - Setting Thermostat${!devId ? "" : " ${devId}"} Mode to: (${mode})", "debug", true)
		if(childDebug && child) { child?.log("setHvacMode( devId: ${devId}, mode: ${mode})") }
		return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.hvacMode, mode.toString(), devId)
	}
}

def setTargetTemp(child, unit, temp, mode, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def virt = virtual.toBoolean()

	if(virt && atomicState?.vThermostats && devId) {
		if(atomicState?."vThermostat${devId}") {
			def pdevId = atomicState?."vThermostatMirrorId${devId}"
			def pChild
			if(pdevId) { pChild = getChildDevice(pdevId) }
			def appId = atomicState?."vThermostatChildAppId${devId}"
			def automationChildApp
			if(appId) { automationChildApp = getChildApps().find{ it.id == appId } }
			if(automationChildApp) {
				def res = automationChildApp.remSenTempUpdate(temp,mode)
				if(res) { return }
			}
			if(pChild) {
				if(mode == 'cool') {
					pChild.setCoolingSetpoint(temp)
				} else if(mode == 'heat') {
					pChild.setHeatingSetpoint(temp)
				} else { LogAction("setTargetTemp - UNKNOWN MODE (${mode}) with child ${pChild}", "warn", true) }
			} else { LogAction("setTargetTemp - CANNOT Set Thermostat${pdevId} Temp to: (${temp})${unit} Mode: (${mode}) with child ${pChild}", "warn", true) }
		}
	} else {
		LogAction("setTargetTemp: ${devId} | (${temp})${unit} | virtual ${virtual}", "debug", true)
		if(childDebug && child) { child?.log("setTargetTemp: ${devId} | (${temp})${unit}") }
		if(unit == "C") {
			return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.targetC, temp, devId)
		}
		else {
			return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.targetF, temp, devId)
		}
	}
}

def setTargetTempLow(child, unit, temp, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def virt = virtual.toBoolean()

	if(virt && atomicState?.vThermostats && devId) {
		if(atomicState?."vThermostat${devId}") {
			def pdevId = atomicState?."vThermostatMirrorId${devId}"
			def pChild
			if(pdevId) { pChild = getChildDevice(pdevId) }

			def appId = atomicState?."vThermostatChildAppId${devId}"
			def automationChildApp
			if(appId) { automationChildApp = getChildApps().find{ it.id == appId } }

			if(automationChildApp) {
				def res = automationChildApp.remSenTempUpdate(temp,"heat")
				if(res) { return }
			}

			if(pChild) {
					pChild.setHeatingSetpoint(temp)
			} else { LogAction("setTargetTemp - CANNOT Set Thermostat${pdevId} HEAT Temp to: (${temp})${unit} with child ${pChild}", "warn", true) }
		}
	} else {
		LogAction("setTargetTempLow: ${devId} | (${temp})${unit} | virtual ${virtual}", "debug", true)
		if(childDebug && child) { child?.log("setTargetTempLow: ${devId} | (${temp})${unit}") }
		if(unit == "C") {
			return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.targetLowC, temp, devId)
		}
		else {
			return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.targetLowF, temp, devId)
		}
	}
}

def setTargetTempHigh(child, unit, temp, virtual=false) {
	def devId = !child?.device?.deviceNetworkId ? null : child?.device?.deviceNetworkId.toString()
	def virt = virtual.toBoolean()

	if(virt && atomicState?.vThermostats && devId) {
		if(atomicState?."vThermostat${devId}") {
			def pdevId = atomicState?."vThermostatMirrorId${devId}"
			def pChild
			if(pdevId) { pChild = getChildDevice(pdevId) }

			def appId = atomicState?."vThermostatChildAppId${devId}"
			def automationChildApp
			if(appId) { automationChildApp = getChildApps().find{ it.id == appId } }

			if(automationChildApp) {
				def res = automationChildApp.remSenTempUpdate(temp,"cool")
				if(res) { return }
			}

			if(pChild) {
				pChild.setCoolingSetpoint(temp)
			} else { LogAction("setTargetTemp - CANNOT Set Thermostat${pdevId} COOL Temp to: (${temp})${unit} with child ${pChild}", "warn", true) }
		}
	} else {
		LogAction("setTargetTempHigh: ${devId} | (${temp})${unit} | virtual ${virtual}", "debug", true)
		if(childDebug && child) { child?.log("setTargetTempHigh: ${devId} | (${temp})${unit}") }
		if(unit == "C") {
			return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.targetHighC, temp, devId)
		}
		else {
			return sendNestApiCmd(devId, apiVar().rootTypes.tstat, apiVar().cmdObjs.targetHighF, temp, devId)
		}
	}
}

def sendNestApiCmd(cmdTypeId, cmdType, cmdObj, cmdObjVal, childId) {
	def childDev = getChildDevice(childId)
	if(childDebug && childDev) { childDev?.log("sendNestApiCmd... $cmdTypeId, $cmdType, $cmdObj, $cmdObjVal, $childId") }
	try {
		if(cmdTypeId) {
			def qnum = getQueueNumber(cmdTypeId, childId)
			if(qnum == -1 ) { return false }

			if(!atomicState?."cmdQ${qnum}" ) { atomicState."cmdQ${qnum}" = [] }
			def cmdQueue = atomicState?."cmdQ${qnum}"
			def cmdData = [cmdTypeId?.toString(), cmdType?.toString(), cmdObj?.toString(), cmdObjVal]

			if(cmdQueue?.contains(cmdData)) {
				LogAction("Command Exists in queue... Skipping...", "warn", true)
				if(childDev) { childDev?.log("Command Exists in queue ${qnum}... Skipping...", "warn") }
				schedNextWorkQ(childId)
			} else {
				LogAction("Adding Command to Queue ${qnum}: $cmdTypeId, $cmdType, $cmdObj, $cmdObjVal, $childId", "info", false)
				if(childDebug && childDev) { childDev?.log("Adding Command to Queue ${qnum}: $cmdData") }
				atomicState?.pollBlocked = true
				cmdQueue = atomicState?."cmdQ${qnum}"
				cmdQueue << cmdData
				atomicState."cmdQ${qnum}" = cmdQueue
				atomicState?.lastQcmd = cmdData
				schedNextWorkQ(childId)
			}
			return true

		} else {
			if(childDebug && childDev) { childDev?.log("sendNestApiCmd null cmdTypeId... $cmdTypeId, $cmdType, $cmdObj, $cmdObjVal, $childId") }
			return false
		}
	}
	catch (ex) {
		log.error "sendNestApiCmd Exception:", ex
		sendExceptionData(ex.message, "sendNestApiCmd")
		if(childDebug && childDev) { childDev?.log("sendNestApiCmd Exception: ${ex.message}", "error") }
		return false
	}
}

private getQueueNumber(cmdTypeId, childId) {
	def childDev = getChildDevice(childId)
	if(!atomicState?.cmdQlist) { atomicState.cmdQlist = [] }
	def cmdQueueList = atomicState?.cmdQlist
	def qnum = cmdQueueList.indexOf(cmdTypeId)
	if(qnum == -1) {
		cmdQueueList = atomicState?.cmdQlist
		cmdQueueList << cmdTypeId
		atomicState.cmdQlist = cmdQueueList
		qnum = cmdQueueList.indexOf(cmdTypeId)
		atomicState?."cmdQ${qnum}" = null
		setLastCmdSentSeconds(qnum, null)
		setRecentSendCmd(qnum, null)
	}
	qnum = cmdQueueList.indexOf(cmdTypeId)
	if(qnum == -1 ) { if(childDev) { childDev?.log("getQueueNumber: NOT FOUND" ) } }
	if(childDebug && childDev) { childDev?.log("getQueueNumber: cmdTypeId ${cmdTypeId} is queue ${qnum}" ) }
	return qnum
}

void schedNextWorkQ(childId) {
	def childDev = getChildDevice(childId)
	def cmdDelay = getChildWaitVal()
	//
	// This is throttling the rate of commands to the Nest service for this access token.
	// If too many commands are sent Nest throttling could shut all write commands down for 1 hour to the device or structure
	// This allows up to 3 commands if none sent in the last hour, then only 1 per 60 seconds.   Nest could still
	// throttle this if the battery state on device is low.
	//

	if(!atomicState?.cmdQlist) { atomicState.cmdQlist = [] }
	def cmdQueueList = atomicState?.cmdQlist
	def done = false
	def nearestQ = 100
	def qnum = -1
	cmdQueueList.eachWithIndex { val, idx ->
		if(done || !atomicState?."cmdQ${idx}" ) { return }
		else {
			if( (getRecentSendCmd(idx) > 0 ) || (getLastCmdSentSeconds(idx) > 60) ) {
				runIn(cmdDelay*2, "workQueue", [overwrite: true])
				qnum = idx
				done = true
				return
			} else {
				if((60 - getLastCmdSentSeconds(idx) + cmdDelay) < nearestQ) {
					nearestQ = (60 - getLastCmdSentSeconds(idx) + cmdDelay)
					qnum = idx
				}
			}
		}
	}
	if(!done) {
		 runIn(nearestQ, "workQueue", [overwrite: true])
	}
	//if(childDebug && childDev) { childDev?.log("schedNextWorkQ queue: ${qnum} | recentSendCmd: ${getRecentSendCmd(qnum)} | last seconds: ${getLastCmdSentSeconds(qnum)} | cmdDelay: ${cmdDelay}") }
	//if(childDev) { childDev?.log("schedNextWorkQ queue: ${qnum} | recentSendCmd: ${getRecentSendCmd(qnum)} | last seconds: ${getLastCmdSentSeconds(qnum)} | cmdDelay: ${cmdDelay}") }
	LogAction("schedNextWorkQ queue: ${qnum} | recentSendCmd: ${getRecentSendCmd(qnum)} | last seconds: ${getLastCmdSentSeconds(qnum)} | cmdDelay: ${cmdDelay}", "info", true)
}

private getRecentSendCmd(qnum) {
	return atomicState?."recentSendCmd${qnum}"
}

private setRecentSendCmd(qnum, val) {
	atomicState."recentSendCmd${qnum}" = val
	return
}

private getLastCmdSentSeconds(qnum) { return atomicState?."lastCmdSentDt${qnum}" ? GetTimeDiffSeconds(atomicState?."lastCmdSentDt${qnum}") : 3601 }

private setLastCmdSentSeconds(qnum, val) {
	atomicState."lastCmdSentDt${qnum}" = val
	atomicState.lastCmdSentDt = val
}

void workQueue() {
	//log.trace "workQueue..."
	def cmdDelay = getChildWaitVal()

	if(!atomicState?.cmdQlist) { atomicState?.cmdQlist = [] }
	def cmdQueueList = atomicState?.cmdQlist
	def done = false
	def nearestQ = 100
	def qnum = 0
	cmdQueueList?.eachWithIndex { val, idx ->
		if(done || !atomicState?."cmdQ${idx}" ) { return }
		else {
			if( (getRecentSendCmd(idx) > 0 ) || (getLastCmdSentSeconds(idx) > 60) ) {
				qnum = idx
				done = true
				return
			} else {
				if((60 - getLastCmdSentSeconds(idx) + cmdDelay) < nearestQ) {
					nearestQ = (60 - getLastCmdSentSeconds(idx) + cmdDelay)
					qnum = idx
				}
			}
		}
	}

	LogAction("workQueue Run queue: ${qnum}", "trace", true)
	if(!atomicState?."cmdQ${qnum}") { atomicState."cmdQ${qnum}" = [] }
	def cmdQueue = atomicState?."cmdQ${qnum}"
	try {
		if(cmdQueue?.size() > 0) {
			runIn(60, "workQueue", [overwrite: true])  // lost schedule catchall
			atomicState?.pollBlocked = true
			cmdQueue = atomicState?."cmdQ${qnum}"
			def cmd = cmdQueue?.remove(0)
			atomicState?."cmdQ${qnum}" = cmdQueue

			if(getLastCmdSentSeconds(qnum) > 3600) { setRecentSendCmd(qnum, 3) } // if nothing sent in last hour, reset 3 command limit

			if(cmd[1] == "poll") {
				atomicState.needStrPoll = true
				atomicState.needDevPoll = true
				atomicState.needChildUpd = true
			} else {
				cmdProcState(true)
				def cmdres = procNestApiCmd(getNestApiUrl(), cmd[0], cmd[1], cmd[2], cmd[3], qnum)
				if( !cmdres ) {
					atomicState.needChildUpd = true
					atomicState.pollBlocked = false
					runIn((cmdDelay * 3), "postCmd", [overwrite: true])
				}
				cmdProcState(false)
			}

			atomicState.needDevPoll = true
			if(cmd[1] == apiVar().rootTypes.struct.toString()) {
				atomicState.needStrPoll = true
				atomicState.needChildUpd = true
			}

			qnum = 0
			done = false
			nearestQ = 100
			cmdQueueList?.eachWithIndex { val, idx ->
				if(done || !atomicState?."cmdQ${idx}" ) { return }
				else {
					if( (getRecentSendCmd(idx) > 0 ) || (getLastCmdSentSeconds(idx) > 60) ) {
						qnum = idx
						done = true
						return
					} else {
						if((60 - getLastCmdSentSeconds(idx) + cmdDelay) < nearestQ) {
							nearestQ = (60 - getLastCmdSentSeconds(idx) + cmdDelay)
							qnum = idx
						}
					}
				}
			}

			if(!atomicState?."cmdQ${qnum}") { atomicState?."cmdQ${qnum}" = [] }
			cmdQueue = atomicState?."cmdQ${qnum}"
			if(cmdQueue?.size() == 0) {
				atomicState.pollBlocked = false
				atomicState.needChildUpd = true
				runIn(cmdDelay * 3, "postCmd", [overwrite: true])
			}
			else { schedNextWorkQ(null) }

			atomicState?.cmdLastProcDt = getDtNow()
			if(cmdQueue?.size() > 10) {
				sendMsg("Warning", "There is now ${cmdQueue?.size()} events in the Command Queue. Something must be wrong...")
				LogAction("There is now ${cmdQueue?.size()} events in the Command Queue. Something must be wrong...", "warn", true)
			}
			return
		} else { atomicState.pollBlocked = false }
	}
	catch (ex) {
		log.error "workQueue Exception Error:", ex
		sendExceptionData(ex.message, "workQueue")
		cmdProcState(false)
		atomicState.needDevPoll = true
		atomicState.needStrPoll = true
		atomicState.needChildUpd = true
		atomicState?.pollBlocked = false
		runIn(60, "workQueue", [overwrite: true])
		runIn((60 + 4), "postCmd", [overwrite: true])
		return
	}
}

def procNestApiCmd(uri, typeId, type, obj, objVal, qnum, redir = false) {
	LogTrace("procNestApiCmd: typeId: ${typeId}, type: ${type}, obj: ${obj}, objVal: ${objVal}, qnum: ${qnum},  isRedirUri: ${redir}")

	def result = false
	try {
		def urlPath = redir ? "" : "/${type}/${typeId}"
		def data = new JsonBuilder("${obj}":objVal)
		def params = [
			uri: uri,
			path: urlPath,
			contentType: "application/json",
			query: [ "auth": atomicState?.authToken ],
			body: data.toString()
		]
		LogAction("procNestApiCmd Url: $uri | params: ${params}", "trace", true)
		atomicState?.lastCmdSent = "$type: (${obj}: ${objVal})"

		if(!redir && (getRecentSendCmd(qnum) > 0) && (getLastCmdSentSeconds(qnum) < 60)) {
			def val = getRecentSendCmd(qnum)
			val -= 1
			setRecentSendCmd(qnum, val)
		}
		setLastCmdSentSeconds(qnum, getDtNow())

		//log.trace "procNestApiCmd time update recentSendCmd:  ${getRecentSendCmd(qnum)}  last seconds:${getLastCmdSentSeconds(qnum)} queue: ${qnum}"

		httpPutJson(params) { resp ->
			if(resp?.status == 307) {
				def newUrl = resp?.headers?.location?.split("\\?")
				LogTrace("NewUrl: ${newUrl[0]}")
				if( procNestApiCmd(newUrl[0], typeId, type, obj, objVal, qnum, true) ) {
					result = true
				}
			}
			else if( resp?.status == 200) {
				LogAction("procNestApiCmd Processed queue: ${qnum} ($type | ($obj:$objVal)) Successfully!!!", "info", true)
				apiIssueEvent(false)
				result = true
				increaseCmdCnt()
				atomicState?.lastCmdSentStatus = "ok"
				//sendEvtUpdateToDevice(typeId, type, obj, objVal)
			}
			else if(resp?.status == 400) {
				LogAction("procNestApiCmd 'Bad Request' Exception: ${resp?.status} ($type | $obj:$objVal)", "error", true)
			}
			else {
				LogAction("procNestApiCmd 'Unexpected' Response: ${resp?.status}", "warn", true)
			}
		}
	}
	catch (ex) {
		log.error "procNestApiCmd Exception: ($type | $obj:$objVal)", ex
		sendExceptionData(ex.message, "procNestApiCmd")
		apiIssueEvent(true)
		atomicState?.lastCmdSentStatus = "failed"
	}
	return result
}

def increaseCmdCnt() {
	try {
		def cmdCnt = atomicState?.apiCommandCnt ?: 0
		cmdCnt = cmdCnt?.toInteger()+1
		LogAction("Api CmdCnt: $cmdCnt", "info", false)
		if(cmdCnt) { atomicState?.apiCommandCnt = cmdCnt?.toInteger() }
	} catch (ex) {
		log.error "increaseCmdCnt Exception:", ex
		sendExceptionData(ex.message, "increaseCmdCnt")
	}
}


/************************************************************************************************
|								Push Notification Functions										|
*************************************************************************************************/
def pushStatus() { return (settings?.recipients || settings?.phone || settings?.usePush) ? (settings?.usePush ? "Push Enabled" : "Enabled") : null }
def getLastMsgSec() { return !atomicState?.lastMsgDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastMsgDt).toInteger() }
def getLastUpdMsgSec() { return !atomicState?.lastUpdMsgDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastUpdMsgDt).toInteger() }
def getLastMisPollMsgSec() { return !atomicState?.lastMisPollMsgDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastMisPollMsgDt).toInteger() }
def getRecipientsSize() { return !settings.recipients ? 0 : settings?.recipients.size() }

//this is parent only method
def getOk2Notify() { return (daysOk(settings?.quietDays) && notificationTimeOk() && modesOk(settings?.quietModes)) }
def isMissedPoll() { return (getLastDevicePollSec() > atomicState?.misPollNotifyWaitVal.toInteger()) ? true : false }

def notificationCheck() {
	if((settings?.recipients || settings?.usePush) && getOk2Notify()) {
		if(sendMissedPollMsg) { missedPollNotify() }
		if(sendAppUpdateMsg && !appDevType()) { appUpdateNotify() }
	}
}

def missedPollNotify() {
	if(isMissedPoll()) {
		if(getOk2Notify() && (getLastMisPollMsgSec() > atomicState?.misPollNotifyMsgWaitVal.toInteger())) {
			sendMsg("Warning", "${app.name} has not refreshed data in the last (${getLastDevicePollSec()}) seconds.  Please try refreshing manually or refresh Nest Authentication settings.")
			atomicState?.lastMisPollMsgDt = getDtNow()
		}
	}
}

def appUpdateNotify() {
	def appUpd = isAppUpdateAvail()
	def protUpd = atomicState?.protects ? isProtUpdateAvail() : null
	def presUpd = atomicState?.presDevice ? isPresUpdateAvail() : null
	def tstatUpd = atomicState?.thermostats ? isTstatUpdateAvail() : null
	def vtstatUpd = atomicState?.vThermostats ? isvTstatUpdateAvail() : null
	def weatherUpd = atomicState?.weatherDevice ? isWeatherUpdateAvail() : null
	def camUpd = atomicState?.cameras ? isCamUpdateAvail() : null
	if((appUpd || protUpd || presUpd || tstatUpd || weatherUpd || camUpd || vtstatUpd) && (getLastUpdMsgSec() > atomicState?.updNotifyWaitVal.toInteger())) {
		def str = ""
		str += !appUpd ? "" : "\nManager App: v${atomicState?.appData?.updater?.versions?.app?.ver?.toString()}"
		str += !protUpd ? "" : "\nProtect: v${atomicState?.appData?.updater?.versions?.protect?.ver?.toString()}"
		str += !camUpd ? "" : "\nCamera: v${atomicState?.appData?.updater?.versions?.camera?.ver?.toString()}"
		str += !presUpd ? "" : "\nPresence: v${atomicState?.appData?.updater?.versions?.presence?.ver?.toString()}"
		str += !tstatUpd ? "" : "\nThermostat: v${atomicState?.appData?.updater?.versions?.thermostat?.ver?.toString()}"
		str += !vtstatUpd ? "" : "\nVirtual Thermostat: v${atomicState?.appData?.updater?.versions?.vthermostat?.ver?.toString()}"
		str += !weatherUpd ? "" : "\nWeather App: v${atomicState?.appData?.updater?.versions?.weather?.ver?.toString()}"
		sendMsg("Info", "Nest Manager Update(s) are Available:${str}...  \n\nPlease visit the IDE to Update your code...")
		atomicState?.lastUpdMsgDt = getDtNow()
	}
}

def updateHandler() {
	//log.trace "updateHandler..."
	if(atomicState?.isInstalled) {
		if(atomicState?.appData?.updater?.updateType.toString() == "critical" && atomicState?.lastCritUpdateInfo?.ver.toInteger() != atomicState?.appData?.updater?.updateVer.toInteger()) {
			sendMsg("Critical", "There are Critical Updates available for the Nest Manager Application!!! Please visit the IDE and make sure to update the App and Devices Code...")
			atomicState?.lastCritUpdateInfo = ["dt":getDtNow(), "ver":atomicState?.appData?.updater?.updateVer?.toInteger()]
		}
		if(atomicState?.appData?.updater?.updateMsg != "" && atomicState?.appData?.updater?.updateMsg != atomicState?.lastUpdateMsg) {
			if(getLastUpdateMsgSec() > 86400) {
				sendMsg("Info", "${atomicState?.updater?.updateMsg}")
				atomicState?.lastUpdateMsgDt = getDtNow()
			}
		}
	}
}

// parent only method
def sendMsg(msgType, msg, people = null, sms = null, push = null, brdcast = null) {
	//log.trace "sendMsg..."
	try {
		if(!getOk2Notify()) {
			LogAction("No Notifications will be sent during Quiet Time...", "info", true)
		} else {
			def newMsg = "${msgType}: ${msg}"
			if(!brdcast) {
				def who = people ? people : settings?.recipients
				if(location.contactBookEnabled) {
					if(who) {
						sendNotificationToContacts(newMsg, who)
						atomicState?.lastMsg = newMsg
						atomicState?.lastMsgDt = getDtNow()
						LogAction("Push Message Sent: ${atomicState?.lastMsgDt}", "debug", true)
					}
				} else {
					LogAction("ContactBook is NOT Enabled on your SmartThings Account...", "warn", true)
					if(push) {
						sendPush(newMsg)
						atomicState?.lastMsg = newMsg
						atomicState?.lastMsgDt = getDtNow()
						LogAction("Push Message Sent: ${atomicState?.lastMsgDt}", "debug", true)
					}
					else if(sms) {
						sendSms(sms, newMsg)
						atomicState?.lastMsg = newMsg
						atomicState?.lastMsgDt = getDtNow()
						LogAction("SMS Message Sent: ${atomicState?.lastMsgDt}", "debug", true)
					}
				}
			} else {
				sendPushMessage(newMsg)
				LogAction("Broadcast Message Sent: ${newMsg} - ${atomicState?.lastMsgDt}", "debug", true)
			}
		}
	} catch (ex) {
		log.error "sendMsg Exception:", ex
		sendExceptionData(ex.message, "sendMsg")
	}
}

def getLastWebUpdSec() { return !atomicState?.lastWebUpdDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastWebUpdDt).toInteger() }
def getLastWeatherUpdSec() { return !atomicState?.lastWeatherUpdDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastWeatherUpdDt).toInteger() }
def getLastForecastUpdSec() { return !atomicState?.lastForecastUpdDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastForecastUpdDt).toInteger() }
def getLastAnalyticUpdSec() { return !atomicState?.lastAnalyticUpdDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastAnalyticUpdDt).toInteger() }
def getLastUpdateMsgSec() { return !atomicState?.lastUpdateMsgDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastUpdateMsgDt).toInteger() }

def getStZipCode() { return location?.zipCode.toString() }
def getNestZipCode() { return atomicState?.structData[atomicState?.structures].postal_code ? atomicState?.structData[atomicState?.structures]?.postal_code.toString() : "" }
def getNestTimeZone() { return atomicState?.structData[atomicState?.structures].time_zone ? atomicState?.structData[atomicState?.structures].time_zone : null}

def updateWebStuff(now = false) {
	//log.trace "updateWebStuff..."
	if(!atomicState?.appData || (getLastWebUpdSec() > (3600*4))) {
		if(now) {
			getWebFileData()
		} else {
			if(canSchedule()) { runIn(45, "getWebFileData", [overwrite: true]) }  //This reads a JSON file from a web server with timing values and version numbers
		}
	}
	if(optInAppAnalytics && atomicState?.isInstalled) {
		if(getLastAnalyticUpdSec() > (3600*24)) {
			sendInstallData()
		}
	}
	if(atomicState?.weatherDevice && getLastWeatherUpdSec() > (settings?.pollWeatherValue ? settings?.pollWeatherValue.toInteger() : 900)) {
		if(now) {
			getWeatherConditions(now)
		} else {
			if(canSchedule()) { runIn(20, "getWeatherConditions", [overwrite: true]) }
		}
	}
}

def getWeatherConditions(force = false) {
	//log.trace "getWeatherConditions..."
	if(atomicState?.weatherDevice) {
		try {
			LogAction("Retrieving Latest Local Weather Conditions", "info", true)
			def loc = ""
			def curWeather = ""
			def curForecast = ""
			def curAstronomy = ""
			def curAlerts = ""
			def err = false
			if(custLocStr) {
				loc = custLocStr
				curWeather = getWeatherFeature("conditions", loc)
				curAlerts = getWeatherFeature("alerts", loc)
			} else {
				curWeather = getWeatherFeature("conditions")
				curAlerts = getWeatherFeature("alerts")
			}
			if(getLastForecastUpdSec() > (1800)) {
				if(custLocStr) {
					loc = custLocStr
					curForecast = getWeatherFeature("forecast", loc)
					curAstronomy = getWeatherFeature("astronomy", loc)
				} else {
					curForecast = getWeatherFeature("forecast")
					curAstronomy = getWeatherFeature("astronomy")
				}
				if(curForecast && curAstronomy) {
					atomicState?.curForecast = curForecast
					atomicState?.curAstronomy = curAstronomy
					atomicState?.lastForecastUpdDt = getDtNow()
				} else {
					LogAction("Could Not Retrieve Latest Local Forecast or astronomy Conditions", "warn", true)
					err = true
				}
			}
			if(curWeather && curAlerts) {
				atomicState?.curWeather = curWeather
				atomicState?.curAlerts = curAlerts
				if(!err) { atomicState?.lastWeatherUpdDt = getDtNow() }
			} else {
				LogAction("Could Not Retrieve Latest Local Weather Conditions or alerts", "warn", true)
				return false
			}
			if(curWeather || curAstronomy || curForecast || curAlerts) {
				atomicState.needChildUpd = true
				if(!force) { runIn(21, "postCmd", [overwrite: true]) }
				return true
			}
		}
		catch (ex) {
			log.error "getWeatherConditions Exception:", ex
			sendExceptionData(ex.message, "getWeatherConditions")
			return false
		}
	} else { return false }
}

def getWData() {
	if(atomicState?.curWeather) {
		return atomicState?.curWeather
	} else {
		if(getWeatherConditions(true)) {
			return atomicState?.curWeather
		}
	}

}

def getWForecastData() {
	if(atomicState?.curForecast) {
		return atomicState?.curForecast
	} else {
		if(getWeatherConditions(true)) {
			return atomicState?.curForecast
		}
	}
}

def getWAstronomyData() {
	if(atomicState?.curAstronomy) {
		return atomicState?.curAstronomy
	} else {
		if(getWeatherConditions(true)) {
			return atomicState?.curAstronomy
		}
	}
}

def getWAlertsData() {
	if(atomicState?.curAlerts) {
		return atomicState?.curAlerts
	} else {
		if(getWeatherConditions(true)) {
			return atomicState?.curAlerts
		}
	}
}

def getWeatherDeviceInst() {
	return atomicState?.weatherDevice ? true : false
}

def getWebFileData() {
	//log.trace "getWebFileData..."
	def params = [ uri: "https://raw.githubusercontent.com/tonesto7/nest-manager/${gitBranch()}/Data/appParams.json", contentType: 'application/json' ]
	def result = false
	try {
		httpGet(params) { resp ->
			if(resp.data) {
				LogAction("Getting Latest Data from appParams.json File...", "info", true)
				atomicState?.appData = resp?.data
				atomicState?.lastWebUpdDt = getDtNow()
				updateHandler()
				broadcastCheck()
				helpHandler()
			}
			LogTrace("getWebFileData Resp: ${resp?.data}")
			result = true
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			   log.warn  "appParams.json file not found..."
		} else {
			log.error "getWebFileData Exception:", ex
		}
		sendExceptionData(ex.message, "getWebFileData")
	}
	return result
}

def broadcastCheck() {
	if(atomicState?.isInstalled && atomicState?.appData.broadcast) {
		if(atomicState?.appData?.broadcast?.msgId != null && atomicState?.lastBroadcastId != atomicState?.appData?.broadcast?.msgId) {
			sendMsg(atomicState?.appData?.broadcast?.type.toString().capitalize(), atomicState?.appData?.broadcast?.message.toString(), null, null, null, true)
			atomicState?.lastBroadcastId = atomicState?.appData?.broadcast?.msgId
		}
	}
}

def helpHandler() {
	if(atomicState?.appData?.help) {
		atomicState.showHelp = atomicState?.appData?.help?.showHelp == "false" ? false : true
	}
}

def getHtmlInfo() {
	if(atomicState?.appData?.html?.cssUrl && atomicState?.appData?.html?.cssVer && atomicState?.appData?.html?.chartJsUrl && atomicState?.appData?.html?.chartJsVer ) {
		return ["cssUrl":atomicState?.appData?.html?.cssUrl, "cssVer":atomicState?.appData?.html?.cssVer, "chartJsUrl":atomicState?.appData?.html?.chartJsUrl, "chartJsVer":atomicState?.appData?.html?.chartJsVer]
	} else {
		if(getWebFileData()) {
			return ["cssUrl":atomicState?.appData?.html?.cssUrl, "cssVer":atomicState?.appData?.html?.cssVer, "chartJsUrl":atomicState?.appData?.html?.chartJsUrl, "chartJsVer":atomicState?.appData?.html?.chartJsVer]
		}
	}
}

def allowDbException() {
	if(atomicState?.appData?.database?.disableExceptions != null) {
		return atomicState?.appData?.database?.disableExceptions == true ? false : true
	} else {
		if(getWebFileData()) {
			return atomicState?.appData?.database?.disableExceptions == true ? false : true
		}
	}
}

def ver2IntArray(val) {
	def ver = val?.split("\\.")
	return [maj:"${ver[0]?.toInteger()}",min:"${ver[1]?.toInteger()}",rev:"${ver[2]?.toInteger()}"]
}
def versionStr2Int(str) { return str ? str.toString().replaceAll("\\.", "").toInteger() : null }

def getChildWaitVal() { return settings?.tempChgWaitVal ? settings?.tempChgWaitVal.toInteger() : 4 }

def getAskAlexaQueueEnabled() {
	if(!parent) { return (atomicState?.appData?.aaSupport?.enabled == true) ? true : false }
}

def isCodeUpdateAvailable(newVer, curVer, type) {
	def result = false
	def latestVer
	if(newVer && curVer) {
		def versions = [newVer, curVer]
		if(newVer != curVer) {
			latestVer = versions?.max { a, b ->
				def verA = a?.tokenize('.')
				def verB = b?.tokenize('.')
				def commonIndices = Math.min(verA?.size(), verB?.size())
				for (int i = 0; i < commonIndices; ++i) {
					//log.debug "comparing $numA and $numB"
					if(verA[i]?.toInteger() != verB[i]?.toInteger()) {
						return verA[i]?.toInteger() <=> verB[i]?.toInteger()
					}
				}
				verA?.size() <=> verB?.size()
			}
			result = (latestVer == newVer) ? true : false
		}
	}
	//log.debug "type: $type | newVer: $newVer | curVer: $curVer | newestVersion: ${latestVer} | result: $result"
	return result
}

def isAppUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.app?.ver, appVersion(), "manager")) { return true }
	return false
}

def isDashUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.webDash?.ver, atomicState?.webDashVer, "webDash")) { return true }
	return false
}

def isPresUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.presence?.ver, atomicState?.presDevVer, "presence")) { return true }
	return false
}

def isProtUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.protect?.ver, atomicState?.pDevVer, "protect")) { return true }
	return false
}

def isCamUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.camera?.ver, atomicState?.camDevVer, "camera")) { return true }
	return false
}

def isTstatUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.thermostat?.ver, atomicState?.tDevVer, "thermostat")) { return true }
	return false
}

def isvTstatUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.vthermostat?.ver, atomicState?.vtDevVer, "vthermostat")) { return true }
	return false
}

def isWeatherUpdateAvail() {
	if(isCodeUpdateAvailable(atomicState?.appData?.updater?.versions?.weather?.ver, atomicState?.weatDevVer, "weather")) { return true }
	return false
}

/************************************************************************************************
|			This Section Discovers all structures and devices on your Nest Account.				|
|			It also Adds/Removes Devices from ST												|
*************************************************************************************************/

def getNestStructures() {
	LogTrace("Getting Nest Structures")
	def struct = [:]
	def thisstruct = [:]
	try {
		if(ok2PollStruct()) { getApiData("str") }
		if(atomicState?.structData) {
			def structs = atomicState?.structData
			structs?.eachWithIndex { struc, index ->
				def strucId = struc?.key
				def strucData = struc?.value

				def dni = [strucData?.structure_id].join('.')
				struct[dni] = strucData?.name.toString()

				if(strucData?.structure_id.toString() == settings?.structures.toString()) {
					thisstruct[dni] = strucData?.name.toString()
				} else {
					if(atomicState?.structures) {
						if(strucData?.structure_id?.toString() == atomicState?.structures?.toString()) {
							thisstruct[dni] = strucData?.name.toString()
						}
					} else {
						if(!settings?.structures) {
							thisstruct[dni] = strucData?.name.toString()
						}
					}
				}
			}
			if(atomicState?.thermostats || atomicState?.protects || atomicState?.cameras || atomicState?.vThermostats || atomicState?.presDevice || atomicState?.weatherDevice || isAutoAppInst() ) {  // if devices are configured, you cannot change the structure until they are removed
				struct = thisstruct
			}
			if(ok2PollDevice()) { getApiData("dev") }
		} else { LogAction("Missing: atomicState.structData  ${atomicState?.structData}", "warn", true) }

	} catch (ex) {
		log.error "getNestStructures Exception:", ex
		sendExceptionData(ex.message, "getNestStructures")
	}
	return struct
}

def getNestThermostats() {
	LogTrace("Getting Thermostat list")
	def stats = [:]
	def tstats = atomicState?.deviceData?.thermostats
	LogTrace("Found ${tstats?.size()} Thermostats...")
	tstats.each { stat ->
		def statId = stat?.key
		def statData = stat?.value

		def adni = [statData?.device_id].join('.')
		if(statData?.structure_id == settings?.structures) {
			stats[adni] = getThermostatDisplayName(statData)
		}
	}
	return stats
}

def getNestProtects() {
	LogTrace("Getting Nest Protect List...")
	def protects = [:]
	def nProtects = atomicState?.deviceData?.smoke_co_alarms
	LogTrace("Found ${nProtects?.size()} Nest Protects...")
	nProtects.each { dev ->
		def devId = dev?.key
		def devData = dev?.value

		def bdni = [devData?.device_id].join('.')
		if(devData?.structure_id == settings?.structures) {
			protects[bdni] = getProtectDisplayName(devData)
		}
	}
	return protects
}

def getNestCameras() {
	LogTrace("Getting Nest Camera List...")
	def cameras = [:]
	def nCameras = atomicState?.deviceData?.cameras
	LogTrace("Found ${nCameras?.size()} Nest Cameras...")
	nCameras.each { dev ->
		def devId = dev?.key
		def devData = dev?.value

		def bdni = [devData?.device_id].join('.')
		if(devData?.structure_id == settings?.structures) {
			cameras[bdni] = getCameraDisplayName(devData)
		}
	}
	return cameras
}

def statState(val) {
	def stats = [:]
	def tstats = getNestThermostats()
	tstats.each { stat ->
		def statId = stat?.key
		def statData = stat?.value
		val.each { st ->
			if(statId == st) {
				def adni = [statId].join('.')
				stats[adni] = statData
			}
		}
	}
	return stats
}

def coState(val) {
	def protects = [:]
	def nProtects = getNestProtects()
	nProtects.each { dev ->
		val.each { pt ->
		if(dev?.key == pt) {
			def bdni = [dev?.key].join('.')
				protects[bdni] = dev?.value
			}
		}
	}
	return protects
}

def camState(val) {
	def cams = [:]
	def nCameras = getNestCameras()
	nCameras.each { dev ->
		val.each { cm ->
		if(dev?.key == cm) {
			def bdni = [dev?.key].join('.')
				cams[bdni] = dev?.value
			}
		}
	}
	return cams
}

def getThermostatDisplayName(stat) {
	if(stat?.name) { return stat.name.toString() }
}

def getProtectDisplayName(prot) {
	if(prot?.name) { return prot.name.toString() }
}

def getCameraDisplayName(cam) {
	if(cam?.name) { return cam.name.toString() }
}

def getNestTstatDni(dni) {
	//log.debug "getNestTstatDni: $dni"
	def d1 = getChildDevice(dni?.key.toString())
	if(d1) { return dni?.key.toString() }
	else {
		def devt =  appDevName()
		return "NestThermostat-${dni?.value.toString()}${devt} | ${dni?.key.toString()}"
	}
	LogAction("getNestTstatDni Issue...", "warn", true)
}

def getNestProtDni(dni) {
	def d2 = getChildDevice(dni?.key.toString())
	if(d2) { return dni?.key.toString() }
	else {
		def devt =  appDevName()
		return "NestProtect-${dni?.value.toString()}${devt} | ${dni?.key.toString()}"
	}
	LogAction("getNestProtDni Issue...", "warn", true)
}

def getNestCamDni(dni) {
	def d5 = getChildDevice(dni?.key.toString())
	if(d5) { return dni?.key.toString() }
	else {
		def devt =  appDevName()
		return "NestCam-${dni?.value.toString()}${devt} | ${dni?.key.toString()}"
	}
	LogAction("getNestCamDni Issue...", "warn", true)
}

def getNestvStatDni(dni) {
	def d6 = getChildDevice(dni.key.toString())
	if(d6) { return dni?.key.toString() }
	else {
		def devt =  appDevName()
		return "NestvThermostat-${dni?.value.toString()}${devt} | ${dni?.key.toString()}"
	}
	LogAction("getNestvStatDni Issue...", "warn", true)
}

def getNestPresId() {
	def dni = "Nest Presence Device" // old name 1
	def d3 = getChildDevice(dni)
	if(d3) { return dni }
	else {
		if(atomicState?.structures) {
			dni = "NestPres${atomicState.structures}" // old name 2
			d3 = getChildDevice(dni)
			if(d3) { return dni }
		}
		def retVal = ""
		def devt =  appDevName()
		if(settings?.structures) { retVal = "NestPres${devt} | ${settings?.structures}" }
		else if(atomicState?.structures) { retVal = "NestPres${devt} | ${atomicState?.structures}" }
		else {
			LogAction("getNestPresID No structures ${atomicState?.structures}", "warn", true)
			return ""
		}
		return retVal
	}
}

def getNestWeatherId() {
	def dni = "Nest Weather Device (${location?.zipCode})"
	def d4 = getChildDevice(dni)
	if(d4) { return dni }
	else {
		if(atomicState?.structures) {
			dni = "NestWeather${atomicState.structures}"
			d4 = getChildDevice(dni)
			if(d4) { return dni }
		}
		def retVal = ""
		def devt = appDevName()
		if(settings?.structures) { retVal = "NestWeather${devt} | ${settings?.structures}" }
		else if(atomicState?.structures) { retVal = "NestWeather${devt} | ${atomicState?.structures}" }
		else {
			LogAction("getNestWeatherId No structures ${atomicState?.structures}", "warn", true)
			return ""
		}
		return retVal
	}
}

def getNestTstatLabel(name) {
	//log.trace "getNestTstatLabel: ${name}"
	def devt = appDevName()
	def defName = "Nest Thermostat${devt} - ${name}"
	if(atomicState?.useAltNames) { defName = "${location.name}${devt} - ${name}" }
	if(atomicState?.custLabelUsed) {
		return settings?."tstat_${name}_lbl" ? settings?."tstat_${name}_lbl" : defName
	}
	else { return defName }
}

def getNestProtLabel(name) {
	def devt = appDevName()
	def defName = "Nest Protect${devt} - ${name}"
	if(atomicState?.useAltNames) { defName = "${location.name}${devt} - ${name}" }
	if(atomicState?.custLabelUsed) {
		return settings?."prot_${name}_lbl" ? settings?."prot_${name}_lbl" : defName
	}
	else { return defName }
}

def getNestCamLabel(name) {
	def devt = appDevName()
	def defName = "Nest Camera${devt} - ${name}"
	if(atomicState?.useAltNames) { defName = "${location.name}${devt} - ${name}" }
	if(atomicState?.custLabelUsed) {
		return settings?."cam_${name}_lbl" ? settings?."cam_${name}_lbl" : defName
	}
	else { return defName }
}

def getNestvStatLabel(name) {
	def devt = appDevName()
	def defName = "Nest vThermostat${devt} - ${name}"
	if(atomicState?.useAltNames) { defName = "${location.name}${devt} - ${name}" }
	if(atomicState?.custLabelUsed) {
		return settings?."vtsat_${name}_lbl" ? settings?."vtstat_${name}_lbl" : defName
	}
	else { return defName }
}

def getNestPresLabel() {
	def devt = appDevName()
	def defName = "Nest Presence Device${devt}"
	if(atomicState?.useAltNames) { defName = "${location.name}${devt} - Nest Presence Device" }
	if(atomicState?.custLabelUsed) {
		return settings?.presDev_lbl ? settings?.presDev_lbl.toString() : defName
	}
	else { return defName }
}

def getNestWeatherLabel() {
	def devt = appDevName()
	def wLbl = custLocStr ? custLocStr.toString() : "${getStZipCode()}"
	def defName = "Nest Weather${devt} (${wLbl})"
	if(atomicState?.useAltNames) { defName = "${location.name}${devt} - Nest Weather Device" }
	if(atomicState?.custLabelUsed) {
		return settings?.weathDev_lbl ? settings?.weathDev_lbl.toString() : defName
	}
	else { return defName }
}

def getWeatherDevice() {
	def res = null
	def d = getChildDevice(getNestWeatherId())
	if(d) { return d }
	return res
}

def getTstats() {
	return atomicState?.thermostats
}

def getThermostatDevice(dni) {
	def d = getChildDevice(getNestTstatDni(dni))
	if(d) { return d }
	return null
}

def addRemoveDevices(uninst = null) {
	//log.trace "addRemoveDevices..."
	def retVal = false
	try {
		def devsInUse = []
		def tstats
		def nProtects
		def nCameras
		def nVstats
		def devsCrt = 0
		if(!uninst) {
			//LogAction("addRemoveDevices() Nest Thermostats ${atomicState?.thermostats}", "debug", false)
			if(atomicState?.thermostats) {
				tstats = atomicState?.thermostats.collect { dni ->
					def d1 = getChildDevice(getNestTstatDni(dni))
					if(!d1) {
						def d1Label = getNestTstatLabel("${dni?.value}")
						d1 = addChildDevice(app.namespace, getThermostatChildName(), dni?.key, null, [label: "${d1Label}"])
						d1.take()
						devsCrt = devsCrt + 1
						LogAction("Created: ${d1?.displayName} with (Id: ${dni?.key})", "debug", true)
					} else {
						LogAction("Found: ${d1?.displayName} with (Id: ${dni?.key}) already exists", "debug", true)
					}
					devsInUse += dni.key
					return d1
				}
			}
			//LogAction("addRemoveDevices Nest Protects ${atomicState?.protects}", "debug", false)
			if(atomicState?.protects) {
				nProtects = atomicState?.protects.collect { dni ->
					def d2 = getChildDevice(getNestProtDni(dni).toString())
					if(!d2) {
						def d2Label = getNestProtLabel("${dni.value}")
						d2 = addChildDevice(app.namespace, getProtectChildName(), dni.key, null, [label: "${d2Label}"])
						d2.take()
						devsCrt = devsCrt + 1
						LogAction("Created: ${d2?.displayName} with (Id: ${dni?.key})", "debug", true)
					} else {
						LogAction("Found: ${d2?.displayName} with (Id: ${dni?.key}) already exists", "debug", true)
					}
					devsInUse += dni.key
					return d2
				}
			}

			if(atomicState?.presDevice) {
				try {
					def dni = getNestPresId()
					def d3 = getChildDevice(dni)
					if(!d3) {
						def d3Label = getNestPresLabel()
						d3 = addChildDevice(app.namespace, getPresenceChildName(), dni, null, [label: "${d3Label}"])
						d3.take()
						devsCrt = devsCrt + 1
						LogAction("Created: ${d3.displayName} with (Id: ${dni})", "debug", true)
					} else {
						LogAction("Found: ${d3.displayName} with (Id: ${dni}) already exists", "debug", true)
					}
					devsInUse += dni
				} catch (ex) {
					LogAction("Nest Presence Device Type is Likely not installed/published", "warn", true)
					retVal = false
				}
			}

			if(atomicState?.weatherDevice) {
				try {
					def dni = getNestWeatherId()
					def d4 = getChildDevice(dni)
					if(!d4) {
						def d4Label = getNestWeatherLabel()
						d4 = addChildDevice(app.namespace, getWeatherChildName(), dni, null, [label: "${d4Label}"])
						d4.take()
						atomicState?.lastWeatherUpdDt = null
						atomicState?.lastForecastUpdDt = null
						devsCrt = devsCrt + 1
						LogAction("Created: ${d4.displayName} with (Id: ${dni})", "debug", true)
					} else {
						LogAction("Found: ${d4.displayName} with (Id: ${dni}) already exists", "debug", true)
					}
					devsInUse += dni
				} catch (ex) {
					LogAction("Nest Weather Device Type is Likely not installed/published", "warn", true)
					retVal = false
				}
			}
			if(atomicState?.cameras) {
				nCameras = atomicState?.cameras.collect { dni ->
					def d5 = getChildDevice(getNestCamDni(dni).toString())
					if(!d5) {
						def d5Label = getNestCamLabel("${dni.value}")
						d5 = addChildDevice(app.namespace, getCameraChildName(), dni.key, null, [label: "${d5Label}"])
						d5.take()
						devsCrt = devsCrt + 1
						LogAction("Created: ${d5?.displayName} with (Id: ${dni?.key})", "debug", true)
					} else {
						LogAction("Found: ${d5?.displayName} with (Id: ${dni?.key}) already exists", "debug", true)
					}
					devsInUse += dni.key
					return d5
				}
			}
			if(atomicState?.vThermostats) {
				nVstats = atomicState?.vThermostats.collect { dni ->
					//LogAction("atomicState.vThermostats: ${atomicState.vThermostats}  dni: ${dni}  dni.key: ${dni.key.toString()}  dni.value: ${dni.value.toString()}", "debug", true)
					def d6 = getChildDevice(getNestvStatDni(dni).toString())
					if(!d6) {
						def d6Label = getNestvStatLabel("${dni.value}")
						//LogAction("CREATED: ${d6Label} with (Id: ${dni.key})", "debug", true)
						d6 = addChildDevice(app.namespace, getvThermostatChildName(), dni.key, null, [label: "${d6Label}"])
						d6.take()
						devsCrt = devsCrt + 1
						LogAction("Created: ${d6?.displayName} with (Id: ${dni?.key})", "debug", true)
					} else {
						LogAction("Found: ${d6?.displayName} with (Id: ${dni?.key}) already exists", "debug", true)
					}
					devsInUse += dni.key
					return d6
				}
			}

			def presCnt = 0
			def weathCnt = 0
			if(atomicState?.presDevice) { presCnt = 1 }
			if(atomicState?.weatherDevice) { weathCnt = 1 }
			if(devsCrt > 0) {
				LogAction("Created Devices;  Current Devices: (${tstats?.size()}) Thermostat(s), (${nVstats?.size()}) Virtual Thermostat(s), (${nProtects?.size()}) Protect(s), (${nCameras?.size()}) Cameras(s), ${presCnt} Presence Device and ${weathCnt} Weather Device", "debug", true)
			}
		}

		if(uninst) {
			atomicState.thermostats = []
			atomicState.vThermostats = []
			atomicState.protects = []
			atomicState.cameras = []
			atomicState.presDevice = false
			atomicState.weatherDevice = false
		}

		if(!atomicState?.weatherDevice) {
			atomicState?.curWeather = null
			atomicState?.curForecast = null
			atomicState?.curAstronomy = null
			atomicState?.curAlerts = null
		}

		def delete
		LogAction("devicesInUse: ${devsInUse}", "debug", false)
		delete = getChildDevices().findAll { !devsInUse?.toString()?.contains(it?.deviceNetworkId) }

		if(delete?.size() > 0) {
			LogAction("Deleting: ${delete}, Removing ${delete.size()} devices", "debug", true)
			delete.each { deleteChildDevice(it.deviceNetworkId) }
		}
		retVal = true
	} catch (ex) {
		if(ex instanceof physicalgraph.exception.ConflictException) {
			def msg = "Error: Can't Delete App because Devices are still in use in other Apps, Routines, or Rules.  Please double check before trying again."
			sendPush(msg)
			LogAction("addRemoveDevices Exception | $msg", "warn", true)
		}
		else if(ex instanceof physicalgraph.app.exception.UnknownDeviceTypeException) {
			def msg = "Error: Device Handlers are likely Missing or Not Published.  Please verify all device handlers are present before continuing."
			sendPush(msg)
			LogAction("addRemoveDevices Exception | $msg", "warn", true)
		}
		else { log.error "addRemoveDevices Exception:", ex }
		sendExceptionData(ex.message, "addRemoveDevices")
		retVal = false
	}
	return retVal
}

def devNamePage() {
	def pagelbl = atomicState?.isInstalled ? "Device Labels" : "Custom Device Labels"
	dynamicPage(name: "devNamePage", title: pageLbl, nextPage: "", install: false) {
		def altName = (atomicState?.useAltNames) ? true : false
		def custName = (atomicState?.custLabelUsed) ? true : false
		section("Settings:") {
			if(atomicState?.isInstalled) {
				paragraph "Changes to device names are only allowed with new devices before they are installed.  Existing devices can only be edited in the devices settings page in the mobile app or in the IDE."
			} else {
				if(!useCustDevNames) {
					input (name: "useAltNames", type: "bool", title: "Use Location Name as Prefix?", required: false, defaultValue: altName, submitOnChange: true, image: "" )
				}
				if(!useAltNames) {
					input (name: "useCustDevNames", type: "bool", title: "Assign Custom Names?", required: false, defaultValue: custName, submitOnChange: true, image: "" )
				}
			}
			if(atomicState?.custLabelUsed) {
				paragraph "Custom Labels Are Active", state: "complete"
			}
			if(atomicState?.useAltNames) {
				paragraph "Using Location Name as Prefix is Active", state: "complete"
			}
			//paragraph "Current Device Handler Names", image: ""
		}
		def str1 = "\n\nName does not match what is expected.\nName Should be:"
		def str2 = "\n\nName cannot be customized"
		atomicState.useAltNames = useAltNames ? true : false
		atomicState.custLabelUsed = useCustDevNames ? true : false

		def found = false
		if(atomicState?.thermostats || atomicState?.vThermostats) {
			section ("Thermostat Device(s):") {
				atomicState?.thermostats?.each { t ->
					found = true
					def d = getChildDevice(getNestTstatDni(t))
					def dstr = ""
					if(d) {
						dstr += "Found: ${d.displayName}"
						if(d.displayName != getNestTstatLabel(t.value)) {
							dstr += "$str1 ${getNestTstatLabel(t.value)}"
						}
						else if(atomicState?.custLabelUsed || atomicState?.useAltNames) { dstr += "$str2" }
					} else {
						dstr += "New Name: ${getNestTstatLabel(t.value)}"
					}
					paragraph "${dstr}", state: "complete", image: (atomicState?.custLabelUsed && !d) ? " " : getAppImg("thermostat_icon.png")
					if(atomicState.custLabelUsed && !d) {
						input "tstat_${t.value}_lbl", "text", title: "Custom name for ${t.value}", defaultValue: getNestTstatLabel("${t.value}"), submitOnChange: true,
								image: getAppImg("thermostat_icon.png")
					}
				}
				atomicState?.vThermostats?.each { t ->
					found = true
					def d = getChildDevice(getNestvStatDni(t))
					def dstr = ""
					if(d) {
						dstr += "Found: ${d.displayName}"
						if(d.displayName != getNestvStatLabel(t.value)) {
							dstr += "$str1 ${getNestvStatLabel(t.value)}"
						}
						else if(atomicState?.custLabelUsed || atomicState?.useAltNames) { dstr += "$str2" }
					} else {
						dstr += "New Name: ${getNestvStatLabel(t.value)}"
					}
					paragraph "${dstr}", state: "complete", image: (atomicState?.custLabelUsed && !d) ? " " : getAppImg("thermostat_icon.png")
					if(atomicState.custLabelUsed && !d) {
						input "tstat_${t.value}_lbl", "text", title: "Custom name for ${t.value}", defaultValue: getNestTstatLabel("${t.value}"), submitOnChange: true,
								image: getAppImg("thermostat_icon.png")
					}
				}
			}
		}
		if(atomicState?.protects) {
			section ("Protect Device Names:") {
				atomicState?.protects?.each { p ->
					found = true
					def dstr = ""
					def d1 = getChildDevice(getNestProtDni(p))
					if(d1) {
						dstr += "Found: ${d1.displayName}"
						if(d1.displayName != getNestProtLabel(p.value)) {
							dstr += "$str1 ${getNestProtLabel(p.value)}"
						}
						else if(atomicState?.custLabelUsed || atomicState?.useAltNames) { dstr += "$str2" }
					} else {
						dstr += "New Name: ${getNestProtLabel(p.value)}"
					}
					paragraph "${dstr}", state: "complete", image: (atomicState.custLabelUsed && !d1) ? " " : getAppImg("protect_icon.png")
					if(atomicState.custLabelUsed && !d1) {
						input "prot_${p.value}_lbl", "text", title: "Custom name for ${p.value}", defaultValue: getNestProtLabel("${p.value}"), submitOnChange: true,
								image: getAppImg("protect_icon.png")
					}
				}
			}
		}
		if(atomicState?.cameras) {
			section ("Camera Device Names:") {
				atomicState?.cameras?.each { c ->
					found = true
					def dstr = ""
					def d1 = getChildDevice(getNestCamDni(c))
					if(d1) {
						dstr += "Found: ${d1.displayName}"
						if(d1.displayName != getNestCamLabel(c.value)) {
							dstr += "$str1 ${getNestCamLabel(c.value)}"
						}
						else if(atomicState?.custLabelUsed || atomicState?.useAltNames) { dstr += "$str2" }
					} else {
						dstr += "New Name: ${getNestCamLabel(c.value)}"
					}
					paragraph "${dstr}", state: "complete", image: (atomicState.custLabelUsed && !d1) ? " " : getAppImg("camera_icon.png")
					if(atomicState.custLabelUsed && !d1) {
						input "cam_${c.value}_lbl", "text", title: "Custom name for ${c.value}", defaultValue: getNestCamLabel("${c.value}"), submitOnChange: true,
								image: getAppImg("camera_icon.png")
					}
				}
			}
		}
		if(atomicState?.presDevice) {
			section ("Presence Device Name:") {
				found = true
				def pLbl = getNestPresLabel()
				def dni = getNestPresId()
				def d3 = getChildDevice(dni)
				def dstr = ""
				if(d3) {
					dstr += "Found: ${d3.displayName}"
					if(d3.displayName != pLbl) {
						dstr += "$str1 ${pLbl}"
					}
					else if(atomicState?.custLabelUsed || atomicState?.useAltNames) { dstr += "$str2" }
				} else {
					dstr += "New Name: ${pLbl}"
				}
				paragraph "${dstr}", state: "complete", image: (atomicState.custLabelUsed && !d3) ? " " : getAppImg("presence_icon.png")
				if(atomicState.custLabelUsed && !d3) {
					input "presDev_lbl", "text", title: "Custom name for Nest Presence Device", defaultValue: pLbl, submitOnChange: true, image: getAppImg("presence_icon.png")
				}
			}
		}
		if(atomicState?.weatherDevice) {
			section ("Weather Device Name:") {
				found = true
				def wLbl = getNestWeatherLabel()
				def dni = getNestWeatherId()
				def d4 = getChildDevice(dni)
				def dstr = ""
				if(d4) {
					dstr += "Found: ${d4.displayName}"
					if(d4.displayName != wLbl) {
						dstr += "$str1 ${wLbl}"
					}
					else if(atomicState?.custLabelUsed || atomicState?.useAltNames) { dstr += "$str2" }
				} else {
					dstr += "New Name: ${wLbl}"
				}
				paragraph "${dstr}", state: "complete", image: (atomicState.custLabelUsed && !d4) ? " " : getAppImg("weather_icon.png")
				if(atomicState.custLabelUsed && !d4) {
					input "weathDev_lbl", "text", title: "Custom name for Nest Weather Device", defaultValue: wLbl, submitOnChange: true, image: getAppImg("weather_icon.png")
				}
			}
		}
		if(!found) {
			paragraph "No Devices Selected"
		}
	}
}

def deviceHandlerTest() {
	//log.trace "deviceHandlerTest()"
	atomicState.devHandlersTested = true
	return true

	if(atomicState?.devHandlersTested || atomicState?.isInstalled || (atomicState?.thermostats && atomicState?.protects && atomicState?.cameras && atomicState?.vThermostats && atomicState?.presDevice && atomicState?.weatherDevice)) {
		atomicState.devHandlersTested = true
		return true
	}
	try {
		def d1 = addChildDevice(app.namespace, getThermostatChildName(), "testNestThermostat-Install123", null, [label:"Nest Thermostat:InstallTest"])
		def d2 = addChildDevice(app.namespace, getPresenceChildName(), "testNestPresence-Install123", null, [label:"Nest Presence:InstallTest"])
		def d3 = addChildDevice(app.namespace, getProtectChildName(), "testNestProtect-Install123", null, [label:"Nest Protect:InstallTest"])
		def d4 = addChildDevice(app.namespace, getWeatherChildName(), "testNestWeather-Install123", null, [label:"Nest Weather:InstallTest"])
		def d5 = addChildDevice(app.namespace, getCameraChildName(), "testNestCamera-Install123", null, [label:"Nest Camera:InstallTest"])
		def d6 = addChildDevice(app.namespace, getvThermostatChildName(), "testNestvThermostat-Install123", null, [label:"Nest vThermostat:InstallTest"])

		log.debug "d1: ${d1.label} | d2: ${d2.label} | d3: ${d3.label} | d4: ${d4.label} | d5: ${d5.label} | d6: ${d6.label}"
		atomicState.devHandlersTested = true
		removeTestDevs()
		//runIn(4, "removeTestDevs")
		return true
	}
	catch (ex) {
		if(ex instanceof physicalgraph.app.exception.UnknownDeviceTypeException) {
			LogAction("Device Handlers are missing: ${getThermostatChildName()}, ${getPresenceChildName()}, and ${getProtectChildName()}, Verify the Device Handlers are installed and Published via the IDE", "error", true)
		} else {
			log.error "deviceHandlerTest Exception:", ex
			sendExceptionData(ex.message, "deviceHandlerTest")
		}
		atomicState.devHandlersTested = false
		return false
	}
}

def removeTestDevs() {
	try {
		def names = [ "testNestThermostat-Install123", "testNestPresence-Install123", "testNestProtect-Install123", "testNestWeather-Install123", "testNestCamera-Install123", "testNestvThermostat-Install123" ]
		names?.each { dev ->
			//log.debug "dev: $dev"
			def delete = getChildDevices().findAll { it?.deviceNetworkId == dev }
			//log.debug "delete: ${delete}"
			if(delete) {
			   delete.each { deleteChildDevice(it.deviceNetworkId) }
			}
		}
	} catch (ex) {
		log.error "deviceHandlerTest Exception:", ex
		sendExceptionData(ex.message, "removeTestDevs")
	}
}

def preReqCheck() {
	//log.trace "preReqCheckTest()"
	generateInstallId()
	if(!location?.timeZone || !location?.zipCode) {
		atomicState.preReqTested = false
		LogAction("SmartThings Location is not returning (TimeZone: ${location?.timeZone}) or (ZipCode: ${location?.zipCode}) Please edit these settings under the IDE...", "warn", true)
		return false
	}
	else {
		atomicState.preReqTested = true
		return true
	}
}

//This code really does nothing at the moment but return the dynamic url of the app's endpoints
def getEndpointUrl() {
	def params = [
		uri: "https://graph.api.smartthings.com/api/smartapps/endpoints",
		query: ["access_token": atomicState?.accessToken],
		   contentType: 'application/json'
	]
	try {
		httpGet(params) { resp ->
			LogAction("EndPoint URL: ${resp?.data?.uri}", "trace", false, false, true)
			return resp?.data?.uri
		}
	} catch (ex) {
		log.error "getEndpointUrl Exception:", ex
		sendExceptionData(ex.message, "getEndpointUrl")
	}
}

def getAccessToken() {
	try {
		if(!atomicState?.accessToken) { atomicState?.accessToken = createAccessToken() }
		else { return true }
	}
	catch (ex) {
		def msg = "Error: OAuth is not Enabled for the Nest Manager application!!!.  Please click remove and Enable Oauth under the SmartApp App Settings in the IDE..."
		sendPush(msg)
		LogAction("getAccessToken Exception | $msg", "warn", true)
		sendExceptionData(ex.message, "getAccessToken")
		return false
	}
}

def generateInstallId() {
	if(!atomicState?.installationId) { atomicState?.installationId = UUID?.randomUUID().toString() }
}

/************************************************************************************************
|					Below This line handle SmartThings >> Nest Token Authentication				|
*************************************************************************************************/

//These are the Nest OAUTH Methods to aquire the auth code and then Access Token.
def oauthInitUrl() {
	//log.debug "oauthInitUrl with callback: ${callbackUrl}"
	atomicState.oauthInitState = UUID?.randomUUID().toString()
	def oauthParams = [
		response_type: "code",
		client_id: clientId(),
		state: atomicState?.oauthInitState,
		redirect_uri: callbackUrl //"https://graph.api.smartthings.com/oauth/callback"
	]
	redirect(location: "https://home.nest.com/login/oauth2?${toQueryString(oauthParams)}")
}

def callback() {
	try {
		LogTrace("callback()>> params: $params, params.code ${params.code}")
		def code = params.code
		LogTrace("Callback Code: ${code}")
		def oauthState = params.state
		LogTrace("Callback State: ${oauthState}")

		if(oauthState == atomicState?.oauthInitState){
			def tokenParams = [
				code: code.toString(),
				client_id: clientId(),
				client_secret: clientSecret(),
				grant_type: "authorization_code",
			]
			def tokenUrl = "https://api.home.nest.com/oauth2/access_token?${toQueryString(tokenParams)}"
			httpPost(uri: tokenUrl) { resp ->
				atomicState.tokenExpires = resp?.data.expires_in
				atomicState.authToken = resp?.data.access_token
				if(atomicState?.authToken) { atomicState?.tokenCreatedDt = getDtNow() }
			}

			if(atomicState?.authToken) {
				LogAction("Nest AuthToken Generated Successfully...", "info", true)
				generateInstallId
				success()
			} else {
				LogAction("There was a Failure Generating the Nest AuthToken!!!", "error", true)
				fail()
			}
		}
		else { LogAction("callback() failed oauthState != atomicState.oauthInitState", "error", true) }
	}
	catch (ex) {
		log.error "Callback Exception:", ex
		sendExceptionData(ex.message, "callback")
	}
}

def revokeNestToken() {
	def params = [
		uri: "https://api.home.nest.com",
		path: "/oauth2/access_tokens/${atomicState?.authToken}",
		contentType: 'application/json'
	]
	try {
		httpDelete(params) { resp ->
			if(resp?.status == 204) {
				atomicState?.authToken = null
				LogAction("Your Nest Token has been revoked successfully...", "warn", true)
				return true
			}
		}
	}
	catch (ex) {
		log.error "revokeNestToken Exception:", ex
		sendExceptionData(ex.message, "revokeNestToken")
		return false
	}
}

//HTML Connections Pages
def success() {
	def message = """
	<p>Your SmartThings Account is now connected to Nest!</p>
	<p>Click 'Done' to finish setup.</p>
	"""
	connectionStatus(message)
}

def fail() {
	def message = """
	<p>The connection could not be established!</p>
	<p>Click 'Done' to return to the menu.</p>
	"""
	connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
	def redirectHtml = ""
	if(redirectUrl) { redirectHtml = """<meta http-equiv="refresh" content="3; url=${redirectUrl}" />""" }

	def html = """
		<!DOCTYPE html>
		<html>
		<head>
		<meta name="viewport" content="width=640">
		<title>SmartThings & Nest connection</title>
		<style type="text/css">
				@font-face {
						font-family: 'Swiss 721 W01 Thin';
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
						font-weight: normal;
						font-style: normal;
				}
				@font-face {
						font-family: 'Swiss 721 W01 Light';
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
						src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
								url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
						font-weight: normal;
						font-style: normal;
				}
				.container {
						width: 90%;
						padding: 4%;
						/*background: #eee;*/
						text-align: center;
				}
				img {
						vertical-align: middle;
				}
				p {
						font-size: 2.2em;
						font-family: 'Swiss 721 W01 Thin';
						text-align: center;
						color: #666666;
						padding: 0 40px;
						margin-bottom: 0;
				}
				span {
						font-family: 'Swiss 721 W01 Light';
				}
		</style>
		</head>
		<body>
				<div class="container">
						<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
						<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
						<img src="https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_icon128.png" alt="nest icon" />
						${message}
				</div>
		</body>
		</html>
		"""
	render contentType: 'text/html', data: html
}

def toJson(Map m) {
	return new org.json.JSONObject(m).toString()
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def clientId() {
	if(!appSettings.clientId) {
		def tokenNum = atomicState?.appData?.token?.tokenNum?.toInteger() ?: 1
		switch(tokenNum) {
			case 1:
				return "63e9befa-dc62-4b73-aaf4-dcf3826dd704" // Original Token Updated with Cam/Image Support
				break
			case 2:
				return "31aea46c-4048-4c2b-b6be-cac7fe305d4c" //token v2 with cam support
				break
		}
	} else {
		return appSettings.clientId
	}
}

def clientSecret() {
	if(!appSettings.clientSecret) {
		def tokenNum = atomicState?.appData?.token?.tokenNum?.toInteger() ?: 1
		switch(tokenNum) {
			case 1:
				return "8iqT8X46wa2UZnL0oe3TbyOa0" // Original Token Updated with Cam/Image Support
				break
			case 2:
				return "FmO469GXfdSVjn7PhKnjGWZlm" //token v2 with cam support
				break
		}
	} else {
		return appSettings.clientSecret
	}
}

/************************************************************************************************
|									LOGGING AND Diagnostic										|
*************************************************************************************************/
def LogTrace(msg) {
	def trOn = advAppDebug ? true : false
	if(trOn) { Logger(msg, "trace") }
}

def LogAction(msg, type = "debug", showAlways = false) {
	def isDbg = parent ? ((atomicState?.showDebug || showDebug)  ? true : false) : (appDebug ? true : false)
	if(showAlways) { Logger(msg, type) }

	else if(isDbg && !showAlways) { Logger(msg, type) }
}

def Logger(msg, type) {
	if(msg && type) {
		def labelstr = ""
		if(!atomicState?.debugAppendAppName) {
			atomicState?.debugAppendAppName = (parent ? parent?.settings?.debugAppendAppName : settings?.debugAppendAppName) ? true : false
		}
		if(atomicState?.debugAppendAppName) { labelstr = "${app.label} | " }
		switch(type) {
			case "debug":
				log.debug "${labelstr}${msg}"
				break
			case "info":
				log.info "${labelstr}${msg}"
				break
			case "trace":
				log.trace "${labelstr}${msg}"
				break
			case "error":
				log.error "${labelstr}${msg}"
				break
			case "warn":
				log.warn "${labelstr}${msg}"
				break
			default:
				log.debug "${labelstr}${msg}"
				break
		}
	}
	else { log.error "Logger Error - type: ${type} | msg: ${msg}" }
}

def setStateVar(frc = false) {
	//log.trace "setStateVar..."
	//If the developer changes the version in the web appParams JSON it will trigger
	//the app to create any new state values that might not exist or reset those that do to prevent errors
	def stateVer = 3
	def stateVar = !atomicState?.stateVarVer ? 0 : atomicState?.stateVarVer.toInteger()
	if(!atomicState?.stateVarUpd || frc || (stateVer < atomicState?.appData.state.stateVarVer.toInteger())) {
		if(!atomicState?.newSetupComplete)		{ atomicState.newSetupComplete = false }
		if(!atomicState?.setupVersion)			{ atomicState?.setupVersion = 0 }
		if(!atomicState?.misPollNotifyWaitVal)		{ atomicState.misPollNotifyWaitVal = 900 }
		if(!atomicState?.misPollNotifyMsgWaitVal)	{ atomicState.misPollNotifyMsgWaitVal = 3600 }
		if(!atomicState?.updNotifyWaitVal)		{ atomicState.updNotifyWaitVal = 7200 }
		if(!atomicState?.custLabelUsed)			{ atomicState?.custLabelUsed = false }
		if(!atomicState?.useAltNames)			{ atomicState.useAltNames = false }
		if(!atomicState?.apiCommandCnt)			{ atomicState?.apiCommandCnt = 0 }
		atomicState?.stateVarUpd = true
		atomicState?.stateVarVer = atomicState?.appData?.state?.stateVarVer ? atomicState?.appData?.state?.stateVarVer?.toInteger() : 0
	}
}

//Things that I need to clear up on updates go here
//IMPORTANT: This must be run in it's own thread, and exit after running as the cleanup occurs on exit
def stateCleanup() {
	LogAction("stateCleanup...", "trace", true)

	state.remove("exLogs")
	state.remove("pollValue")
	state.remove("pollStrValue")
	state.remove("pollWaitVal")
	state.remove("tempChgWaitVal")
	state.remove("cmdDelayVal")
	state.remove("testedDhInst")
	state.remove("missedPollNotif")
	state.remove("updateMsgNotif")
	state.remove("updChildOnNewOnly")
	state.remove("disAppIcons")
	state.remove("showProtAlarmStateEvts")
	state.remove("showAwayAsAuto")
	state.remove("cmdQ")
	state.remove("recentSendCmd")
	state.remove("currentWeather")
	state.remove("altNames")
	state.remove("locstr")
	state.remove("custLocStr")
	state.remove("autoAppInstalled")
	state.remove("nestStructures")
	if(!atomicState?.cmdQlist) {
		state.remove("cmdQ2")
		state.remove("cmdQ3")
		state.remove("cmdQ4")
		state.remove("cmdQ5")
		state.remove("cmdQ6")
		state.remove("cmdQ7")
		state.remove("cmdQ8")
		state.remove("cmdQ9")
		state.remove("cmdQ10")
		state.remove("cmdQ11")
		state.remove("cmdQ12")
		state.remove("cmdQ13")
		state.remove("cmdQ14")
		state.remove("cmdQ15")
		state.remove("lastCmdSentDt2")
		state.remove("lastCmdSentDt3")
		state.remove("lastCmdSentDt4")
		state.remove("lastCmdSentDt5")
		state.remove("lastCmdSentDt6")
		state.remove("lastCmdSentDt7")
		state.remove("lastCmdSentDt8")
		state.remove("lastCmdSentDt9")
		state.remove("lastCmdSentDt10")
		state.remove("lastCmdSentDt11")
		state.remove("lastCmdSentDt12")
		state.remove("lastCmdSentDt13")
		state.remove("lastCmdSentDt14")
		state.remove("lastCmdSentDt15")
		state.remove("recentSendCmd2")
		state.remove("recentSendCmd3")
		state.remove("recentSendCmd4")
		state.remove("recentSendCmd5")
		state.remove("recentSendCmd6")
		state.remove("recentSendCmd7")
		state.remove("recentSendCmd8")
		state.remove("recentSendCmd9")
		state.remove("recentSendCmd10")
		state.remove("recentSendCmd11")
		state.remove("recentSendCmd12")
		state.remove("recentSendCmd13")
		state.remove("recentSendCmd14")
		state.remove("recentSendCmd15")
	}
}

/******************************************************************************
*					Keep These Methods						  *
*******************************************************************************/
def getThermostatChildName() { return getChildName("Nest Thermostat") }
def getProtectChildName()    { return getChildName("Nest Protect") }
def getPresenceChildName()   { return getChildName("Nest Presence") }
def getWeatherChildName()    { return getChildName("Nest Weather") }
def getCameraChildName()     { return getChildName("Nest Camera") }
def getvThermostatChildName() { return getChildName("Nest Virtual Thermostat") }

def getAutoAppChildName()    { return getChildName("Nest Automations") }
def getWatchdogAppChildName(){ return getChildName("Nest Location ${location.name} Watchdog") }
def getWebDashAppChildName() { return getChildName("Nest Web Dashboard") }

def getChildName(str)     { return "${str}${appDevName()}" }

def getServerUrl()		{ return "https://graph.api.smartthings.com" }
def getShardUrl()		{ return getApiServerUrl() }
def getCallbackUrl()		{ return "https://graph.api.smartthings.com/oauth/callback" }
def getBuildRedirectUrl()	{ return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState?.accessToken}&apiServerUrl=${shardUrl}" }
def getNestApiUrl()		{ return "https://developer-api.nest.com" }
def getAppEndpointUrl(subPath)	{ return "${apiServerUrl("/api/smartapps/installations/${app.id}/${subPath}?access_token=${atomicState.accessToken}")}" }
def getHelpPageUrl()		{ return "https://rawgit.com/tonesto7/nest-manager/${gitBranch()}/Documents/help-page.html" }
def getReadmePageUrl()		{ return "https://rawgit.com/tonesto7/nest-manager/${gitBranch()}/README.html" }
def getAutoHelpPageUrl()	{ return "https://rawgit.com/tonesto7/nest-manager/${gitBranch()}/Documents/help/nest-automations.html" }
def getFirebaseAppUrl() 	{ return "https://st-nest-manager.firebaseio.com" }
def getAppImg(imgName, on = null)	{ return (!disAppIcons || on) ? "https://raw.githubusercontent.com/tonesto7/nest-manager/${gitBranch()}/Images/App/$imgName" : "" }
def getDevImg(imgName, on = null)	{ return (!disAppIcons || on) ? "https://raw.githubusercontent.com/tonesto7/nest-manager/${gitBranch()}/Images/Devices/$imgName" : "" }

def latestTstatVer()    { return atomicState?.appData?.updater?.versions?.thermostat ?: "unknown" }
def latestProtVer()     { return atomicState?.appData?.updater?.versions?.protect ?: "unknown" }
def latestPresVer()     { return atomicState?.appData?.updater?.versions?.presence ?: "unknown" }
def latestWeathVer()    { return atomicState?.appData?.updater?.versions?.weather ?: "unknown" }
def latestCamVer()      { return atomicState?.appData?.updater?.versions?.camera ?: "unknown" }
def latestvStatVer()    { return atomicState?.appData?.updater?.versions?.vthermostat ?: "unknown" }
def latestWebDashVer()  { return atomicState?.appData?.updater?.versions?.webDashApp ?: "unknown" }
def getUse24Time()      { return useMilitaryTime ? true : false }

//Returns app State Info
def getStateSize()      { return state?.toString().length() }
def getStateSizePerc()  { return (int) ((stateSize/100000)*100).toDouble().round(0) }

def debugStatus() { return !appDebug ? "Off" : "On" }
def deviceDebugStatus() { return !childDebug ? "Off" : "On" }
def isAppDebug() { return !appDebug ? false : true }
def isChildDebug() { return !childDebug ? false : true }

def getLocationModes() {
	def result = []
	location?.modes.sort().each {
		if(it) { result.push("${it}") }
	}
	return result
}

def getShowHelp() { return atomicState?.showHelp == false ? false : true }

def getTimeZone() {
	def tz = null
	if(location?.timeZone) { tz = location?.timeZone }
	else { tz = TimeZone.getTimeZone(getNestTimeZone()) }
	if(!tz) { LogAction("getTimeZone: Hub or Nest TimeZone is not found ...", "warn", true) }
	return tz
}

def formatDt(dt) {
	def tf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
	if(getTimeZone()) { tf.setTimeZone(getTimeZone()) }
	else {
		LogAction("SmartThings TimeZone is not found or is not set... Please Try to open your ST location and Press Save...", "warn", true)
	}
	return tf.format(dt)
}

def GetTimeDiffSeconds(lastDate) {
	if(lastDate?.contains("dtNow")) { return 10000 }
	def now = new Date()
	def lastDt = Date.parse("E MMM dd HH:mm:ss z yyyy", lastDate)
	def start = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(lastDt)).getTime()
	def stop = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(now)).getTime()
	def diff = (int) (long) (stop - start) / 1000
	return diff
}

def daysOk(days) {
	if(days) {
		def dayFmt = new SimpleDateFormat("EEEE")
		if(getTimeZone()) { dayFmt.setTimeZone(getTimeZone()) }
		return days.contains(dayFmt.format(new Date())) ? false : true
	} else { return true }
}

// parent only Method
def notificationTimeOk() {
	try {
		def strtTime = null
		def stopTime = null
		def now = new Date()
		def sun = getSunriseAndSunset() // current based on geofence, previously was: def sun = getSunriseAndSunset(zipCode: zipCode)
		if(settings?.qStartTime && settings?.qStopTime) {
			if(settings?.qStartInput == "sunset") { strtTime = sun.sunset }
			else if(settings?.qStartInput == "sunrise") { strtTime = sun.sunrise }
			else if(settings?.qStartInput == "A specific time" && settings?.qStartTime) { strtTime = settings?.qStartTime }

			if(settings?.qStopInput == "sunset") { stopTime = sun.sunset }
			else if(settings?.qStopInput == "sunrise") { stopTime = sun.sunrise }
			else if(settings?.qStopInput == "A specific time" && settings?.qStopTime) { stopTime = settings?.qStopTime }
		} else { return true }
		if(strtTime && stopTime) {
			return timeOfDayIsBetween(strtTime, stopTime, new Date(), getTimeZone()) ? false : true
		} else { return true }
	} catch (ex) {
		log.error "notificationTimeOk Exception:", ex
		sendExceptionData(ex.message, "notificationTimeOk")
	}
}

def time2Str(time) {
	if(time) {
		def t = timeToday(time, getTimeZone())
		def f = new java.text.SimpleDateFormat("h:mm a")
		f.setTimeZone(getTimeZone() ?: timeZone(time))
		f.format(t)
	}
}

def epochToTime(tm) {
	def tf = new SimpleDateFormat("h:mm a")
		tf?.setTimeZone(getTimeZone())
	return tf.format(tm)
}

def getDtNow() {
	def now = new Date()
	return formatDt(now)
}

def modesOk(modeEntry) {
	def res = true
	if(modeEntry) {
		modeEntry?.each { m ->
			if(m.toString() == location?.mode.toString()) { res = false }
		}
	}
	return res
}

def isInMode(modeList) {
	if(modeList) {
		//log.debug "mode (${location.mode}) in list: ${modeList} | result: (${location?.mode in modeList})"
		return location.mode.toString() in modeList
	}
	return false
}

def minDevVersions() {
	return [
		"thermostat":["val":310, "desc":"3.1.0"],
		"protect":["val":310, "desc":"3.1.0"],
		"presence":["val":310, "desc":"3.1.0"],
		"weather":["val":310, "desc":"3.1.0"],
		"camera":["val":111 , "desc":"1.1.1"],
		"vthermostat":["val":310, "desc":"3.1.0"]
	]
}

def notifValEnum(allowCust = true) {
	def valsC = [
		60:"1 Minute", 300:"5 Minutes", 600:"10 Minutes", 900:"15 Minutes", 1200:"20 Minutes", 1500:"25 Minutes", 1800:"30 Minutes",
		3600:"1 Hour", 7200:"2 Hours", 14400:"4 Hours", 21600:"6 Hours", 43200:"12 Hours", 86400:"24 Hours", 1000000:"Custom"
	]
	def vals = [
		60:"1 Minute", 300:"5 Minutes", 600:"10 Minutes", 900:"15 Minutes", 1200:"20 Minutes", 1500:"25 Minutes",
		1800:"30 Minutes", 3600:"1 Hour", 7200:"2 Hours", 14400:"4 Hours", 21600:"6 Hours", 43200:"12 Hours", 86400:"24 Hours"
	]
	return allowCust ? valsC : vals
}

def pollValEnum() {
	def vals = [
		60:"1 Minute", 120:"2 Minutes", 180:"3 Minutes", 240:"4 Minutes", 300:"5 Minutes",
		600:"10 Minutes", 900:"15 Minutes", 1200:"20 Minutes", 1500:"25 Minutes",
		1800:"30 Minutes", 2700:"45 Minutes", 3600:"60 Minutes"
	]
	return vals
}

def waitValEnum() {
	def vals = [
		1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds",
		8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 15:"15 Seconds", 30:"30 Seconds"
	]
	return vals
}

def strCapitalize(str) {
	return str ? str?.toString().capitalize() : null
}

def getInputEnumLabel(inputName, enumName) {
	def result = "Not Set"
	if(input && enumName) {
		enumName.each { item ->
			if(item?.key.toString() == inputName?.toString()) {
				result = item?.value
			}
		}
	}
	return result
}

def getShowProtAlarmEvts() { return showProtAlarmStateEvts ? true : false }

/******************************************************************************
|					Application Pages						  |
*******************************************************************************/
def pollPrefPage() {
	dynamicPage(name: "pollPrefPage", install: false) {
		section("") {
			paragraph "Polling Preferences", image: getAppImg("timer_icon.png")
		}
		section("Device Polling:") {
			input ("pollValue", "enum", title: "Device Poll Rate", required: false, defaultValue: 180, metadata: [values:pollValEnum()],
					submitOnChange: true)
		}
		section("Location Polling:") {
			input ("pollStrValue", "enum", title: "Location Poll Rate", required: false, defaultValue: 180, metadata: [values:pollValEnum()],
					submitOnChange: true)
		}
		if(atomicState?.weatherDevice) {
			section("Weather Polling:") {
				input ("pollWeatherValue", "enum", title: "Weather Refresh Rate", required: false, defaultValue: 900, metadata: [values:notifValEnum()],
						submitOnChange: true)
			}
		}
		section("Wait Values:") {
			input ("pollWaitVal", "enum", title: "Forced Poll Refresh Limit", required: false, defaultValue: 10, metadata: [values:waitValEnum()],
					submitOnChange: true)
		}
	}
}

def getPollingConfDesc() {
	def pollValDesc = (!pollValue || pollValue == 180) ? "" : " (Custom)"
	def pollStrValDesc = (!pollStrValue || pollStrValue == 180) ? "" : " (Custom)"
	def pollWeatherValDesc = (!pollWeatherValue || pollWeatherValue == 900) ? "" : " (Custom)"
	def pollWaitValDesc = (!pollWaitVal || pollWaitVal == 10) ? "" : " (Custom)"
	def pStr = ""
	pStr += "Polling: (${!atomicState?.pollingOn ? "Not Active" : "Active"})"
	pStr += "\n • Device: (${getInputEnumLabel(pollValue?:180, pollValEnum())})"
	pStr += "\n • Structure: (${getInputEnumLabel(pollStrValue?:180, pollValEnum())})"
	pStr += atomicState?.weatherDevice ? "\n• Weather Polling: (${getInputEnumLabel(pollWeatherValue?:900, notifValEnum())})" : ""
	pStr += "\n • Forced Poll Refresh Limit:\n    └ (${getInputEnumLabel(pollWaitVal ?: 10, waitValEnum())})"
	return ((pollValDesc || pollStrValDesc || pollWEatherValDesc || pollWaitValDesc) ? pStr : "")
}

def notifPrefPage() {
	dynamicPage(name: "notifPrefPage", install: false) {
		def sectDesc = !location.contactBookEnabled ? "Enable push notifications below..." : "Select People or Devices to Receive Notifications..."
		section(sectDesc) {
			if(!location.contactBookEnabled) {
				input(name: "usePush", type: "bool", title: "Send Push Notitifications", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("notification_icon.png"))
			} else {
				input(name: "recipients", type: "contact", title: "Send notifications to", required: false, submitOnChange: true, image: getAppImg("recipient_icon.png")) {
					input ("phone", "phone", title: "Phone Number to send SMS to...", required: false, submitOnChange: true, image: getAppImg("notification_icon.png"))
				}
			}
		}

		if(settings?.recipients || settings?.phone || settings?.usePush) {
			if(settings?.recipients && !atomicState?.pushTested) {
				sendMsg("Info", "Push Notification Test Successful... Notifications have been Enabled for ${textAppName()}")
				atomicState.pushTested = true
			} else { atomicState.pushTested = true }

			section(title: "Time Restrictions") {
				href "setNotificationTimePage", title: "Silence Notifications...", description: (getNotifSchedDesc() ?: "Tap to configure..."), params: [pName: ""], state: (getNotifSchedDesc() ? "complete" : null),
					image: getAppImg("quiet_time_icon.png")
			}
			section("Missed Poll Notification:") {
				input (name: "sendMissedPollMsg", type: "bool", title: "Send Missed Poll Messages?", defaultValue: true, submitOnChange: true, image: getAppImg("late_icon.png"))
				if(sendMissedPollMsg == null || sendMissedPollMsg) {
					def misPollNotifyWaitValDesc = !misPollNotifyWaitVal ? "Default: 15 Minutes" : misPollNotifyWaitVal
					input (name: "misPollNotifyWaitVal", type: "enum", title: "Time Past the missed Poll?", required: false, defaultValue: 900, metadata: [values:notifValEnum()], submitOnChange: true)
					if(misPollNotifyWaitVal) {
						atomicState.misPollNotifyWaitVal = !misPollNotifyWaitVal ? 900 : misPollNotifyWaitVal.toInteger()
						if(misPollNotifyWaitVal.toInteger() == 1000000) {
							input (name: "misPollNotifyWaitValCust", type: "number", title: "Custom Missed Poll Value in Seconds", range: "60..86400", required: false, defaultValue: 900, submitOnChange: true)
							if(misPollNotifyWaitValCust) { atomicState?.misPollNotifyWaitVal = misPollNotifyWaitValCust ? misPollNotifyWaitValCust.toInteger() : 900 }
						}
					} else { atomicState.misPollNotifyWaitVal = !misPollNotifyWaitVal ? 900 : misPollNotifyWaitVal.toInteger() }

					def misPollNotifyMsgWaitValDesc = !misPollNotifyMsgWaitVal ? "Default: 1 Hour" : misPollNotifyMsgWaitVal
					input (name: "misPollNotifyMsgWaitVal", type: "enum", title: "Delay before sending again?", required: false, defaultValue: 3600, metadata: [values:notifValEnum()], submitOnChange: true)
					if(misPollNotifyMsgWaitVal) {
						atomicState.misPollNotifyMsgWaitVal = !misPollNotifyMsgWaitVal ? 3600 : misPollNotifyMsgWaitVal.toInteger()
						if(misPollNotifyMsgWaitVal.toInteger() == 1000000) {
							input (name: "misPollNotifyMsgWaitValCust", type: "number", title: "Custom Msg Wait Value in Seconds", range: "60..86400", required: false, defaultValue: 3600, submitOnChange: true)
							if(misPollNotifyMsgWaitValCust) { atomicState.misPollNotifyMsgWaitVal = misPollNotifyMsgWaitValCust ? misPollNotifyMsgWaitValCust.toInteger() : 3600 }
						}
					} else { atomicState.misPollNotifyMsgWaitVal = !misPollNotifyMsgWaitVal ? 3600 : misPollNotifyMsgWaitVal.toInteger() }
				}
			}
			section("App and Device Updates:") {
				input (name: "sendAppUpdateMsg", type: "bool", title: "Send for Updates...", defaultValue: true, submitOnChange: true, image: getAppImg("update_icon.png"))
				if(sendMissedPollMsg == null || sendAppUpdateMsg) {
					def updNotifyWaitValDesc = !updNotifyWaitVal ? "Default: 2 Hours" : updNotifyWaitVal
					input (name: "updNotifyWaitVal", type: "enum", title: "Send reminders every?", required: false, defaultValue: 7200, metadata: [values:notifValEnum()], submitOnChange: true)
					if(updNotifyWaitVal) {
						atomicState.updNotifyWaitVal = !updNotifyWaitVal ? 7200 : updNotifyWaitVal.toInteger()
						if(updNotifyWaitVal.toInteger() == 1000000) {
							input (name: "updNotifyWaitValCust", type: "number", title: "Custom Missed Poll Value in Seconds", range: "30..86400", required: false, defaultValue: 7200, submitOnChange: true)
							if(updNotifyWaitValCust) { atomicState.updNotifyWaitVal = updNotifyWaitValCust ? updNotifyWaitValCust.toInteger() : 7200 }
						}
					} else { atomicState.updNotifyWaitVal = !updNotifyWaitVal ? 7200 : updNotifyWaitVal.toInteger() }
				}
			}
		} else { atomicState.pushTested = false }
	}
}

// Parent only method
def getNotifSchedDesc() {
	def sun = getSunriseAndSunset()
	//def schedInverted = settings?.DmtInvert
	def startInput = settings?.qStartInput
	def startTime = settings?.qStartTime
	def stopInput = settings?.qStopInput
	def stopTime = settings?.qStopTime
	def dayInput = settings?.quietDays
	def modeInput = settings?.quietModes
	def notifDesc = ""
	def getNotifTimeStartLbl = ( (startInput == "Sunrise" || startInput == "Sunset") ? ( (startInput == "Sunset") ? epochToTime(sun?.sunset.time) : epochToTime(sun?.sunrise.time) ) : (startTime ? time2Str(startTime) : "") )
	def getNotifTimeStopLbl = ( (stopInput == "Sunrise" || stopInput == "Sunset") ? ( (stopInput == "Sunset") ? epochToTime(sun?.sunset.time) : epochToTime(sun?.sunrise.time) ) : (stopTime ? time2Str(stopTime) : "") )
	notifDesc += (getNotifTimeStartLbl && getNotifTimeStopLbl) ? " • Silent Time: ${getNotifTimeStartLbl} - ${getNotifTimeStopLbl}" : ""
	def days = getInputToStringDesc(dayInput)
	def modes = getInputToStringDesc(modeInput)
	notifDesc += days ? "${(getNotifTimeStartLbl || getNotifTimeStopLbl) ? "\n" : ""} • Silent Day${isPluralString(dayInput)}: ${days}" : ""
	notifDesc += modes ? "${(getNotifTimeStartLbl || getNotifTimeStopLbl || days) ? "\n" : ""} • Silent Mode${isPluralString(modeInput)}: ${modes}" : ""
	return (notifDesc != "") ? "${notifDesc}" : null
}

// Parent only method
def setNotificationTimePage() {
	dynamicPage(name: "setNotificationTimePage", title: "Prevent Notifications\nDuring these Days, Times or Modes", uninstall: false) {
		def timeReq = (settings["qStartTime"] || settings["qStopTime"]) ? true : false
		section() {
			input "qStartInput", "enum", title: "Starting at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: null, submitOnChange: true, required: false, image: getAppImg("start_time_icon.png")
			if(settings["qStartInput"] == "A specific time") {
				input "qStartTime", "time", title: "Start time", required: timeReq, image: getAppImg("start_time_icon.png")
			}
			input "qStopInput", "enum", title: "Stopping at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: null, submitOnChange: true, required: false, image: getAppImg("stop_time_icon.png")
			if(settings?."qStopInput" == "A specific time") {
				input "qStopTime", "time", title: "Stop time", required: timeReq, image: getAppImg("stop_time_icon.png")
			}
			input "quietDays", "enum", title: "Only on these days of the week", multiple: true, required: false, image: getAppImg("day_calendar_icon.png"), options: timeDayOfWeekOptions()
			input "quietModes", "mode", title: "When these Modes are Active", multiple: true, submitOnChange: true, required: false, image: getAppImg("mode_icon.png")
		}
	}
}

def getAppNotifConfDesc() {
	def str = ""
	str += pushStatus() ? "Notifications:" : ""
	str += (pushStatus() && settings?.recipients) ? "\n • Contacts: (${settings?.recipients?.size()})" : ""
	str += (pushStatus() && settings?.usePush) ? "\n • Push Messages: Enabled" : ""
	str += (pushStatus() && sms) ? "\n • SMS: (${sms?.size()})" : ""
	str += (pushStatus() && settings?.phone) ? "\n • SMS: (${settings?.phone?.size()})" : ""
	str += (pushStatus() && getNotifSchedDesc()) ? "\n${getNotifSchedDesc()}" : ""
	return pushStatus() ? "${str}" : null
}

def devPrefPage() {
	dynamicPage(name: "devPrefPage", title: "Device Preferences", uninstall: false) {
		if(settings?.thermostats || settings?.protects || settings?.presDevice || settings?.weatherDevice) {
			section("Device Name Customization:") {
				def devDesc = (atomicState?.custLabelUsed || atomicState?.useAltNames) ? "Custom Labels Set...\n\nTap to Modify..." : "Tap to Configure..."
				href "devNamePage", title: "Device Names...", description: devDesc, state:(atomicState?.custLabelUsed || atomicState?.useAltNames) ? "complete" : "", image: getAppImg("device_name_icon.png")
			}
		}
		if(atomicState?.thermostats) {
			section("Thermostat Devices:") {
				input ("tempChgWaitVal", "enum", title: "Manual Temp Change Delay", required: false, defaultValue: 4, metadata: [values:waitValEnum()], submitOnChange: true, image: getAppImg("temp_icon.png"))
				atomicState.needChildUpd = true
			}
		}
		if(atomicState?.protects) {
			section("Protect Devices:") {
				input "showProtActEvts", "bool", title: "Show Non-Alarm Events in Device Activity Feed?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("list_icon.png")
				atomicState.needChildUpd = true
			}
		}
		if(atomicState?.vThermostats) {
			section("Virtual Thermostat Devices:") {
				paragraph "Nothing to Show!!!"
			}
		}
		if(atomicState?.presDevice) {
			section("Presence Device:") {
				paragraph "Nothing to Show!!!"
			}
		}
		if(atomicState?.weatherDevice) {
			section("Weather Device:") {
				href "custWeatherPage", title: "Customize Weather Location?", description: (getWeatherConfDesc() ? "${getWeatherConfDesc()}\n\nTap to Modify..." : ""), state: (getWeatherConfDesc() ? "complete":""), image: getAppImg("weather_icon_grey.png")
				input ("weathAlertNotif", "bool", title: "Notify on Weather Alerts?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("weather_icon.png"))
			}
		}
	}
}

def custWeatherPage() {
	dynamicPage(name: "custWeatherPage", title: "", nextPage: "", install: false) {
		section("Set Custom Weather Location") {
			def validEnt = "\n\nWeather Stations: [pws:station_id]\nZipCodes: [90250]"
			href url:"https://www.wunderground.com/weatherstation/ListStations.asp", style:"embedded", required:false, title:"Weather Station ID Lookup",
					description: "Lookup Weather Station ID...", image: getAppImg("search_icon.png")
			def defZip = getStZipCode() ? getStZipCode() : getNestZipCode()
			input("custLocStr", "text", title: "Set Custom Weather Location?", required: false, defaultValue: defZip, submitOnChange: true,
					image: getAppImg("weather_icon_grey.png"))
			paragraph "Valid location entries are:${validEnt}", image: getAppImg("blank_icon.png")
			atomicState.lastWeatherUpdDt = 0
			atomicState?.lastForecastUpdDt = 0
		}
	}
}

def getWeatherConfDesc() {
	def str = ""
	def defZip = getStZipCode() ? getStZipCode() : getNestZipCode()
	str += custLocStr ? " • Weather Location: (${custLocStr})" : " • Default Weather Location: (${defZip})"
	return (str != "") ? "${str}" : null
}

def devCustomizePageDesc() {
	def tempChgWaitValDesc = (!tempChgWaitVal || tempChgWaitVal == 4) ? "" : tempChgWaitVal
	def wstr = weathAlertNotif  ? "Enabled" : "Disabled"
	def str = "Device Customizations:"
	str += "\n • Man. Temp Change Delay:\n    └ (${getInputEnumLabel(tempChgWaitVal ?: 4, waitValEnum())})"
	str += "\n${getWeatherConfDesc()}"
	str += "\n • Weather Alerts: (${wstr})"
	return ((tempChgWaitValDesc || custLocStr || weathAlertNotif) ? str : "")
}

def getDevicesDesc() {
	def str = ""
	str += settings?.thermostats ? "\n • [${settings?.thermostats?.size()}] Thermostat${(settings?.thermostats?.size() > 1) ? "s" : ""}" : ""
	str += settings?.protects ? "\n • [${settings?.protects?.size()}] Protect${(settings?.protects?.size() > 1) ? "s" : ""}" : ""
	str += settings?.cameras ? "\n • [${settings?.cameras?.size()}] Camera${(settings?.cameras?.size() > 1) ? "s" : ""}" : ""
	str += settings?.presDevice ? "\n • [1] Presence Device" : ""
	str += settings?.weatherDevice ? "\n • [1] Weather Device" : ""
	str += (!settings?.thermostats && !settings?.protects && !settings?.presDevice && !settings?.weatherDevice) ? "\n • No Devices Selected..." : ""
	return (str != "") ? str : null
}

def debugPrefPage() {
	dynamicPage(name: "debugPrefPage", install: false) {
		section ("Application Logs") {
			input (name: "appDebug", type: "bool", title: "Show App Logs in the IDE?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("log.png"))
			if(appDebug) {
				input (name: "advAppDebug", type: "bool", title: "Show Verbose Logs?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("list_icon.png"))
				LogAction("Debug Logs are Enabled...", "info", false)
			}
			else { LogAction("Debug Logs are Disabled...", "info", false) }
		}
		section ("Child Device Logs") {
			input (name: "childDebug", type: "bool", title: "Show Device Logs in the IDE?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("log.png"))
			if(childDebug) { LogAction("Device Debug Logs are Enabled...", "info", false) }
			else { LogAction("Device Debug Logs are Disabled...", "info", false) }
		}
		atomicState.needChildUpd = true
	}
}

def getAppDebugDesc() {
	def str = ""
	str += isAppDebug() ? "App Debug: (${debugStatus()})${advAppDebug ? "(Trace)" : ""}" : ""
	str += isChildDebug() ? "${isAppDebug() ? "\n" : ""}Device Debug: (${deviceDebugStatus()})" : ""
	return (str != "") ? "${str}" : null
}

def infoPage () {
	dynamicPage(name: "infoPage", title: "Help, Info and Instructions", install: false) {
		section("About this App:") {
			paragraph appInfoDesc(), image: getAppImg("nest_manager%402x.png", true)
		}
		section("Help and Instructions:") {
			href url: getReadmePageUrl(), style:"embedded", required:false, title:"Readme File",
				description:"View the Projects Readme File...", state: "complete", image: getAppImg("readme_icon.png")
			href url: getHelpPageUrl(), style:"embedded", required:false, title:"Help Pages",
				description:"View the Help and Instructions Page...", state: "complete", image: getAppImg("help_icon.png")
		}
		section("Donations:") {
			href url: textDonateLink(), style:"external", required: false, title:"Donations",
				description:"Tap to Open in Mobile Browser...", state: "complete", image: getAppImg("donate_icon.png")
		}
		section("Created by:") {
			paragraph "Anthony S. (@tonesto7)", state: "complete"
		}
		section("Collaborators:") {
			paragraph "Ben W. (@desertblade)\nEric S. (@E_Sch)", state: "complete"
		}
		section("App Revision History:") {
			href "changeLogPage", title: "View App Change Log Info", description: "Tap to View...", image: getAppImg("change_log_icon.png")
		}
		if(atomicState?.installationId) {
			section("InstallationID:") {
				paragraph "InstallationID:\n${atomicState?.installationId}"
			}
		}
		section("Licensing Info:") {
			paragraph "${textCopyright()}\n${textLicense()}"
		}
	}
}

def changeLogPage () {
	dynamicPage(name: "changeLogPage", title: "View Change Info", install: false) {
		section("App Revision History:") {
			paragraph appVerInfo()
		}
	}
}

def uninstallPage() {
	dynamicPage(name: "uninstallPage", title: "Uninstall", uninstall: true) {
		section("") {
			if(parent) {
				paragraph "This will uninstall the ${app?.label} Automation!!!"
			} else {
				paragraph "This will uninstall the App, All Automation Apps and Child Devices.\n\nPlease make sure that any devices created by this app are removed from any routines/rules/smartapps before tapping Remove."
			}
		}
	}
}

/******************************************************************************
*					  NEST LOGIN PAGES		  	  		  *
*******************************************************************************/
def nestLoginPrefPage () {
	if(!atomicState?.authToken) {
		return authPage()
	} else {
		return dynamicPage(name: "nestLoginPrefPage", nextPage: atomicState?.authToken ? "" : "authPage", install: false) {
			section("Authorization Info:") {
				paragraph "Token Created:\n• ${atomicState?.tokenCreatedDt.toString() ?: "Not Found..."}"
				paragraph "Token Expires:\n• ${atomicState?.tokenExpires ? "Never" : "Not Found..."}"
				paragraph "Last Connection:\n• ${atomicState.lastDevDataUpd ? atomicState?.lastDevDataUpd.toString() : ""}"
			}
			section("Nest Login Preferences:") {
				href "nestTokenResetPage", title: "Log Out and Reset your Nest Token", description: "Tap to Reset the Token...", required: true, state: null, image: getAppImg("reset_icon.png")
			}
		}
	}
}

def nestTokenResetPage() {
	return dynamicPage(name: "nestTokenResetPage", install: false) {
		section ("Resetting Nest Token...") {
			revokeNestToken()
			atomicState.authToken = null
			paragraph "Token has been reset...\nPress Done to return to Login page..."
		}
	}
}


/******************************************************************************
*					  NEST API INFO PAGES		  	  		  *
*******************************************************************************/

def nestInfoPage () {
	dynamicPage(name: "nestInfoPage", install: false) {
		section("View All API Data Received from Nest:") {
			if(atomicState?.structures) {
				href "structInfoPage", title: "Nest Location(s) Info...", description: "Tap to view...", image: getAppImg("nest_structure_icon.png")
			}
			if(atomicState?.thermostats) {
				href "tstatInfoPage", title: "Nest Thermostat(s) Info...", description: "Tap to view...", image: getAppImg("nest_like.png")
			}
			if(atomicState?.protects) {
				href "protInfoPage", title: "Nest Protect(s) Info...", description: "Tap to view...", image: getAppImg("protect_icon.png")
			}
			if(atomicState?.cameras) {
				href "camInfoPage", title: "Nest Camera(s) Info...", description: "Tap to view...", image: getAppImg("camera_icon.png")
			}
			if(!atomicState?.structures && !atomicState?.thermostats && !atomicState?.protects && !atomicState?.cameras) {
				paragraph "There is nothing to show here...", image: getAppImg("instruct_icon.png")
			}
		}

		if(atomicState?.protects) {
			section("Nest Protect Alarm Simulation:") {
				if(atomicState?.protects) {
					href "alarmTestPage", title: "Test your Protect Devices\nBy Simulating Alarm Events", required: true , image: getAppImg("test_icon.png"), state: null, description: "Tap to Begin"
				}
			}
		}
		section("Diagnostics") {
			href "diagPage", title: "View Diagnostic Info...", description: null, image: getAppImg("diag_icon.png")
		}
	}
}

def structInfoPage () {
	dynamicPage(name: "structInfoPage", refreshInterval: 30, install: false) {
		def noShow = [ "wheres", "cameras", "thermostats", "smoke_co_alarms", "structure_id" ]
		section("") {
			paragraph "Locations", state: "complete", image: getAppImg("nest_structure_icon.png")
		}
		atomicState?.structData?.each { struc ->
			if(struc?.key == atomicState?.structures) {
				def str = ""
				def cnt = 0
				section("Location Name: ${struc?.value?.name}") {
					def data = struc?.value.findAll { !(it.key in noShow) }
					data?.sort().each { item ->
						cnt = cnt+1
						str += "${(cnt <= 1) ? "" : "\n\n"}• ${item?.key?.toString()}: (${item?.value})"
					}
					paragraph "${str}"
				}
			}
		}
	}
}

def tstatInfoPage () {
	dynamicPage(name: "tstatInfoPage", refreshInterval: 30, install: false) {
		def noShow = [ "where_id", "device_id", "structure_id" ]
		section("") {
			paragraph "Thermostats", state: "complete", image: getAppImg("nest_like.png")
		}
		atomicState?.thermostats?.sort().each { tstat ->
			def str = ""
			def cnt = 0
			section("Thermostat Name: ${tstat?.value}") {
				def data = atomicState?.deviceData?.thermostats[tstat?.key].findAll { !(it.key in noShow) }
				data?.sort().each { item ->
					cnt = cnt+1
					str += "${(cnt <= 1) ? "" : "\n\n"}• ${item?.key?.toString()}: (${item?.value})"
				}
				paragraph "${str}"
			}
		}
	}
}

def protInfoPage () {
	dynamicPage(name: "protInfoPage", refreshInterval: 30, install: false) {
		def noShow = [ "where_id", "device_id", "structure_id" ]
		section("") {
			paragraph "Protects", state: "complete", image: getAppImg("protect_icon.png")
		}
		atomicState?.protects.sort().each { prot ->
			def str = ""
			def cnt = 0
			section("Protect Name: ${prot?.value}") {
				def data = atomicState?.deviceData?.smoke_co_alarms[prot?.key].findAll { !(it.key in noShow) }
				data?.sort().each { item ->
					cnt = cnt+1
					str += "${(cnt <= 1) ? "" : "\n\n"}• ${item?.key?.toString()}: (${item?.value})"
				}
				paragraph "${str}"
			}
		}
	}
}

def camInfoPage () {
	dynamicPage(name: "camInfoPage", refreshInterval: 30, install: false) {
		def noShow = [ "where_id", "device_id", "structure_id" ]
		section("") {
			paragraph "Cameras", state: "complete", image: getAppImg("camera_icon.png")
		}
		atomicState?.cameras.sort().each { cam ->
			def str = ""
			def evtStr = ""
			def cnt = 0
			def cnt2 = 0
			section("Camera Name: ${cam?.value}") {
				def data = atomicState?.deviceData?.cameras[cam?.key].findAll { !(it.key in noShow) }
				data?.sort().each { item ->
					if(item?.key != "last_event") {
						if(item?.key in ["app_url", "web_url"]) {
							href url: item?.value, style:"external", required: false, title: item?.key.toString().replaceAll("\\_", " ").capitalize(), description:"Tap to View in Mobile Browser...", state: "complete"
						} else {
							cnt = cnt+1
							str += "${(cnt <= 1) ? "" : "\n\n"}• ${item?.key?.toString()}: (${item?.value})"
						}
					} else {
						item?.value?.sort().each { item2 ->
							if(item2?.key in ["app_url", "web_url", "image_url", "animated_image_url"]) {
								href url: item2?.value, style:"external", required: false, title: "LastEvent: ${item2?.key.toString().replaceAll("\\_", " ").capitalize()}", description:"Tap to View in Mobile Browser...", state: "complete"
							}
							else {
								cnt2 = cnt2+1
								evtStr += "${(cnt2 <= 1) ? "" : "\n\n"}  • (LastEvent) ${item2?.key?.toString()}: (${item2?.value})"
							}
						}
					}
				}
				paragraph "${str}"
				if(evtStr != "") {
					paragraph "Last Event Data:\n\n${evtStr}"
				}
			}
		}
	}
}

def alarmTestPage () {
	dynamicPage(name: "alarmTestPage", install: false, uninstall: false) {
		if(atomicState?.protects) {
			section("Select Carbon/Smoke Device to Test:") {
				input(name: "alarmCoTestDevice", title:"Select the Protect to Test", type: "enum", required: false, multiple: false, submitOnChange: true,
						metadata: [values:atomicState?.protects], image: getAppImg("protect_icon.png"))
			}
			if(settings?.alarmCoTestDevice) {
				section("Select the Events to Generate:") {
					input "alarmCoTestDeviceSimSmoke", "bool", title: "Simulate a Smoke Event?", defaultValue: false, submitOnChange: true, image: getDevImg("smoke_emergency.png")
					input "alarmCoTestDeviceSimCo", "bool", title: "Simulate a Carbon Event?", defaultValue: false, submitOnChange: true, image: getDevImg("co_emergency.png")
					input "alarmCoTestDeviceSimLowBatt", "bool", title: "Simulate a Low Battery Event?", defaultValue: false, submitOnChange: true, image: getDevImg("battery_low.png")
				}
				if(settings?.alarmCoTestDeviceSimLowBatt || settings?.alarmCoTestDeviceSimCo || settings?.alarmCoTestDeviceSimSmoke) {
					section("Execute Selected Tests from Above:") {
						if(!atomicState?.isAlarmCoTestActive) {
							paragraph "WARNING: If your protect devices are used Smart Home Monitor (SHM) it will not see these as a test and will trigger any action/alarms you have configured...",
									required: true, state: null
						}
						if(settings?.alarmCoTestDeviceSimSmoke && !settings?.alarmCoTestDeviceSimCo && !settings?.alarmCoTestDeviceSimLowBatt) {
							href "simulateTestEventPage", title: "Simulate Smoke Event", params: ["testType":"smoke"], description: "Tap to Execute Test", required: true, state: null
						}

						if(settings?.alarmCoTestDeviceSimCo && !settings?.alarmCoTestDeviceSimSmoke && !settings?.alarmCoTestDeviceSimLowBatt) {
							href "simulateTestEventPage", title: "Simulate Carbon Event", params: ["testType":"co"], description: "Tap to Execute Test", required: true, state: null
						}

						if(settings?.alarmCoTestDeviceSimLowBatt && !settings?.alarmCoTestDeviceSimCo && !settings?.alarmCoTestDeviceSimSmoke) {
							href "simulateTestEventPage", title: "Simulate Battery Event", params: ["testType":"battery"], description: "Tap to Execute Test", required: true, state: null
						}
					}
				}

				if(atomicState?.isAlarmCoTestActive && (settings?.alarmCoTestDeviceSimLowBatt || settings?.alarmCoTestDeviceSimCo || settings?.alarmCoTestDeviceSimSmoke)) {
					section("Instructions") {
						paragraph "FYI: Clear ALL Selected Tests to Reset for New Alarm Test", required: true, state: null
					}
					if(!settings?.alarmCoTestDeviceSimLowBatt && !settings?.alarmCoTestDeviceSimCo && !settings?.alarmCoTestDeviceSimSmoke) {
						atomicState?.isAlarmCoTestActive = false
						atomicState?.curProtTestPageData = null
					}
				}
			}
		}
	}
}

def simulateTestEventPage(params) {
	def pName = getAutoType()
	def testType
	if(params?.testType) {
		atomicState.curProtTestType = params?.testType
		testType = params?.testType
	} else {
		testType = atomicState?.curProtTestType
	}
	dynamicPage(name: "simulateTestEventPage", refreshInterval: 10, install: false, uninstall: false) {
		if(settings?.alarmCoTestDevice) {
			def dev = getChildDevice(settings?.alarmCoTestDevice)
			def testText
			if(dev) {
				section("Testing ${dev}...") {
					def isRun = false
					if(!atomicState?.isAlarmCoTestActive) {
						atomicState?.isAlarmCoTestActive = true
						if(testType == "co") {
							testText = "Carbon 'Detected'"
							dev?.runCoTest()
						}
						else if(testType == "smoke") {
							testText = "Smoke 'Detected'"
							dev?.runSmokeTest()
						}
						else if(testType == "co") {
							testText = "Battery 'Replace'"
							dev?.runBatteryTest()
						}
						LogAction("Sending ${testText} Event to '$dev'", "info", true)
						paragraph "Sending ${testText} Event to '$dev'", state: "complete"
					} else {
						paragraph "Skipping... A Test is already Running...", required: true, state: null
					}
				}
			}
		}
	}
}

def diagPage () {
	dynamicPage(name: "diagPage", install: false) {
		section("") {
			paragraph "This page will allow you to view all diagnostic data related to the apps/devices in order to assist the developer in troubleshooting...", image: getAppImg("diag_icon.png")
		}
		section("State Size Info:") {
			paragraph "Current State Usage:\n${getStateSizePerc()}% (${getStateSize()} bytes)", required: true, state: (getStateSizePerc() <= 70 ? "complete" : null),
					image: getAppImg("progress_bar.png")
		}
		section("View Apps & Devices Data") {
			href "managAppDataPage", title:"View Manager Data", description:"Tap to view...", image: getAppImg("view_icon.png")
			href "childAppDataPage", title:"View Automations Data", description:"Tap to view...", image: getAppImg("view_icon.png")
			href "childDevDataPage", title:"View Device Data", description:"Tap to view...", image: getAppImg("view_icon.png")
			href "appParamsDataPage", title:"View AppParams Data", description:"Tap to view...", image: getAppImg("view_icon.png")
		}
		if(settings?.optInAppAnalytics || settings?.optInSendExceptions) {
			section("Analytics Data") {
				if(settings?.optInAppAnalytics) {
					href url: getAppEndpointUrl("renderInstallData"), style:"embedded", required: false, title:"View Shared Install Data", description:"Tap to view Data...", image: getAppImg("app_analytics_icon.png")
				}
				href url: getAppEndpointUrl("renderInstallId"), style:"embedded", required: false, title:"View Your Installation ID", description:"Tap to view...", image: getAppImg("view_icon.png")
			}
		}
		section("Recent Nest Command") {
			def cmdDesc = ""
			cmdDesc += "Last Command Details:"
			cmdDesc += "\n • DateTime: (${atomicState?.lastCmdSentDt ?: "Nothing found..."})"
			cmdDesc += "\n • Cmd Sent: (${atomicState?.lastCmdSent ?: "Nothing found..."})"
			cmdDesc += "\n • Cmd Result: (${atomicState?.lastCmdSentStatus ?: "Nothing found..."})"

			cmdDesc += "\n\n • Totals Commands Sent: (${!atomicState?.apiCommandCnt ? 0 : atomicState?.apiCommandCnt})"
			paragraph "${cmdDesc}"
		}
	}
}

def appParamsDataPage() {
	dynamicPage(name: "appParamsDataPage", refreshInterval: 30, install: false) {
		if(atomicState?.appData) {
			atomicState?.appData?.sort().each { sec ->
				section("${sec?.key.toString().capitalize()}:") {
					def str = ""
					def cnt = 0
					sec?.value.each { par ->
						cnt = cnt+1
						str += "${(cnt <= 1) ? "" : "\n\n"}• ${par?.key.toString()}: ${par?.value}"
					}
					paragraph "${str}"
				}
			}
		}
	}
}

def managAppDataPage() {
	dynamicPage(name: "managAppDataPage", refreshInterval:30, install: false) {
		def noShow = ["accessToken", "authToken" /*, "curAlerts", "curAstronomy", "curForecast", "curWeather"*/]
		section("SETTINGS DATA:") {
			def str = ""
			def cnt = 0
			def data = settings?.findAll { !(it.key in noShow) }
			   data?.sort().each { item ->
				cnt = cnt+1
				str += "${(cnt <= 1) ? "" : "\n\n"}• ${item?.key.toString()}: (${item?.value})"
			}
			paragraph "${str}"
		}
		section("STATE DATA:") {
			def str = ""
			def cnt = 0
			def data = state?.findAll { !(it.key in noShow) }
			data?.sort().each { item ->
				cnt = cnt+1
				str += "${(cnt <= 1) ? "" : "\n\n"}• ${item?.key.toString()}: (${item?.value})"
			}
			paragraph "${str}"
		}
		section("APP METADATA:") {
			def str = ""
			def cnt = 0
			getMetadata()?.sort().each { item ->
				cnt = cnt+1
				str += "${(cnt <= 1) ? "" : "\n\n\n"}${item?.key.toString().toUpperCase()}:\n\n"
				def cnt2 = 0
				item?.value.sort().each { vals ->
					cnt2 = cnt2+1
					str += "${(cnt2 <= 1) ? "" : "\n\n"}• ${vals?.key.toString()}: (${vals?.value})"
				}
			}
			paragraph "${str}"
		}
	}
}

def childAppDataPage() {
	dynamicPage(name: "childAppDataPage", refreshInterval:30, install:false) {
		def apps = getChildApps()
		if(apps) {
			apps?.each { ca ->
				def str = ""
				section("${ca?.label.toString().capitalize()}:") {
					str += "   ─────SETTINGS DATA─────"
					def setData = ca?.getSettingsData()
					setData?.sort().each { sd ->
						str += "\n\n• ${sd?.key.toString()}: (${sd?.value})"
					}
					def appData = ca?.getAppStateData()
					str += "\n\n\n  ───────STATE DATA──────"
					appData?.sort().each { par ->
						str += "\n\n• ${par?.key.toString()}: (${par?.value})"
					}
					paragraph "${str}"
				}
			}
		} else {
			section("") { paragraph "No Child Apps Installed..." }
		}
	}
}

def childDevDataPage() {
	dynamicPage(name: "childDevDataPage", refreshInterval:180, install: false) {
		getAllChildDevices().each { dev ->
			def str = ""
			section("${dev?.displayName.toString().capitalize()}:") {
				str += "  ───────STATE DATA──────"
				dev?.getDeviceStateData()?.sort().each { par ->
					str += "\n\n• ${par?.key.toString()}: (${par?.value})"
				}
				str += "\n\n\n  ────SUPPORTED ATTRIBUTES────"
				def devData = dev?.supportedAttributes.collect { it as String }
				devData?.sort().each {
					str += "\n\n• ${"$it" as String}: (${dev.currentValue("$it")})"
				}
				   str += "\n\n\n  ────SUPPORTED COMMANDS────"
				dev?.supportedCommands?.sort().each { cmd ->
					str += "\n\n• ${cmd.name}(${!cmd?.arguments ? "" : cmd?.arguments.toString().toLowerCase().replaceAll("\\[|\\]", "")})"
				}

				str += "\n\n\n  ─────DEVICE CAPABILITIES─────"
				dev?.capabilities?.sort().each { cap ->
					str += "\n\n• ${cap}"
				}
				paragraph "${str}"
			}
		}
	}
}

// This is part of the dashboard and needs to remain
def getDevIdsByType(type) {
	def results = []
	def devs
	if(type) {
		switch (type) {
			case "camera":
				devs = state?.cameras
				break
			case "thermostat":
				devs = state?.thermostats
				break
			case "protect":
				devs = state?.protects
				break
			case "presence":
				devs = getNestPresId()
				break
			case "weather":
				devs = getNestWeatherId()
				break
		}
		if(devs instanceof Map) {
			devs.each { results.push(it?.key) }
		} else {
			results.push(devs)
		}
	}
	return results
}

def apiDevNoShow() {
	return [
		"cssData", "coolSetpointTable", "heatSetpointTable", "heatSetpointTableYesterday", "coolSetpointTableYesterday", "humidityTable", "humidityTableYesterday",
		"curForecast", "curWeather", "operatingStateTable", "operatingStateTableYesterday", "temperatureTable", "temperatureTableYesterday", "dewpointTable",
		"dewpointTableYesterday", "lastWeatherAlertNotif", "walertMessage"
	]
}

def api_deviceData(params) {
	log.trace "api_deviceData..."
	try {
		def noShow = apiDevNoShow()
		def devices = []
		def data = [:]
		if(params?.deviceType) {
			if(params?.deviceType in ["camera", "thermostat", "protect", "presence", "weather"]) {
				getDevIdsByType(params?.deviceType).each { devices.push(getChildDevice(it)) }
			}
		} else { devices = getAllChildDevices()}
		if(devices.size() > 0) {
			devices?.each { dev ->
				def devId = dev.deviceNetworkId
				data."${devId}" = [:]
				data."${devId}".label = dev?.displayName
				data."${devId}".devVersion = dev?.devVer()
				data."${devId}".state = [:]
				data."${devId}".state = dev?.getDeviceStateData()?.sort().findAll { !(it.key in noShow) }
				data."${devId}".attr = []
				def attr = dev?.supportedAttributes.collect { it as String }
				attr?.sort().each { data."${devId}".attr.push("${it as String}":dev.currentValue(it)) }
				data."${devId}".cmds = []
				def cmds = dev?.supportedCommands?.findAll { }
				cmds?.sort().each { cmd ->
					data."${devId}".cmds.push ("${cmd.name}":"${!cmd?.arguments ? "" : cmd?.arguments.toString().toLowerCase().replaceAll("\\[|\\]", "")}")
				}
				data."${devId}".capabilities = []
				def caps = dev?.capabilities?.sort().findAll { }
				caps?.each() { cap ->
					data."${devId}".capabilities.push(cap.toString())
				}
			}
		}
		def result = ["devData":data.toString()]
		return result
	} catch (ex) {
		log.error "api_deviceData: Exception:", ex
		sendExceptionData(ex.message, "api_deviceData")
		return null
	}
}

def api_singleDeviceData(params) {
	try {
		def noShow = apiDevNoShow()
		def dTypes = ["attrs", "cmds", "state", "capabilities", "label", "devVer"]
		def dev = []
		def data = [:]
		if(params?.deviceId) {
			dev = getChildDevice(params.deviceId)
			if(dev) {
				def devId = params?.deviceId
				data."${devId}" = [:]
				data."${devId}".label = dev?.displayName
				data."${devId}".devVersion = dev?.devVersion()
				if(!params?.dataType || params?.dataType == "state") {
					data."${devId}".state = [:]
					if(!params?.variable) {
						data."${devId}".state = dev?.getDeviceStateData()?.sort().findAll { !(it.key in noShow) }
					} else {
						data."${devId}".state = dev?.getDeviceStateData()?.sort().find { (it.key == params?.variable) }
					}
				}
				if(!params?.dataType || params?.dataType == "attrs") {
					data."${devId}".attrs = []
					def attrs = dev?.supportedAttributes.collect { it as String }
					attr?.sort().each { data."${devId}".attrs.push("${it as String}":dev.currentValue(it)) }
				}
				if(!params?.dataType || params?.dataType == "cmds") {
					data."${devId}".cmds = []
					def cmds = dev?.supportedCommands?.findAll { }
					cmds?.sort().each { data."${devId}".cmds.push ("${it.name}":"${!it?.arguments ? "" : it?.arguments.toString().toLowerCase().replaceAll("\\[|\\]", "")}") }
				}
				if(!params?.dataType || params?.dataType == "capabilities") {
					data."${devId}".capabilities = []
					def caps = dev?.capabilities?.sort().findAll { }
					data."${devId}".capabilities = caps
				}
			}
		} else {
			data = ["Error":"No Device ID Received..."]
		}
		def result = ["devData":data.toString()]
		return result
	} catch (ex) {
		log.error "api_singleDeviceData: Exception:", ex
		sendExceptionData(ex.message, "api_singleDeviceData")
		return null
	}
}

def api_managerData(params) {
	try {
		def noShow = ["accessToken", "authToken", "cmdQlist", "curAlerts", "curAstronomy", "curForecast", "curWeather"]
		def settingData
		def stateData
		def data = [:]
		data.managerVer = appVersion()
		//log.debug "data: $data"
		if(!params.dataType || params.dataType == "state") {
			data.states = [:]
			if(!params.variable || (!params.dataType && !params.variable)) {
				stateData = state?.findAll { !(it.key in noShow) }
				data.states = stateData
			} else {
				data.states = ["${params?.variable}":state["${params?.variable}"]]
			}
		}
		if(!params.dataType || params.dataType == "settings") {
			data.settings = [:]
			if(!params.variable || (!params.dataType && !params.variable)) {
				settingData = settings?.findAll { !(it.key in noShow) }
				data.settings = settingData
			} else {
				data.settings = ["${params?.variable}":settings["${params?.variable}"]]
			}
		}
		def result = ["appData":data.toString()]
		return result
	} catch (ex) {
		log.error "api_managerData: Exception:", ex
		sendExceptionData(ex.message, "api_managerData")
		return null
	}
}

def api_childAppData(params) {
	try {
		def noShow = ["accessToken", "authToken", "cmdQlist", "curAlerts", "curAstronomy", "curForecast", "curWeather"]
		def settingData
		def stateData
		def data = [:]
		def chldApp
		if(params.autoType) {
			chldApp = getChildApps().sort().findAll { it.getAutomationType() == params?.autoType }
		} else {
			chldApp = getChildApps()
		}
		if(chldApp) {
			chldApp?.each { ca ->
				data."${ca?.id}" = [:]
				data."${ca?.id}".childLabel = ca.label.toString()
				if(!params.dataType || params.dataType == "state") {
					data."${ca?.id}".states = [:]
					if(!params.variable || (!params.dataType && !params.variable)) {
						stateData = ca.getAppStateData()?.findAll { !(it.key in noShow) }
						data."${ca?.id}".states = stateData
					} else {
						data."${ca?.id}".states = ["${params?.variable}":ca?.getStateVal("${params?.variable}")]
					}
				}
				if(!params.dataType || params.dataType == "settings") {
					data."${ca?.id}".settings = [:]
					if(!params.variable || (!params.dataType && !params.variable)) {
						settingData = ca.getSettingsData()?.findAll { !(it.key in noShow) }
						data."${ca?.id}".settings = settingData
					} else {
						data."${ca?.id}".settings = ["${params?.variable}":ca?.getSettingVal("${params?.variable}")]
					}
				}
			}
		}
		def result = ["childData":data.toString()]
		return result
	} catch (ex) {
		log.error "api_childAppData: Exception:", ex
		sendExceptionData(ex.message, "api_childAppData")
		return null
	}
}


/******************************************************************************
*					Firebase Analytics Functions		  	  *
*******************************************************************************/
def createInstallDataJson() {
	try {
		generateInstallId()
		def tsVer = atomicState?.tDevVer ?: "Not Installed"
		def ptVer = atomicState?.pDevVer ?: "Not Installed"
		def cdVer = atomicState?.camDevVer ?: "Not Installed"
		def pdVer = atomicState?.presDevVer ?: "Not Installed"
		def wdVer = atomicState?.weatDevVer ?: "Not Installed"
		def vtsVer = atomicState?.vtDevVer ?: "Not Installed"
		def dashVer = atomicState?.dashVer ?: "Not Installed"
		def versions = ["apps":["manager":appVersion()?.toString(), "dash":dashVer], "devices":["thermostat":tsVer, "vthermostat":vtsVer, "protect":ptVer, "camera":cdVer, "presence":pdVer, "weather":wdVer]]

		def tstatCnt = atomicState?.thermostats?.size() ?: 0
		def protCnt = atomicState?.protects?.size() ?: 0
		def camCnt = atomicState?.cameras?.size() ?: 0
		def vstatCnt = atomicState?.vThermostats?.size() ?: 0
		def automations = !atomicState?.installedAutomations ? "No Automations Installed" : atomicState?.installedAutomations
		def tz = getTimeZone()?.ID?.toString()
		def apiCmdCnt = !atomicState?.apiCommandCnt ? 0 : atomicState?.apiCommandCnt
		def cltType = !mobileClientType ? "Not Configured" : mobileClientType?.toString()
		def appErrCnt = !atomicState?.appExceptionCnt ? 0 : atomicState?.appExceptionCnt
		def devErrCnt = !atomicState?.childExceptionCnt ? 0 : atomicState?.childExceptionCnt
		def data = [
			"guid":atomicState?.installationId, "versions":versions, "thermostats":tstatCnt, "protects":protCnt, "vthermostats":vstatCnt, "cameras":camCnt, "appErrorCnt":appErrCnt, "devErrorCnt":devErrCnt,
			"automations":automations, "timeZone":tz, "apiCmdCnt":apiCmdCnt, "stateUsage":"${getStateSizePerc()}%", "mobileClient":cltType, "datetime":getDtNow()?.toString()
		]
		def resultJson = new groovy.json.JsonOutput().toJson(data)
		return resultJson

	} catch (ex) {
		log.error "createInstallDataJson: Exception:", ex
		sendExceptionData(ex.message, "createInstallDataJson")
	}
}

def renderInstallData() {
	try {
		def resultJson = createInstallDataJson()
		def resultString = new groovy.json.JsonOutput().prettyPrint(resultJson)
		render contentType: "application/json", data: resultString
	} catch (ex) { log.error "renderInstallData Exception:", ex }
}

def renderInstallId() {
	try {
		def resultJson = new groovy.json.JsonOutput().toJson(atomicState?.installationId)
		render contentType: "application/json", data: resultJson
	} catch (ex) { log.error "renderInstallId Exception:", ex }
}

def sendInstallData() {
	if(settings?.optInAppAnalytics) {
		sendFirebaseData(createInstallDataJson(), "installData/clients/${atomicState?.installationId}.json")
	}
}

def removeInstallData() {
	if(settings?.optInAppAnalytics) {
		return removeFirebaseData("installData/clients/${atomicState?.installationId}.json")
	}
}

def sendExceptionData(exMsg, methodName, isChild = false, autoType = null) {
	if(atomicState?.appData?.database?.disableExceptions == true) {
	  return
	} else {
		def exCnt = 0
		def exString = "${exMsg}"
		exCnt = atomicState?.appExceptionCnt ? atomicState?.appExceptionCnt + 1 : 1
		atomicState?.appExceptionCnt = exCnt ?: 1
		if(settings?.optInSendExceptions) {
			def appType = isChild && autoType ? "automationApp/${autoType}" : "managerApp"
			def exData
			if(isChild) {
				exData = ["methodName":methodName, "automationType":autoType, "appVersion":(appVersion() ?: "Not Available"),"errorMsg":exString, "errorDt":getDtNow().toString()]
			} else {
				exData = ["methodName":methodName, "appVersion":(appVersion() ?: "Not Available"),"errorMsg":exString, "errorDt":getDtNow().toString()]
			}
			def results = new groovy.json.JsonOutput().toJson(exData)
			sendFirebaseExceptionData(results, "errorData/${appType}/${methodName}.json")
		}
	}
}

def sendChildExceptionData(devType, devVer, exMsg, methodName) {
	def exCnt = 0
	def exString = "${exMsg}"
	exCnt = atomicState?.childExceptionCnt ? atomicState?.childExceptionCnt + 1 : 1
	atomicState?.childExceptionCnt = exCnt ?: 1
	if(settings?.optInSendExceptions) {
		def exData = ["deviceType":devType, "devVersion":(devVer ?: "Not Available"), "methodName":methodName, "errorMsg":exString, "errorDt":getDtNow().toString()]
		def results = new groovy.json.JsonOutput().toJson(exData)
		sendFirebaseExceptionData(results, "errorData/${devType}/${methodName}.json")
	}
}

def sendFirebaseData(data, pathVal) {
	//log.trace "sendFirebaseData(${data}, ${pathVal}"
	def json = new groovy.json.JsonOutput().prettyPrint(data)
	def result = false
	def params = [ uri: "${getFirebaseAppUrl()}/${pathVal}", body: json.toString() ]
	try {
		httpPutJson(params) { resp ->
			//log.debug "resp: ${resp}"
			if( resp?.status == 200) {
				LogAction("sendFirebaseData: Data Sent Successfully!!!", "info", true)
				atomicState?.lastAnalyticUpdDt = getDtNow()
				result = true
			}
			else if(resp?.status == 400) {
				LogAction("sendFirebaseData: 'Bad Request' Exception: ${resp?.status}", "error", true)
			}
			else {
				LogAction("sendFirebaseData: 'Unexpected' Response: ${resp?.status}", "warn", true)
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			LogAction("sendFirebaseData: 'HttpResponseException' Exception: ${ex.message}", "error", true)
		}
		else { log.error "sendFirebaseData: Exception:", ex }
		sendExceptionData(ex.message, "sendFirebaseData")
	}
	return result
}

def sendFirebaseExceptionData(data, pathVal) {
	//log.trace "sendFirebaseExceptionData(${data}, ${pathVal}"
	def json = new groovy.json.JsonOutput().prettyPrint(data)
	def result = false
	def params = [ uri: "${getFirebaseAppUrl()}/${pathVal}", body: json.toString() ]
	try {
		httpPostJson(params) { resp ->
			//log.debug "resp: ${resp}"
			if( resp?.status == 200) {
				LogAction("sendFirebaseExceptionData: Exception Data Sent Successfully!!!", "info", true)
				atomicState?.lastSentExceptionDataDt = getDtNow()
				result = true
			}
			else if(resp?.status == 400) {
				LogAction("sendFirebaseExceptionData: 'Bad Request' Exception: ${resp?.status}", "error", true)
			}
			else {
				LogAction("sendFirebaseExceptionData: 'Unexpected' Response: ${resp?.status}", "warn", true)
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			LogAction("sendFirebaseExceptionData: 'HttpResponseException' Exception: ${ex.message}", "error", true)
		}
		else { log.error "sendFirebaseExceptionData: Exception:", ex }
	}
	return result
}

def removeFirebaseData(pathVal) {
	log.trace "removeFirebaseData(${pathVal}"
	def result = true
	try {
		httpDelete(uri: "${getFirebaseAppUrl()}/${pathVal}") { resp ->
			log.debug "resp status: ${resp?.status}"
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.ResponseParseException) {
			LogAction("removeFirebaseData: Response: ${ex.message}", "info", true)
		} else {
			LogAction("removeFirebaseData: Exception: ${ex.message}", "error", true)
			sendExceptionData(ex.message, "removeFirebaseData")
			result = false
		}
	}
	return result
}

/////////////////////////////////////////////////////////////////////////////////////////////
/********************************************************************************************
|    						Application Name: Nest Automations								|
|    						Author: Anthony S. (@tonesto7) | Eric S. (@E_Sch)			    |
|********************************************************************************************/
/////////////////////////////////////////////////////////////////////////////////////////////

def selectAutoPage() {
	//log.trace "selectAutoPage()..."
	if(!atomicState?.automationType) {
		return dynamicPage(name: "selectAutoPage", title: "Choose an Automation Type...", uninstall: false, install: false, nextPage: null) {
			def thereIsChoice = !parent.automationNestModeEnabled(null)
			if(thereIsChoice) {
				section("Set Nest Presence Based on ST Modes, Presence Sensor, or Switches:") {
					href "mainAutoPage", title: "Nest Mode Automations", description: "", params: [autoType: "nMode"], image: getAppImg("mode_automation_icon.png")
				}
			}
			section("Thermostat Automations: Setpoints, Remote Sensor, External Temp, Contact Sensor, Leak Sensor, Fan Control") {
				href "mainAutoPage", title: "Thermostat Automations", description: "", params: [autoType: "schMot"], image: getAppImg("thermostat_automation_icon.png")
			}
		}
	}
	else { return mainAutoPage( [autoType: atomicState?.automationType]) }
}

def mainAutoPage(params) {
	//log.trace "mainAutoPage()"
	if(!atomicState?.tempUnit) { atomicState?.tempUnit = getTemperatureScale()?.toString() }
	if(!atomicState?.disableAutomation) { atomicState.disableAutomation = false }
	atomicState?.showHelp = (parent?.getShowHelp() != null) ? parent?.getShowHelp() : true
	def autoType = null
	//If params.autoType is not null then save to atomicState.
	if(!params?.autoType) { autoType = atomicState?.automationType }
	else { atomicState.automationType = params?.autoType; autoType = params?.autoType }

	// If the selected automation has not been configured take directly to the config page.  Else show main page
	if(autoType == "nMode" && !isNestModesConfigured())   { return nestModePresPage() }
	else if(autoType == "watchDog" && !isWatchdogConfigured()) { return watchDogPage() }
	else if(autoType == "schMot" && !isSchMotConfigured())     { return schMotModePage() }

	else {
		// Main Page Entries
		//return dynamicPage(name: "mainAutoPage", title: "Automation Configuration", uninstall: false, install: false, nextPage: "nameAutoPage" ) {
		return dynamicPage(name: "mainAutoPage", title: "Automation Configuration", uninstall: false, install: true, nextPage:null ) {
			section("Automation Name:") {
				if(autoType == "watchDog") {
					paragraph "${app?.label}"
				} else {
					def newName = getAutoTypeLabel()
					label title: "Label this Automation:", description: "Suggested Name: ${newName}", defaultValue: newName, required: true, wordWrap: true, image: getAppImg("name_tag_icon.png")
					if(!atomicState?.isInstalled) {
						paragraph "FYI:\nMake sure to name it something that will help you easily identify the automation later."
					}
				}
			}

			section() {
				if(disableAutomationreq) {
					paragraph "This Automation is currently disabled!!!\nTurn it back on to to make changes or resume operation...", required: true, state: null, image: getAppImg("instruct_icon.png")
				}
				if(autoType == "nMode" && !atomicState?.disableAutomation) {
					//paragraph title:"Set Nest Presence Based on ST Modes, Presence Sensor, or Switches:", ""
					def nDesc = ""
					nDesc += isNestModesConfigured() ? "Nest Mode:\n • Status: (${getNestLocPres().toString().capitalize()})" : ""
					if(((!nModePresSensor && !nModeSwitch) && (nModeAwayModes && nModeHomeModes))) {
						nDesc += nModeHomeModes ? "\n • Home Modes: (${nModeHomeModes.size()})" : ""
						nDesc += nModeAwayModes ? "\n • Away Modes: (${nModeAwayModes.size()})" : ""
					}
					nDesc += (nModePresSensor && !nModeSwitch) ? "\n\n${nModePresenceDesc()}" : ""
					nDesc += (nModeSwitch && !nModePresSensor) ? "\n • Using Switch: (State: ${isSwitchOn(nModeSwitch) ? "ON" : "OFF"})" : ""
					nDesc += (nModeDelay && nModeDelayVal) ? "\n • Delay: ${getEnumValue(longTimeSecEnum(), nModeDelayVal)}" : ""
					nDesc += (settings?."${getAutoType()}Modes" || settings?."${getAutoType()}Days" || (settings?."${getAutoType()}StartTime" && settings?."${getAutoType()}StopTime")) ?
							"\n • Evaluation Allowed: (${autoScheduleOk(getAutoType()) ? "ON" : "OFF"})" : ""
					nDesc += (nModePresSensor || nModeSwitch) || (!nModePresSensor && !nModeSwitch && (nModeAwayModes && nModeHomeModes)) ? "\n\nTap to Modify..." : ""
					def nModeDesc = isNestModesConfigured() ? "${nDesc}" : null
					href "nestModePresPage", title: "Nest Mode Automation Config", description: nModeDesc ?: "Tap to Configure...", state: (nModeDesc ? "complete" : null), image: getAppImg("mode_automation_icon.png")
				}

				if(autoType == "schMot" && !atomicState?.disableAutomation) {
					//paragraph title:"Thermostat Automation:", ""
					def sDesc = ""
					sDesc += settings?.schMotTstat ? "${settings?.schMotTstat?.label}" : ""
					//sDesc += settings?.schMotTstat ? getTstatModeDesc() : ""

					if(settings?.schMotWaterOff) {
						sDesc += "\n • Turn Off if Leak Detected"
					}
					if(settings?.schMotContactOff) {
						sDesc += "\n • Turn Off if Contact Open"
					}
					if(settings?.schMotExternalTempOff) {
						sDesc += "\n • Turn Off based on External Temp"
					}
					if(settings?.schMotRemoteSensor) {
						sDesc += "\n • Use Remote Temp Sensors"
					}
					if(settings?.schMotSetTstatTemp) {
						sDesc += "\n • Setpoint Schedules Created"
					}
					if(settings?.schMotOperateFan) {
						sDesc += "\n • Control Fans with HVAC"
					}

					sDesc += settings?.schMotTstat ? "\n\nTap to Modify..." : ""
					def sModeDesc = isSchMotConfigured() ? "${sDesc}" : null
					href "schMotModePage", title: "Thermostat Automation Config", description: sModeDesc ?: "Tap to Configure...", state: (sModeDesc ? "complete" : null), image: getAppImg("thermostat_automation_icon.png")
				}

				if(autoType == "watchDog" && !atomicState?.disableAutomation) {
					//paragraph title:"Watch your Nest Location for Events:", ""
					def watDesc = ""
					watDesc += (settings["${getAutoType()}AllowSpeechNotif"] && (settings["${getAutoType()}SpeechDevices"] || settings["${getAutoType()}SpeechMediaPlayer"]) && getVoiceNotifConfigDesc("watchDog")) ?
							"\n\nVoice Notifications:${getVoiceNotifConfigDesc("watchDog")}" : ""
					def watDogDesc = isWatchdogConfigured() ? "${watDesc}" : null
					href "watchDogPage", title: "Nest Location Watchdog...", description: watDogDesc ?: "Tap to Configure...", state: (watDogDesc ? "complete" : null), image: getAppImg("watchdog_icon.png")
				}
			}
			section("Automation Options:") {
 				if(atomicState?.isInstalled && (isNestModesConfigured() || isWatchdogConfigured() || isSchMotConfigured())) {
					//paragraph title:"Enable/Disable this Automation", ""
					input "disableAutomationreq", "bool", title: "Disable this Automation?", required: false, defaultValue: disableAutomation, submitOnChange: true, image: getAppImg("disable_icon.png")
					if(!atomicState?.disableAutomation && disableAutomationreq) {
						LogAction("This Automation was Disabled at (${getDtNow()})", "info", true)
						atomicState?.disableAutomationDt = getDtNow()
					} else if(atomicState?.disableAutomation && !disableAutomationreq) {
						LogAction("This Automation was Restored at (${getDtNow()})", "info", true)
						atomicState?.disableAutomationDt = null
					}
					atomicState.disableAutomation = disableAutomationreq
				}
				input (name: "showDebug", type: "bool", title: "Debug Option", description: "Show App Logs in the IDE?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("log.png"))
				atomicState?.showDebug = showDebug
			}

		}
	}
}

// parent only method
def automationNestModeEnabled(val) {
LogAction("automationNestModeEnabled: val: $val", "info", true)
	if(val == null) {
		return atomicState?.automationNestModeEnabled ?: false
	} else {
		atomicState.automationNestModeEnabled = val.toBoolean()
	}
	return atomicState?.automationNestModeEnabled ?: false
}

def initAutoApp() {
	if(settings["webDashFlag"]) {
		atomicState?.automationType = "webDash"
		LogAction("initAutoApp: We are running webDash in wrong code base","error", true)
	} else {
		if(settings["watchDogFlag"]) {
			atomicState?.automationType = "watchDog"
		}
		def autoType = getAutoType()
		if(autoType == "nMode") {
			parent.automationNestModeEnabled(true)
		}
		unschedule()
		unsubscribe()
		automationsInst()

		if(autoType == "schMot" && isSchMotConfigured()) {
			atomicState.scheduleList = [ 1,2,3,4 ]
			def schedList = atomicState.scheduleList
			def timersActive = false
			def sLbl
			def cnt = 1
			def numact = 0
			schedList?.each { scd ->
				sLbl = "schMot_${scd}_"
				atomicState."schedule${cnt}SwEnabled" = null
				atomicState."schedule${cnt}MotionEnabled" = null
				atomicState."schedule${cnt}SensorEnabled" = null
				//atomicState."schedule${cnt}FanCtrlEnabled" = null

				def newscd = []
				def act = settings["${sLbl}SchedActive"]
				if(act) {
					newscd = cleanUpMap([
						m: settings["${sLbl}restrictionMode"],
						tf: settings["${sLbl}restrictionTimeFrom"],
						tfc: settings["${sLbl}restrictionTimeFromCustom"],
						tfo: settings["${sLbl}restrictionTimeFromOffset"],
						tt: settings["${sLbl}restrictionTimeTo"],
						ttc: settings["${sLbl}restrictionTimeToCustom"],
						tto: settings["${sLbl}restrictionTimeToOffset"],
						w: settings["${sLbl}restrictionDOW"],
						s1: buildDeviceNameList(settings["${sLbl}restrictionSwitchOn"], "and"),
						s0: buildDeviceNameList(settings["${sLbl}restrictionSwitchOff"], "and"),
//ERSERS
						ctemp: roundTemp(settings["${sLbl}CoolTemp"]),
						htemp: roundTemp(settings["${sLbl}HeatTemp"]),
						hvacm: settings["${sLbl}HvacMode"],
						sen0: settings["schMotRemoteSensor"] ? buildDeviceNameList(settings["${sLbl}remSensor"], "and") : null,
						m0: buildDeviceNameList(settings["${sLbl}Motion"], "and"),
						mctemp: settings["${sLbl}Motion"] ? roundTemp(settings["${sLbl}MCoolTemp"]) : null,
						mhtemp: settings["${sLbl}Motion"] ? roundTemp(settings["${sLbl}MHeatTemp"]) : null,
						mhvacm: settings["${sLbl}Motion"] ? settings["${sLbl}MHvacMode"] : null,
						mdelayOn: settings["${sLbl}Motion"] ? settings["${sLbl}MDelayValOn"] : null,
						mdelayOff: settings["${sLbl}Motion"] ? settings["${sLbl}MDelayValOff"] : null
					])

					/*fan0: buildDeviceNameList(settings["${sLbl}Fans"], "and"),
					ftemp: settings["${sLbl}FansUseTemp"],
					ftempl: settings["${sLbl}FansLowTemp"],
					ftemph: settings["${sLbl}FansHighTemp"],
					fmot: settings["${sLbl}FansMotion"],
					fmoton: settings["${sLbl}FansMDelayValOn"],
					fmotoff: settings["${sLbl}FansMDelayValOff"]*/
					numact += 1
				}
				LogAction("initAutoApp: [Schedule: $scd | sLbl: $sLbl | act: $act | newscd: $newscd]", "info", true)
				atomicState."sched${cnt}restrictions" = newscd
				atomicState."schedule${cnt}SwEnabled" = (newscd?.s1 || newscd?.s0)  ? true : false
				atomicState."schedule${cnt}MotionEnabled" = (newscd?.m0) ? true : false
				atomicState."schedule${cnt}SensorEnabled" = (newscd?.sen0) ? true : false
				//atomicState."schedule${cnt}FanCtrlEnabled" = (newscd?.fan0) ? true : false
				atomicState."schedule${cnt}TimeActive" = (newscd?.tf || newscd?.tfc || newscd?.tfo || newscd?.tt || newscd?.ttc || newscd?.tto || newscd?.w) ? true : false

				atomicState?."motion${cnt}InBtwn" = null 		// clear automation state of schedule in use motion state

				def oldact = atomicState?."${sLbl}MotionActive"
				def newact = isMotionActive(settings["${sLbl}Motion"])
				if(oldact != newact) {
					atomicState."${sLbl}MotionActive" = newact
					if(newact) { atomicState."${sLbl}MotionActiveDt" = getDtNow()
					} else { atomicState."${sLbl}MotionInActiveDt" = getDtNow() }
				}
				timersActive = (timersActive || atomicState."schedule${cnt}TimeActive") ? true : false

				cnt += 1
			}
			atomicState.scheduleTimersActive = timersActive
			atomicState.lastSched = null  // clear automation state of schedule in use
			atomicState.scheduleSchedActiveCount = numact
		}

		subscribeToEvents()
		scheduler()
		app.updateLabel(getAutoTypeLabel())
		atomicState?.lastAutomationSchedDt = null
		watchDogAutomation()
	}
}

def uninstAutomationApp() {
	log.trace "uninstAutomationApp..."
	def autoType = getAutoType()
	if(autoType == "schMot") {
		def myID = getMyLockId()
		if(schMotTstat && myID && parent) {
			if(parent?.addRemoveVthermostat(schMotTstat.deviceNetworkId, false, myID)) {
				LogAction("cleanup check for virtual thermostat", "debug", true)
			}
			if( parent?.remSenUnlock(atomicState?.remSenTstat, myID) ) { // attempt unlock old ID
				LogAction("Released remote sensor lock", "debug", true)
			}
		}
	}
	if(autoType == "nMode") {
		parent?.automationNestModeEnabled(false)
	}
}

def getAutoTypeLabel() {
	//LogAction("getAutoTypeLabel:","trace", true)
	def type = atomicState?.automationType
	def appLbl = app?.label?.toString()
	def newName = appName() == "Nest Manager" ? "Nest Automations" : "${appName()}"
	def typeLabel = ""
	def newLbl
	def dis = atomicState?.disableAutomation ? "\n(Disabled)" : ""
	if(type == "nMode")	{ typeLabel = "${newName} (NestMode)" }
	else if(type == "watchDog")	{ typeLabel = "Nest Location ${location.name} Watchdog"}
	else if(type == "schMot")	{ typeLabel = "${newName} (${schMotTstat?.label})" }

	if(appLbl != "Nest Manager") {
		if(appLbl.contains("\n(Disabled)")) {
			newLbl = appLbl.replaceAll('\\\n\\(Disabled\\)', '')
		} else {
			newLbl = appLbl
		}
	} else {
		newLbl = typeLabel
	}
	return "${newLbl}${dis}"
}

def getAppStateData() {
	return getState()
}

def getSettingsData() {
	def sets = []
	settings?.sort().each { st ->
		sets << st
	}
	return sets
}

def getSettingVal(var) {
	return settings[var] ?: null
}

def getStateVal(var) {
	return state[var] ?: null
}

def getAutoType() { return !parent ? "" : atomicState?.automationType }

def getAutoIcon(type) {
	if(type) {
		switch(type) {
			case "remSen":
				return getAppImg("remote_sensor_icon.png")
				break
			case "fanCtrl":
				return getAppImg("fan_control_icon.png")
				break
			case "conWat":
				return getAppImg("open_window.png")
				break
			case "leakWat":
				return getAppImg("leak_icon.png")
				break
			case "extTmp":
				return getAppImg("external_temp_icon.png")
				break
			case "nMode":
				return getAppImg("mode_automation_icon.png")
				break
			case "schMot":
				return getAppImg("thermostat_automation_icon.png")
				break
			case "tMode":
				return getAppImg("mode_setpoints_icon.png")
				break
			case "webDash":
				return getAppImg("dashboard_icon.png")
				break
			case "watchDog":
				return getAppImg("watchdog_icon.png")
				break
		}
	}
}

def automationsInst() {
	atomicState.isNestModesConfigured = 	isNestModesConfigured() ? true : false
	atomicState.isSchMotConfigured = 		isSchMotConfigured() ? true : false
	atomicState.isWatchdogConfigured = 		isWatchdogConfigured() ? true : false
	atomicState.isFanCtrlConfigured = 		isFanCtrlConfigured() ? true : false
	atomicState.isTstatSchedConfigured = 	isTstatSchedConfigured() ? true : false
	atomicState.isExtTmpConfigured = 		isExtTmpConfigured() ? true : false
	atomicState.isConWatConfigured = 		isConWatConfigured() ? true : false
	atomicState.isLeakWatConfigured = 		isLeakWatConfigured() ? true : false
	atomicState.isFanCircConfigured = 		isFanCircConfigured() ? true : false
	atomicState?.isInstalled = true
}

def getAutomationsInstalled() {
	def list = []
	def aType = atomicState?.automationType
	switch(aType) {
		case "nMode":
			list.push(aType)
			break
		case "schMot":
			def tmp = [:]
			tmp[aType] = []
			if(isFanCtrlConfigured()) 		{ tmp[aType].push("fanCtrl") }
			if(isFanCircConfigured()) 		{ tmp[aType].push("fanCirc") }
			if(isTstatSchedConfigured()) 	{ tmp[aType].push("tSched") }
			if(isExtTmpConfigured()) 		{ tmp[aType].push("extTmp") }
			if(isConWatConfigured()) 		{ tmp[aType].push("conWat") }
			if(isLeakWatConfigured()) 		{ tmp[aType].push("leakWat") }
			if(tmp?.size()) { list.push(tmp) }
			break
		case "watchDog":
			list.push(aType)
			break
	}
	log.debug "getAutomationsInstalled List: $list"
	return list
}

def getAutomationType() {
	return atomicState?.automationType ?: null
}

def getIsAutomationDisabled() {
	return atomicState?.disableAutomation ? true : false
}

def subscribeToEvents() {
	//Remote Sensor Subscriptions
	def autoType = getAutoType()

	//Nest Mode Subscriptions
	if(autoType == "nMode") {
		if(isNestModesConfigured()) {
			if(!settings?.nModePresSensor && !settings?.nModeSwitch && (settings?.nModeHomeModes || settings?.nModeAwayModes)) { subscribe(location, "mode", nModeSTModeEvt, [filterEvents: false]) }
			if(settings?.nModePresSensor && !settings?.nModeSwitch) { subscribe(nModePresSensor, "presence", nModePresEvt) }
			if(settings?.nModeSwitch && !settings?.nModePresSensor) { subscribe(nModeSwitch, "switch", nModeSwitchEvt) }
		}
	}

	//ST Thermostat Motion
	if(autoType == "schMot") {
		def needThermTemp
		def needThermMode
		def needThermPres

		if(isSchMotConfigured()) {
			if(settings?.schMotWaterOff) {
				if(isLeakWatConfigured()) { subscribe(leakWatSensors, "water", leakWatSensorEvt) }
			}
			if(settings?.schMotContactOff) {
				if(isConWatConfigured()) { subscribe(conWatContacts, "contact", conWatContactEvt) }
			}
			if(settings?.schMotExternalTempOff) {
				if(isExtTmpConfigured()) {
					if(!settings?.extTmpUseWeather && settings?.extTmpTempSensor) { subscribe(extTmpTempSensor, "temperature", extTmpTempEvt, [filterEvents: false]) }
					if(settings?.extTmpUseWeather) {
						atomicState.NeedwUpd = true
						if(parent?.getWeatherDeviceInst()) {
							def weather = parent?.getWeatherDevice()
							if(weather) {
								subscribe(weather, "temperature", extTmpTempEvt)
								subscribe(weather, "dewpoint", extTmpDpEvt)
							}
						} else { LogAction("No weather device found", "error", true) }
					}
					atomicState.extTmpChgWhileOnDt = getDtNow()
					atomicState.extTmpChgWhileOffDt = getDtNow()
				}
			}
			def senlist = []
			if(settings?.schMotRemoteSensor) {
				if(isRemSenConfigured()) {
					if(settings?.remSensorDay) {
						for(sen in settings?.remSensorDay) {
							if(senlist?.contains(sen)) {
								//log.trace "found $sen"
							} else {
								senlist.push(sen)
								subscribe(sen, "temperature", automationTempSenEvt)
							}
						}
					}
				}
			}
			if(settings?.schMotSetTstatTemp) {
				if(isTstatSchedConfigured()) {
				}
			}
			if(settings?.schMotOperateFan) {
				if(isFanCtrlConfigured() && fanCtrlFanSwitches) {
					subscribe(fanCtrlFanSwitches, "switch", automationFanSwitchEvt)
					subscribe(fanCtrlFanSwitches, "level", automationFanSwitchEvt)
				}
			}
			if(settings?.schMotOperateFan || settings?.schMotRemoteSensor) {
				subscribe(schMotTstat, "thermostatFanMode", automationTstatFanEvt)
			}

			def schedList = atomicState?.scheduleList
			def sLbl
			def cnt = 1
			def swlist = []
			def mtlist = []
			schedList?.each { scd ->
				sLbl = "schMot_${scd}_"
				def restrict = atomicState?."sched${cnt}restrictions"
				def act = settings["${sLbl}SchedActive"]
				if(act) {
					if(atomicState?."schedule${cnt}SwEnabled") {
						if(restrict?.s1) {
							for(sw in settings["${sLbl}restrictionSwitchOn"]) {
								if(swlist?.contains(sw)) {
									//log.trace "found $sw"
								} else {
									swlist.push(sw)
									subscribe(sw, "switch", automationSwitchEvt)
								}
							}
						}
						if(restrict?.s0) {
							for(sw in settings["${sLbl}restrictionSwitchOff"]) {
								if(swlist?.contains(sw)) {
									//log.trace "found $sw"
								} else {
									swlist.push(sw)
									subscribe(sw, "switch", automationSwitchEvt)
								}
							}
						}
					}
					if(atomicState?."schedule${cnt}MotionEnabled") {
						if(restrict?.m0) {
							for(mt in settings["${sLbl}Motion"]) {
								if(mtlist?.contains(mt)) {
									//log.trace "found $mt"
								} else {
									mtlist.push(mt)
									subscribe(mt, "motion", automationMotionEvt)
								}
							}
						}
					}
					if(atomicState?."schedule${cnt}SensorEnabled") {
						if(restrict?.sen0) {
							for(sen in settings["${sLbl}remSensor"]) {
								if(senlist?.contains(sen)) {
									//log.trace "found $sen"
								} else {
									senlist.push(sen)
									subscribe(sen, "temperature", automationTempSenEvt)
								}
							}
						}
					}
				}
				cnt += 1
			}
			subscribe(schMotTstat, "thermostatMode", automationTstatModeEvt)
			subscribe(schMotTstat, "thermostatOperatingState", automationTstatOperEvt)
			subscribe(schMotTstat, "temperature", automationTstatTempEvt)
			subscribe(schMotTstat, "presence", automationPresenceEvt)
			subscribe(schMotTstat, "coolingSetpoint", automationTstatCTempEvt)
			subscribe(schMotTstat, "heatingSetpoint", automationTstatHTempEvt)
			subscribe(schMotTstat, "safetyTempExceeded", automationSafetyTempEvt)
			subscribe(location, "sunset", automationSunEvtHandler)
			subscribe(location, "sunrise", automationSunEvtHandler)
			subscribe(location, "mode", automationSTModeEvt, [filterEvents: false])
		}
	}
	//watchDog Subscriptions
	if(autoType == "watchDog") {
		// if(isWatchdogConfigured()) {
		def tstats = parent?.getTstats()
		def foundTstats

		if(tstats) {
			foundTstats = tstats?.collect { dni ->
				def d1 = parent.getThermostatDevice(dni)
				if(d1) {
					LogAction("Found: ${d1?.displayName} with (Id: ${dni?.key})", "debug", true)

					// temperature is for DEBUG
					subscribe(d1, "temperature", automationTstatTempEvt)
					subscribe(d1, "safetyTempExceeded", automationSafetyTempEvt)
				}
				return d1
			}
		}
		//Alarm status monitoring
		if(settings["${autoType}AlarmDevices"] && settings?."${pName}AllowAlarmNotif") {
			if(settings["${autoType}_Alert_1_Use_Alarm"] || settings["${autoType}_Alert_2_Use_Alarm"]) {
				subscribe(settings["${autoType}AlarmDevices"], "alarm", alarmAlertEvt)
			}
		}
	}
}

def scheduler() {
	def random = new Random()
	def random_int = random.nextInt(60)
	def random_dint = random.nextInt(9)

	def autoType = getAutoType()
	if(autoType == "schMot" && atomicState?.scheduleSchedActiveCount && atomicState?.scheduleTimersActive) {
		LogAction("${autoType} scheduled using Cron (${random_int} ${random_dint}/5 * * * ?)", "info", true)
		schedule("${random_int} ${random_dint}/5 * * * ?", watchDogAutomation)
	} else {
		LogAction("${autoType} scheduled using Cron (${random_int} ${random_dint}/30 * * * ?)", "info", true)
		schedule("${random_int} ${random_dint}/30 * * * ?", watchDogAutomation)
	}
}

def watchDogAutomation() {
	LogAction("Heartbeat: watchDogAutomation()...", "trace", false)
	def autoType = getAutoType()
	def val = 900
	if(autoType == "schMot") {
		val = 220
	}
	if(getLastAutomationSchedSec() > val) {
		LogAction("${autoType} Heartbeat run requested...", "trace", true)
		runAutomationEval()
	}
}

def scheduleAutomationEval(schedtime = 20) {
	if(schedtime < 20) { schedtime = 20 }
	if(getLastAutomationSchedSec() > 14) {
		atomicState?.lastAutomationSchedDt = getDtNow()
		runIn(schedtime, "runAutomationEval", [overwrite: true])
	}
}

def getLastAutomationSchedSec() { return !atomicState?.lastAutomationSchedDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastAutomationSchedDt).toInteger() }

def runAutomationEval() {
	LogAction("runAutomationEval...", "trace", false)
	def autoType = getAutoType()
	switch(autoType) {
		case "nMode":
			if(isNestModesConfigured()) {
				checkNestMode()
			}
			break
		case "schMot":
			if(isSchMotConfigured()) {
				schMotCheck()
			}
			break
		case "watchDog":
			if(isWatchdogConfigured()) {
				watchDogCheck()
			}
			break
		default:
			LogAction("runAutomationEval: Invalid Option Received... ${autoType}", "warn", true)
			break
	}
}

def getAutomationStats() {
	return [
		"lastUpdatedDt":atomicState?.lastUpdatedDt,
		"lastEvalDt":atomicState?.lastEvalDt,
		"lastEvent":atomicState?.lastEventData,
		"lastActionData":getAutoActionData(),
		"lastSchedDt":atomicState?.lastAutomationSchedDt,
		"lastExecVal":atomicState?.lastExecutionTime,
		"execAvgVal":(atomicState?.evalExecutionHistory != [] ? getAverageValue(atomicState?.evalExecutionHistory) : null)
	]
}

def storeLastAction(actionDesc, actionDt) {
	if(actionDesc && actionDt) {
		atomicState?.lastAutoActionData = ["actionDesc":actionDesc, "dt":actionDt]
	}
}

def getAutoActionData() {
	if(atomicState?.lastAutoActionData) {
		return atomicState?.lastAutoActionData
	}
}

def automationTempSenEvt(evt) {
	LogAction("Event | Sensor Temp: ${evt?.displayName} - Temperature is (${evt?.value}°${getTemperatureScale()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationTstatTempEvt(evt) {
	LogAction("Event | Thermostat Temp: ${evt?.displayName} - Temperature is (${evt?.value}°${getTemperatureScale()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationTstatModeEvt(evt) {
	LogAction("Event | Thermostat Mode: ${evt?.displayName} - Mode is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		def modeOff = (evt?.value == "off") ? true : false
		if(!modeOff) { atomicState?.TstatTurnedOff = false }
		else { atomicState?.TstatTurnedOff = true }
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationPresenceEvt(evt) {
	LogAction("Event | Presence: ${evt?.displayName} - Presence is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationSwitchEvt(evt) {
	LogAction("Event | Switch: ${evt?.displayName} - is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationFanSwitchEvt(evt) {
	LogAction("Event | Fan Switch: ${evt?.displayName} - is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationTstatFanEvt(evt) {
	LogAction("Event | Thermostat Fan: ${evt?.displayName} - Fan is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationTstatOperEvt(evt) {
	LogAction("Event | Thermostat Operating State: ${evt?.displayName} - OperatingState is  (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationTstatCTempEvt(evt) {
	LogAction("Event | Thermostat Cooling Setpoint: ${evt?.displayName} - Cooling Setpoint is  (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationTstatHTempEvt(evt) {
	LogAction("Event | Thermostat Heating Setpoint: ${evt?.displayName} - Heating Setpoint is  (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationSTModeEvt(evt) {
	LogAction("Event | ST Mode is (${evt?.value.toString().toUpperCase()})", "trace", false)
	if(atomicState?.disableAutomation) { return }
	else {
		scheduleAutomationEval()
		storeLastEventData(evt)
	}
}

def automationSunEvtHandler(evt) {
	LogAction("Event | ST Sunrise / Sunset is (${evt?.value.toString().toUpperCase()})", "trace", false)
	if(atomicState?.disableAutomation) { return }
	scheduleAutomationEval()
	storeLastEventData(evt)
}


/******************************************************************************
|						WATCHDOG AUTOMATION LOGIC CODE						  |
*******************************************************************************/
def watchDogPrefix() { return "watchDog" }

def watchDogPage() {
	def pName = watchDogPrefix()
	dynamicPage(name: "watchDogPage", title: "Nest Location Watchdog", uninstall: true, install: true) {
		section("Notifications:") {
			def pageDesc = getNotifConfigDesc(pName)
			href "setNotificationPage", title: "Configured Alerts...", description: pageDesc, params: ["pName":"${pName}", "allowSpeech":true, "allowAlarm":true, "showSchedule":true],
					state: (pageDesc ? "complete" : null), image: getAppImg("notification_icon.png")
		}
	}
}

def automationSafetyTempEvt(evt) {
	LogAction("Event | Thermostat Safety Temp Exceeded: '${evt.displayName}' (${evt.value})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		if(evt?.value == "true") {
			scheduleAutomationEval()
		}
	}
	storeLastEventData(evt)
}

// Alarms will repeat every watDogRepeatMsgDelay (1 hr default) ALL thermostats
def watchDogCheck() {
	if(atomicState?.disableAutomation) { return }
	else {
		def execTime = now()
		atomicState?.lastEvalDt = getDtNow()
		def tstats = parent?.getTstats()
		def foundTstats
		if(tstats) {
			foundTstats = tstats?.collect { dni ->
				def d1 = parent.getThermostatDevice(dni)
				if(d1) {
					def exceeded = d1?.currentValue("safetyTempExceeded")?.toString()
					if(exceeded == "true") {
						watchDogAlarmActions(d1.displayName, dni, "temp")
						LogAction("watchDogCheck() | Thermostat: ${d1?.displayName} Temp Exceeded: ${exceeded}", "trace", true)
					}
					return d1
				}
			}
		}
		storeExecutionHistory((now()-execTime), "watchDogCheck")
	}
}

def watchDogAlarmActions(dev, dni, actType) {
	def pName = watchDogPrefix()
	//def allowNotif = (settings["${pName}NotificationsOn"] && (settings["${pName}NotifRecips"] || settings["${pName}NotifPhones"] || settings["${pName}UsePush"]))  ? true : false
	def allowNotif = settings["${pName}NotificationsOn"] ? true : false
	def allowSpeech = allowNotif && settings?."${pName}AllowSpeechNotif" ? true : false
	def allowAlarm = allowNotif && settings?."${pName}AllowAlarmNotif" ? true : false
	def evtNotifMsg = ""
	def evtVoiceMsg = ""
	switch(actType) {
		case "temp":
			evtNotifMsg = "Safety Temp has been exceeded on ${dev}."
			evtVoiceMsg = "Safety Temp has been exceeded on ${dev}."
			break
	}
	if(getLastWatDogSafetyAlertDtSec(dni) > getWatDogRepeatMsgDelayVal()) {
		LogAction("watchDogAlarmActions() | ${evtNotifMsg}", "trace", true)

		if(allowNotif) {
			sendEventPushNotifications(evtNotifMsg, "Warning", pName)
			if(allowSpeech) {
				sendEventVoiceNotifications(voiceNotifString(evtVoiceMsg, pName), pName, "nmWatDogEvt_${app?.id}", true, "nmWatDogEvt_${app?.id}")
			}
			if(allowAlarm) {
				scheduleAlarmOn(pName)
			}
		} else {
			sendNofificationMsg("Warning", evtNotifMsg)
		}
		atomicState?."lastWatDogSafetyAlertDt${dni}" = getDtNow()
	}
}

def getLastWatDogSafetyAlertDtSec(dni) { return !atomicState?."lastWatDogSafetyAlertDt{$dni}" ? 10000 : GetTimeDiffSeconds(atomicState?."lastWatDogSafetyAlertDt${dni}").toInteger() }
def getWatDogRepeatMsgDelayVal() { return !watDogRepeatMsgDelay ? 3600 : watDogRepeatMsgDelay.toInteger() }

def isWatchdogConfigured() {
	return (atomicState?.automationType == "watchDog") ? true : false
}


/////////////////////THERMOSTAT AUTOMATION CODE LOGIC ///////////////////////

/****************************************************************************
|					REMOTE SENSOR AUTOMATION CODE			  				|
*****************************************************************************/

def remSenPrefix() { return "remSen" }

def remSenLock(val, myId) {
	def res = false
	if(val && myId && !parent) {
		def lval = atomicState?."remSenLock${val}"
		if(!lval) {
			atomicState?."remSenLock${val}" = myId
			res = true
		} else if(lval == myId) { res = true }
	}
	return res
}

def remSenUnlock(val, myId) {
	def res = false
	if(val && myId && !parent) {
		def lval = atomicState?."remSenLock${val}"
		if(lval) {
			if(lval == myId) {
				atomicState?."remSenLock${val}" = null
				res = true
			}
		} else { res = true }
	}
	return res
}

//Requirements Section
def remSenCoolTempsReq() { return (remSenRuleType in [ "Cool", "Heat_Cool", "Cool_Circ", "Heat_Cool_Circ" ]) ? true : false }
def remSenHeatTempsReq() { return (remSenRuleType in [ "Heat", "Heat_Cool", "Heat_Circ", "Heat_Cool_Circ" ]) ? true : false }
def remSenDayHeatTempOk()   { return (!remSenHeatTempsReq() || (remSenHeatTempsReq() && remSenDayHeatTemp)) ? true : false }
def remSenDayCoolTempOk()   { return (!remSenCoolTempsReq() || (remSenCoolTempsReq() && remSenDayCoolTemp)) ? true : false }

def isRemSenConfigured() {
	def devOk = (remSensorDay) ? true : false
	return (devOk && settings?.remSenRuleType && remSenDayHeatTempOk() && remSenDayCoolTempOk() ) ? true : false
}

def getLastMotionActiveSec(mySched) {
	def sLbl = "schMot_${mySched}_"
	return !atomicState?."${sLbl}MotionActiveDt" ? 0 : GetTimeDiffSeconds(atomicState?."${sLbl}MotionActiveDt").toInteger()
}

def getLastMotionInActiveSec(mySched) {
	def sLbl = "schMot_${mySched}_"
	return !atomicState?."${sLbl}MotionInActiveDt" ? 0 : GetTimeDiffSeconds(atomicState?."${sLbl}MotionInActiveDt").toInteger()
}

def automationMotionEvt(evt) {
	LogAction("Event | Motion Sensor: '${evt?.displayName}' Motion is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else {
		storeLastEventData(evt)
		def dorunIn = false
		def delay
		def sLbl

		def mySched = getCurrentSchedule()

		def schedList = atomicState?.scheduleList
		for (cnt in schedList) {
			sLbl = "schMot_${cnt}_"
			def act = settings["${sLbl}SchedActive"]
			if(act && settings["${sLbl}Motion"]) {
				def str = settings["${sLbl}Motion"].toString()
				if(str.contains( evt.displayName)) {
					def oldActive = atomicState?."${sLbl}MotionActive"
					def newActive = isMotionActive(settings["${sLbl}Motion"])
					atomicState."${sLbl}MotionActive" = newActive
					if(oldActive != newActive) {
						if(newActive) {
							if(cnt == mySched) { delay = settings."${sLbl}MDelayValOn"?.toInteger() ?: 15 }
							atomicState."${sLbl}MotionActiveDt" = getDtNow()
						} else {
							if(cnt == mySched) { delay = settings."${sLbl}MDelayValOff"?.toInteger() ?: 15*60 }
							atomicState."${sLbl}MotionInActiveDt" = getDtNow()
						}
					}
					LogAction("Updating Schedule Motion Sensor State: [ Schedule: (${cnt}) | Previous Active: (${oldActive}) | Current Status: ({$newActive}) ]", "trace", true)
					if(cnt == mySched) { dorunIn = true }
				}
			}
		}

		if(dorunIn) {
			LogAction(" Motion: [ Scheduling for Delay: ($delay) | Schedule ${mySched} ]", "trace", true)
			delay = delay > 20 ? delay : 20
			delay = delay < 60 ? delay : 60
			scheduleAutomationEval(delay)
		} else {
			def str = "Event | Skipping Motion Check because "
			if(mySched) {
				str += "motion sensor not in used in active schedule (${mySched})"
			} else {
				str += "no active schedule"
			}
			LogAction(str, "info", true)
		}
	}
}

def isMotionActive(sensors) {
	def result
	sensors?.each { sen ->
		if(sen) {
			def sval = sen?.currentState("motion").value
			if(sval == "active") { result = true }
		}
	}
	return result
	//return sensors?.currentState("motion")?.value.equals("active") ? true : false
}

def getDeviceTempAvg(items) {
	def tmpAvg = []
	def tempVal = 0
	if(!items) { return tempVal }
	else if(items?.size() > 1) {
		tmpAvg = items*.currentTemperature
		if(tmpAvg && tmpAvg?.size() > 1) { tempVal = (tmpAvg?.sum().toDouble() / tmpAvg?.size().toDouble()).round(1) }
	}
	else { tempVal = getDeviceTemp(items) }
	return tempVal.toDouble()
}

def remSenShowTempsPage() {
	dynamicPage(name: "remSenShowTempsPage", uninstall: false) {
		if(remSensorDay) {
			section("Default Sensor Temps: (Schedules can override)") {
				def cnt = 0
				def rCnt = remSensorDay?.size()
				def str = ""
				str += "Sensor Temp (average): (${getDeviceTempAvg(remSensorDay)}°${getTemperatureScale()})\n│"
				remSensorDay?.each { t ->
					cnt = cnt+1
					str += "${(cnt >= 1) ? "${(cnt == rCnt) ? "\n└" : "\n├"}" : "\n└"} ${t?.label}: ${(t?.label.length() > 10) ? "\n${(rCnt == 1 || cnt == rCnt) ? "    " : "│"}└ " : ""}(${getDeviceTemp(t)}°${getTemperatureScale()})"
				}
				paragraph "${str}", state: "complete", image: getAppImg("temperature_icon.png")
			}
		}
	}
}

def remSendoSetCool(chgval, onTemp, offTemp) {
	def remSenTstat = settings?.schMotTstat
	def remSenTstatMir = settings?.schMotTstatMir

	def hvacMode = remSenTstat ? remSenTstat?.currentThermostatMode.toString() : null
	def curCoolSetpoint = getTstatSetpoint(remSenTstat, "cool")
	def curHeatSetpoint = getTstatSetpoint(remSenTstat, "heat")
	def tempChangeVal = !remSenTstatTempChgVal ? 5.0 : remSenTstatTempChgVal.toDouble()
	def maxTempChangeVal = tempChangeVal * 3

	chgval = (chgval > (onTemp + maxTempChangeVal)) ? onTemp + maxTempChangeVal : chgval
	chgval = (chgval < (offTemp - maxTempChangeVal)) ? offTemp - maxTempChangeVal : chgval
	if(chgval != curCoolSetpoint) {
		scheduleAutomationEval(60)
		def cHeat = null
		if(hvacMode in ["auto"]) {
			if(curHeatSetpoint > (chgval-5.0)) {
				cHeat = chgval - 5.0
				LogAction("Remote Sensor: HEAT - Adjusting HeatSetpoint to (${cHeat}°${getTemperatureScale()}) to allow COOL setting", "info", true)
				if(remSenTstatMir) { remSenTstatMir*.setHeatingSetpoint(cHeat) }
			}
		}
		if(setTstatAutoTemps(remSenTstat, chgval, cHeat)) {
			LogAction("Remote Sensor: COOL - Adjusting CoolSetpoint to (${chgval}°${getTemperatureScale()}) ", "info", true)
			storeLastAction("Adjusted Cool Setpoint to (${chgval}°${getTemperatureScale()}) Heat Setpoint to (${cHeat}°${getTemperatureScale()})", getDtNow())
			if(remSenTstatMir) { remSenTstatMir*.setCoolingSetpoint(chgval) }
		}
		return  true // let all this take effect
	} else {
		LogAction("Remote Sensor: COOL - CoolSetpoint is already (${chgval}°${getTemperatureScale()}) ", "info", true)
	}
	return  false
}

def remSendoSetHeat(chgval, onTemp, offTemp) {
	def remSenTstat = schMotTstat
	def remSenTstatMir = schMotTstatMir

	def hvacMode = remSenTstat ? remSenTstat?.currentThermostatMode.toString() : null
	def curCoolSetpoint = getTstatSetpoint(remSenTstat, "cool")
	def curHeatSetpoint = getTstatSetpoint(remSenTstat, "heat")
	def tempChangeVal = !remSenTstatTempChgVal ? 5.0 : remSenTstatTempChgVal.toDouble()
	def maxTempChangeVal = tempChangeVal * 3

	chgval = (chgval < (onTemp - maxTempChangeVal)) ? onTemp - maxTempChangeVal : chgval
	chgval = (chgval > (offTemp + maxTempChangeVal)) ? offTemp + maxTempChangeVal : chgval
	if(chgval != curHeatSetpoint) {
		scheduleAutomationEval(60)
		def cCool = null
		if(hvacMode in ["auto"]) {
			if(curCoolSetpoint < (chgval+5)) {
				cCool = chgval + 5.0
				LogAction("Remote Sensor: COOL - Adjusting CoolSetpoint to (${cCool}°${getTemperatureScale()}) to allow HEAT setting", "info", true)
				if(remSenTstatMir) { remSenTstatMir*.setCoolingSetpoint(cCool) }
			}
		}
		if(setTstatAutoTemps(remSenTstat, cCool, chgval)) {
			LogAction("Remote Sensor: HEAT - Adjusting HeatSetpoint to (${chgval}°${getTemperatureScale()})", "info", true)
			storeLastAction("Adjusted Heat Setpoint to (${chgval}°${getTemperatureScale()}) Cool Setpoint to (${cCool}°${getTemperatureScale()})", getDtNow())
			if(remSenTstatMir) { remSenTstatMir*.setHeatingSetpoint(chgval) }
		}
		return  true // let all this take effect
	} else {
		LogAction("Remote Sensor: HEAT - HeatSetpoint is already (${chgval}°${getTemperatureScale()})", "info", true)
	}
	return  false
}

/*
private remSenCheck() {
	LogAction("remSenCheck.....", "trace", true)
	if(atomicState?.disableAutomation) { return }
	remSenCheck()
	//remSenTstatFanSwitchCheck()
}
*/

private remSenCheck() {
	LogAction("remSenCheck.....", "trace", true)
	if(atomicState?.disableAutomation) { return }
	try {
		def remSenTstat = schMotTstat
		def remSenTstatMir = schMotTstatMir

		def execTime = now()
		//atomicState?.lastEvalDt = getDtNow()

		def home = false
		def away = false
		if(remSenTstat && getTstatPresence(remSenTstat) == "present") { home = true }
		else { away = true }

		def noGoDesc = ""
		if( !remSensorDay || !remSenTstat || !home) {
			noGoDesc += !remSensorDay ? "Missing Required Sensor Selections..." : ""
			noGoDesc += !remSenTstat ? "Missing Required Thermostat device" : ""
			noGoDesc += !home ? "Ignoring because thermostat is in away mode." : ""
			LogAction("Remote Sensor NOT Evaluating...Evaluation Status: ${noGoDesc}", "warn", true)
		} else if(home) {
			//log.info "remSenCheck:  Evaluating Event..."

			def hvacMode = remSenTstat ? remSenTstat?.currentThermostatMode.toString() : null
			if(hvacMode == "off") {
				LogAction("Remote Sensor: Skipping Evaluation... The Current Thermostat Mode is 'OFF'...", "info", true)
				atomicState?.coolOverride = null
				atomicState?.heatOverride = null
				storeExecutionHistory((now() - execTime), "remSenCheck")
				return
			}

			def reqSenHeatSetPoint = getRemSenHeatSetTemp()
			def reqSenCoolSetPoint = getRemSenCoolSetTemp()

			if(hvacMode in ["auto"]) {
				// check that requested setpoints make sense & notify
				def coolheatDiff = Math.abs(reqSenCoolSetPoint - reqSenHeatSetPoint)
				if( !((reqSenCoolSetPoint >= reqSenHeatSetPoint) && (coolheatDiff > 2)) ) {
					LogAction("remSenCheck: Bad requested setpoints with auto mode ${reqSenCoolSetPoint} ${reqSenHeatSetPoint}...", "warn", true)
					storeExecutionHistory((now() - execTime), "remSenCheck")
					return
				}
			}

			def threshold = !remSenTempDiffDegrees ? 2.0 : remSenTempDiffDegrees.toDouble()
			def tempChangeVal = !remSenTstatTempChgVal ? 5.0 : remSenTstatTempChgVal.toDouble()
			def maxTempChangeVal = tempChangeVal * 3
			def curTstatTemp = getDeviceTemp(remSenTstat).toDouble()
			def curSenTemp = (remSensorDay) ? getRemoteSenTemp().toDouble() : null

			def curTstatOperState = remSenTstat?.currentThermostatOperatingState.toString()
			def curTstatFanMode = remSenTstat?.currentThermostatFanMode.toString()
			def fanOn = (curTstatFanMode == "on" || curTstatFanMode == "circulate") ? true : false
			def curCoolSetpoint = getTstatSetpoint(remSenTstat, "cool")
			def curHeatSetpoint = getTstatSetpoint(remSenTstat, "heat")
			def acRunning = (curTstatOperState == "cooling") ? true : false
			def heatRunning = (curTstatOperState == "heating") ? true : false

			LogAction("remSenCheck: Remote Sensor Rule Type: ${getEnumValue(remSenRuleEnum(), remSenRuleType)}", "info", false)
			LogAction("remSenCheck: Remote Sensor Temp: ${curSenTemp}", "info", false)
			LogAction("remSenCheck: Thermostat Info - ( Temperature: (${curTstatTemp}) | HeatSetpoint: (${curHeatSetpoint}) | CoolSetpoint: (${curCoolSetpoint}) | HvacMode: (${hvacMode}) | OperatingState: (${curTstatOperState}) | FanMode: (${curTstatFanMode}) )", "info", false)
			LogAction("remSenCheck: Desired Temps - Heat: ${reqSenHeatSetPoint} | Cool: ${reqSenCoolSetPoint}", "info", false)
			LogAction("remSenCheck: Threshold Temp: ${threshold} | Change Temp Increments: ${tempChangeVal}", "info", false)

			def modeOk = true
			if(!modeOk || !getRemSenModeOk()) {
				noGoDesc = ""
				noGoDesc += (!modeOk && getRemSenModeOk()) ? "Mode Filters were set and the current mode was not selected for Evaluation" : ""
				noGoDesc += (!getRemSenModeOk() && modeOk) ? "This mode is not one of those selected for evaluation..." : ""

// if we have heat on, ac on, or fan on, turn them off once

				if(atomicState?.haveRun) {
					if(remSenRuleType in ["Cool", "Heat_Cool", "Heat_Cool_Circ"]
					    && atomicState?.remSenCoolOn != null && !atomicState.remSenCoolOn
					    && (hvacMode in ["cool","auto"])
					    && acRunning) {
						def onTemp = reqSenCoolSetPoint + threshold
						def offTemp = reqSenCoolSetPoint
						chgval = curTstatTemp + tempChangeVal
						if(remSendoSetCool(chgval, onTemp, offTemp)) {
							noGoDesc +=  "   Turning off COOL due to mode change"
						}
						atomicState?.remSenCoolOn = false
					}

					if(remSenRuleType in ["Heat", "Heat_Cool", "Heat_Cool_Circ"]
					    && atomicState?.remSenHeatOn != null && !atomicState.remSenHeatOn
					    && (hvacMode in ["heat", "emergency heat", "auto"])
					    && heatRunning) {
						def onTemp = reqSenHeatSetPoint - threshold
						def offTemp = reqSenHeatSetPoint
						chgval = curTstatTemp - tempChangeVal
						if(remSendoSetHeat(chgval, onTemp, offTemp)) {
							noGoDesc +=  "   Turning off HEAT due to mode change"
						}
						atomicState?.remSenHeatOn = false
					}

					atomicState.haveRun = false
				}
				LogAction("Remote Sensor: Skipping Evaluation...Remote Sensor Evaluation Status: ${noGoDesc}", "info", true)
				storeExecutionHistory((now() - execTime), "remSenCheck")
				return
			}

			atomicState.haveRun = true

			def chg = false
			def chgval = 0
			if(hvacMode in ["cool","auto"]) {
				//Changes Cool Setpoints
				if(remSenRuleType in ["Cool", "Heat_Cool", "Heat_Cool_Circ"]) {
					def onTemp = reqSenCoolSetPoint + threshold
					def offTemp = reqSenCoolSetPoint
					def turnOn = false
					def turnOff = false

					LogAction("Remote Sensor: COOL - (Sensor Temp: ${curSenTemp} - CoolSetpoint: ${reqSenCoolSetPoint})", "trace", true)
					if(curSenTemp <= offTemp) {
						turnOff = true
					} else if(curSenTemp >= onTemp) {
						turnOn = true
					}

					if(turnOff && acRunning) {
						chgval = curTstatTemp + tempChangeVal
						chg = true
						LogAction("Remote Sensor: COOL - Adjusting CoolSetpoint to Turn Off Thermostat", "info", true)
						acRunning = false
						atomicState?.remSenCoolOn = false
					} else if(turnOn && !acRunning) {
						chgval = curTstatTemp - tempChangeVal
						chg = true
						acRunning = true
						atomicState.remSenCoolOn = true
						LogAction("Remote Sensor: COOL - Adjusting CoolSetpoint to Turn On Thermostat", "info", true)
					} else {
						// logic to decide if we need to nudge thermostat to keep it on or off
						if(acRunning) {
							chgval = curTstatTemp - tempChangeVal
							atomicState.remSenCoolOn = true
						} else {
							chgval = curTstatTemp + tempChangeVal
							atomicState?.remSenCoolOn = false
						}
						def coolDiff1 = Math.abs(curTstatTemp - curCoolSetpoint)
						LogAction("Remote Sensor: COOL - coolDiff1: ${coolDiff1} tempChangeVal: ${tempChangeVal}", "trace", false)
						if(coolDiff1 < (tempChangeVal / 2)) {
							chg = true
							LogAction("Remote Sensor: COOL - Adjusting CoolSetpoint to maintain state", "info", true)
						}
					}
					if(chg) {
						if(remSendoSetCool(chgval, onTemp, offTemp)) {
							storeExecutionHistory((now() - execTime), "remSenCheck")
							return // let all this take effect
						}

					} else {
						LogAction("Remote Sensor: NO CHANGE TO COOL - CoolSetpoint is (${curCoolSetpoint}°${getTemperatureScale()}) ", "info", true)
					}
				}
			}

			chg = false
			chgval = 0

			LogAction("remSenCheck: Thermostat Info - ( Temperature: (${curTstatTemp}) | HeatSetpoint: (${curHeatSetpoint}) | CoolSetpoint: (${curCoolSetpoint}) | HvacMode: (${hvacMode}) | OperatingState: (${curTstatOperState}) | FanMode: (${curTstatFanMode}) )", "info", false)

			//Heat Functions....
			if(hvacMode in ["heat", "emergency heat", "auto"]) {
				if(remSenRuleType in ["Heat", "Heat_Cool", "Heat_Cool_Circ"]) {
					def onTemp = reqSenHeatSetPoint - threshold
					def offTemp = reqSenHeatSetPoint
					def turnOn = false
					def turnOff = false

					LogAction("Remote Sensor: HEAT - (Sensor Temp: ${curSenTemp} - HeatSetpoint: ${reqSenHeatSetPoint})", "trace", false)
					if(curSenTemp <= onTemp) {
						turnOn = true
					} else if(curSenTemp >= offTemp) {
						turnOff = true
					}

					if(turnOff && heatRunning) {
						chgval = curTstatTemp - tempChangeVal
						chg = true
						LogAction("Remote Sensor: HEAT - Adjusting HeatSetpoint to Turn Off Thermostat", "info", true)
						heatRunning = false
						atomicState.remSenHeatOn = false
					} else if(turnOn && !heatRunning) {
						chgval = curTstatTemp + tempChangeVal
						chg = true
						LogAction("Remote Sensor: HEAT - Adjusting HeatSetpoint to Turn On Thermostat", "info", true)
						atomicState.remSenHeatOn = true
						heatRunning = true
					} else {
						// logic to decide if we need to nudge thermostat to keep it on or off
						if(heatRunning) {
							chgval = curTstatTemp + tempChangeVal
							atomicState.remSenHeatOn = true
						} else {
							chgval = curTstatTemp - tempChangeVal
							atomicState.remSenHeatOn = false
						}
						def heatDiff1 = Math.abs(curTstatTemp - curHeatSetpoint)
						LogAction("Remote Sensor: HEAT - heatDiff1: ${heatDiff1} tempChangeVal: ${tempChangeVal}", "trace", false)
						if(heatDiff1 < (tempChangeVal / 2)) {
							chg = true
							LogAction("Remote Sensor: HEAT - Adjusting HeatSetpoint to maintain state", "info", true)
						}
					}
					if(chg) {
						if(remSendoSetHeat(chgval, onTemp, offTemp)) {
							storeExecutionHistory((now() - execTime), "remSenCheck")
							return // let all this take effect
						}
					} else {
						LogAction("Remote Sensor: NO CHANGE TO HEAT - HeatSetpoint is already (${curHeatSetpoint}°${getTemperatureScale()})", "info", true)
					}
				}
			}
		}
		else {
			//
			// if all thermostats (primary and mirrors) are Nest, then AC/HEAT & fan will be off (or set back) with away mode.
			// if thermostats were not all Nest, then non Nest units could still be on for AC/HEAT or FAN...
			// current presumption in this implementation is:
			//      they are all nests or integrated with Nest (Works with Nest) as we don't have away/home temps for each mirror thermostats.   (They could be mirrored from primary)
			//      all thermostats in an automation are in the same Nest structure, so that all react to home/away changes
			//
			LogAction("Remote Sensor: Skipping Evaluation... Thermostat is set to away...", "info", true)
		}
		storeExecutionHistory((now() - execTime), "remSenCheck")
	} catch (ex) {
		log.error "remSenCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "remSenCheck", true, getAutoType())
	}
}

def getRemSenTempsToList() {
	def mySched = getCurrentSchedule()
	def sensors
	if(mySched) {
		def sLbl = "schMot_${mySched}_"
		if(settings["${sLbl}remSensor"]) {
			sensors = settings["${sLbl}remSensor"]
		}
	}
	if(!sensors) {  sensors = remSensorDay }
	if(sensors?.size() >= 1) {
		def info = []
		sensors?.sort().each {
			info.push("${it?.displayName}": " ${it?.currentTemperature.toString()}°${getTemperatureScale()}")
		}
		return info
	}
}

def getRemSenModeOk() {
	def result = false
	if(remSensorDay ) { result = true }
	//log.debug "getRemSenModeOk: $result"
	return result
}

def getDeviceTemp(dev) {
	return dev ? dev?.currentValue("temperature")?.toString().replaceAll("\\[|\\]", "").toDouble() : 0
}

def getTstatSetpoint(tstat, type) {
	if(tstat) {
		if(type == "cool") {
			def coolSp = tstat?.currentCoolingSetpoint
			return coolSp ? coolSp.toDouble() : 0
		} else {
			def heatSp = tstat?.currentHeatingSetpoint
			return heatSp ? heatSp.toDouble() : 0
		}
	}
	else { return 0 }
}

def getRemoteSenTemp() {
	def mySched = getCurrentSchedule()
	if(!atomicState.remoteTempSourceStr) { atomicState.remoteTempSourceStr = null }
	if(!atomicState.currentSchedNum) { atomicState.currentSchedNum = null }
	def sens
	if(mySched) {
		def sLbl = "schMot_${mySched}_"
		if(settings["${sLbl}remSensor"]) {
			atomicState.remoteTempSourceStr = "Schedule"
			atomicState.currentSchedNum = mySched
			sens = settings["${sLbl}remSensor"]
			return getDeviceTempAvg(sens).toDouble()
		}
	}
	if(remSensorDay) {
		atomicState.remoteTempSourceStr = "Remote Sensor"
		atomicState.currentSchedNum = null
		return getDeviceTempAvg(remSensorDay).toDouble()
	}
	else {
		atomicState.remoteTempSourceStr = "Thermostat"
		atomicState.currentSchedNum = null
		return getDeviceTemp(schMotTstat).toDouble()
/*
	else {
		log.warn "getRemoteSenTemp: No Temperature Found!!!"
		return 0.0
*/
	}
}

def getRemSenCoolSetTemp() {
	if(getLastOverrideCoolSec() < (3600 * 4)) {
		if(atomicState?.coolOverride != null) {
			return atomicState?.coolOverride.toDouble()
		}
	} else { atomicState?.coolOverride = null }

	def mySched = getCurrentSchedule()
	def coolTemp
	if(mySched) {
		def isBtwn = checkOnMotion(mySched)
		def hvacSettings = atomicState?."sched${mySched}restrictions"
		coolTemp = !isBtwn ? hvacSettings?.ctemp : hvacSettings?.mctemp ?: hvacSettings?.ctemp
	}
	if (coolTemp) {
		return coolTemp.toDouble()
	} else if(remSenDayCoolTemp) {
		return remSenDayCoolTemp.toDouble()
	}
	else {
		def desiredCoolTemp = getGlobalDesiredCoolTemp()
		if(desiredCoolTemp) { return desiredCoolTemp.toDouble() }
		else { return schMotTstat ? getTstatSetpoint(schMotTstat, "cool") : 0 }
	}
}

def getRemSenHeatSetTemp() {
	if(getLastOverrideHeatSec() < (3600 * 4)) {
		if(atomicState?.heatOverride != null) {
			return atomicState.heatOverride.toDouble()
		}
	} else { atomicState?.heatOverride = null }

	def mySched = getCurrentSchedule()
	def heatTemp
	if(mySched) {
		def isBtwn = checkOnMotion(mySched)
		def hvacSettings = atomicState?."sched${mySched}restrictions"
		heatTemp = !isBtwn ? hvacSettings?.htemp : hvacSettings?.mhtemp ?: hvacSettings?.htemp
	}
	if (heatTemp) {
		return heatTemp.toDouble()
	} else if(remSenDayHeatTemp) {
		return remSenDayHeatTemp.toDouble()
	}
	else {
		def desiredHeatTemp = getGlobalDesiredHeatTemp()
		if(desiredHeatTemp) { return desiredHeatTemp.toDouble() }
		else { return schMotTstat ? getTstatSetpoint(schMotTstat, "heat") : 0 }
	}
}

def getRemoteSenAutomationEnabled() {
	return atomicState?.disableAutomation ? false : true
}

// TODO When a temp change is sent to virtual device, it lasts for 4 hours, or next turn off, then we return to automation settings
// Other choices could be to change the schedule setpoint permanently if one is active,  or allow folks to set timer,  or have next schedule change clear override

def getLastOverrideCoolSec() { return !atomicState?.lastOverrideCoolDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastOverrideCoolDt).toInteger() }
def getLastOverrideHeatSec() { return !atomicState?.lastOverrideHeatDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastOverrideHeatDt).toInteger() }

def remSenTempUpdate(temp, mode) {
	LogAction("remSenTempUpdate(${temp}, ${mode})", "trace", true)

	def res = false
	if(atomicState?.disableAutomation) { return res }
	switch(mode) {
		case "heat":
			if(remSenHeatTempsReq()) {
				LogAction("remSenTempUpdate Set Heat Override to: ${temp} for 4 hours", "trace", true)
				atomicState?.heatOverride = temp.toDouble()
				atomicState?.lastOverrideHeatDt = getDtNow()
				scheduleAutomationEval()
				res = true
			}
			break
		case "cool":
			if(remSenCoolTempsReq()) {
				LogAction("remSenTempUpdate Set Cool Override to: ${temp} for 4 hours", "trace", true)
				atomicState?.coolOverride = temp.toDouble()
				atomicState?.lastOverrideCoolDt = getDtNow()
				scheduleAutomationEval()
				res = true
			}
			break
		default:
			LogAction("remSenTempUpdate Received an Invalid Request: ${mode}", "warn", true)
			break
	}
	return res
}

def remSenRuleEnum(excludeheatcool = false ) {
	// Determines that available rules to display based on the selected thermostats capabilites.
	def canCool = atomicState?.schMotTstatCanCool ? true : false
	def canHeat = atomicState?.schMotTstatCanHeat ? true : false
	def hasFan = atomicState?.schMotTstatHasFan ? true : false

	//log.debug "remSenRuleEnum -- hasFan: $hasFan (${atomicState?.schMotTstatHasFan} | canCool: $canCool (${atomicState?.schMotTstatCanCool} | canHeat: $canHeat (${atomicState?.schMotTstatCanHeat}"

	def vals = []
	if(excludeheatcool) {
		if(canCool && !canHeat && hasFan) { vals = ["Circ":"Circulate(Fan)", "Cool_Circ":"Cool/Circulate(Fan)"] }
		else if(!canCool && canHeat && hasFan) { vals = ["Circ":"Circulate(Fan)", "Heat_Circ":"Heat/Circulate(Fan)"] }
		else if(!canCool && !canHeat && hasFan) { vals = ["Circ":"Circulate(Fan)"] }
		else { vals = [ "Circ":"Circulate(Fan)", "Heat_Cool_Circ":"Auto/Circulate(Fan)", "Heat_Circ":"Heat/Circulate(Fan)", "Cool_Circ":"Cool/Circulate(Fan)" ] }
	} else {
		if(canCool && !canHeat && hasFan) { vals = ["Cool":"Cool", "Circ":"Circulate(Fan)", "Cool_Circ":"Cool/Circulate(Fan)"] }
		else if(canCool && !canHeat && !hasFan) { vals = ["Cool":"Cool"] }
		else if(!canCool && canHeat && hasFan) { vals = ["Circ":"Circulate(Fan)", "Heat":"Heat", "Heat_Circ":"Heat/Circulate(Fan)"] }
		else if(!canCool && canHeat && !hasFan) { vals = ["Heat":"Heat"] }
		else if(!canCool && !canHeat && hasFan) { vals = ["Circ":"Circulate(Fan)"] }
		else if(canCool && canHeat && !hasFan) { vals = ["Heat_Cool":"Auto", "Heat":"Heat", "Cool":"Cool"] }
		else { vals = [ "Heat_Cool":"Auto", "Heat":"Heat", "Cool":"Cool", "Circ":"Circulate(Fan)", "Heat_Cool_Circ":"Auto/Circulate(Fan)", "Heat_Circ":"Heat/Circulate(Fan)", "Cool_Circ":"Cool/Circulate(Fan)" ] }
	}
	//log.debug "remSenRuleEnum vals: $vals"
	return vals
}

/************************************************************************
|				    FAN CONTROL AUTOMATION CODE	     				    |
*************************************************************************/

def fanCtrlPrefix() { return "fanCtrl" }

def isFanCtrlConfigured() {
	return ( (settings?.fanCtrlFanSwitches && settings?.fanCtrlFanSwitchTriggerType && settings?.fanCtrlFanSwitchHvacModeFilter) || (isFanCircConfigured())) ? true : false
}

def isFanCircConfigured() {
	return (settings?.schMotCirculateTstatFan && settings?.schMotFanRuleType) ? true : false
}

def getFanSwitchDesc(showOpt = true) {
	def swDesc = ""
	def swCnt = 0
	def pName = fanCtrlPrefix()
	if(showOpt) {
		swDesc += (settings?."${pName}FanSwitches" && (settings?."${pName}FanSwitchSpeedCtrl" || settings?."${pName}FanSwitchTriggerType" || settings?."${pName}FanSwitchHvacModeFilter")) ? "Fan Switch Config:" : ""
	}
	swDesc += settings?."${pName}FanSwitches" ? "${showOpt ? "\n" : ""}• Fan Switches:" : ""
	def rmSwCnt = settings?."${pName}FanSwitches"?.size() ?: 0
	settings?."${pName}FanSwitches"?.each { sw ->
		swCnt = swCnt+1
		swDesc += "${swCnt >= 1 ? "${swCnt == rmSwCnt ? "\n   └" : "\n   ├"}" : "\n   └"} ${sw?.label}: (${sw?.currentSwitch?.toString().capitalize()})"
		swDesc += checkFanSpeedSupport(sw) ? "\n	 └ Current Spd: (${sw?.currentValue("currentState").toString()})" : ""
	}
	if(showOpt) {
		if (settings?."${pName}FanSwitches") {
			swDesc += (settings?."${pName}FanSwitchSpeedCtrl" || settings?."${pName}FanSwitchTriggerType" || settings?."${pName}FanSwitchHvacModeFilter") ? "\n\nFan Triggers:" : ""
			swDesc += (settings?."${pName}FanSwitchSpeedCtrl") ? "\n  • Fan Speed Support: (Active)" : ""
			swDesc += (settings?."${pName}FanSwitchTriggerType") ? "\n  • Fan Trigger: (${getEnumValue(switchRunEnum(), settings?."${pName}FanSwitchTriggerType")})" : ""
			swDesc += (settings?."${pName}FanSwitchHvacModeFilter") ? "\n  • Hvac Mode Filter: (${getEnumValue(fanModeTrigEnum(), settings?."${pName}FanSwitchHvacModeFilter")})" : ""
		}
	}

	swDesc += (settings?.schMotCirculateTstatFan) ? "\n  • Fan Circulation Enabled" : ""
	swDesc += (settings?.schMotCirculateTstatFan) ? "\n  • Fan Circulation Rule: (${getEnumValue(remSenRuleEnum(true), settings?.schMotCirculateTstatFan)})" : ""

	return (swDesc == "") ? null : "${swDesc}"
}

def getSchFanSwitchDesc(devs, showOpt = false) {
	def swDesc = ""
	def swCnt = 0
	def pName = fanCtrlPrefix()
	if(showOpt) {
		swDesc += (devs /*&& (settings?."${pName}FanSwitchSpeedCtrl" || settings?."${pName}FanSwitchTriggerType" || settings?."${pName}FanSwitchHvacModeFilter")*/) ? "Fan Switch Config:" : ""
	}
	swDesc += devs ? "${showOpt ? "\n" : ""}• Fan Switches:" : ""
	def rmSwCnt = devs?.size() ?: 0
	devs?.each { sw ->
		swCnt = swCnt+1
		swDesc += "${swCnt >= 1 ? "${swCnt == rmSwCnt ? "\n  └" : "\n  ├"}" : "\n  └"} ${sw?.label}: (${sw?.currentSwitch?.toString().capitalize()})"
		swDesc += "${checkFanSpeedSupport(sw) ? "\n       └ Fan Speed (${sw?.currentValue("currentState").toString()})" : ""}"
	}
	if(showOpt) {
		if (devs) {
			//swDesc += (settings?."${pName}FanSwitchSpeedCtrl" || settings?."${pName}FanSwitchTriggerType" || settings?."${pName}FanSwitchHvacModeFilter") ? "\n\nFan Triggers:" : ""
			//swDesc += (settings?."${pName}FanSwitchSpeedCtrl") ? "\n  • 3-Speed Fan Support: (Active)" : ""
			//swDesc += (settings?."${pName}FanSwitchTriggerType") ? "\n  • Fan Trigger: (${getEnumValue(switchRunEnum(), settings?."${pName}FanSwitchTriggerType")})" : ""
			//swDesc += (settings?."${pName}FanSwitchHvacModeFilter") ? "\n  • Hvac Mode Filter: (${getEnumValue(fanModeTrigEnum(), settings?."${pName}FanSwitchHvacModeFilter")})" : ""
		}
	}
	return (swDesc == "") ? null : "${swDesc}"
}

def getFanSwitchesSpdChk() {
	def devCnt = 0
	def pName = fanCtrlPrefix()
	if(settings?."${pName}FanSwitches") {
		settings?."${pName}FanSwitches"?.each { sw ->
			if(checkFanSpeedSupport(sw)) { devCnt = devCnt+1 }
		}
	}
	return (devCnt >= 1) ? true : false
}

def fanCtrlCheck() {
	//LogAction("FanControl Event | Fan Switch Check", "trace", false)
	try {
		def fanCtrlTstat = schMotTstat

		if(atomicState?.disableAutomation) { return }
		if(!fanCtrlFanSwitches) { return }

		def execTime = now()
		//atomicState?.lastEvalDt = getDtNow()

		def reqHeatSetPoint = getRemSenHeatSetTemp()
		def reqCoolSetPoint = getRemSenCoolSetTemp()

		//def curTstatTemp = getDeviceTemp(fanCtrlTstat).toDouble()

		def curTstatTemp = getRemoteSenTemp().toDouble()

		def curSetPoint = getReqSetpointTemp(curTstatTemp, reqHeatSetPoint, reqCoolSetPoint).req.toDouble() ?: 0
		def tempDiff = Math.abs(curSetPoint - curTstatTemp)
		LogAction("fanCtrlCheck: Desired Temps - Heat: ${reqHeatSetPoint} | Cool: ${reqCoolSetPoint}", "info", false)
		LogAction("fanCtrlCheck: Current Thermostat Sensor Temp: ${curTstatTemp} Temp Difference: (${tempDiff})", "info", false)

		doFanOperation(tempDiff)

		if(schMotCirculateTstatFan) {
			def threshold = !remSenTempDiffDegrees ? 2.0 : remSenTempDiffDegrees.toDouble()
			def curTstatFanMode = schMotTstat?.currentThermostatFanMode.toString()
			def fanOn = (curTstatFanMode == "on" || curTstatFanMode == "circulate") ? true : false
			def hvacMode = schMotTstat ? schMotTstat?.currentThermostatMode.toString() : null
/*
			if(atomicState?.haveRunFan) {
				if(schMotFanRuleType in ["Circ", "Cool_Circ", "Heat_Circ", "Heat_Cool_Circ"]) {
					if(fanOn) {
						LogAction("fantCtrlCheck: Turning OFF '${schMotTstat?.displayName}' Fan as modes do not match evaluation", "info", true)
						storeLastAction("Turned ${schMotTstat} Fan to (Auto)", getDtNow())
						schMotTstat?.fanAuto()
						if(schMotTstatMir) { schMotTstatMir*.fanAuto() }
					}
				}
				atomicState.haveRunFan = false
			}
*/
			if( (hvacMode in ["cool"] && schMotFanRuleType in ["Cool_Circ"]) ||
			    (hvacMode in ["heat"] && schMotFanRuleType in ["Heat_Circ"]) ||
			    (hvacMode in ["auto"] && schMotFanRuleType in ["Heat_Cool_Circ"]) ||
			    (hvacMode in ["off"] && schMotFanRuleType in ["Circ"]) ) {

				def sTemp = getReqSetpointTemp(curTstatTemp, reqHeatSetPoint, reqCoolSetPoint)
				circulateFanControl(sTemp?.type?.toString(), curTstatTemp, sTemp?.req?.toDouble(), threshold, fanOn)
			}
		}
		storeExecutionHistory((now()-execTime), "fanCtrlCheck")

	} catch (ex) {
		log.error "fanCtrlCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "fanCtrlCheck", true, getAutoType())
	}
}

// similar to getFanAutoModeTemp
def getReqSetpointTemp(curTemp, reqHeatSetPoint, reqCoolSetPoint) {
	LogAction("getReqSetpointTemp: Current Temp: ${curTemp} Req Heat: ${reqHeatSetPoint}  Req Cool: ${reqCoolSetPoint}", "info", false)
	def tstat = schMotTstat

	def hvacMode = tstat ? tstat?.currentThermostatMode.toString() : null
	def operState = tstat ? tstat?.currentThermostatOperatingState.toString() : null
	def opType = hvacMode.toString()

	if((hvacMode == "cool") || (operState == "cooling")) {
		opType = "cool"
	} else if((hvacMode == "heat") || (operState == "heating")) {
		opType = "heat"
	} else if(hvacMode == "auto") {
		def coolDiff = Math.abs(curTemp - reqCoolSetPoint)
		def heatDiff = Math.abs(curTemp - reqHeatSetPoint)
		opType = coolDiff < heatDiff ? "cool" : "heat"
	}
	def temp = (opType == "cool") ?  reqCoolSetPoint.toDouble() : reqHeatSetPoint.toDouble()
	return ["req":temp, "type":opType]
	//return temp
}

def doFanOperation(tempDiff) {
	def pName = fanCtrlPrefix()
	LogAction("doFanOperation: Temp Difference: (${tempDiff})", "info", false)
	try {
		def tstat = schMotTstat

		def curTstatTemp = tstat ?  getRemoteSenTemp().toDouble() : null
		def curCoolSetpoint = getRemSenCoolSetTemp()
		def curHeatSetpoint = getRemSenHeatSetTemp()

		def hvacMode = tstat ? tstat?.currentThermostatMode.toString() : null
		def curTstatOperState = tstat?.currentThermostatOperatingState.toString()
		def curTstatFanMode = tstat?.currentThermostatFanMode.toString()
		LogAction("doFanOperation: Thermostat Info - ( Temperature: (${curTstatTemp}) | HeatSetpoint: (${curHeatSetpoint}) | CoolSetpoint: (${curCoolSetpoint}) | HvacMode: (${hvacMode}) | OperatingState: (${curTstatOperState}) | FanMode: (${curTstatFanMode}) )", "info", false)

		def hvacFanOn = false
		 //1:"Heating/Cooling", 2:"With Fan Only"

		if( settings?."${pName}FanSwitchTriggerType".toInteger() ==  1) {
			hvacFanOn = (curTstatOperState in ["heating", "cooling"]) ? true : false
		}
		if( settings?."${pName}FanSwitchTriggerType".toInteger() ==  2) {
			hvacFanOn = (curTstatFanMode in ["on", "circulate"]) ? true : false
		}
		if(settings?."${pName}FanSwitchHvacModeFilter" != "any" && (settings?."${pName}FanSwitchHvacModeFilter" != hvacMode)) {
			LogAction("doFanOperation: Evaluating turn fans off Because Thermostat Mode does not Match the required Mode to Run Fans", "info", true)
			hvacFanOn = false  // force off of fans
		}
		if(atomicState?.haveRunFan == null) { atomicState.haveRunFan = false }
		def savedHaveRun = atomicState.haveRunFan

		settings?."${pName}FanSwitches"?.each { sw ->
			def swOn = (sw?.currentSwitch.toString() == "on") ? true : false
			if(hvacFanOn) {
				if(!swOn && !savedHaveRun) {
					LogAction("doFanOperation: Fan Switch (${sw?.displayName}) is (${swOn ? "ON" : "OFF"}) | Turning '${sw}' Switch (ON)", "info", true)
					sw.on()
					swOn = true
					atomicState.haveRunFan = true
					storeLastAction("Turned On $sw)", getDtNow())
				} else {
					if(!swOn && savedHaveRun) {
						LogAction("doFanOperation: Saved have run state shows switch ${sw} turned OFF outside of automation requests", "info", true)
					}
				}
				if(swOn && atomicState?.haveRunFan && checkFanSpeedSupport(sw)) {
					def speed = sw?.currentValue("currentState") ?: null
					if(settings?."${pName}FanSwitchSpeedCtrl" && settings?."${pName}FanSwitchHighSpeed" && settings?."${pName}FanSwitchMedSpeed" && settings?."${pName}FanSwitchLowSpeed") {
						if(tempDiff < settings?."${pName}FanSwitchMedSpeed".toDouble()) {
							if(speed != "LOW") {
								sw.lowSpeed()
								LogAction("doFanOperation: Temp Difference (${tempDiff}°${getTemperatureScale()}) is BELOW the Medium Speed Threshold of (${settings?."${pName}FanSwitchMedSpeed"}) | Turning '${sw}' Fan Switch on (LOW SPEED)", "info", true)
								storeLastAction("Set Fan $sw to Low Speed", getDtNow())
							}
						}
						else if(tempDiff >= settings?."${pName}FanSwitchMedSpeed".toDouble() && tempDiff < settings?."${pName}FanSwitchHighSpeed".toDouble()) {
							if(speed != "MED") {
								sw.medSpeed()
								LogAction("doFanOperation: Temp Difference (${tempDiff}°${getTemperatureScale()}) is ABOVE the Medium Speed Threshold of (${settings?."${pName}FanSwitchMedSpeed"}) | Turning '${sw}' Fan Switch on (MEDIUM SPEED)", "info", true)
								storeLastAction("Set Fan $sw to Medium Speed", getDtNow())
							}
						}
						else if(tempDiff >= settings?."${pName}FanSwitchHighSpeed".toDouble()) {
							if(speed != "HIGH") {
								sw.highSpeed()
								LogAction("doFanOperation: Temp Difference (${tempDiff}°${getTemperatureScale()}) is ABOVE the High Speed Threshold of (${settings?."${pName}FanSwitchHighSpeed"}) | Turning '${sw}' Fan Switch on (HIGH SPEED)", "info", true)
								storeLastAction("Set Fan $sw to High Speed", getDtNow())
							}
						}
					} else {
						if(speed != "HIGH") {
							sw.highSpeed()
							LogAction("doFanOperation: Fan supports multiple speeds, with speed control disabled | Turning '${sw}' Fan Switch on (HIGH SPEED)", "info", true)
							storeLastAction("Set Fan $sw to High Speed", getDtNow())
						}
					}
				}
			} else {
				if(swOn && savedHaveRun) {
					LogAction("doFanOperation: Fan Switch (${sw?.displayName}) is (${swOn ? "ON" : "OFF"}) | Turning '${sw}' Switch (OFF)", "info", true)
					storeLastAction("Turned Off (${sw})", getDtNow())
					sw.off()
					atomicState.haveRunFan = false
				} else {
					if(swOn && !savedHaveRun) {
						LogAction("doFanOperation: Saved have run state shows switch ${sw} turned ON outside of automation requests", "info", true)
					}
				}
			}
		}
	} catch (ex) {
		log.error "doFanOperation Exception:", ex
		parent?.sendExceptionData(ex.message, "doFanOperation", true, getAutoType())
	}
}

def getLastRemSenFanRunDtSec() { return !atomicState?.lastRemSenFanRunDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastRemSenFanRunDt).toInteger() }
def getLastRemSenFanOffDtSec() { return !atomicState?.lastRemSenFanOffDt ? 100000 : GetTimeDiffSeconds(atomicState?.lastRemSenFanOffDt).toInteger() }


// CONTROLS THE THERMOSTAT FAN
def circulateFanControl(operType, Double curSenTemp, Double reqSetpointTemp, Double threshold, Boolean fanOn) {
	def tstat = schMotTstat
	def tstatsMir = schMotTstatMir

	def hvacMode = tstat ? tstat?.currentThermostatMode.toString() : null
	def home = false
	def away = false
	if(tstat && getTstatPresence(tstat) == "present") { home = true }
	else { away = true }

	if(home && hvacMode in ["heat", "auto", "cool"]) {
		def curOperState = tstat?.currentThermostatOperatingState.toString()
		def curFanMode = tstat?.currentThermostatFanMode.toString()

		def returnToAuto = false
		def tstatOperStateOk = (curOperState == "idle") ? true : false
		// if ac or heat is on, we should put fan back to auto
		if(!tstatOperStateOk) {
			LogAction("Circulate Fan Run: The Thermostat OperatingState is Currently (${curOperState?.toString().toUpperCase()})... Skipping!!!", "info", true)

			if( atomicState?.lastRemSenFanOffDt > atomicState?.lastRemSenFanRunDt) { return }
			returnToAuto = true
		}

		def fanTempOk = getCirculateFanTempOk(curSenTemp, reqSetpointTemp, threshold, fanOn, operType)
		if(fanTempOk && !fanOn && !returnToAuto) {
			def waitTimeVal = remSenTimeBetweenRuns?.toInteger() ?: 3600
			def timeSinceLastOffOk = (getLastRemSenFanOffDtSec() > waitTimeVal) ? true : false
			if(!timeSinceLastOffOk) {
				def remaining = waitTimeVal - getLastRemSenFanOffDtSec()
				LogAction("Circulate Fan: Want to RUN Fan | Delaying for wait period ${waitTimeVal}, remaining ${remaining} seconds", "info", true)
				remaining = remaining > 20 ? remaining : 20
				remaining = remaining < 60 ? remaining : 60
				scheduleAutomationEval(remaining)
				return
			}
			LogAction("Circulate Fan: Activating '${tstat?.displayName}'' Fan for ${operType.toString().toUpperCase()}ING Circulation...", "debug", true)
			tstat?.fanOn()
			storeLastAction("Turned ${tstat} Fan 'On'", getDtNow())
			if(tstatsMir) {
				tstatsMir?.each { mt ->
					LogAction("Circulate Fan: Mirroring Primary Thermostat: Activating '${mt?.displayName}' Fan for ${operType.toString().toUpperCase()}ING Circulation", "debug", true)
					mt?.fanOn()
				}
			}
			atomicState?.lastRemSenFanRunDt = getDtNow()

		} else {
			if(fanOn && (returnToAuto || !fanTempOk)) {
				if(!returnToAuto) {
					def timeSinceLastRunOk = (getLastRemSenFanRunDtSec() > 600) ? true : false
					if(!timeSinceLastRunOk) {
						def remaining = 600 - getLastRemSenFanRunDtSec()
						LogAction("Circulate Fan Run: Want to STOP Fan  Delaying for wait period 600, remaining ${remaining} seconds", "info", true)
						remaining = remaining > 20 ? remaining : 20
						remaining = remaining < 60 ? remaining : 60
						scheduleAutomationEval(remaining)
						return
					}
				}
				LogAction("Circulate Fan: Turning OFF '${remSenTstat?.displayName}' Fan that was used for ${operType.toString().toUpperCase()}ING Circulation", "info", true)
				tstat?.fanAuto()
				storeLastAction("Turned ${tstat} Fan to 'Auto'", getDtNow())
				if(tstatsMir) {
					tstatsMir?.each { mt ->
						LogAction("Circulate Fan: Mirroring Primary Thermostat: Turning OFF '${mt?.displayName}' Fan that was used for ${operType.toString().toUpperCase()}ING Circulation", "info", true)
						mt?.fanAuto()
					}
				}
				atomicState?.lastRemSenFanOffDt = getDtNow()
			}
		}
	}
}

def getCirculateFanTempOk(Double senTemp, Double reqsetTemp, Double threshold, Boolean fanOn, operType) {
	LogAction("RemSenFanTempOk Debug:", "debug", false)

	def turnOn = false
	def adjust = (getTemperatureScale() == "C") ? 0.5 : 1.0
	if(threshold > (adjust * 2.0)) {
		adjust = adjust * 2.0
	}

	if(adjust >= threshold) {
		LogAction("Circulate Fan Temp: Bad threshold setting ${threshold} <= ${adjust}", "warn", true)
		return false
	}

	LogAction(" ├ adjust: ${adjust}}°${getTemperatureScale()}", "debug", false)
	LogAction(" ├ operType: (${operType.toString().toUpperCase()})", "debug", false)
	LogAction(" ├ Sensor Temp: ${senTemp}°${getTemperatureScale()} |  Requested Setpoint Temp: ${reqsetTemp}°${getTemperatureScale()}", "debug", false)

	def ontemp
	def offtemp

	if(operType == "cool") {
		ontemp = reqsetTemp + threshold
		offtemp = reqsetTemp
		if((senTemp > offtemp) && (senTemp <= (ontemp - adjust))) { turnOn = true }
	}
	if(operType == "heat") {
		ontemp = reqsetTemp - threshold
		offtemp = reqsetTemp
		if((senTemp < offtemp) && (senTemp >= (ontemp + adjust))) { turnOn = true }
	}

	LogAction(" ├ onTemp: ${ontemp}   | offTemp: ${offtemp}}°${getTemperatureScale()}", "debug", false)
	LogAction(" ├ FanAlreadyOn: (${fanOn.toString().toUpperCase()})", "debug", false)
	LogAction(" ┌ Final Result: (${turnOn.toString().toUpperCase()})", "debug", false)
	LogAction("getCirculateFanTempOk: ", "debug", false)

	if(!turnOn && fanOn) {
		LogAction("getCirculateFanTempOk: The Temperature Difference is Outside of Threshold Limits | Turning Thermostat Fan OFF", "info", true)
	}
	if(turnOn && !fanOn) {
		LogAction("getCirculateFanTempOk: The Temperature Difference is within the Threshold Limit | Turning Thermostat Fan ON", "info", true)
	}

	return turnOn
}


/********************************************************************************
|					EXTERNAL TEMP AUTOMATION CODE	     				|
*********************************************************************************/
def extTmpPrefix() { return "extTmp" }

def isExtTmpConfigured() {
	return ((settings?.extTmpUseWeather || settings?.extTmpTempSensor) && settings?.extTmpDiffVal) ? true : false
}

def getExtConditions( doEvent = false ) {
	//log.trace "getExtConditions..."
	if(atomicState?.NeedwUpd && parent?.getWeatherDeviceInst()) {
		def cur = parent?.getWData()
		def weather = parent.getWeatherDevice()

		atomicState?.curWeather = cur?.current_observation
		atomicState?.curWeatherTemp_f = Math.round(cur?.current_observation?.temp_f).toInteger()
		atomicState?.curWeatherTemp_c = Math.round(cur?.current_observation?.temp_c.toDouble())
		atomicState?.curWeatherLoc = cur?.current_observation?.display_location?.full.toString()  // This is not available as attribute in dth
		//atomicState?.curWeatherHum = cur?.current_observation?.relative_humidity?.toString().replaceAll("\\%", "")

		def dp = 0.0
		if(weather) {  // Dewpoint is calculated in dth
			dp = weather?.currentValue("dewpoint")?.toString().replaceAll("\\[|\\]", "").toDouble()
		}
		def c_temp = 0.0
		def f_temp = 0 as Integer
		if(getTemperatureScale() == "C") {
			c_temp = dp as Double
			f_temp = c_temp * 9/5 + 32
		} else {
			f_temp = dp as Integer
			c_temp = (f_temp - 32) * 5/9 as Double
		}
		atomicState?.curWeatherDewpointTemp_c = Math.round(c_temp.round(1) * 2) / 2.0f
		atomicState?.curWeatherDewpointTemp_f = Math.round(f_temp) as Integer

		atomicState.NeedwUpd = false
	}
}

def getExtTmpTemperature() {
	def extTemp = 0.0
	if(!settings?.extTmpUseWeather && settings?.extTmpTempSensor) {
		extTemp = getDeviceTemp(settings?.extTmpTempSensor)
	} else {
		if(settings?.extTmpUseWeather && (atomicState?.curWeatherTemp_f || atomicState?.curWeatherTemp_c)) {
			if(location?.temperatureScale == "C" && atomicState?.curWeatherTemp_c) { extTemp = atomicState?.curWeatherTemp_c.toDouble() }
			else { extTemp = atomicState?.curWeatherTemp_f.toDouble() }
		}
	}
	return extTemp
}

def getExtTmpDewPoint() {
	def extDp = 0.0
	if(settings?.extTmpUseWeather && (atomicState?.curWeatherDewpointTemp_f || atomicState?.curWeatherDewpointTemp_c)) {
		if(location?.temperatureScale == "C" && atomicState?.curWeatherDewpointTemp_c) { extDp = roundTemp(atomicState?.curWeatherDewpointTemp_c.toDouble()) }
		else { extDp = roundTemp(atomicState?.curWeatherDewpointTemp_f.toDouble()) }
	}
//TODO if an external sensor, if it has temp and humidity, we can calculate DP
	return extDp
}
def getDesiredTemp(curMode) {
	def modeOff = (curMode == "off") ? true : false
	def modeCool = (curMode == "cool") ? true : false
	def modeHeat = (curMode == "heat") ? true : false
	def modeAuto = (curMode == "auto") ? true : false

	def desiredHeatTemp = getRemSenHeatSetTemp()
	def desiredCoolTemp = getRemSenCoolSetTemp()
	def desiredTemp = 0
	if(desiredHeatTemp && modeHeat) { desiredTemp = desiredHeatTemp }
	if(desiredCoolTemp && modeCool) { desiredTemp = desiredCoolTemp  }
	if(desiredHeatTemp && desiredCoolTemp && (desiredHeatTemp < desiredCoolTemp) && modeAuto) { desiredTemp = (desiredCoolTemp + desiredHeatTemp)/2.0  }
	if(modeOff && !desiredTemp && atomicState?.extTmpLastDesiredTemp) { desiredTemp = atomicState?.extTmpLastDesiredTemp }

	LogAction("extTmpTempOk: Desired Temp: ${desiredTemp} | Desired Heat Temp: ${desiredHeatTemp} | Desired Cool Temp: ${desiredCoolTemp} atomicState.extTmpLastDesiredTemp: ${atomicState?.extTmpLastDesiredTemp}", "info", false)

	return desiredTemp

}

def extTmpTempOk() {
	//log.trace "extTmpTempOk..."
	def pName = extTmpPrefix()
	try {
		def extTmpTstat = settings?.schMotTstat
		def extTmpTstatMir = settings?.schMotTstatMir

		//def execTime = now()
		def intTemp = extTmpTstat ?  getRemoteSenTemp().toDouble() : null
		def extTemp = getExtTmpTemperature()
		def curMode = extTmpTstat.currentThermostatMode.toString()
		def dpLimit = getComfortDewpoint(extTmpTstat) ?: (getTemperatureScale() == "C" ? 19 : 66)
		def curDp = getExtTmpDewPoint()
		def diffThresh = getExtTmpTempDiffVal()
		def dpOk = (curDp < dpLimit) ? true : false

		def modeOff = (curMode == "off") ? true : false
		def modeCool = (curMode == "cool") ? true : false
		def modeHeat = (curMode == "heat") ? true : false
		def modeAuto = (curMode == "auto") ? true : false

		def desiredTemp = getDesiredTemp(curMode)
/*
		def desiredHeatTemp = getRemSenHeatSetTemp()
		def desiredCoolTemp = getRemSenCoolSetTemp()
		def desiredTemp = 0
		if(desiredHeatTemp && modeHeat) { desiredTemp = desiredHeatTemp }
		if(desiredCoolTemp && modeCool) { desiredTemp = desiredCoolTemp  }
		if(desiredHeatTemp && desiredCoolTemp && (desiredHeatTemp < desiredCoolTemp) && modeAuto) { desiredTemp = (desiredCoolTemp + desiredHeatTemp)/2.0  }
		if(modeOff && !desiredTemp && atomicState?.extTmpLastDesiredTemp) { desiredTemp = atomicState?.extTmpLastDesiredTemp }
		LogAction("extTmpTempOk: Desired Temp: ${desiredTemp} | Desired Heat Temp: ${desiredHeatTemp} | Desired Cool Temp: ${desiredCoolTemp} atomicState.extTmpLastDesiredTemp: ${atomicState?.extTmpLastDesiredTemp}", "info", false)

		if(!modeOff && desiredTemp) { atomicState?.extTmpLastDesiredTemp =  desiredTemp }
*/

		LogAction("extTmpTempOk: Inside Temp: ${intTemp} | curMode: ${curMode} | modeOff: ${modeOff} | atomicState.extTmpTstatOffRequested: ${atomicState?.extTmpTstatOffRequested}", "debug", false)

		if(!desiredTemp) {
			desiredTemp = intTemp
			LogAction("extTmpTempOk: No Desired Temp found, using interior Temp", "warn", true)
		}
		intTemp = desiredTemp

		def tempDiff = Math.abs(extTemp - intTemp)
		//LogAction("extTmpTempOk: Outside Temp: ${extTemp} | Temp Threshold: ${diffThresh} | Actual Difference: ${tempDiff} | Outside Dew point: ${curDp} | Dew point Limit: ${dpLimit}", "debug", false)

		def retval = true
		def tempOk = true
		def str = "enough different (${tempDiff})"
		if(intTemp && extTemp && diffThresh) {
			if(!modeAuto && tempDiff < diffThresh) {
				retval = false
				tempOk = false
			}
			def extTempHigh = (extTemp > intTemp - diffThresh) ? true : false
			def extTempLow = (extTemp < intTemp + diffThresh) ? true : false
			def oldMode = atomicState?.extTmpRestoreMode
			if(modeCool || oldMode == "cool") {
				str = "greater than"
				if(extTempHigh) { retval = false; tempOk = false }
			}
			if(modeHeat || oldMode == "heat") {
				str = "less than"
				if(extTempLow) { retval = false; tempOk = false }
			}
			if(modeAuto) { retval = false; str = "in supported mode" } // no point in turning off if in auto mode
			if(!dpOk) { retval = false }
			LogAction("extTmpTempOk: extTempHigh: ${extTempHigh} | extTempLow: ${extTempLow} | dpOk: ${dpOk}", "debug", false)
		}
		LogAction("extTmpTempOk: ${retval} Desired Inside Temp: (${intTemp}°${getTemperatureScale()}) is ${tempOk ? "" : "Not"} ${str} $diffThresh° of Outside Temp: (${extTemp}°${getTemperatureScale()}) or Dewpoint: (${curDp}°${getTemperatureScale()}) is ${dpOk ? "ok" : "TOO HIGH"}", "info", false)
		//storeExecutionHistory((now() - execTime), "getExtTmpTempOk")
		return retval
	} catch (ex) {
		log.error "getExtTmpTempOk Exception:", ex
		parent?.sendExceptionData(ex.message, "extTmpTempOk", true, getAutoType())
	}
}

def extTmpScheduleOk() { return autoScheduleOk(extTmpPrefix()) }
def getExtTmpTempDiffVal() { return !settings?.extTmpDiffVal ? 1.0 : settings?.extTmpDiffVal.toDouble() }
def getExtTmpWhileOnDtSec() { return !atomicState?.extTmpChgWhileOnDt ? 100000 : GetTimeDiffSeconds(atomicState?.extTmpChgWhileOnDt).toInteger() }
def getExtTmpWhileOffDtSec() { return !atomicState?.extTmpChgWhileOffDt ? 100000 : GetTimeDiffSeconds(atomicState?.extTmpChgWhileOffDt).toInteger() }

// TODO allow override from schedule?
def getExtTmpOffDelayVal() { return !settings?.extTmpOffDelay ? 300 : settings?.extTmpOffDelay.toInteger() }
def getExtTmpOnDelayVal() { return !settings?.extTmpOnDelay ? 300 : settings?.extTmpOnDelay.toInteger() }

def extTmpTempCheck(cTimeOut = false) {
	LogAction("extTmpTempCheck...", "trace", false)
	def pName = extTmpPrefix()

	try {
		if(atomicState?.disableAutomation) { return }
		else {

			def extTmpTstat = settings?.schMotTstat
			def extTmpTstatMir = settings?.schMotTstatMir

			def execTime = now()
			//atomicState?.lastEvalDt = getDtNow()

			if(!atomicState?."${pName}timeOutOn") { atomicState."${pName}timeOutOn" = false }
			if(cTimeOut) { atomicState."${pName}timeOutOn" = true }
			def timeOut = atomicState."${pName}timeOutOn" ?: false

			def curMode = extTmpTstat?.currentThermostatMode?.toString()
			def modeOff = (curMode == "off") ? true : false
			def safetyOk = getSafetyTempsOk(extTmpTstat)
			def schedOk = extTmpScheduleOk()
			def allowNotif = settings?."${pName}NotificationsOn" ? true : false
			def allowSpeech = allowNotif && settings?."${pName}AllowSpeechNotif" ? true : false
			def allowAlarm = allowNotif && settings?."${pName}AllowAlarmNotif" ? true : false
			def speakOnRestore = allowSpeech && settings?."${pName}SpeechOnRestore" ? true : false

			def home = false
			def away = false
			if(extTmpTstat && getTstatPresence(extTmpTstat) == "present") { home = true }
			else { away = true }

			if(!modeOff) { atomicState."${pName}timeOutOn" = false; timeOut = false }
			if(!modeOff && atomicState.extTmpTstatOffRequested) {  // someone switched us on when we had turned things off, so reset timer and states
				LogAction("extTmpTempCheck() | System turned on when automation had OFF, resetting state to match", "warn", true)
				atomicState.extTmpChgWhileOnDt = getDtNow()
				atomicState.extTmpTstatOffRequested = false
				atomicState?.extTmpRestoreMode = null
				atomicState."${pName}timeOutOn" = false
				unschedTimeoutRestore(pName)
			}

			def lastaway = atomicState?."${pName}lastaway"  // when we state change that could change desired Temp ensure delays happen before off can happen again
			atomicState?."${pName}lastaway" = home

			def lastDesired = atomicState?.extTmpLastDesiredTemp   // this catches scheduled temp or hvac mode changes
			def desiredTemp = getDesiredTemp(curMode)
			if(desiredTemp) { atomicState?.extTmpLastDesiredTemp =  desiredTemp }

			if(!modeOff && ( (home && lastaway != home) || (desiredTemp && desiredTemp != lastDesired)) ) { atomicState.extTmpChgWhileOnDt = getDtNow() }

			def okToRestore = (modeOff && atomicState?.extTmpTstatOffRequested && atomicState?.extTmpRestoreMode) ? true : false
			def tempWithinThreshold = extTmpTempOk()

			if(!tempWithinThreshold || timeOut || !safetyOk || away) {
				if(allowAlarm) { alarmEvtSchedCleanup(extTmpPrefix()) }
				def rmsg = ""
				if(okToRestore) {
					if(getExtTmpWhileOffDtSec() >= (getExtTmpOnDelayVal() - 5) || timeOut || !safetyOk) {
						def lastMode = null
						if(atomicState?.extTmpRestoreMode) { lastMode = atomicState?.extTmpRestoreMode }
						if(lastMode && (lastMode != curMode || timeOut || !safetyOk)) {
							scheduleAutomationEval(60)
							if(setTstatMode(extTmpTstat, lastMode)) {
								storeLastAction("Restored Mode ($lastMode)", getDtNow())
								atomicState?.extTmpRestoreMode = null
								atomicState?.extTmpTstatOffRequested = false
								atomicState?.extTmpRestoredDt = getDtNow()
								atomicState.extTmpChgWhileOnDt = getDtNow()
								atomicState."${pName}timeOutOn" = false
								unschedTimeoutRestore(pName)
								modeOff = false

								if(extTmpTstatMir) {
									if(setMultipleTstatMode(extTmpTstatMir, lastMode)) {
										LogAction("Mirroring (${lastMode}) Restore to ${extTmpTstatMir}", "info", true)
									}
								}

								rmsg = "extTmpTempCheck: Restoring '${extTmpTstat?.label}' to '${lastMode.toUpperCase()}' mode "
								def needAlarm = false
								if(!safetyOk) {
									rmsg += "because External Temp Safefy Temps have been reached..."
									needAlarm = true
								} else if(timeOut) {
									rmsg += "because the (${getEnumValue(longTimeSecEnum(), extTmpOffTimeout)}) Timeout has been reached..."
								} else if(away) {
									rmsg += "because of AWAY Nest mode..."
								} else {
									rmsg += "because External Temp has been above the Threshold for (${getEnumValue(longTimeSecEnum(), extTmpOnDelay)})..."
								}
								LogAction(rmsg, (needAlarm ? "warn" : "info"), true)
								if(allowNotif) {
									if(!timeOut && safetyOk) {
										sendEventPushNotifications(rmsg, "Info", pName)  // this uses parent and honors quiet times others do NOT
										if(speakOnRestore) { sendEventVoiceNotifications(voiceNotifString(atomicState?."${pName}OnVoiceMsg", pName), pName, "nmExtTmpOn_${app?.id}", true, "nmExtTmpOff_${app?.id}") }
									} else if(needAlarm) {
										sendEventPushNotifications(rmsg, "Warning", pName)
										if(allowAlarm) { scheduleAlarmOn(pName) }
									}
								}
								storeExecutionHistory((now() - execTime), "extTmpTempCheck")
								return

							} else { LogAction("extTmpTempCheck() | There was problem restoring the last mode to '...", "error", true) }
						} else {
							if(!lastMode) {
								LogAction("extTmpTempCheck() | Unable to restore settings because previous mode was not found. Likely due to other automation making changes.", "warn", true)
								atomicState?.extTmpTstatOffRequested = false
							} else if(!timeOut && safetyOk) { LogAction("extTmpTstatCheck() | Skipping Restore because the Mode to Restore is same as Current Mode ${curMode}", "info", true) }
							if(!safetyOk) { LogAction("extTmpTempCheck() | Unable to restore mode and safety temperatures are exceeded", "warn", true) }
 						// TODO check if timeout quickly cycles back...
						}
					} else {
						if(safetyOk) {
							def remaining = getExtTmpOnDelayVal() - getExtTmpWhileOffDtSec()
							LogAction("extTmpTempCheck: Delaying restore for wait period ${getExtTmpOnDelayVal()}, remaining ${remaining}", "info", true)
							remaining = remaining > 20 ? remaining : 20
							remaining = remaining < 60 ? remaining : 60
							scheduleAutomationEval(remaining)
						}
					}
				} else {
					if(modeOff) {
						if(timeout || !safetyOk) {
							LogAction("extTmpTempCheck() | Timeout or Safety temps exceeded and Unable to restore settings okToRestore is false", "warn", true)
							atomicState."${pName}timeOutOn" = false
						}
						else if(!atomicState?.extTmpRestoreMode && atomicState?.extTmpTstatOffRequested) {
							LogAction("extTmpTempCheck() | Unable to restore settings because previous mode was not found. Likely due to other automation making changes.", "warn", true)
							atomicState?.extTmpTstatOffRequested = false
						}
					}
				}
			}

			if(tempWithinThreshold && !timeOut && safetyOk && schedOk && home) {
				def rmsg = ""
				if(!modeOff) {
					if(getExtTmpWhileOnDtSec() >= (getExtTmpOffDelayVal() - 2)) {
						atomicState."${pName}timeOutOn" = false
						atomicState?.extTmpRestoreMode = curMode
						LogAction("extTmpTempCheck: Saving ${extTmpTstat?.label} (${atomicState?.extTmpRestoreMode.toString().toUpperCase()}) mode for Restore later.", "info", true)
						scheduleAutomationEval(60)
						if(setTstatMode(extTmpTstat, "off")) {
							storeLastAction("Turned Off Thermostat", getDtNow())
							atomicState?.extTmpTstatOffRequested = true
							atomicState.extTmpChgWhileOffDt = getDtNow()
							scheduleTimeoutRestore(pName)
							modeOff = true
							rmsg = "${extTmpTstat.label} has been turned 'Off' because External Temp is at the temp threshold for (${getEnumValue(longTimeSecEnum(), extTmpOffDelay)})!!!"
							if(extTmpTstatMir) {
								setMultipleTstatMode(extTmpTstatMir, "off") {
									LogAction("Mirroring (Off) Mode to ${extTmpTstatMir}", "info", true)
								}
							}
							LogAction(rmsg, "info", true)
							if(allowNotif) {
								sendEventPushNotifications(rmsg, "Info", pName) // this uses parent and honors quiet times, others do NOT
								if(allowSpeech) { sendEventVoiceNotifications(voiceNotifString(atomicState?."${pName}OffVoiceMsg",pName), pName, "nmExtTmpOff_${app?.id}", true, "nmExtTmpOn_${app?.id}") }
								if(allowAlarm) { scheduleAlarmOn(pName) }
							}
						} else { LogAction("extTmpTempCheck(): Error turning themostat Off", "warn", true) }
					} else {
						def remaining = getExtTmpOffDelayVal() - getExtTmpWhileOnDtSec()
						LogAction("extTmpTempCheck: Delaying OFF for wait period ${getExtTmpOffDelayVal()}, remaining ${remaining}", "info", true)
						remaining = remaining > 20 ? remaining : 20
						remaining = remaining < 60 ? remaining : 60
						scheduleAutomationEval(remaining)
					}
				} else {
				   LogAction("extTmpTempCheck() | Skipping because Exterior temperatures in range and '${extTmpTstat?.label}' mode is already 'OFF'", "info", true)
				}
			} else {
				if(!home) { LogAction("extTmpTempCheck: Skipping because of AWAY Nest mode...", "info", true) }
				else if(!schedOk) { LogAction("extTmpTempCheck: Skipping because of Schedule Restrictions...", "info", true) }
				else if(!safetyOk) { LogAction("extTmpTempCheck: Skipping because of Safety Temps Exceeded...", "info", true) }
				else if(timeOut) { LogAction("extTmpTempCheck: Skipping because of active timeout...", "info", true) }
				else if(!tempWithinThreshold) { LogAction("extTmpTempCheck: Exterior temperatures not in range...", "info", true) }
			}
			storeExecutionHistory((now() - execTime), "extTmpTempCheck")
		}
	} catch (ex) {
		log.error "extTmpTempCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "extTmpTempCheck", true, getAutoType())
	}
}

def extTmpTempEvt(evt) {
	LogAction("extTmp Event | External Sensor Temperature: ${evt?.displayName} - Temperature is (${evt?.value.toString().toUpperCase()})", "trace", true)
	storeLastEventData(evt)
	extTmpDpOrTempEvt("extTmpTempEvt()")
}

def extTmpDpEvt(evt) {
	LogAction("extTmp Event | External Sensor Dew point: ${evt?.displayName} - Dew point Temperature is (${evt?.value.toString().toUpperCase()})", "trace", true)
	storeLastEventData(evt)
	extTmpDpOrTempEvt("extTmpDpEvt()")
}

def extTmpDpOrTempEvt(type) {
	if(atomicState?.disableAutomation) { return }
	else {
		def extTmpTstat = settings?.schMotTstat
		def extTmpTstatMir = settings?.schMotTstatMir

		def curMode = extTmpTstat?.currentThermostatMode.toString()
		def modeOff = (curMode == "off") ? true : false
		def offVal = getExtTmpOffDelayVal()
		def onVal = getExtTmpOnDelayVal()
		def timeVal

		atomicState.NeedwUpd = true
		getExtConditions()

		def lastTempWithinThreshold = atomicState?.extTmpLastWithinThreshold
		def tempWithinThreshold = extTmpTempOk()
		atomicState?.extTmpLastWithinThreshold = tempWithinThreshold

		if(lastTempWithinThreshold != null && tempWithinThreshold != lastTempWithinThreshold) {
			if(!modeOff) {
				atomicState.extTmpChgWhileOnDt = getDtNow()
				timeVal = ["valNum":offVal, "valLabel":getEnumValue(longTimeSecEnum(), offVal)]
			} else {
				atomicState.extTmpChgWhileOffDt = getDtNow()
				timeVal = ["valNum":onVal, "valLabel":getEnumValue(longTimeSecEnum(), onVal)]
			}
			def val = timeVal?.valNum > 20 ? timeVal?.valNum : 20
			val = timeVal?.valNum < 60 ? timeVal?.valNum : 60
			LogAction("${type} | External Temp Check scheduled for (${timeVal.valLabel}) HVAC mode: ${curMode}...", "info", true)
			scheduleAutomationEval(val)
		} else {
			LogAction("${type}: Skipping Event...no state change | tempWithinThreshold: ${tempWithinThreshold}", "info", true)
		}
	}
}

/******************************************************************************
|						WATCH CONTACTS AUTOMATION CODE			  			  |
*******************************************************************************/
def conWatPrefix() { return "conWat" }

def conWatContactDesc() {
	if(settings?.conWatContacts) {
		def cCnt = settings?.conWatContacts?.size() ?: 0
		def str = ""
		def cnt = 0
		str += "Contact Status:"
		settings?.conWatContacts?.each { dev ->
			cnt = cnt+1
			str += "${(cnt >= 1) ? "${(cnt == cCnt) ? "\n└" : "\n├"}" : "\n└"} ${dev?.label}: (${dev?.currentContact?.toString().capitalize()})"
		}
		return str
	}
	return null
}

def isConWatConfigured() {
	return (settings?.conWatContacts && settings?.conWatOffDelay) ? true : false
}

def getConWatContactsOk() { return settings?.conWatContacts?.currentState("contact")?.value.contains("open") ? false : true }
def conWatContactOk() { return (!settings?.conWatContacts) ? false : true }
def conWatScheduleOk() { return autoScheduleOk(conWatPrefix()) }
def getConWatOpenDtSec() { return !atomicState?.conWatOpenDt ? 100000 : GetTimeDiffSeconds(atomicState?.conWatOpenDt).toInteger() }
def getConWatCloseDtSec() { return !atomicState?.conWatCloseDt ? 100000 : GetTimeDiffSeconds(atomicState?.conWatCloseDt).toInteger() }
def getConWatRestoreDelayBetweenDtSec() { return !atomicState?.conWatRestoredDt ? 100000 : GetTimeDiffSeconds(atomicState?.conWatRestoredDt).toInteger() }

// TODO allow override from schedule?
def getConWatOffDelayVal() { return !settings?.conWatOffDelay ? 300 : (settings?.conWatOffDelay.toInteger()) }
def getConWatOnDelayVal() { return !settings?.conWatOnDelay ? 300 : (settings?.conWatOnDelay.toInteger()) }
def getConWatRestoreDelayBetweenVal() { return !settings?.conWatRestoreDelayBetween ? 600 : settings?.conWatRestoreDelayBetween.toInteger() }

def conWatCheck(cTimeOut = false) {
	//log.trace "conWatCheck..."
	//
	// Should consider not turning thermostat off, as much as setting it more toward away settings?
	// There should be monitoring of actual temps for min and max warnings given on/off automations
	//
	// Should have some check for stuck contacts
	// if we cannot save/restore settings, don't bother turning things off
	//
	def pName = conWatPrefix()

	def conWatTstat = settings?.schMotTstat
	def conWatTstatMir = settings?.schMotTstatMir

	try {
		if(atomicState?.disableAutomation) { return }
		else {
			def execTime = now()
			//atomicState?.lastEvalDt = getDtNow()

			if(!atomicState?."${pName}timeOutOn") { atomicState."${pName}timeOutOn" = false }
			if(cTimeOut) { atomicState."${pName}timeOutOn" = true }
			def timeOut = atomicState."${pName}timeOutOn" ?: false
			def curMode = conWatTstat?.currentState("thermostatMode")?.value.toString()
			def curNestPres = getTstatPresence(conWatTstat)
			def modeOff = (curMode == "off") ? true : false
			def openCtDesc = getOpenContacts(conWatContacts) ? " '${getOpenContacts(conWatContacts)?.join(", ")}' " : " a selected contact "
			def safetyOk = getSafetyTempsOk(conWatTstat)
			def schedOk = conWatScheduleOk()
			def allowNotif = settings?."${pName}NotificationsOn" ? true : false
			def allowSpeech = allowNotif && settings?."${pName}AllowSpeechNotif" ? true : false
			def allowAlarm = allowNotif && settings?."${pName}AllowAlarmNotif" ? true : false
			def speakOnRestore = allowSpeech && settings?."${pName}SpeechOnRestore" ? true : false

			def home = false
			def away = false
			if(conWatTstat && getTstatPresence(conWatTstat) == "present") { home = true }
			else { away = true }

			//log.debug "curMode: $curMode | modeOff: $modeOff | conWatRestoreOnClose: $conWatRestoreOnClose | lastMode: $lastMode"
			//log.debug "conWatTstatOffRequested: ${atomicState?.conWatTstatOffRequested} | getConWatCloseDtSec(): ${getConWatCloseDtSec()}"

			if(!modeOff) { atomicState."${pName}timeOutOn" = false; timeOut = false }

			if(!modeOff && atomicState.conWatTstatOffRequested) {  // someone switched us on when we had turned things off, so reset timer and states
				LogAction("conWatCheck() | System turned on when automation had OFF, resetting state to match", "warn", true)
				atomicState?.conWatRestoreMode = null
				atomicState?.conWatTstatOffRequested = false
				atomicState?.conWatOpenDt = getDtNow()
				atomicState."${pName}timeOutOn" = false
				unschedTimeoutRestore(pName)
			}

			def lastaway = atomicState?."${pName}lastaway"  // when we state change from away to home, ensure delays happen before off can happen again
			if(!modeOff && (home && lastaway != home)) { atomicState?.conWatOpenDt = getDtNow() }
			atomicState?."${pName}lastaway" = home

			def okToRestore = (modeOff && atomicState?.conWatTstatOffRequested) ? true : false
			def contactsOk = getConWatContactsOk()

			if(contactsOk || timeOut || !safetyOk || away) {
				if(allowAlarm) { alarmEvtSchedCleanup(conWatPrefix()) }
				def rmsg = ""
				if(okToRestore) {
					if(getConWatCloseDtSec() >= (getConWatOnDelayVal() - 5) || timeOut || !safetyOk) {
						def lastMode = null
						if(atomicState?.conWatRestoreMode) { lastMode = atomicState?.conWatRestoreMode }
						if(lastMode && (lastMode != curMode || timeOut || !safetyOk)) {
							scheduleAutomationEval(60)
							if(setTstatMode(conWatTstat, lastMode)) {
								storeLastAction("Restored Mode ($lastMode) to $conWatTstat", getDtNow())
								atomicState?.conWatRestoreMode = null
								atomicState?.conWatTstatOffRequested = false
								atomicState?.conWatRestoredDt = getDtNow()
								atomicState?.conWatOpenDt = getDtNow()
								atomicState."${pName}timeOutOn" = false
								unschedTimeoutRestore(pName)
								modeOff = false

								if(conWatTstatMir) {
									if(setMultipleTstatMode(conWatTstatMir, lastMode)) {
										LogAction("Mirroring (${lastMode}) Restore to ${conWatTstatMir}", "info", true)
									}
								}
								rmsg = "Restoring '${conWatTstat?.label}' to '${lastMode?.toString().toUpperCase()}' mode "
								def needAlarm = false
								if(!safetyOk) {
									rmsg += "because Global Safefy Values have been reached..."
									needAlarm = true
								} else if(timeOut) {
									rmsg += "because the (${getEnumValue(longTimeSecEnum(), conWatOffTimeout)}) Timeout has been reached..."
								} else if(away) {
									rmsg += "because of AWAY Nest mode..."
								} else {
									rmsg += "because ALL contacts have been 'Closed' again for (${getEnumValue(longTimeSecEnum(), conWatOnDelay)})..."
								}

								LogAction(rmsg, (needAlarm ? "warn" : "info"), true)
//ERS
								if(allowNotif) {
									if(!timeOut && safetyOk) {
										sendEventPushNotifications(rmsg, "Info", pName) // this uses parent and honors quiet times, others do NOT
										if(speakOnRestore) { sendEventVoiceNotifications(voiceNotifString(atomicState?."${pName}OnVoiceMsg",pName), pName, "nmConWatOn_${app?.id}", true, "nmConWatOff_${app?.id}") }
									} else if(needAlarm) {
										sendEventPushNotifications(rmsg, "Warning", pName)
										if(allowAlarm) { scheduleAlarmOn(pName) }
									}
								}
								storeExecutionHistory((now() - execTime), "conWatCheck")
								return

							} else { LogAction("conWatCheck() | There was Problem Restoring the Last Mode to ($lastMode)", "error", true) }
						} else {
							if(!lastMode) {
								LogAction("conWatCheck() | Unable to restore settings because previous mode was not found. Likely due to other automation making changes.", "warn", true)
								atomicState?.conWatTstatOffRequested = false
							} else if(!timeOut && safetyOk) { LogAction("conWatCheck() | Skipping Restore because the Mode to Restore is same as Current Mode ${curMode}", "info", true) }
							if(!safetyOk) { LogAction("conWatCheck() | Unable to restore mode and safety temperatures are exceeded", "warn", true) }
						}
					} else {
						if(safetyOk) {
							def remaining = getConWatOnDelayVal() - getConWatCloseDtSec()
							LogAction("conWatCheck: Delaying restore for wait period ${getConWatOnDelayVal()}, remaining ${remaining}", "info", true)
							remaining = remaining > 20 ? remaining : 20
							remaining = remaining < 60 ? remaining : 60
							scheduleAutomationEval(remaining)
						}
					}
				} else {
					if(modeOff) {
						if(timeOut || !safetyOk) {
							LogAction("conWatCheck() | Timeout or Safety temps exceeded and Unable to restore settings okToRestore is false", "warn", true)
							atomicState."${pName}timeOutOn" = false
						}
						else if(!atomicState?.conWatRestoreMode && atomicState?.conWatTstatOffRequested) {
							LogAction("conWatCheck() | Unable to restore settings because previous mode was not found. Likely due to other automation making changes.", "warn", true)
							atomicState?.conWatTstatOffRequested = false
						}
					}
				}
			}

			if(!contactsOk && safetyOk && !timeOut && schedOk && home) {
				def rmsg = ""
				if(!modeOff) {
					if((getConWatOpenDtSec() >= (getConWatOffDelayVal() - 2)) && (getConWatRestoreDelayBetweenDtSec() >= (getConWatRestoreDelayBetweenVal() - 2))) {
						atomicState."${pName}timeOutOn" = false
						atomicState?.conWatRestoreMode = curMode
						LogAction("conWatCheck: Saving ${conWatTstat?.label} mode (${atomicState?.conWatRestoreMode.toString().toUpperCase()}) for Restore later.", "info", true)
						LogAction("conWatCheck: ${openCtDesc}${getOpenContacts(conWatContacts).size() > 1 ? "are" : "is"} still Open: Turning 'OFF' '${conWatTstat?.label}'", "debug", true)
						scheduleAutomationEval(60)
						if(setTstatMode(conWatTstat, "off")) {
							storeLastAction("Turned Off $conWatTstat", getDtNow())
							atomicState?.conWatTstatOffRequested = true
							atomicState?.conWatCloseDt = getDtNow()
							scheduleTimeoutRestore(pName)
							if(conWatTstatMir) {
								setMultipleTstatMode(conWatTstatMir, "off") {
									LogAction("Mirroring (Off) Mode to ${conWatTstatMir}", "info", true)
								}
							}
							rmsg = "${conWatTstat.label} has been turned 'OFF' because${openCtDesc}has been Opened for (${getEnumValue(longTimeSecEnum(), conWatOffDelay)})..."
							LogAction(rmsg, "info", true)
							if(allowNotif) {
								sendEventPushNotifications(rmsg, "Info", pName) // this uses parent and honors quiet times, others do NOT
								if(allowSpeech) { sendEventVoiceNotifications(voiceNotifString(atomicState?."${pName}OffVoiceMsg",pName), pName, "nmConWatOff_${app?.id}", true, "nmConWatOn_${app?.id}") }
								if(allowAlarm) { scheduleAlarmOn(pName) }
							}
						} else { LogAction("conWatCheck(): Error turning themostat Off", "warn", true) }
					} else {
						if(getConWatRestoreDelayBetweenDtSec() < (getConWatRestoreDelayBetweenVal() - 2)) {
							def remaining = getConWatRestoreDelayBetweenVal() -  getConWatRestoreDelayBetweenDtSec()
							LogAction("conWatCheck() | Skipping OFF change because the delay since last restore has been less than (${getEnumValue(longTimeSecEnum(), conWatRestoreDelayBetween)})", "info", false)
							remaining = remaining > 20 ? remaining : 20
							remaining = remaining < 60 ? remaining : 60
							scheduleAutomationEval(remaining)
						} else {
							def remaining = getConWatOffDelayVal() - getConWatOpenDtSec()
							LogAction("conWatCheck: Delaying OFF for wait period ${getConWatOffDelayVal()}, remaining ${remaining}", "info", true)
							remaining = remaining > 20 ? remaining : 20
							remaining = remaining < 60 ? remaining : 60
							scheduleAutomationEval(remaining)
						}
					}
				} else {
					LogAction("conWatCheck() | Skipping OFF change because '${conWatTstat?.label}' mode is already 'OFF'", "info", true)
				}
			} else {
				if(!home) { LogAction("conWatCheck: Skipping because of AWAY Nest mode...", "info", true) }
				else if(!schedOk) { LogAction("conWatCheck: Skipping because of Schedule Restrictions...", "info", true) }
				else if(!safetyOk) { LogAction("conWatCheck: Skipping because of Safety Temps Exceeded...", "warn", true) }
				else if(timeOut) { LogAction("conWatCheck: Skipping because of active timeout...", "info", true) }
				else if(contactsOk) { LogAction("conWatCheck: Contacts are closed...", "info", true) }
			}
			storeExecutionHistory((now() - execTime), "conWatCheck")
		}
	} catch (ex) {
		log.error "conWatCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "conWatCheck", true, getAutoType())
	}
}

def conWatContactEvt(evt) {
	LogAction("ContactWatch Contact Event | '${evt?.displayName}' is now (${evt?.value.toString().toUpperCase()})", "trace", false)
	if(atomicState?.disableAutomation) { return }
	else {
		def conWatTstat = settings?.schMotTstat
		def curMode = conWatTstat?.currentThermostatMode.toString()
		def isModeOff = (curMode == "off") ? true : false
		def conOpen = (evt?.value == "open") ? true : false
		def canSched = false
		def timeVal
		if(conOpen) {
			atomicState?.conWatOpenDt = getDtNow()
			timeVal = ["valNum":getConWatOffDelayVal(), "valLabel":getEnumValue(longTimeSecEnum(), getConWatOffDelayVal())]
			canSched = true
		}
		else if(!conOpen && getConWatContactsOk()) {
			if(isModeOff) {
				atomicState.conWatCloseDt = getDtNow()
				timeVal = ["valNum":getConWatOnDelayVal(), "valLabel":getEnumValue(longTimeSecEnum(), getConWatOnDelayVal())]
				canSched = true
			}
		}
		storeLastEventData(evt)
		if(canSched) {
			LogAction("conWatContactEvt: ${!evt ? "A monitored Contact is " : "'${evt?.displayName}' is "} '${evt?.value.toString().toUpperCase()}' | Contact Check scheduled for (${timeVal?.valLabel})...", "info", true)
			def val = timeVal?.valNum > 20 ? timeVal?.valNum : 20
			val = timeVal?.valNum < 60 ? timeVal?.valNum : 60
			scheduleAutomationEval(val)
		} else {
			LogAction("conWatContactEvt: Skipping Event...", "info", true)
		}
	}
}

/******************************************************************************
|					WATCH FOR LEAKS AUTOMATION LOGIC CODE			  	  	  |
******************************************************************************/
def leakWatPrefix() { return "leakWat" }

def leakWatSensorsDesc() {
	if(settings?.leakWatSensors) {
		def cCnt = settings?.leakWatSensors?.size() ?: 0
		def str = ""
		def cnt = 0
		str += "Leak Sensors:"
		settings?.leakWatSensors?.each { dev ->
			cnt = cnt+1
			str += "${(cnt >= 1) ? "${(cnt == cCnt) ? "\n└" : "\n├"}" : "\n└"} ${dev?.label}: ${dev?.currentWater?.toString().capitalize()}"
		}
		return str
	}
	return null
}

def isLeakWatConfigured() {
	return (settings?.leakWatSensors) ? true : false
}

def getLeakWatSensorsOk() { return settings?.leakWatSensors?.currentState("water")?.value.contains("wet") ? false : true }
def leakWatSensorsOk() { return (!settings?.leakWatSensors) ? false : true }
def leakWatScheduleOk() { return autoScheduleOk(leakWatPrefix()) }

// TODO allow override from schedule?
def getLeakWatOnDelayVal() { return !settings?.leakWatOnDelay ? 300 : settings?.leakWatOnDelay.toInteger() }
def getLeakWatDryDtSec() { return !atomicState?.leakWatDryDt ? 100000 : GetTimeDiffSeconds(atomicState?.leakWatDryDt).toInteger() }

def leakWatCheck() {
	//log.trace "leakWatCheck..."
//
// TODO Should have some check for stuck contacts
//    if we cannot save/restore settings, don't bother turning things off
//
	def pName = leakWatPrefix()
	try {
		if(atomicState?.disableAutomation) { return }
		else {
			def leakWatTstat = settings?.schMotTstat
			def leakWatTstatMir = settings?.schMotTstatMir

			def execTime = now()
			//atomicState?.lastEvalDt = getDtNow()

			def curMode = leakWatTstat?.currentState("thermostatMode")?.value.toString()
			def curNestPres = getTstatPresence(leakWatTstat)
			def modeOff = (curMode == "off") ? true : false
			def wetCtDesc = getWetWaterSensors(leakWatSensors) ? " '${getWetWaterSensors(leakWatSensors)?.join(", ")}' " : " a selected leak sensor "
			def safetyOk = getSafetyTempsOk(leakWatTstat)
			def schedOk = leakWatScheduleOk()
			def allowNotif = settings?."${pName}NotificationsOn" ? true : false
			def allowSpeech = allowNotif && settings?."${pName}AllowSpeechNotif" ? true : false
			def allowAlarm = allowNotif && settings?."${pName}AllowAlarmNotif" ? true : false
			def speakOnRestore = allowSpeech && settings?."${pName}SpeechOnRestore" ? true : false

			if(!modeOff && atomicState.leakWatTstatOffRequested) {  // someone switched us on when we had turned things off, so reset timer and states
				LogAction("leakWatCheck() | System turned on when automation had OFF, resetting state to match", "warn", true)
				atomicState?.leakWatRestoreMode = null
				atomicState?.leakWatTstatOffRequested = false
			}

			def okToRestore = (modeOff && atomicState?.leakWatTstatOffRequested) ? true : false
			def sensorsOk = getLeakWatSensorsOk()

			if(sensorsOk || !safetyOk) {
				if(allowAlarm) { alarmEvtSchedCleanup(leakWatPrefix()) }
				def rmsg = ""

				if(okToRestore) {
					if(getLeakWatDryDtSec() >= (getLeakWatOnDelayVal() - 5) || !safetyOk) {
						def lastMode = null
						if(atomicState?.leakWatRestoreMode) { lastMode = atomicState?.leakWatRestoreMode }
						if(lastMode && (lastMode != curMode || !safetyOk)) {
							scheduleAutomationEval(60)
							if(setTstatMode(leakWatTstat, lastMode)) {
								storeLastAction("Restored Mode ($lastMode) to $leakWatTstat", getDtNow())
								atomicState?.leakWatTstatOffRequested = false
								atomicState?.leakWatRestoreMode = null
								atomicState?.leakWatRestoredDt = getDtNow()

								if(leakWatTstatMir) {
									if(setMultipleTstatMode(leakWatTstatMir, lastmode)) {
										LogAction("leakWatCheck: Mirroring Restoring Mode (${lastMode}) to ${tstat}", "info", true)
									}
								}
								rmsg = "Restoring '${leakWatTstat?.label}' to '${lastMode.toUpperCase()}' mode "
								def needAlarm = false
								if(!safetyOk) {
									rmsg += "because External Temp Safefy Temps have been reached..."
									needAlarm = true
								} else {
									rmsg += "because ALL leak sensors have been 'Dry' again for (${getEnumValue(longTimeSecEnum(), leakWatOnDelay)})..."
								}

								LogAction(rmsg, needAlarm ? "warn" : "info", true)
								if(allowNotif) {
									if(safetyOk) {
										sendEventPushNotifications(rmsg, "Info", pName) // this uses parent and honors quiet times, others do NOT
										if(speakOnRestore) { sendEventVoiceNotifications(voiceNotifString(atomicState?."${pName}OnVoiceMsg", pName), pName, "nmLeakWatOn_${app?.id}", true, "nmLeakWatOff_${app?.id}") }
									} else if(needAlarm) {
										sendEventPushNotifications(rmsg, "Warning", pName)
										if(allowAlarm) { scheduleAlarmOn(pName) }
									}
								}
								storeExecutionHistory((now() - execTime), "leakWatCheck")
								return

							} else { LogAction("leakWatCheck() | There was problem restoring the last mode to ${lastMode}...", "error", true) }
						} else {
							if(!safetyOk) {
								LogAction("leakWatCheck() | Unable to restore mode and safety temperatures are exceeded", "warn", true)
							} else {
								LogAction("leakWatCheck() | Skipping Restore because the Mode to Restore is same as Current Mode ${curMode}", "info", true)
							}
						}
					} else {
						if(safetyOk) {
							def remaining = getLeakWatOnDelayVal() - getLeakWatDryDtSec()
							LogAction("leakWatCheck: Delaying restore for wait period ${getLeakWatOnDelayVal()}, remaining ${remaining}", "info", true)
							remaining = remaining > 20 ? remaining : 20
							remaining = remaining < 60 ? remaining : 60
							scheduleAutomationEval(remaining)
						}
					}
				} else {
					if(modeOff) {
						if(!safetyOk) {
							LogAction("leakWatCheck() | Safety temps exceeded and Unable to restore settings okToRestore is false", "warn", true)
						}
						else if(!atomicState?.leakWatRestoreMode && atomicState?.leakWatTstatOffRequested) {
							LogAction("leakWatCheck() | Unable to restore settings because previous mode was not found. Likely due to other automation making changes.", "warn", true)
							atomicState?.leakWatTstatOffRequested = false
						}
					}
				}
			}

// tough decision here:  there is a leak, do we care about schedule ?
//		if(!getLeakWatSensorsOk() && safetyOk && schedOk) {
			if(!sensorsOk && safetyOk) {
				def rmsg = ""
				if(!modeOff) {
					atomicState?.leakWatRestoreMode = curMode
					LogAction("leakWatCheck: Saving ${leakWatTstat?.label} mode (${atomicState?.leakWatRestoreMode.toString().toUpperCase()}) for Restore later.", "info", true)
					LogAction("leakWatCheck: ${wetCtDesc}${getWetWaterSensors(leakWatSensors).size() > 1 ? "are" : "is"} Wet: Turning 'OFF' '${leakWatTstat?.label}'", "debug", true)
					scheduleAutomationEval(60)
					if(setTstatMode(leakWatTstat, "off")) {
						storeLastAction("Turned Off $leakWatTstat", getDtNow())
						atomicState?.leakWatTstatOffRequested = true
						atomicState?.leakWatDryDt = getDtNow()

						if(leakWatTstatMir) {
							if(setMultipleTstatMode(leakWatTstatMir, "off")) {
								LogAction("leakWatCheck: Mirroring (Off) Mode to ${tstat}", "info", true)
							}
						}
						rmsg = "${leakWatTstat.label} has been turned 'OFF' because${wetCtDesc}has reported it's WET..."
						LogAction(rmsg, "warn", true)
						if(allowNotif) {
							sendEventPushNotifications(rmsg, "Warning", pName) // this uses parent and honors quiet times, others do NOT
							if(allowSpeech) { sendEventVoiceNotifications(voiceNotifString(atomicState?."${pName}OffVoiceMsg",pName), pName, "nmLeakWatOff_${app?.id}", true, "nmLeakWatOn_${app?.id}") }
							if(allowAlarm) { scheduleAlarmOn(pName) }
						}
					} else { LogAction("leakWatCheck(): Error turning themostat Off", "warn", true) }
				} else {
					LogAction("leakWatCheck() | Skipping change because '${leakWatTstat?.label}' mode is already 'OFF'", "info", true)
				}
			} else {
				//if(!schedOk) { LogAction("leakWatCheck: Skipping because of Schedule Restrictions...", "warn", true) }
				if(!safetyOk) { LogAction("leakWatCheck: Skipping because of Safety Temps Exceeded...", "warn", true) }
				if(sensorsOk) { LogAction("leakWatCheck: Sensors are ok...", "info", true) }
			}
			storeExecutionHistory((now() - execTime), "leakWatCheck")
		}
	} catch (ex) {
		log.error "leakWatCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "leakWatCheck", true, getAutoType())
	}
}

def leakWatSensorEvt(evt) {
  LogAction("LeakWatch Sensor Event | '${evt?.displayName}' is now (${evt?.value.toString().toUpperCase()})", "trace", false)
   if(atomicState?.disableAutomation) {  return }
	else {
		def curMode = leakWatTstat?.currentThermostatMode.toString()
		def isModeOff = (curMode == "off") ? true : false
		def leakWet = (evt?.value == "wet") ? true : false

		def canSched = false
		def timeVal
		if(leakWet) {
			canSched = true
		}
		else if(!leakWet && getLeakWatSensorsOk()) {
			if(isModeOff) {
				atomicState?.leakWatDryDt = getDtNow()
				timeVal = ["valNum":getLeakWatOnDelayVal(), "valLabel":getEnumValue(longTimeSecEnum(), getLeakWatOnDelayVal())]
				canSched = true
			}
		}

		storeLastEventData(evt)
		if(canSched) {
			LogAction("leakWatSensorEvt: ${!evt ? "A monitored Leak Sensor is " : "'${evt?.displayName}' is "} '${evt?.value.toString().toUpperCase()}' | Leak Check scheduled for (${timeVal?.valLabel})...", "info", true)
			def val = timeVal?.valNum > 20 ? timeVal?.valNum : 20
			val = timeVal?.valNum < 60 ? timeVal?.valNum : 60
			scheduleAutomationEval(val)
		} else {
			LogAction("leakWatSensorEvt: Skipping Event...", "info", true)
		}
	}
}

/********************************************************************************
|					MODE AUTOMATION CODE	     						|
*********************************************************************************/
def nModePrefix() { return "nMode" }

def nestModePresPage() {
	def pName = nModePrefix()
	dynamicPage(name: "nestModePresPage", title: "Nest Mode - Nest Home/Away Automation", uninstall: false, install: false) {
		if(!nModePresSensor && !nModeSwitch) {
			def modeReq = (!nModePresSensor && (nModeHomeModes || nModeAwayModes))
			section("Set Nest Presence with ST Modes:") {
				input "nModeHomeModes", "mode", title: "Modes to Set Nest Location 'Home'", multiple: true, submitOnChange: true, required: modeReq,
						image: getAppImg("mode_home_icon.png")
				if(checkModeDuplication(nModeHomeModes, nModeAwayModes)) {
					paragraph "ERROR:\nDuplicate Mode(s) were found under both the Home and Away Modes!!!\nPlease Correct to Proceed...", required: true, state: null, image: getAppImg("error_icon.png")
				}
				input "nModeAwayModes", "mode", title: "Modes to Set Nest Location 'Away'", multiple: true, submitOnChange: true, required: modeReq,
						image: getAppImg("mode_away_icon.png")
				if(nModeHomeModes && nModeAwayModes) {
					def str = ""
					def pLocationPresence = getNestLocPres()
					str += location?.mode && plocationPresence ? "Location Status:" : ""
					str += location?.mode ? "\n ├ SmartThings Mode: ${location?.mode}" : ""
					str += plocationPresence ? "\n └ Nest Location: (${plocationPresence == "away" ? "Away" : "Home"})" : ""
					paragraph "${str}", state: (str != "" ? "complete" : null), image: getAppImg("instruct_icon.png")
				}
			}
		}
		if(!nModeHomeModes && !nModeAwayModes && !nModeSwitch) {
			section("(Optional) Set Nest Presence using Presence Sensor:") {
				//paragraph "Choose a Presence Sensor(s) to use to set your Nest to Home/Away", image: getAppImg("instruct_icon")
				def presDesc = nModePresenceDesc() ? "\n\n${nModePresenceDesc()}\n\nTap to Modify" : "Tap to Configure"
				input "nModePresSensor", "capability.presenceSensor", title: "Select a Presence Sensor", description: presDesc, multiple: true, submitOnChange: true, required: false,
						image: getAppImg("presence_icon.png")
				if(nModePresSensor) {
					if(nModePresSensor.size() > 1) {
						paragraph "Nest Location will be set to 'Away' when all Presence sensors leave and will return to 'Home' when someone arrives", title: "How this Works!", image: getAppImg("instruct_icon.png")
					}
					paragraph "${nModePresenceDesc()}", state: "complete", image: getAppImg("instruct_icon.png")
				}
			}
		}
		if(!nModePresSensor && !nModeHomeModes && !nModeAwayModes) {
			section("(Optional) Set Nest Presence based on the state of a Switch:") {
				input "nModeSwitch", "capability.switch", title: "Select a Switch", required: false, multiple: false, submitOnChange: true, image: getAppImg("switch_on_icon.png")
				if(nModeSwitch) {
					input "nModeSwitchOpt", "enum", title: "Switch State to Trigger 'Away'?", required: true, defaultValue: "On", options: ["On", "Off"], submitOnChange: true, image: getAppImg("settings_icon.png")
				}
			}
		}
		if((nModeHomeModes && nModeAwayModes) || nModePresSensor || nModeSwitch) {
			section("Delay Changes:") {
				input (name: "nModeDelay", type: "bool", title: "Delay Changes?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("delay_time_icon.png"))
				if(nModeDelay) {
					input "nModeDelayVal", "enum", title: "Delay before change?", required: false, defaultValue: 60, metadata: [values:longTimeSecEnum()],
							submitOnChange: true, image: getAppImg("configure_icon.png")
				}
				if(parent?.settings?.cameras) {
					input (name: "nModeCamOnAway", type: "bool", title: "Turn On Nest Cams when Away?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("camera_green_icon.png"))
					input (name: "nModeCamOffHome", type: "bool", title: "Turn Off Nest Cams when Home?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("camera_gray_icon.png"))
				}
			}
		}
		if(((nModeHomeModes && nModeAwayModes) && !nModePresSensor) || nModePresSensor) {
			section(getDmtSectionDesc(nModePrefix())) {
				def pageDesc = getDayModeTimeDesc(pName)
				href "setDayModeTimePage", title: "Configured Restrictions", description: pageDesc, params: ["pName": "${pName}"], state: (pageDesc ? "complete" : null),
						image: getAppImg("cal_filter_icon.png")
			}
			section("Notifications:") {
				def pageDesc = getNotifConfigDesc(pName)
				href "setNotificationPage", title: "Configured Alerts...", description: pageDesc, params: ["pName":"${pName}", "allowSpeech":false, "allowAlarm":false, "showSchedule":true],
						state: (pageDesc ? "complete" : null), image: getAppImg("notification_icon.png")
			}
		}
		if(atomicState?.showHelp) {
			section("Help:") {
				href url:"${getAutoHelpPageUrl()}", style:"embedded", required:false, title:"Help and Instructions...", description:"", image: getAppImg("info.png")
			}
		}
	}
}

def nModePresenceDesc() {
	if(settings?.nModePresSensor) {
		def cCnt = nModePresSensor?.size() ?: 0
		def str = ""
		def cnt = 0
		str += "Presence Status:"
		settings?.nModePresSensor?.each { dev ->
			cnt = cnt+1
			def presState = dev?.currentPresence ? dev?.currentPresence?.toString().capitalize() : "No State"
			str += "${(cnt >= 1) ? "${(cnt == cCnt) ? "\n└" : "\n├"}" : "\n└"} ${dev?.label}: ${(dev?.label.length() > 10) ? "\n${(cCnt == 1 || cnt == cCnt) ? "    " : "│"}└ " : ""}(${presState})"
		}
		return str
	}
	return null
}

def isNestModesConfigured() {
	def devOk = ((!nModePresSensor && !nModeSwitch && (nModeHomeModes && nModeAwayModes)) || (nModePresSensor && !nModeSwitch) || (!nModePresSensor && nModeSwitch)) ? true : false
	return devOk
}

def nModeSTModeEvt(evt) {
	LogAction("Event | ST Mode is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else if(!nModePresSensor && !nModeSwitch) {
		storeLastEventData(evt)
		if(nModeDelay) {
			def delay = nModeDelayVal.toInteger()

			if(delay > 20) {
				LogAction("Event | A Mode Check is scheduled for (${getEnumValue(longTimeSecEnum(), nModeDelayVal)})", "info", true)
				scheduleAutomationEval(delay)
			} else { scheduleAutomationEval() }
		} else {
			scheduleAutomationEval()
		}
	}
}

def nModePresEvt(evt) {
	LogAction("Event | Presence: ${evt?.displayName} - Presence is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else if(nModeDelay) {
		storeLastEventData(evt)
		def delay = nModeDelayVal.toInteger()

		if(delay > 20) {
			LogAction("Event | A Presence Check is scheduled for (${getEnumValue(longTimeSecEnum(), nModeDelayVal)})", "info", true)
			scheduleAutomationEval(delay)
		} else { scheduleAutomationEval() }
	} else {
		scheduleAutomationEval()
	}
}

def nModeSwitchEvt(evt) {
	LogAction("Event | Switch: ${evt?.displayName} - is (${evt?.value.toString().toUpperCase()})", "trace", true)
	if(atomicState?.disableAutomation) { return }
	else if(nModeSwitch && !nModePresSensor) {
		storeLastEventData(evt)
		if(nModeDelay) {
			def delay = nModeDelayVal.toInteger()
			if(delay > 20) {
				LogAction("Event | A Switch Check is scheduled for (${getEnumValue(longTimeSecEnum(), nModeDelayVal)})", "info", true)
				scheduleAutomationEval(delay)
			} else { scheduleAutomationEval() }
		} else {
			scheduleAutomationEval()
		}
	}
}

def nModeScheduleOk() { return autoScheduleOk(nModePrefix()) }

def checkNestMode() {
	LogAction("checkNestMode...", "trace", false)
//
// This automation only works with Nest as it toggles non-ST standard home/away
//
	def pName = nModePrefix()
	try {
		if(atomicState?.disableAutomation) { return }
		else if(!nModeScheduleOk()) {
			LogAction("checkNestMode: Skipping because of Schedule Restrictions...", "info", true)
			atomicState.lastStMode = null
			atomicState.lastPresSenAway = null
		} else {
			def execTime = now()
			//atomicState?.lastEvalDt = getDtNow()

			def curStMode = location?.mode
			def allowNotif = settings?."${nModePrefix()}NotificationsOn" ? true : false
			def nestModeAway = (getNestLocPres() == "home") ? false : true
			def awayPresDesc = (nModePresSensor && !nModeSwitch) ? "All Presence device(s) have left setting " : ""
			def homePresDesc = (nModePresSensor && !nModeSwitch) ? "A Presence Device is Now Present setting " : ""
			def awaySwitDesc = (nModeSwitch && !nModePresSensor) ? "${nModeSwitch} State is 'Away' setting " : ""
			def homeSwitDesc = (nModeSwitch && !nModePresSensor) ? "${nModeSwitch} State is 'Home' setting " : ""
			def modeDesc = ((!nModeSwitch && !nModePresSensor) && nModeHomeModes && nModeAwayModes) ? "The mode (${curStMode}) has triggered " : ""
			def awayDesc = "${awayPresDesc}${awaySwitDesc}${modeDesc}"
			def homeDesc = "${homePresDesc}${homeSwitDesc}${modeDesc}"

			def previousStMode = atomicState?.lastStMode
			def previousPresSenAway = atomicState?.lastPresSenAway

			def away = false
			def home = false

			if(nModePresSensor && !nModeSwitch) {
				if(!isPresenceHome(nModePresSensor)) {
					away = true
				} else {
					home = true
				}
			} else if(nModeSwitch && !nModePresSensor) {
				def swOptAwayOn = (nModeSwitchOpt == "On") ? true : false
				if(swOptAwayOn) {
					!isSwitchOn(nModeSwitch) ? (home = true) : (away = true)
				} else {
					!isSwitchOn(nModeSwitch) ? (away = true) : (home = true)
				}
			} else if(nModeHomeModes && nModeAwayModes) {
				if(isInMode(nModeHomeModes)) {
					home = true
				} else {
					if(isInMode(nModeAwayModes)) { away = true }
				}
			} else {
				LogAction("checkNestMode: Nothing Matched", "info", true)
			}

			def modeMatch = false   // these check that we only change once per ST or presence change
			if(nModeHomeModes && nModeAwayModes) {
				if(previousStMode == curStMode) {
					modeMatch = true
				}
			}
			if(nModePresSensor && !nModeSwitch) {
				if(previousPresSenAway != null && previousPresSenAway == away) {
					modeMatch = true
				}
			}

			LogAction("checkNestMode: isPresenceHome: (${nModePresSensor ? "${isPresenceHome(nModePresSensor)}" : "Presence Not Used"}) | ST-Mode: ($curStMode) | NestModeAway: ($nestModeAway) | Away?: ($away) | Home?: ($home) | modeMatch: ($modeMatch)", "info", true)

			if(away && !nestModeAway && !modeMatch) {
				LogAction("${awayDesc} Nest 'Away'", "info", true)
				if(parent?.setStructureAway(null, true)) {
					storeLastAction("Set Nest Location (Away)", getDtNow())
					atomicState?.nModeTstatLocAway = true
					atomicState.lastStMode = curStMode
					atomicState.lastPresSenAway = away
					if(allowNotif) {
						sendEventPushNotifications("${awayDesc} Nest 'Away'", "Info", pName)
					}
					if(nModeCamOnAway) {
						def cams = parent?.settings?.cameras
						cams?.each { cam ->
							def dev = getChildDevice(cam)
							if(dev) {
								//storeLastAction("Turned On Streaming for $cam", getDtNow())
								dev?.on()
								LogAction("checkNestMode: Turning Streaming On for (${dev}) because Location is now Away...", "info", true)
							}
						}
					}
				} else {
					LogAction("checkNestMode: There was an issue sending the AWAY command to Nest", "error", true)
				}
				scheduleAutomationEval(60)
			}
			else if(home && nestModeAway && !modeMatch) {
				LogAction("${homeDesc} Nest 'Home'", "info", true)
				if(parent?.setStructureAway(null, false)) {
					storeLastAction("Set Nest Location (Home)", getDtNow())
					atomicState?.nModeTstatLocAway = false
					atomicState.lastStMode = curStMode
					atomicState.lastPresSenAway = away
					if(allowNotif) {
						sendEventPushNotifications("${homeDesc} Nest 'Home'", "Info", pName)
					}
					if(nModeCamOffHome) {
						def cams = parent?.settings?.cameras
						cams?.each { cam ->
							def dev = getChildDevice(cam)
							if(dev) {
								dev?.off()
								LogAction("checkNestMode: Turning Streaming Off for (${dev}) because Location is now Home...", "info", true)
								//storeLastAction("Turned Streaming Off for $cam", getDtNow())
							}
						}
					}
				} else {
					LogAction("checkNestMode: There was an issue sending the AWAY command to Nest", "error", true)
				}
				scheduleAutomationEval(60)
			}
			else {
				LogAction("checkNestMode: Conditions are not valid to change mode | isPresenceHome: (${nModePresSensor ? "${isPresenceHome(nModePresSensor)}" : "Presence Not Used"}) | ST-Mode: ($curStMode) | NestModeAway: ($nestModeAway) | Away?: ($away) | Home?: ($home) | modeMatch: ($modeMatch)", "info", true)
			}
			storeExecutionHistory((now() - execTime), "checkNestMode")
		}
	} catch (ex) {
		log.error "checkNestMode Exception:", ex
		parent?.sendExceptionData(ex.message, "checkNestMode", true, getAutoType())
	}
}

def getNestLocPres() {
	if(atomicState?.disableAutomation) { return }
	else {
		def plocationPresence = parent?.locationPresence()
		if(!plocationPresence) { return null }
		else {
			return plocationPresence
		}
	}
}

/********************************************************************************
|		SCHEDULE, MODE, or MOTION CHANGES ADJUST THERMOSTAT SETPOINTS			|
|		(AND THERMOSTAT MODE) AUTOMATION CODE									|
*********************************************************************************/
def tModePrefix() { return "tMode" }

private tempRangeValues() {
	return (getTemperatureScale() == "C") ? "10..32" : "50..90"
}

private timeComparisonOptionValues() {
	return ["custom time", "midnight", "sunrise", "noon", "sunset"]
}

private timeDayOfWeekOptions() {
	return ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
}

private getDayOfWeekName(date = null) {
	if (!date) {
		date = adjustTime(now())
	}
	switch (date.day) {
		case 0: return "Sunday"
		case 1: return "Monday"
		case 2: return "Tuesday"
		case 3: return "Wednesday"
		case 4: return "Thursday"
		case 5: return "Friday"
		case 6: return "Saturday"
	}
	return null
}

private getDayOfWeekNumber(date = null) {
	if (!date) {
		date = adjustTime(now())
	}
	if (date instanceof Date) {
		return date.day
	}
	switch (date) {
		case "Sunday": return 0
		case "Monday": return 1
		case "Tuesday": return 2
		case "Wednesday": return 3
		case "Thursday": return 4
		case "Friday": return 5
		case "Saturday": return 6
	}
	return null
}

//adjusts the time to local timezone
private adjustTime(time = null) {
	if (time instanceof String) {
		//get UTC time
		time = timeToday(time, location.timeZone).getTime()
	}
	if (time instanceof Date) {
		//get unix time
		time = time.getTime()
	}
	if (!time) {
		time = now()
	}
	if (time) {
		return new Date(time + location.timeZone.getOffset(time))
	}
	return null
}

private formatLocalTime(time, format = "EEE, MMM d yyyy @ h:mm a z") {
	if (time instanceof Long) {
		time = new Date(time)
	}
	if (time instanceof String) {
		//get UTC time
		time = timeToday(time, location.timeZone)
	}
	if (!(time instanceof Date)) {
		return null
	}
	def formatter = new java.text.SimpleDateFormat(format)
	formatter.setTimeZone(location.timeZone)
	return formatter.format(time)
}

private convertDateToUnixTime(date) {
	if (!date) {
		return null
	}
	if (!(date instanceof Date)) {
		date = new Date(date)
	}
	return date.time - location.timeZone.getOffset(date.time)
}

private convertTimeToUnixTime(time) {
	if (!time) {
		return null
	}
	return time - location.timeZone.getOffset(time)
}

private formatTime(time, zone = null) {
	//we accept both a Date or a settings' Time
	return formatLocalTime(time, "h:mm a${zone ? " z" : ""}")
}

private formatHour(h) {
	return (h == 0 ? "midnight" : (h < 12 ? "${h} AM" : (h == 12 ? "noon" : "${h-12} PM"))).toString()
}

private cleanUpMap(map) {
	def washer = []
	//find dirty laundry
	for (item in map) {
		if (item.value == null) washer.push(item.key)
	}
	//clean it
	for (item in washer) {
		map.remove(item)
	}
	washer = null
	return map
}

private buildDeviceNameList(devices, suffix) {
	def cnt = 1
	def result = ""
	for (device in devices) {
		def label = getDeviceLabel(device)
		result += "$label" + (cnt < devices.size() ? (cnt == devices.size() - 1 ? " $suffix " : ", ") : "")
		cnt++
	}
	if(result == "") { result = null }
	return result
}

private getDeviceLabel(device) {
	return device instanceof String ? device : (device ? ( device.label ? device.label : (device.name ? device.name : "$device")) : "Unknown device")
}

def getCurrentSchedule() {
	def noSched = false
	def mySched

	def schedList = atomicState?.scheduleList
	def res1
	def ccnt = 1
	for (cnt in schedList) {
		res1 = checkRestriction(cnt)
		if(res1 == null) { break }
		ccnt += 1
	}
	if(ccnt > schedList?.size()) { noSched = true }
	else { mySched = ccnt }
	//LogAction("getCurrentSchedule:  mySched: $mySched  noSched: $noSched  ccnt: $ccnt res1: $res1", "trace", false)
	return mySched
}

private checkRestriction(cnt) {
//	LogAction("checkRestriction:( $cnt )...", "trace", false)
	def sLbl = "schMot_${cnt}_"

	def restriction

	def act = settings["${sLbl}SchedActive"]
	if(act) {
		def apprestrict = atomicState?."sched${cnt}restrictions"

		if (apprestrict?.m && apprestrict?.m.size() && !(location.mode in apprestrict?.m)) {
			restriction = "a mode mismatch"
		} else if (apprestrict?.w && apprestrict?.w.size() && !(getDayOfWeekName() in apprestrict?.w)) {
			restriction = "a day of week mismatch"
		} else if (apprestrict?.tf && apprestrict?.tt && !(checkTimeCondition(apprestrict?.tf, apprestrict?.tfc, apprestrict?.tfo, apprestrict?.tt, apprestrict?.ttc, apprestrict?.tto))) {
			restriction = "a time of day mismatch"
		} else {
			if (settings["${sLbl}restrictionSwitchOn"]) {
				for(sw in settings["${sLbl}restrictionSwitchOn"]) {
					if (sw.currentValue("switch") != "on") {
						restriction = "switch ${sw} being ${sw.currentValue("switch")}"
						break
					}
				}
			}
			if (!restriction && settings["${sLbl}restrictionSwitchOff"]) {
				for(sw in settings["${sLbl}restrictionSwitchOff"]) {
					if (sw.currentValue("switch") != "off") {
						restriction = "switch ${sw} being ${sw.currentValue("switch")}"
						break
					}
				}
			}
		}
	} else {
		restriction = "an inactive schedule"
	}
//	LogAction("checkRestriction:( $cnt ) restriction: $restriction", "trace", false)
	return restriction
}

def getActiveScheduleState() {
	return atomicState?.activeSchedData ?: null
}

def getSchRestrictDoWOk(cnt) {
	def apprestrict = atomicState?.activeSchedData
	def result = true
	apprestrict?.each { sch ->
		if(sch?.key.toInteger() == cnt.toInteger()) {
			if (!(getDayOfWeekName().toString() in sch?.value?.w)) {
				result = false
			}
		}
	}
	return result
}

private checkTimeCondition(timeFrom, timeFromCustom, timeFromOffset, timeTo, timeToCustom, timeToOffset) {
	def time = adjustTime()
	//convert to minutes since midnight
	def tc = time.hours * 60 + time.minutes
	def tf
	def tt
	def i = 0
	while (i < 2) {
		def t = null
		def h = null
		def m = null
		switch(i == 0 ? timeFrom : timeTo) {
			case "custom time":
				t = adjustTime(i == 0 ? timeFromCustom : timeToCustom)
				if (i == 0) {
					timeFromOffset = 0
				} else {
					timeToOffset = 0
				}
				break
			case "sunrise":
				t = getSunrise()
				break
			case "sunset":
				t = getSunset()
				break
			case "noon":
				h = 12
				break
			case "midnight":
				h = (i == 0 ? 0 : 24)
			break
		}
		if (h != null) {
			m = 0
		} else {
			h = t.hours
			m = t.minutes
		}
		switch (i) {
			case 0:
				tf = h * 60 + m + cast(timeFromOffset, "number")
				break
			case 1:
				tt = h * 60 + m + cast(timeFromOffset, "number")
				break
		}
		i += 1
	}
	//due to offsets, let's make sure all times are within 0-1440 minutes
	while (tf < 0) tf += 1440
	while (tf > 1440) tf -= 1440
	while (tt < 0) tt += 1440
	while (tt > 1440) tt -= 1440
	if (tf < tt) {
		return (tc >= tf) && (tc < tt)
	} else {
		return (tc < tt) || (tc >= tf)
	}
}

private cast(value, dataType) {
	def trueStrings = ["1", "on", "open", "locked", "active", "wet", "detected", "present", "occupied", "muted", "sleeping"]
	def falseStrings = ["0", "false", "off", "closed", "unlocked", "inactive", "dry", "clear", "not detected", "not present", "not occupied", "unmuted", "not sleeping"]
	switch (dataType) {
		case "string":
		case "text":
			if (value instanceof Boolean) {
				return value ? "true" : "false"
			}
			return value ? "$value" : ""
		case "number":
			if (value == null) return (int) 0
			if (value instanceof String) {
				if (value.isInteger())
					return value.toInteger()
				if (value.isFloat())
					return (int) Math.floor(value.toFloat())
				if (value in trueStrings)
					return (int) 1
			}
			def result = (int) 0
			try {
				result = (int) value
			} catch(all) {
				result = (int) 0
			}
			return result ? result : (int) 0
		case "long":
			if (value == null) return (long) 0
			if (value instanceof String) {
				if (value.isInteger())
					return (long) value.toInteger()
				if (value.isFloat())
					return (long) Math.round(value.toFloat())
				if (value in trueStrings)
					return (long) 1
			}
			def result = (long) 0
			try {
				result = (long) value
			} catch(all) {
			}
			return result ? result : (long) 0
		case "decimal":
			if (value == null) return (float) 0
			if (value instanceof String) {
				if (value.isFloat())
					return (float) value.toFloat()
				if (value.isInteger())
					return (float) value.toInteger()
				if (value in trueStrings)
					return (float) 1
			}
			def result = (float) 0
			try {
				result = (float) value
			} catch(all) {
			}
			return result ? result : (float) 0
		case "boolean":
			if (value instanceof String) {
				if (!value || (value in falseStrings))
					return false
				return true
			}
			return !!value
		case "time":
			return value instanceof String ? adjustTime(value).time : cast(value, "long")
		case "vector3":
			return value instanceof String ? adjustTime(value).time : cast(value, "long")
	}
	return value
}

//TODO is this expensive in ST?
private getSunrise() {
	def sunTimes = getSunriseAndSunset()
	return adjustTime(sunTimes.sunrise)
}

private getSunset() {
	def sunTimes = getSunriseAndSunset()
	return adjustTime(sunTimes.sunset)
}

def isTstatSchedConfigured() {
	return (settings?.schMotSetTstatTemp && atomicState?.activeSchedData?.size())
}

/*
def isTimeBetween(start, end, now, tz) {
	def startDt = Date.parse("E MMM dd HH:mm:ss z yyyy", start).getTime()
	def endDt = Date.parse("E MMM dd HH:mm:ss z yyyy", end).getTime()
	def nowDt = Date.parse("E MMM dd HH:mm:ss z yyyy", now).getTime()
	def result = false
	if(nowDt > startDt && nowDt < endDt) {
		result = true
	}
	//def result = timeOfDayIsBetween(startDt, endDt, nowDt, tz) ? true : false
	return result
}
*/

def checkOnMotion(mySched) {
	//log.trace "checkOnMotion($mySched)"
	def sLbl = "schMot_${mySched}_"

	def motionOn
	if(settings["${sLbl}Motion"] && atomicState?."${sLbl}MotionActiveDt") {
		motionOn = isMotionActive(settings["${sLbl}Motion"])

		def lastActiveMotionDt = Date.parse("E MMM dd HH:mm:ss z yyyy", atomicState?."${sLbl}MotionActiveDt").getTime()
		def lastActiveMotionSec = getLastMotionActiveSec(mySched)

		def lastInactiveMotionDt = lastActiveMotionDt
		def lastInactiveMotionSec = lastActiveMotionSec

		if(atomicState?."${sLbl}MotionInActiveDt") {
			lastInactiveMotionDt = Date.parse("E MMM dd HH:mm:ss z yyyy", atomicState?."${sLbl}MotionInActiveDt").getTime()
			lastInactiveMotionSec = getLastMotionInActiveSec(mySched)
		}

		LogAction("checkOnMotion: [ Active Dt: $lastActiveMotionDt ($lastActiveMotionSec sec.) | Inactive Dt: $lastInactiveMotionDt ($lastInactiveMotionSec sec.) | MotionOn: ($motionOn) ]", "trace", true)

		def ontimedelay = (settings."${sLbl}MDelayValOn"?.toInteger() ?: 15) * 1000 		// default to 15s
		def offtimedelay = (settings."${sLbl}MDelayValOff"?.toInteger() ?: 15*60) * 1000	// default to 15 min
		def ontime = formatDt( (lastActiveMotionDt + ontimedelay) )
		def offtime = formatDt( (lastInactiveMotionDt + offtimedelay) )

		if(ontime > offtime) { offtime = formatDt( (lastActiveMotionDt + ontimedelay + offtimedelay) ) }	

		LogAction("checkOnMotion: [ Active Dt: (${atomicState."${sLbl}MotionActiveDt"}) | OnTime: ($ontime) | Inactive Dt: (${atomicState?."${sLbl}MotionInActiveDt"}) | OffTime: ($offtime) ]", "info", true)
		def startDt = Date.parse("E MMM dd HH:mm:ss z yyyy", ontime).getTime()
		def endDt = Date.parse("E MMM dd HH:mm:ss z yyyy", offtime).getTime()
		def nowDt = Date.parse("E MMM dd HH:mm:ss z yyyy", getDtNow()).getTime()
		def result = false
		if(nowDt > startDt && nowDt < endDt) {
			result = true
		}
		if(nowDt < startDt || (result && !motionOn)) { 
			LogAction("checkOnMotion($mySched): scheduling motion check", "trace", true)
			scheduleAutomationEval(60)
		}
		return result
	}
	return false
}

def setTstatTempCheck() {
	LogAction("setTstatTempCheck...", "trace", false)
//
// This automation only works with Nest as it checks non-ST presence & thermostat capabilities
// Presumes:
//       all thermostats in an automation are in the same Nest structure, so that all react to home/away changes
//
	try {
		def tstat = settings?.schMotTstat
		def tstatMir = settings?.schMotTstatMir

		if(atomicState?.disableAutomation) { return }
		def execTime = now()

		def away = (getNestLocPres() == "home") ? false : true

		def mySched = getCurrentSchedule()
		def noSched = (mySched == null) ? true : false

		LogAction("setTstatTempCheck: [Current Schedule: ($mySched) | No Schedule: ($noSched)]", "trace", true)

		if(away || noSched) {
			if(away) {
				LogAction("setTstatTempCheck: Skipping check because [Nest is set AWAY]", "info", true)
				atomicState.lastSched = null
			} else {
				LogAction("setTstatTempCheck: Skipping check because [No matching Schedule]", "info", true)
				atomicState.lastSched = null
			}
		}
		else {
			def heatTemp = 0.0
			def coolTemp = 0.0

			def isBtwn = checkOnMotion(mySched)
			def previousBtwn = atomicState?."motion${mySched}InBtwn"
			atomicState?."motion${mySched}InBtwn" = isBtwn

			LogAction("setTstatTempCheck: isBtwn: $isBtwn ", "trace", true)

			def previousSched = atomicState?.lastSched
			def schedMatch
			if(previousSched == curSched && previousBtwn == isBtwn) {
				schedMatch = true
			}

			if(tstat && !schedMatch) {
				def hvacSettings = atomicState?."sched${mySched}restrictions"

				def newHvacMode = (!isBtwn ? hvacSettings?.hvacm : (hvacSettings?.mhvacm ?: hvacSettings?.hvacm))
				def tstatHvacMode = tstat?.currentThermostatMode?.toString()
				if(newHvacMode && (newHvacMode.toString() != tstatHvacMode)) {
					if(setTstatMode(schMotTstat, newHvacMode)) {
						storeLastAction("Set ${tstat} Mode to ${strCapitalize(newHvacMode)}", getDtNow())
						LogAction("setTstatTempCheck: Setting Thermostat Mode to '${strCapitalize(newHvacMode)}' on (${tstat})", "info", true)
					} else { LogAction("setTstatTempCheck: Error Setting Thermostat Mode to '${strCapitalize(newHvacMode)}' on (${tstat})", "warn", true) }
				}

				// if remote sensor is on, let it handle temp changes (above took care of a mode change)
				if(settings?.schMotRemoteSensor && isRemSenConfigured()) {
 					atomicState.lastSched = curSched
 					storeExecutionHistory((now() - execTime), "setTstatTempCheck")
 					return
 				}

				def curMode = tstat?.currentThermostatMode?.toString()
				def isModeOff = (curMode == "off") ? true : false
				tstatHvacMode = curMode

				heatTemp = null
				coolTemp = null

				if(!isModeOff && atomicState?.schMotTstatCanHeat) {
					// MY Heating Setpoint has not been set so it was null
					def oldHeat = getTstatSetpoint(tstat, "heat")
//ERSERS
					heatTemp = getRemSenHeatSetTemp()
/* TODO CLEANUP
					heatTemp = !isBtwn ? hvacSettings?.htemp.toDouble() : hvacSettings?.mhtemp.toDouble() ?: hvacSettings?.htemp.toDouble()
					def temp = 0.0
					if( getTemperatureScale() == "C") {
						temp = Math.round(heatTemp.round(1) * 2) / 2.0f
					} else {
						temp = Math.round(heatTemp.round(0)).toInteger()
					}
					heatTemp = temp
*/
					if(oldHeat != heatTemp) {
						LogAction("setTstatTempCheck Setting Heat Setpoint to '${heatTemp}' on (${tstat}) old: ${oldHeat}", "info", false)
						//storeLastAction("Set ${settings?.schMotTstat} Heat Setpoint to ${heatTemp}", getDtNow())
					} else { heatTemp = null }
				}

				if(!isModeOff && atomicState?.schMotTstatCanCool) {
					def oldCool = getTstatSetpoint(tstat, "cool")
//ERSERS
					coolTemp = getRemSenCoolSetTemp()
/*
					coolTemp = !isBtwn ? hvacSettings?.ctemp.toDouble() : hvacSettings?.mctemp.toDouble() ?: hvacSettings?.ctemp.toDouble()
					def temp = 0.0
					if( getTemperatureScale() == "C") {
						temp = Math.round(coolTemp.round(1) * 2) / 2.0f
					} else {
						temp = Math.round(coolTemp.round(0)).toInteger()
					}
					coolTemp = temp
*/
					if(oldCool != coolTemp) {
						LogAction("setTstatTempCheck: Setting Cool Setpoint to '${coolTemp}' on (${tstat}) old: ${oldCool}", "info", false)
						//storeLastAction("Set ${settings?.schMotTstat} Cool Setpoint to ${coolTemp}", getDtNow())
					} else { coolTemp = null }
				}
				if(setTstatAutoTemps(settings?.schMotTstat, coolTemp?.toDouble(), heatTemp?.toDouble())) {
					LogAction("setTstatTempCheck: [Temp Change | $modes | newHvacMode: $newHvacMode | tstatHvacMode: $tstatHvacMode | heatTemp: $heatTemp | coolTemp: $coolTemp | curStMode: $curStMode]", "info", true)
					storeLastAction("Set ${tstat} Cool Setpoint to ${coolTemp} Set Heat Setpoint to ${heatTemp}", getDtNow())
				} else {
					LogAction("setTstatTempCheck: [set ERROR | $modes | newHvacMode: $newHvacMode | tstatHvacMode: $tstatHvacMode | heatTemp: $heatTemp | coolTemp: $coolTemp | curStMode: $curStMode]", "info", true)
				}
			}
			atomicState.lastSched = curSched
		}
		storeExecutionHistory((now() - execTime), "setTstatTempCheck")
	} catch (ex) {
		log.error "setTstatTempCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "setTstatTempCheck", true, getAutoType())
	}
}

/********************************************************************************
|       				MASTER AUTOMATION FOR THERMOSTATS						|
*********************************************************************************/
def schMotPrefix() { return "schMot" }

def schMotModePage() {
	//def pName = schMotPrefix()
	dynamicPage(name: "schMotModePage", title: "Thermostat Automation", uninstall: false) {
		def dupTstat
		def dupTstat1
		def dupTstat2
		def dupTstat3
		def tStatPhys
		def tempScale = getTemperatureScale()
		def tempScaleStr = "°${tempScale}"
		section("Configure your Thermostat") {
			input name: "schMotTstat", type: "capability.thermostat", title: "Select your Thermostat?", multiple: false, submitOnChange: true, required: true, image: getAppImg("thermostat_icon.png")
			def tstat = settings?.schMotTstat
			def tstatMir = settings?.schMotTstatMir
			if(tstat) {
				getTstatCapabilities(tstat, schMotPrefix())
				def canHeat = atomicState?.schMotTstatCanHeat
				def canCool = atomicState?.schMotTstatCanCool
				tStatPhys = tstat?.currentNestType == "physical" ? true : false

				def str = ""
				def reqSenHeatSetPoint = getRemSenHeatSetTemp()
				def reqSenCoolSetPoint = getRemSenCoolSetTemp()
				def curZoneTemp = getRemoteSenTemp()
				def tempSrcStr = (getCurrentSchedule() && atomicState?.remoteTempSourceStr == "Schedule") ? "Schedule ${getCurrentSchedule()} (${"${getSchedLbl(getCurrentSchedule())}" ?: "Not Found"})" : "(${atomicState?.remoteTempSourceStr})"

				str += tempSrcStr ? "Zone Status:\n• Temp Source:${tempSrcStr?.toString().length() > 15 ? "\n  └" : ""} ${tempSrcStr}" : ""
				str += curZoneTemp ? "\n• Temperature: (${curZoneTemp}°${getTemperatureScale()})" : ""

				def hstr = canHeat ? "H: ${reqSenHeatSetPoint}°${getTemperatureScale()}" : ""
				def cstr = canHeat && canCool ? "/" : ""
				cstr += canCool ? "C: ${reqSenCoolSetPoint}°${getTemperatureScale()}" : ""
				str += "\n• Setpoints: (${hstr}${cstr})\n"

				str += "\nThermostat Status:\n• Temperature: (${getDeviceTemp(tstat)}${tempScaleStr})"
				hstr = canHeat ? "H: ${getTstatSetpoint(tstat, "heat")}${tempScaleStr}" : ""
				cstr = canHeat && canCool ? "/" : ""
				cstr += canCool ? "C: ${getTstatSetpoint(tstat, "cool")}${tempScaleStr}" : ""
				str += "\n• Setpoints: (${hstr}${cstr})"

				str += "\n• Mode: (${tstat ? ("${tstat?.currentThermostatOperatingState.toString().capitalize()}/${tstat?.currentThermostatMode.toString().capitalize()}") : "unknown"})"
				str += (atomicState?.schMotTstatHasFan) ? "\n• FanMode: (${tstat?.currentThermostatFanMode.toString().capitalize()})" : "\n• No Fan on HVAC system"
				str += "\n• Presence: (${getTstatPresence(tstat).toString().capitalize()})"
				def safetyTemps = getSafetyTemps(tstat)
			       	str +=  safetyTemps ? "\n• Safefy Temps: \n     └ Min: ${safetyTemps.min}°${getTemperatureScale()}/Max: ${safetyTemps.max}${tempScaleStr}" : ""
			       	str +=  "\n• Virtual: (${tstat?.currentNestType.toString() == "virtual" ? "True" : "False"})"
				paragraph "${str}", title: "${tstat.displayName} Zone Status", state: (str != "" ? "complete" : null), image: getAppImg("info_icon2.png")

				if(!tStatPhys) {      // if virtual thermostat, check if physical thermostat is in mirror list
					def mylist = [ deviceNetworkId:"${tstat.deviceNetworkId.toString().replaceFirst("v", "")}" ]
					dupTstat1 = checkThermostatDupe(mylist, tstatMir)
					if(dupTstat1) {
						paragraph "ERROR:\nThe Virtual version of the Primary Thermostat was found in Mirror Thermostat List!!!\nPlease Correct to Proceed...", required: true, state: null,  image: getAppImg("error_icon.png")
					}
				} else {	      // if physcial thermostat, see if virtual version is in mirror list
					def mylist = [ deviceNetworkId:"v${tstat.deviceNetworkId.toString()}" ]
					dupTstat2 = checkThermostatDupe(mylist, tstatMir)
					if(dupTstat2) {
						paragraph "ERROR:\nThe Virtual version of the Primary Thermostat was found in Mirror Thermostat List!!!\nPlease Correct to Proceed...", required: true, state: null,  image: getAppImg("error_icon.png")
					}
				}
				dupTstat3 = checkThermostatDupe(tstat, tstatMir)  // make sure thermostat is not in mirror list
				dupTstat = dupTstat1 || dupTstat2 || dupTstat3

				if(dupTstat) {
					paragraph "ERROR:\nThe Primary Thermostat was also found in the Mirror Thermostat List!!!\nPlease Correct to Proceed...", required: true, state: null, image: getAppImg("error_icon.png")
				}
				if(!tStatPhys) {
				}
				input "schMotTstatMir", "capability.thermostat", title: "Mirror Changes to these Thermostats", multiple: true, submitOnChange: true, required: false, image: getAppImg("thermostat_icon.png")
				if(tstatMir && !dupTstat) {
					tstatMir?.each { t ->
						paragraph "Thermostat Temp: ${getDeviceTemp(t)}${tempScaleStr}", image: " "
					}
				}
			}
		}

		if(settings?.schMotTstat && !dupTstat) {
			updateScheduleStateMap()
			section {
				paragraph "The options below allow you to configure your thermostat with automations that will help you save energy and keep your home feeling more comfortable", title: "Choose Automations:", required: false
			}

			section("Schedule Automation:") {
				input (name: "schMotSetTstatTemp", type: "bool", title: "Use Schedules to adjust Temp Setpoints and HVAC mode?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("schedule_icon.png"))
				if(settings?.schMotSetTstatTemp) {
					def actSch = atomicState?.activeSchedData?.size()
					//def curSchData = actSch ? atomicState?.activeSchedData?."$curSch" : null
					if (actSch) {
						def schInfo = getScheduleDesc()
						def curSch = getCurrentSchedule()
						if (schInfo?.size()) {
							schInfo?.each { schItem ->
								def schNum = schItem?.key
								def schDesc = schItem?.value
								def schInUse = (curSch?.toInteger() == schNum?.toInteger()) ? true : false
								if(schNum && schDesc) {
									href "schMotSchedulePage", title: "", description: "${schDesc}\n\nTap to Modify Schedule...", params: ["sNum":schNum], state: (schInUse ? "complete" : "")
								}
							}
						}
					}
					def tDesc = isTstatSchedConfigured() ? "Tap to Add/Modify Schedules..." : null
					href "tstatConfigAutoPage", title: "Configure Setpoint Schedules...", description: (tDesc != null ? tDesc : "None Configured..."), params: ["configType":"tstatSch"], state: (tDesc != null ? "complete" : null), required: true, image: getAppImg("configure_icon.png")
				}
			}

			section("Fan Control:") {
				def desc = ""
				def titStr = "Run External Fan while HVAC is Operating"
				if(atomicState?.schMotTstatHasFan) { titStr +=  " or Use HVAC Fan for Circulation" }
				input (name: "schMotOperateFan", type: "bool", title: "${titStr}?", description: desc, required: false, defaultValue: false, submitOnChange: true, image: getAppImg("fan_control_icon.png"))
				if(settings?.schMotOperateFan) {
					def fanCtrlDescStr = ""
					//fanCtrlDescStr += (atomicState?.schMotTstatHasFan) ? "\n • Current Fan Mode: (${schMotTstat?.currentThermostatFanMode.toString().capitalize()})" : ""
					fanCtrlDescStr += getFanSwitchDesc() ? "${getFanSwitchDesc()}" : ""
					def fanCtrlDesc = isFanCtrlConfigured() ? "${fanCtrlDescStr}\n\nTap to Modify..." : null
					href "tstatConfigAutoPage", title: "Fan Control Config...", description: fanCtrlDesc ?: "Not Configured...", params: ["configType":"fanCtrl"], state: (fanCtrlDesc ? "complete" : null),
							required: true, image: getAppImg("configure_icon.png")
				}
			}

			section("Remote Sensor:") {
				def desc = ""
				input (name: "schMotRemoteSensor", type: "bool", title: "Use Alternate Temp Sensors Control Zone temperature?", description: desc, required: false, defaultValue: false, submitOnChange: true,
						image: getAppImg("remote_sensor_icon.png"))
				if(settings?.schMotRemoteSensor) {
					def remSenDescStr = ""
					remSenDescStr += settings?.remSenRuleType ? "Rule-Type: ${getEnumValue(remSenRuleEnum(), settings?.remSenRuleType)}" : ""
					remSenDescStr += settings?.remSenTempDiffDegrees ? ("\n • Threshold: (${settings?.remSenTempDiffDegrees}${tempScaleStr}") : ""
					remSenDescStr += settings?.remSenTstatTempChgVal ? ("\n • Adjust Temp: (${settings?.remSenTstatTempChgVal}${tempScaleStr})") : ""

					def hstr = remSenHeatTempsReq() ? "H: ${settings?.remSenDayHeatTemp ?: 0}${tempScaleStr}" : ""
					def cstr = remSenHeatTempsReq() && remSenCoolTempsReq() ? "/" : ""
					cstr += remSenCoolTempsReq() ? "C: ${settings?.remSenDayCoolTemp ?: 0}${tempScaleStr}" : ""
					remSenDescStr += (settings?.remSensorDay && (settings?.remSenDayHeatTemp || settings?.remSenDayCoolTemp)) ? "\n • Default Temps:\n   └ (${hstr}${cstr})" : ""


					remSenDescStr += (settings?.vthermostat) ? "\n\nVirtual Thermostat:" : ""
					remSenDescStr += (settings?.vthermostat) ? "\n• Enabled" : ""

					//remote sensor/Day
					def dayModeDesc = ""
					dayModeDesc += settings?.remSensorDay ? "\n\nDefault Sensor${settings?.remSensorDay?.size() > 1 ? "s" : ""}:" : ""
					//dayModeDesc += settings?.remSensorDay ? "\n ${settings.remSensorDay}" : ""
					def rCnt = settings?.remSensorDay?.size()
					settings?.remSensorDay?.each { t ->
						dayModeDesc += "\n ├ ${t?.label}: ${(t?.label.length() > 10) ? "\n │ └ " : ""}(${getDeviceTemp(t)}${tempScaleStr})"
					}
					dayModeDesc += settings?.remSensorDay ? "\n └ Temp${(settings?.remSensorDay?.size() > 1) ? " (avg):" : ":"} (${getDeviceTempAvg(settings?.remSensorDay)}${tempScaleStr})" : ""
					//dayModeDesc += settings?.remSensorDay ? "\n • Temp${(settings?.remSensorDay?.size() > 1) ? " (avg):" : ":"} (${getDeviceTempAvg(settings?.remSensorDay)}${tempScaleStr})" : ""
					remSenDescStr += settings?.remSensorDay ? "${dayModeDesc}" : ""

					def remSenDesc = isRemSenConfigured() ? "${remSenDescStr}\n\nTap to Modify..." : null
					href "tstatConfigAutoPage", title: "Remote Sensor Config", description: remSenDesc ?: "Not Configured...", params: ["configType":"remSen"], required: true, state: (remSenDesc ? "complete" : null),
							image: getAppImg("configure_icon.png")
				}
			}

			section("Leak Detection:") {
				def desc = ""
				input (name: "schMotWaterOff", type: "bool", title: "Turn Off if Water Leak is detected?", description: desc, required: false, defaultValue: false, submitOnChange: true, image: getAppImg("leak_icon.png"))
				if(settings?.schMotWaterOff) {
					def leakDesc = ""
					leakDesc += (settings?.leakWatSensors && leakWatSensorsDesc()) ? "${leakWatSensorsDesc()}" : ""
					leakDesc += (settings?.leakWatSensors) ? "\n\nSettings:" : ""
					leakDesc += settings?.leakWatOnDelay ? "\n • On Delay: (${getEnumValue(longTimeSecEnum(), settings?.leakWatOnDelay)})" : ""
					leakDesc += "\n • Last Mode: (${atomicState?.leakWatRestoreMode ? atomicState?.leakWatRestoreMode.toString().capitalize() : "Not Set"})"
					leakDesc += (settings?.leakWatModes || settings?.leakWatDays || (settings?.leakWatStartTime && settings?.leakWatStopTime)) ?
						"\n • Evaluation Allowed: (${autoScheduleOk(leakWatPrefix()) ? "ON" : "OFF"})" : ""
					leakDesc += getNotifConfigDesc(leakWatPrefix()) ? "\n\n${getNotifConfigDesc(leakWatPrefix())}" : ""
					leakDesc += (settings?.leakWatSensors) ? "\n\nTap to Modify..." : ""
					def leakWatDesc = isLeakWatConfigured() ? "${leakDesc}" : null
					href "tstatConfigAutoPage", title: "Leak Sensors Config...", description: leakWatDesc ?: "Not Configured...", params: ["configType":"leakWat"], required: true, state: (leakWatDesc ? "complete" : null),
							image: getAppImg("configure_icon.png")
				}
			}

			section("Contact Automation:") {
				def desc = ""
				input (name: "schMotContactOff", type: "bool", title: "Turn Off if Door/Window Contact Open?", description: desc, required: false, defaultValue: false, submitOnChange: true, image: getAppImg("open_window.png"))
				if(settings?.schMotContactOff) {
					def conDesc = ""
					conDesc += (settings?.conWatContacts && conWatContactDesc()) ? "${conWatContactDesc()}" : ""
					conDesc += settings?.conWatContacts ? "\n\nSettings:" : ""
					conDesc += settings?.conWatOffDelay ? "\n • Off Delay: (${getEnumValue(longTimeSecEnum(), settings?.conWatOffDelay)})" : ""
					conDesc += settings?.conWatOnDelay ? "\n • On Delay: (${getEnumValue(longTimeSecEnum(), settings?.conWatOnDelay)})" : ""
					conDesc += settings?.conWatRestoreDelayBetween ? "\n • Delay Between Restores:\n     └ (${getEnumValue(longTimeSecEnum(), settings?.conWatRestoreDelayBetween)})" : ""
					conDesc += "\n • Last Mode: (${atomicState?.conWatRestoreMode ? atomicState?.conWatRestoreMode.toString().capitalize() : "Not Set"})"
					conDesc += (settings?."${conWatPrefix()}Modes" || settings?."${conWatPrefix()}Days" || (settings?."${conWatPrefix()}StartTime" && settings?."${conWatPrefix()}StopTime")) ?
						"\n • Evaluation Allowed: (${autoScheduleOk(conWatPrefix()) ? "ON" : "OFF"})" : ""
					conDesc += getNotifConfigDesc(conWatPrefix()) ? "\n\n${getNotifConfigDesc(conWatPrefix())}" : ""
					conDesc += (settings?.conWatContacts) ? "\n\nTap to Modify..." : ""
					def conWatDesc = isConWatConfigured() ? "${conDesc}" : null
					href "tstatConfigAutoPage", title: "Contact Sensors Config...", description: conWatDesc ?: "Not Configured...", params: ["configType":"conWat"], required: true, state: (conWatDesc ? "complete" : null),
							image: getAppImg("configure_icon.png")
				}
			}

			section("External Temp:") {
				def desc = ""
				input (name: "schMotExternalTempOff", type: "bool", title: "Turn Off if External Temperature is near Comfort Settings?", description: desc, required: false, defaultValue: false, submitOnChange: true, image: getAppImg("external_temp_icon.png"))
				if(settings?.schMotExternalTempOff) {
					def extDesc = ""
					extDesc += (settings?.extTmpUseWeather || settings?.extTmpTempSensor) ? "Settings:" : ""
					extDesc += (!settings?.extTmpUseWeather && settings?.extTmpTempSensor) ? "\n • Sensor: (${getExtTmpTemperature()}${tempScaleStr})" : ""
					extDesc += (settings?.extTmpUseWeather && !settings?.extTmpTempSensor) ? "\n • Weather: (${getExtTmpTemperature()}${tempScaleStr})" : ""
					//TODO need this in schedule
					extDesc += settings?.extTmpDiffVal ? "\n • Threshold: (${settings?.extTmpDiffVal}${tempScaleStr})" : ""
					extDesc += settings?.extTmpOffDelay ? "\n • Off Delay: (${getEnumValue(longTimeSecEnum(), settings?.extTmpOffDelay)})" : ""
					extDesc += settings?.extTmpOnDelay ? "\n • On Delay: (${getEnumValue(longTimeSecEnum(), settings?.extTmpOnDelay)})" : ""
					extDesc += "\n • Last Mode: (${atomicState?.extTmpRestoreMode ? atomicState?.extTmpRestoreMode.toString().capitalize() : "Not Set"})"
					extDesc += (settings?."${extTmpPrefix()}Modes" || settings?."${extTmpPrefix()}Days" || (settings?."${extTmpPrefix()}StartTime" && settings?."${extTmpPrefix()}StopTime")) ?
						"\n • Evaluation Allowed: (${autoScheduleOk(extTmpPrefix()) ? "ON" : "OFF"})" : ""
					extDesc += getNotifConfigDesc(extTmpPrefix()) ? "\n\n${getNotifConfigDesc(extTmpPrefix())}" : ""
					extDesc += ((settings?.extTmpTempSensor || settings?.extTmpUseWeather) ) ? "\n\nTap to Modify..." : ""
					def extTmpDesc = isExtTmpConfigured() ? "${extDesc}" : null
					href "tstatConfigAutoPage", title: "External Temps Config...", description: extTmpDesc ?: "Not Configured...", params: ["configType":"extTmp"], required: true, state: (extTmpDesc ? "complete" : null),
							image: getAppImg("configure_icon.png")
				}
			}

			section("Settings:") {
				input "schMotWaitVal", "enum", title: "Minimum Wait Time between Evaluations?", required: false, defaultValue: 60, metadata: [values:[30:"30 Seconds", 60:"60 Seconds"]], image: getAppImg("delay_time_icon.png")
			}
		}
		if(atomicState?.showHelp) {
			section("Help:") {
				href url:"${getAutoHelpPageUrl()}", style:"embedded", required:false, title:"Help and Instructions...", description:"", image: getAppImg("info.png")
			}
		}
	}
}

def getSchedLbl(num) {
	def result = ""
	if(num) {
		def schData = atomicState?.activeSchedData
		schData?.each { sch ->
			if(num?.toInteger() == sch?.key.toInteger()) {
				log.debug "Label:(${sch?.value?.lbl})"
				result = sch?.value?.lbl
			}
		}
	}
	return result
}

def tstatConfigAutoPage(params) {
	def configType = params.configType
	if(params?.configType) {
		atomicState.tempTstatConfigPageData = params; configType = params?.configType;
	} else { configType = atomicState?.tempTstatConfigPageData?.configType }
	def pName = ""
	def pTitle = ""
	def pDesc = null
	switch(configType) {
		case "tstatSch":
			pName = schMotPrefix()
			pTitle = "Thermostat Schedule Automation"
			pDesc = "Configure Schedules and Setpoints"
			break
		case "fanCtrl":
			pName = fanCtrlPrefix()
			break
		case "remSen":
			pName = remSenPrefix()
			pTitle = "Remote Sensor Automation"
			break
		case "leakWat":
			pName = leakWatPrefix()
			pTitle = "Thermostat/Leak Automation"
			break
		case "conWat":
			pName = conWatPrefix()
			pTitle = "Thermostat/Contact Automation"
			break
		case "extTmp":
			pName = extTmpPrefix()
			pTitle = "Thermostat/External Temps Automation"
			break
	}
	dynamicPage(name: "tstatConfigAutoPage", title: pTitle, description: pDesc, uninstall: false) {
		def tstat = settings?.schMotTstat
		if (tstat) {
			def tempScale = getTemperatureScale()
			def tempScaleStr = "°${tempScale}"
			def tStatName = tstat?.displayName.toString()
			def tStatHeatSp = getTstatSetpoint(tstat, "heat")
			def tStatCoolSp = getTstatSetpoint(tstat, "cool")
			def tStatMode = tstat?.currentThermostatMode
			def tStatTemp = "${getDeviceTemp(tstat)}${tempScaleStr}"
			def canHeat = atomicState?.schMotTstatCanHeat
			def canCool = atomicState?.schMotTstatCanCool
			def locMode = location?.mode

			// RULE - You ALWAYS HAVE TEMPS in A SCHEDULE
			// RULE - You ALWAYS OFFER OPTION OF MOTION TEMPS in A SCHEDULE
			// RULE - if MOTION is ENABLED, it MUST HAVE MOTION TEMPS
			// RULE - you ALWAYS OFFER RESTRICTION OPTIONS in A SCHEDULE
			// RULE - if REMSEN is ON, you offer remote sensors options

			def hidestr = null
			hidestr = ["fanCtrl"]   // fan schedule is turned off
			if(!settings?.schMotRemoteSensor) { // no remote sensors requested or used
				hidestr = ["fanCtrl", "remSen"]
			}
			if(!settings?.schMotOperateFan) {

			}
			if(!settings?.schMotSetTstatTemp) {   //motSen means no motion sensors offered   restrict means no restrictions offered  tstatTemp says no tstat temps offered
				//"tstatTemp", "motSen" "restrict"
			}
			if(!settings?.schMotExternalTempOff) {
			}

			if(configType == "tstatSch") {
				section {
					def str = ""
					str += "• Temperature: (${tStatTemp})"
					str += "\n• Setpoints: (H: ${canHeat ? "${tStatHeatSp}${tempScaleStr}" : "NA"}/C: ${canCool ? "${tStatCoolSp}${tempScaleStr}" : "NA"})"
					paragraph title: "${tStatName}\nSchedules and Setpoints:", "${str}", state: "complete", image: getAppImg("info_icon2.png")
				}
				showUpdateSchedule(null, hidestr)
			}

			if(configType == "fanCtrl") {
				def reqinp = !(settings["schMotCirculateTstatFan"] || settings["${pName}FanSwitches"])
				section("Control Fans/Switches based on your Thermostat\n(3-Speed Fans Supported)") {
					input "${pName}FanSwitches", "capability.switch", title: "Select Fan Switches?", required: reqinp, submitOnChange: true, multiple: true,
							image: getAppImg("fan_ventilation_icon.png")
					if(settings?."${pName}FanSwitches") {
						paragraph "${getFanSwitchDesc(false)}", state: getFanSwitchDesc() ? "complete" : null, image: getAppImg("blank_icon.png")
					}
				}
				if(settings["${pName}FanSwitches"]) {
					section("Fan Event Triggers") {
						paragraph "Event based triggers occur when the Thermostat sends an event.  Depending on your configured Poll time it may take 1 minute or more",
								image: getAppImg("instruct_icon.png")
						input "${pName}FanSwitchTriggerType", "enum", title: "Control Switches When?", defaultValue: 1, metadata: [values:switchRunEnum()],
							submitOnChange: true, image: getAppImg("${settings?."${pName}FanSwitchTriggerType" == 1 ? "thermostat" : "home_fan"}_icon.png")
						input "${pName}FanSwitchHvacModeFilter", "enum", title: "Thermostat Mode Triggers?", defaultValue: "any", metadata: [values:fanModeTrigEnum()],
								submitOnChange: true, image: getAppImg("mode_icon.png")
					}
					if(getFanSwitchesSpdChk()) {
						section("Fan Speed Options") {
							input("${pName}FanSwitchSpeedCtrl", "bool", title: "Enable Speed Control?", defaultValue: true, submitOnChange: true, image: getAppImg("speed_knob_icon.png"))
							if(settings["${pName}FanSwitchSpeedCtrl"]) {
								paragraph "These Threshold settings allow you to configure the speed of the fan based on it's closeness to the desired temp", title: "What do these mean?"
								input "${pName}FanSwitchLowSpeed", "decimal", title: "Low Speed Threshold (${tempScaleStr})", required: true, defaultValue: 1.0, submitOnChange: true, image: getAppImg("fan_low_speed.png")
								input "${pName}FanSwitchMedSpeed", "decimal", title: "Medium Speed Threshold (${tempScaleStr})", required: true, defaultValue: 2.0, submitOnChange: true, image: getAppImg("fan_med_speed.png")
								input "${pName}FanSwitchHighSpeed", "decimal", title: "High Speed Threshold (${tempScaleStr})", required: true, defaultValue: 4.0, submitOnChange: true, image: getAppImg("fan_high_speed.png")
							}
						}
					}
				}

				if(atomicState?.schMotTstatHasFan) {
					section("Fan Circulation:") {
						def desc = ""
						input (name: "schMotCirculateTstatFan", type: "bool", title: "Run HVAC Fan for Circulation?", description: desc, required: reqinp, defaultValue: false, submitOnChange: true, image: getAppImg("fan_circulation_icon.png"))
						if(settings?.schMotCirculateTstatFan) {
							input("schMotFanRuleType", "enum", title: "(Rule) Action Type", options: remSenRuleEnum(true), required: true, image: getAppImg("rule_icon.png"))
						}
					}
				}
				def schTitle
				if(!atomicState?.activeSchedData?.size()) {
					schTitle = "Optionally create schedules to set temperatures based on schedule..."
				} else {
					schTitle = "Temperature settings based on schedule..."
				}
				section("${schTitle}") { // FANS USE TEMPS IN LOGIC
					href "scheduleConfigPage", title: "Modify Schedule Settings...", description: pageDesc, params: ["sData":["hideStr":"${hideStr}"]], state: (pageDesc ? "complete" : null), image: getAppImg("schedule_icon.png")
			 	}
			}

			def cannotLock
			def defHeat
			def defCool
			if(!getMyLockId()) {
				setMyLockId(app.id)
			}
			if(atomicState?.remSenTstat) {
				if(tstat.deviceNetworkId != atomicState?.remSenTstat) {
					parent?.addRemoveVthermostat(atomicState.remSenTstat, false, getMyLockId())
					if( parent?.remSenUnlock(atomicState.remSenTstat, getMyLockId()) ) { // attempt unlock old ID
						atomicState.oldremSenTstat = atomicState?.remSenTstat
						atomicState?.remSenTstat = null
					}
				}
			}
			if(settings?.schMotRemoteSensor) {
				if( parent?.remSenLock(tstat?.deviceNetworkId, getMyLockId()) ) {  // lock new ID
					atomicState?.remSenTstat = tstat?.deviceNetworkId
					cannotLock = false
				} else { cannotLock = true }
			}

			if(configType == "remSen") {
				//   can check if any vthermostat is owned by us, and delete it
				//   have issue request for vthermostat is still on as input below

				if(cannotLock) {
					section("") {
						paragraph "Cannot Lock thermostat for remote sensor - thermostat may already be in use.  Please Correct...", image: getAppImg("error_icon.png")
					}
				}

				if(!cannotLock) {
					section("Select the Allowed (Rule) Action Type:") {
						if(!settings?.remSenRuleType) {
							paragraph "(Rule) Actions determine actions taken when the temperature threshold is reached, to balance" +
									" temperatures...", image: getAppImg("instruct_icon.png")
						}
						input(name: "remSenRuleType", type: "enum", title: "(Rule) Action Type", options: remSenRuleEnum(), required: true, submitOnChange: true, image: getAppImg("rule_icon.png"))
					}
					if(settings?.remSenRuleType) {
						def senLblStr = "Default"
						section("Choose Sensor(s) to use instead of the Thermostat's...") {
							def daySenReq = (!settings?.remSensorDay) ? true : false
							input "remSensorDay", "capability.temperatureMeasurement", title: "${senLblStr} Temp Sensor(s)", submitOnChange: true, required: daySenReq,
									multiple: true, image: getAppImg("temperature_icon.png")
							if(settings?.remSensorDay) {
								def tmpVal = "Temp${(settings?.remSensorDay?.size() > 1) ? " (avg):" : ":"} (${getDeviceTempAvg(settings?.remSensorDay)}${tempScaleStr})"
								if(settings?.remSensorDay.size() > 1) {
									href "remSenShowTempsPage", title: "View ${senLblStr} Sensor Temps...", description: "${tmpVal}", state: "complete", image: getAppImg("blank_icon.png")
									//paragraph "Multiple temp sensors will return the average of those sensors.", image: getAppImg("i_icon.png")
								} else { paragraph "${tmpVal}", state: "complete", image: getAppImg("instruct_icon.png") }
							}
						}
						if(settings?.remSensorDay) {
							section("Desired Setpoints...") {
								paragraph "These temps are used when remote sensors are enabled and no schedules are created or active", title: "What are these temps for?", image: getAppImg("info_icon2.png")
								def tempStr = "Default "
								if(remSenHeatTempsReq()) {
									defHeat = getGlobalDesiredHeatTemp()
									defHeat = defHeat ?: tStatHeatSp
									input "remSenDayHeatTemp", "decimal", title: "Desired ${tempStr}Heat Temp (${tempScaleStr})", description: "Range within ${tempRangeValues()}", range: tempRangeValues(),
											required: true, defaultValue: defHeat, image: getAppImg("heat_icon.png")
								}
								if(remSenCoolTempsReq()) {
									defCool = getGlobalDesiredCoolTemp()
									defCool = defCool ?: tStatCoolSp
									input "remSenDayCoolTemp", "decimal", title: "Desired ${tempStr}Cool Temp (${tempScaleStr})", description: "Range within ${tempRangeValues()}", range: tempRangeValues(),
											required: true, defaultValue: defCool, image: getAppImg("cool_icon.png")
								}
							}
							section("Remote Sensor Settings...") {
								paragraph "Action Threshold Temp:\nIs the temp difference trigger for Action Type.", image: getAppImg("instruct_icon.png")
								input "remSenTempDiffDegrees", "decimal", title: "Action Threshold Temp (${tempScaleStr})", required: true, defaultValue: 2.0, image: getAppImg("temp_icon.png")
								if(settings?.remSenRuleType != "Circ") {
									paragraph "Temp Increments:\nIs the amount the thermostat temp is adjusted +/- to enable the HVAC system.", image: getAppImg("instruct_icon.png")
									input "remSenTstatTempChgVal", "decimal", title: "Change Temp Increments (${tempScaleStr})", required: true, defaultValue: 5.0, image: getAppImg("temp_icon.png")
								}
							}

							section("(Optional) Create a Virtual Nest Thermostat:") {
								input(name: "vthermostat", type: "bool", title:"Create Virtual Nest Thermostat", required: false, submitOnChange: true, image: getAppImg("thermostat_icon.png"))
								if(settings?.vthermostat != null  && !parent?.addRemoveVthermostat(tstat.deviceNetworkId, vthermostat, getMyLockId())) {
									paragraph "Unable to ${(vthermostat ? "enable" : "disable")} Virtual Thermostat!!!.  Please Correct...", image: getAppImg("error_icon.png")
								}
							}

							def schTitle
							if(!atomicState?.activeSchedData?.size()) {
								schTitle = "Optionally create schedules to set temperatures, alternate sensors based on schedule..."
							} else {
								schTitle = "Temperature settings and optionally alternate sensors based on schedule..."
							}
							section("${schTitle}") {
								href "scheduleConfigPage", title: "Modify Schedule Settings...", description: pageDesc, params: ["sData":["hideStr":"${hideStr}"]], state: (pageDesc ? "complete" : null), image: getAppImg("schedule_icon.png")
							}
						}
					}
				}
			}

			if(configType == "leakWat") {
				section("When Leak is Detected, Turn Off this Thermostat") {
					def req = (settings?.leakWatSensors || setting?.schMotTstat) ? true : false
					input name: "leakWatSensors", type: "capability.waterSensor", title: "Which Leak Sensor(s)?", multiple: true, submitOnChange: true, required: req,
							image: getAppImg("water_icon.png")
					if(settings?.leakWatSensors) {
						paragraph "${leakWatSensorsDesc()}", state: "complete", image: getAppImg("instruct_icon.png")
					}
				}
				if(settings?.leakWatSensors) {
					section("Restore On when Dry:") {
						input name: "leakWatOnDelay", type: "enum", title: "Delay Restore (in minutes)", defaultValue: 300, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
					}
					section("Notifications:") {
						def pageDesc = getNotifConfigDesc(pName)
						href "setNotificationPage", title: "Configured Alerts...", description: pageDesc, params: ["pName":"${pName}", "allowSpeech":true, "allowAlarm":true, "showSchedule":true],
								state: (pageDesc ? "complete" : null), image: getAppImg("notification_icon.png")
					}
				}
			}

			if(configType == "conWat") {
				section("When these Contacts are open, Turn Off this Thermostat") {
					def req = !settings?.conWatContacts ? true : false
					input name: "conWatContacts", type: "capability.contactSensor", title: "Which Contact(s)?", multiple: true, submitOnChange: true, required: req,
							image: getAppImg("contact_icon.png")
					if(settings?.conWatContacts) {
						def str = ""
						str += settings?.conWatContacts ? "${conWatContactDesc()}\n" : ""
						paragraph "${str}", state: (str != "" ? "complete" : null), image: getAppImg("instruct_icon.png")
					}
				}
				if(settings?.conWatContacts) {
					section("Trigger Actions:") {
						// TODO can these delays be set to 0?
						input name: "conWatOffDelay", type: "enum", title: "Delay Off (in Minutes)", defaultValue: 300, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")

						input name: "conWatOnDelay", type: "enum", title: "Delay Restore (in Minutes)", defaultValue: 300, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
						input name: "conWatRestoreDelayBetween", type: "enum", title: "Delay Between On/Off Cycles\n(Optional)", defaultValue: 600, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
					}
					section("Restoration Preferences (Optional):") {
						// TODO can these delays be set to 0? to turn back off?
						input "${pName}OffTimeout", "enum", title: "Auto Restore after...", defaultValue: 3600, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
						if(!settings?."${pName}OffTimeout") { atomicState."${pName}timeOutScheduled" = false }
					}

					section(getDmtSectionDesc(conWatPrefix())) {
						def pageDesc = getDayModeTimeDesc(pName)
						href "setDayModeTimePage", title: "Configured Restrictions", description: pageDesc, params: ["pName": "${pName}"], state: (pageDesc ? "complete" : null),
								image: getAppImg("cal_filter_icon.png")
					}
					section("Notifications:") {
						def pageDesc = getNotifConfigDesc(pName)
						href "setNotificationPage", title: "Configured Alerts...", description: pageDesc, params: ["pName":"${pName}", "allowSpeech":true, "allowAlarm":true, "showSchedule":true],
								state: (pageDesc ? "complete" : null), image: getAppImg("notification_icon.png")
					}
				}
			}

			if(configType == "extTmp") {
				section("Select the External Temps to Use:") {
					if(!parent?.getWeatherDeviceInst()) {
						paragraph "Please Enable the Weather Device under the Manager App before trying to use External Weather as an External Sensor!!!", required: true, state: null
					} else {
						if(!settings?.extTmpTempSensor) {
							input "extTmpUseWeather", "bool", title: "Use Local Weather as External Sensor?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("weather_icon.png")
							if(settings?.extTmpUseWeather){
								if(atomicState?.curWeather == null) {
									atomicState.NeedwUpd = true
									getExtConditions()
								}
								def tmpVal = (tempScale == "C") ? atomicState?.curWeatherTemp_c : atomicState?.curWeatherTemp_f
								def curDp = getExtTmpDewPoint()
								paragraph "Local Weather:\n• ${atomicState?.curWeatherLoc}\n• Temp: (${tmpVal}${tempScaleStr})\n• Dewpoint: (${curDp}${tempScaleStr})", state: "complete", image: getAppImg("instruct_icon.png")
							}
						}
					}
					if(!settings?.extTmpUseWeather) {
						atomicState.curWeather = null  // force refresh of weather if toggled
						def senReq = (!settings?.extTmpUseWeather && !settings?.extTmpTempSensor) ? true : false
						input "extTmpTempSensor", "capability.temperatureMeasurement", title: "Select a Temp Sensor?", submitOnChange: true, multiple: false, required: senReq, image: getAppImg("temperature_icon.png")
						if(settings?.extTmpTempSensor) {
							def str = ""
							str += settings?.extTmpTempSensor ? "Sensor Status:" : ""
							str += settings?.extTmpTempSensor ? "\n└ Temp: (${settings?.extTmpTempSensor?.currentTemperature}${tempScaleStr})" : ""
							paragraph "${str}", state: (str != "" ? "complete" : null), image: getAppImg("instruct_icon.png")
						}
					}
				}
				if(settings?.extTmpUseWeather || settings?.extTmpTempSensor) {
					section("When the threshold Temp is Reached\nTurn Off the Thermostat...") {
						input name: "extTmpDiffVal", type: "decimal", title: "When difference between the thermostat and external temp is this many degrees (${tempScaleStr})?", defaultValue: 1.0, submitOnChange: true, required: true,
								image: getAppImg("temp_icon.png")
					}
					section("Delay Values:") {
						// TODO can these delays be set to 0?
						input name: "extTmpOffDelay", type: "enum", title: "Delay Off (in minutes)", defaultValue: 300, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
						input name: "extTmpOnDelay", type: "enum", title: "Delay Restore (in minutes)", defaultValue: 300, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
					}
					section("Restoration Preferences (Optional):") {
						// TODO can these delays be set to 0? to turn back off?
						input "${pName}OffTimeout", "enum", title: "Auto Restore after (Optional)", defaultValue: 43200, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true,
								image: getAppImg("delay_time_icon.png")
						if(!settings?."${pName}OffTimeout") { atomicState."${pName}timeOutScheduled" = false }
					}
					section(getDmtSectionDesc(extTmpPrefix())) {
						def pageDesc = getDayModeTimeDesc(pName)
						href "setDayModeTimePage", title: "Configured Restrictions", description: pageDesc, params: ["pName": "${pName}"], state: (pageDesc ? "complete" : null),
								image: getAppImg("cal_filter_icon.png")
					}
					section("Notifications:") {
						def pageDesc = getNotifConfigDesc(pName)
						href "setNotificationPage", title: "Configured Alerts...", description: pageDesc, params: ["pName":"${pName}", "allowSpeech":true, "allowAlarm":true, "showSchedule":true],
								state: (pageDesc ? "complete" : null), image: getAppImg("notification_icon.png")
					}
					def schTitle
					if(!atomicState?.activeSchedData?.size()) {
						schTitle = "Optionally create schedules to set temperatures based on schedule..."
					} else {
						schTitle = "Temperature settings based on schedule..."
					}
					section("${schTitle}") { // EXTERNAL TEMPERATURE has TEMP Setting
						href "scheduleConfigPage", title: "Modify Schedule Settings...", description: pageDesc, params: ["sData":["hideStr":"${hideStr}"]], state: (pageDesc ? "complete" : null), image: getAppImg("schedule_icon.png")
					}
				}
			}
		}
	}
}

def getScheduleTimeDesc(timeFrom, timeFromCustom, timeFromOffset, timeTo, timeToCustom, timeToOffset, showPreLine = false) {
	def tf = new SimpleDateFormat("h:mm a")
    	tf.setTimeZone(location?.timeZone)
	def spl = showPreLine ? " │" : ""
	def timeToVal = null
	def timeFromVal = null
	def i = 0
	if(timeFrom && timeTo) {
		while (i < 2) {
			switch(i == 0 ? timeFrom : timeTo) {
				case "custom time":
					if(i == 0) { timeFromVal = tf.format(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", timeFromCustom)) }
					else { timeToVal = tf.format(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", timeToCustom)) }
					break
				case "sunrise":
					def sunTime = ((timeFromOffset > 0 || timeToOffset > 0) ? getSunriseAndSunset(zipCode: location.zipCode, sunriseOffset: "00:${i == 0 ? timeFromOffset : timeToOffset}") : getSunriseAndSunset(zipCode: location.zipCode))
					if(i == 0) { timeFromVal = "Sunrise: (" + tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", sunTime?.sunrise.toString())) + ")" }
					else { timeToVal = "Sunrise: (" + tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", sunTime?.sunrise.toString())) + ")" }
					break
				case "sunset":
					def sunTime = ((timeFromOffset > 0 || timeToOffset > 0) ? getSunriseAndSunset(zipCode: location.zipCode, sunriseOffset: "00:${i == 0 ? timeFromOffset : timeToOffset}") : getSunriseAndSunset(zipCode: location.zipCode))
					if(i == 0) { timeFromVal = "Sunset: (" + tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", sunTime?.sunset.toString())) + ")" }
					else { timeToVal = "Sunset: (" + tf.format(Date.parse("E MMM dd HH:mm:ss z yyyy", sunTime?.sunset.toString())) + ")" }
					break
				case "noon":
					def rightNow = adjustTime().time
					def offSet = (timeFromOffset != null || timeToOffset != null) ? (i == 0 ? (timeFromOffset * 60 * 1000) : (timeToOffset * 60 * 1000)) : 0
					def res = "Noon: " + formatTime(convertDateToUnixTime((rightNow - rightNow.mod(86400000) + 43200000) + offSet))
					if(i == 0) { timeFromVal = res }
					else { timeToVal = res }
					break
				case "midnight":
					def rightNow = adjustTime().time
					def offSet = (timeFromOffset != null || timeToOffset != null) ? (i == 0 ? (timeFromOffset * 60 * 1000) : (timeToOffset * 60 * 1000)) : 0
					def res = "Midnight: " + formatTime(convertDateToUnixTime((rightNow - rightNow.mod(86400000)) + offSet))
					if(i == 0) { timeFromVal = res }
					else { timeToVal = res }
				break
			}
			i += 1
		}
	}
	def timeOk = (checkTimeCondition(timeFrom, timeFromCustom, timeFromOffset, timeTo, timeToCustom, timeToOffset)) ? true : false
	def out = ""
	out += ((timeFromVal.length() + timeToVal.length()) > 15) ? "Time:${timeOk ? " (OK)" : " (NOT OK)"}\n${spl} │ ├ $timeFromVal\n${spl} │ ├   to\n${spl} │ └ $timeToVal" : "Time: $timeFromVal to $timeToVal"
	return out
}

def scheduleConfigPage(params) {
	LogTrace("scheduleConfigPage... ($params)")
	def sData = params?.sData
	if(params?.sData) {
		atomicState.tempSchPageData = params
		sData = params?.sData
	} else {
		sData = atomicState?.tempSchPageData?.sData
	}
	dynamicPage(name: "scheduleConfigPage", title: "Thermostat Schedule Page", description: "Configure/View Schedules", uninstall: false) {
		if(settings?.schMotTstat) {
			def tstat = settings?.schMotTstat
			def canHeat = atomicState?.schMotTstatCanHeat
			def canCool = atomicState?.schMotTstatCanCool
			def str = ""
			def reqSenHeatSetPoint = getRemSenHeatSetTemp()
			def reqSenCoolSetPoint = getRemSenCoolSetTemp()
			def curZoneTemp = getRemoteSenTemp()
			def tempSrcStr = atomicState?.remoteTempSourceStr
			section {
				str += "Zone Status:\n• Temp Source: (${tempSrcStr})\n• Temperature: (${curZoneTemp}°${getTemperatureScale()})"

				def hstr = canHeat ? "H: ${reqSenHeatSetPoint}°${getTemperatureScale()}" : ""
				def cstr = canHeat && canCool ? "/" : ""
				cstr += canCool ? "C: ${reqSenCoolSetPoint}°${getTemperatureScale()}" : ""
				str += "\n• Setpoints: (${hstr}${cstr})\n"

				str += "\nThermostat Status:\n• Temperature: (${getDeviceTemp(tstat)}°${getTemperatureScale()})"
				hstr = canHeat ? "H: ${getTstatSetpoint(tstat, "heat")}°${getTemperatureScale()}" : ""
				cstr = canHeat && canCool ? "/" : ""
				cstr += canCool ? "C: ${getTstatSetpoint(tstat, "cool")}°${getTemperatureScale()}" : ""
				str += "\n• Setpoints: (${hstr}${cstr})"

				str += "\n• Mode: (${tstat ? ("${tstat?.currentThermostatOperatingState.toString().capitalize()}/${tstat?.currentThermostatMode.toString().capitalize()}") : "unknown"})"
				str += (atomicState?.schMotTstatHasFan) ? "\n• FanMode: (${tstat?.currentThermostatFanMode.toString().capitalize()})" : "\n• No Fan on HVAC system"
				str += "\n• Presence: (${getTstatPresence(tstat).toString().capitalize()})"
				paragraph title: "${tstat?.displayName}\nSchedules and Setpoints:", "${str}", state: "complete", image: getAppImg("info_icon2.png")
			}
			showUpdateSchedule(null,sData?.hideStr)
		}
	}
}

def schMotSchedulePage(params) {
	LogTrace("schMotSchedulePage($params)")
	def sNum = params?.sNum
	if(params?.sNum) {
		atomicState.tempMotSchPageData = params
		sNum = params?.sNum
	} else {
		sNum = atomicState?.tempMotSchPageData?.sNum
	}
	dynamicPage(name: "schMotSchedulePage", title: "Edit Schedule Page", description: "Modify Schedules", uninstall: false) {
		if(sNum) {
			showUpdateSchedule(sNum)
		}
	}
}

def showUpdateSchedule(sNum=null,hideStr=null) {
	updateScheduleStateMap()
	def schedList = atomicState?.scheduleList  // setting in initAutoApp adjust # of schedule slots
	if(schedList == null) { atomicState.scheduleList = [ 1,2,3,4 ]; schedList = atomicState?.scheduleList }
	def lact
	def act = 1
	def sLbl
	def cnt = 1
	schedList?.each { scd ->
		sLbl = "schMot_${scd}_"
		if(sNum != null) {
			if(sNum?.toInteger() == scd?.toInteger()) {
				lact = act
				act = settings["${sLbl}SchedActive"]
				def scdn =  settings["${sLbl}name"]
				def mstr = scdn? "${scdn} "+ (act ? "Enabled":"Disabled") : "Tap to Enable"
				def titleStr = "Schedule ${scd} (${mstr})"
				editSchedule("secData":["scd":scd, "titleStr":titleStr, "hideable":(sNum ? false : true), "hidden":((act || scd == 1) ? false : true), "hideStr":hideStr])
			}
		} else {
			lact = act
			act = settings["${sLbl}SchedActive"]
			if (lact || act) {
				def scdn =  settings["${sLbl}name"]
				def mstr = scdn ? (act ? "Enabled": "Disabled") : "Tap to Enable"
				def titleStr = "Schedule ${scd} (${mstr})"
				editSchedule("secData":["scd":scd, "titleStr":titleStr, "hideable":true, "hidden":((act || scd == 1) ? false : true), "hideStr":hideStr])
			}
		}
	}
}

def editSchedule(schedData) {
	def cnt = schedData?.secData?.scd
	LogAction("editSchedule (${schedData?.secData})", "trace", false)

	def sLbl = "schMot_${cnt}_"
	def canHeat = atomicState?.schMotTstatCanHeat
	def canCool = atomicState?.schMotTstatCanCool
	def tempScaleStr = "°${getTemperatureScale()}"
	def act = settings["${sLbl}SchedActive"]
	def actIcon = act ? "active" : "inactive"

	section(title: "\n\n${schedData?.secData?.titleStr}                                                 ", hideable:schedData?.secData?.hideable, hidden: schedData?.secData?.hidden) {

		// RULE - YOU ALWAYS HAVE TEMPS in A SCHEDULE
		// RULE - you ALWAYS OFFER OPTION OF MOTION TEMPS in A SCHEDULE
		// RULE - if MOTION is ENABLED, it MUST HAVE MOTION TEMPS
		// RULE - you ALWAYS OFFER RESTRICTION OPTIONS in A SCHEDULE
		// RULE - if REMSEN is ON, you offer remote sensors options

		input "${sLbl}SchedActive", "bool", title: "Schedule Enabled", description: (cnt == 1 && !settings?."${sLbl}SchedActive" ? "Enable to Edit Schedule..." : null), required: true,
				defaultValue: false, submitOnChange: true, image: getAppImg("${actIcon}_icon.png")
		if(act) {
			input "${sLbl}name", "text", title: "Schedule Name", required: true, defaultValue: "Schedule ${cnt}", multiple: false, image: getAppImg("name_tag_icon.png")
		}
	}
	if(act) {
		//if(settings?.schMotSetTstatTemp && !("tstatTemp" in hideStr)) {
		section("Setpoint Configuration:") {
			paragraph "Configure Setpoints and HVAC modes that will be set when this Schedule is in use...", title: "Setpoints and Mode"
			if(canHeat) {
				input "${sLbl}HeatTemp", "decimal", title: "Heat Set Point(${tempScaleStr})", description: "Range within ${tempRangeValues()}", required: true, range: tempRangeValues(), image: getAppImg("heat_icon.png")
			}
			if(canCool) {
				input "${sLbl}CoolTemp", "decimal", title: "Cool Set Point (${tempScaleStr})", description: "Range within ${tempRangeValues()}", required: true, range: tempRangeValues(), image: getAppImg("cool_icon.png")
			}
			input "${sLbl}HvacMode", "enum", title: "Set Hvac Mode:", required: false, description: "No change set", metadata: [values:tModeHvacEnum(canHeat,canCool)], multiple: false, image: getAppImg("hvac_mode_icon.png")
		}

		if(settings?.schMotRemoteSensor && !("remSen" in hideStr)) {
			section("Remote Sensor Options:") {
				paragraph "Configure alternate Remote Temp sensors that are active with this schedule...", title: "Alternate Remote Sensors\n(Optional)"
				input "${sLbl}remSensor", "capability.temperatureMeasurement", title: "Alternate Temp Sensors", description: "For Remote Sensor Automation", submitOnChange: true, required: false, multiple: true, image: getAppImg("temperature_icon.png")
				if(settings?."${sLbl}remSensor" != null) {
					def tmpVal = "Temp${(settings["${sLbl}remSensor"]?.size() > 1) ? " (avg):" : ":"} (${getDeviceTempAvg(settings["${sLbl}remSensor"])}${tempScaleStr})"
					paragraph "${tmpVal}", state: "complete", image: getAppImg("instruct_icon.png")
				}
			}
		}
		//if(!("motSen" in hideStr)) {
		section("Motion Sensor Setpoints:") {
			paragraph "Set alternate setpoint temps based on Motion...", title: "Motion Sensors (Optional)"
			def mmot = settings["${sLbl}Motion"]
			input "${sLbl}Motion", "capability.motionSensor", title: "Motion Sensors", description: "Enables alternate hvac settings based on motion", required: false, multiple: true, submitOnChange: true, image: getAppImg("motion_icon.png")
			if(settings["${sLbl}Motion"]) {
				paragraph " • Motion State: (${isMotionActive(mmot) ? "Active" : "Not Active"})", state: "complete", image: getAppImg("instruct_icon.png")
				if(canHeat) {
					input "${sLbl}MHeatTemp", "decimal", title: "Heat Setpoint with Motion(${tempScaleStr})", description: "Range within ${tempRangeValues()}", required: true, range: tempRangeValues(), image: getAppImg("heat_icon.png")
				}
				if(canCool) {
					input "${sLbl}MCoolTemp", "decimal", title: "Cool Setpoint with Motion (${tempScaleStr})", description: "Range within ${tempRangeValues()}", required: true, range: tempRangeValues(), image: getAppImg("cool_icon.png")
				}
				input "${sLbl}MHvacMode", "enum", title: "Set Hvac Mode with Motion:", required: false, description: "No change set", metadata: [values:tModeHvacEnum(canHeat,canCool)], multiple: false, image: getAppImg("hvac_mode_icon.png")
				//input "${sLbl}MRestrictionMode", "mode", title: "Ignore in these modes", description: "Any location mode", required: false, multiple: true, image: getAppImg("mode_icon.png")
				input "${sLbl}MDelayValOn", "enum", title: "Delay Motion Setting Changes", required: false, defaultValue: 60, metadata: [values:longTimeSecEnum()], multiple: false, image: getAppImg("delay_time_icon.png")
				input "${sLbl}MDelayValOff", "enum", title: "Delay disabling Motion Settings", required: false, defaultValue: 1800, metadata: [values:longTimeSecEnum()], multiple: false, image: getAppImg("delay_time_icon.png")
			}
		}
/*
		if(settings?.schMotOperateFan && !("fanCtrl" in hideStr)) {
			paragraph null, title: "\nConfigure Fans that run only when Schedule is Active..."
			def sFans = settings["${sLbl}Fans"]
			input "${sLbl}Fans", "capability.switch", title: "Alternate Fans", description: "Use Alternate Fans with this schedule", required: false, multiple: true, submitOnChange: true, image: getAppImg("fan_ventilation_icon.png")
			if(sFans) {
				paragraph "${getSchFanSwitchDesc(sFans)}", state: "complete", image: getAppImg("instruct_icon.png")
				input ("${sLbl}FansUseTemp", "bool", title: "Only When in Temp Range?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("temperature_icon.png"))
				if(settings["${sLbl}FansUseTemp"]) {
					input "${sLbl}FansLowTemp", "decimal", title: "Low Temp (${tempScaleStr})", description: "Range within ${tempRangeValues()}", required: settings["${sLbl}FansHighTemp"], range: tempRangeValues(), image: getAppImg("cool_icon.png")
					if(settings["${sLbl}FansLowTemp"] && settings["${sLbl}FansHighTemp"]) {
						if(settings["${sLbl}FansLowTemp"].toDouble() >= settings["${sLbl}FansHighTemp"].toDouble()) {
							paragraph "ERROR:\nThe Low Temp is greater than or equal to the high temp!!!\nPlease Correct to Proceed...", required: true, state: null,  image: getAppImg("error_icon.png")
						}
					}
					input "${sLbl}FansHighTemp", "decimal", title: "High Temp (${tempScaleStr})", description: "Range within ${tempRangeValues()}", required: settings["${sLbl}FansLowTemp"], range: tempRangeValues(), image: getAppImg("heat_icon.png")
				}
				input ("${sLbl}FansMotion", "bool", title: "Only Run these fans when Motion after Motion was detected?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("motion_icon.png"))
				if(settings["${sLbl}FansMotion"]) {
					input "${sLbl}FansMDelayValOn", "enum", title: "Delay After Motion before turning On", required: false, defaultValue: 60, metadata: [values:longTimeSecEnum()], multiple: false, image: getAppImg("delay_time_icon.png")
					input "${sLbl}FansMDelayValOff", "enum", title: "Delay After Motion before turning Off", required: false, defaultValue: 1800, metadata: [values:longTimeSecEnum()], multiple: false, image: getAppImg("delay_time_icon.png")
				}
			}
		}
*/
		//if(!("restrict" in hideStr)) {
		section("Schedule Restrictions:") {
			paragraph "Restrict when this Schedule is in use...", title: "Evaluation Restrictions (Optional)"
			input "${sLbl}restrictionMode", "mode", title: "Only execute in these modes", description: "Any location mode", required: false, multiple: true, image: getAppImg("mode_icon.png")
			input "${sLbl}restrictionDOW", "enum", options: timeDayOfWeekOptions(), title: "Only execute on these days", description: "Any week day", required: false, multiple: true, image: getAppImg("day_calendar_icon2.png")
			def timeFrom = settings["${sLbl}restrictionTimeFrom"]
			input "${sLbl}restrictionTimeFrom", "enum", title: (timeFrom ? "Only execute if time is between" : "Only execute during this time"), options: timeComparisonOptionValues(), required: false, multiple: false, submitOnChange: true, image: getAppImg("start_time_icon.png")
			if (timeFrom) {
				if (timeFrom.contains("custom")) {
					input "${sLbl}restrictionTimeFromCustom", "time", title: "Custom time", required: true, multiple: false
				} else {
					input "${sLbl}restrictionTimeFromOffset", "number", title: "Offset (+/- minutes)", range: "*..*", required: true, multiple: false, defaultValue: 0, image: getAppImg("offset_icon.png")
				}
				def timeTo = settings["${sLbl}restrictionTimeTo"]
				input "${sLbl}restrictionTimeTo", "enum", title: "And", options: timeComparisonOptionValues(), required: true, multiple: false, submitOnChange: true, image: getAppImg("stop_time_icon.png")
				if (timeTo && (timeTo.contains("custom"))) {
					input "${sLbl}restrictionTimeToCustom", "time", title: "Custom time", required: true, multiple: false
				} else {
					input "${sLbl}restrictionTimeToOffset", "number", title: "Offset (+/- minutes)", range: "*..*", required: true, multiple: false, defaultValue: 0, image: getAppImg("offset_icon.png")
				}
			}
			input "${sLbl}restrictionSwitchOn", "capability.switch", title: "Only execute when these switches are all on", description: "Always", required: false, multiple: true, image: getAppImg("switch_on_icon.png")
			input "${sLbl}restrictionSwitchOff", "capability.switch", title: "Only execute when these switches are all off", description: "Always", required: false, multiple: true, image: getAppImg("switch_off_icon.png")
		}
	}
}

def getScheduleDesc(num = null) {
	def result = [:]
	def schedData = atomicState?.activeSchedData
	def actSchedNum = getCurrentSchedule()
	def tempScaleStr = "°${getTemperatureScale()}"
	def schNum
	def schData

	def sCnt = 1
	schedData?.sort().each { scd ->
		def str = ""
		schNum = scd?.key
		schData = scd?.value
		def sLbl = "schMot_${schNum}_"
		def isRestrict = (schData?.m || schData?.tf || schData?.tfc || schData?.tfo || schData?.tt || schData?.ttc || schData?.tto || schData?.w || schData?.s1 || schData?.s0)
		def isTimeRes = (schData?.tf || schData?.tfc || schData?.tfo || schData?.tt || schData?.ttc || schData?.tto)
		def isDayRes = schData?.w
		def isTemp = (schData?.ctemp || schData?.htemp || schData?.hvacm)
		def isSw = (schData?.s1 || schData?.s0)
		def isMot = schData?.m0
		def isRemSen = schData?.sen0
		def isFanEn = schData?.fan0
		def showPreBar = isSw || isTemp || isMot || isRemSen


		str += schData?.lbl ? " • ${schData?.lbl}${(actSchedNum?.toInteger() == schNum?.toInteger()) ? " (In Use)" : " (Not In Use)"}" : ""

		//restriction section
		str += isRestrict ? "\n ${isSw || isTemp ? "├" : "└"} Restrictions:" : ""
		def mLen = schData?.m ? schData?.m?.toString().length() : 0
		def mStr = ""
		if (mLen > 15) {
			def mdSize = 1
			schData?.m?.each { md ->
				mStr += md ? "\n ${isSw || isTemp ? "│ ${(isDayRes || isTimeRes || isSw) ? "│" : "    "}" : "   "} ${mdSize < schData?.m.size() ? "├" : "└"} ${md.toString()}" : ""
				mdSize = mdSize+1
			}
		} else {
			mStr += schData?.m.toString()
		}
		str += schData?.m ? "\n ${isSw || isTemp ? "│" : "   "} ${(isTimeRes || schData?.w) ? "├" : "└"} Mode${schData?.m?.size() > 1 ? "s" : ""}:${isInMode(schData?.m) ? " (OK)" : " (NOT OK)"}" : ""
		str += schData?.m ? "\n ${isSw || isTemp ? "│" : "   "} │ └ $mStr" : ""

		def dayStr = getAbrevDay(schData?.w)
		str += isTimeRes ? 		"\n ${isSw || isTemp ? "│" : " "} ${schData?.w ? "├" : "└"} ${getScheduleTimeDesc(schData?.tf, schData?.tfc, schData?.tfo, schData?.tt, schData?.ttc, schData?.tto, (isSw || isTemp))}" : ""
		str += schData?.w ?  	"\n ${isSw || isTemp ? "│" : " "} ${schData?.s1 ? "├" : "└"} Days:${getSchRestrictDoWOk(schNum) ? " (OK)" : " (NOT OK)"}" : ""
		str += schData?.w ?		"\n ${isSw || isTemp ? "│" : " "} ${isSw ? "│" :"    "} └ ${dayStr}" : ""
		str += schData?.s1 ?	"\n ${isSw || isTemp ? "│" : " "} ${schData?.s0 ? "├" : "└"} Switches On:${isSwitchOn(settings["${sLbl}restrictionSwitchOn"]) ? " (OK)" : " (NOT OK)"}" : ""
		str += schData?.s1 ? 	"\n ${isSw || isTemp ? "│" : " "} ${schData?.s0 ? "│" : "    "} └ (${schData?.s1.size()} Selected)" : ""
		str += schData?.s0 ?	"\n ${isSw || isTemp ? "│" : " "} └ Switches Off:${!isSwitchOn(settings["${sLbl}restrictionSwitchOff"]) ? " (OK)" : " (NOT OK)"}" : ""
		str += schData?.s0 ? 	"\n ${isSw || isTemp ? "│" : " "}      └ (${schData?.s0.size()} Selected)" : ""

		//Temp Setpoints
		str += isTemp  ? 		"${isRestrict ? "\n │\n" : "\n"} ${(isMot || isRemSen) ? "├" : "└"} Temp Setpoints:" : ""
		str += schData?.ctemp ? "\n ${isMot || isRemSen ? "│" : "   "}  ${schData?.htemp ? "├" : "└"} Cool Setpoint: (${schData?.ctemp}${tempScaleStr})" : ""
		str += schData?.htemp ? "\n ${isMot || isRemSen ? "│" : "   "}  ${schData?.hvacm ? "├" : "└"} Heat Setpoint: (${schData?.htemp}${tempScaleStr})" : ""
		str += schData?.hvacm ? "\n ${isMot || isRemSen ? "│" : "   "}  └ HVAC Mode: (${schData?.hvacm.toString().capitalize()})" : ""

		//Motion Info
		str += isMot ?						"${isTemp || isFanEn || isRemSen || isRestrict ? "\n │\n" : "\n"} ${isRemSen ? "├" : "└"} Motion Settings:" : ""
		str += isMot ?		 				"\n ${isRemSen ? "│" : "   "} ${(schData?.mctemp || schData?.mhtemp) ? "├" : "└"} Motion Sensors: (${schData?.m0.size()})" : ""
		str += isMot ?						"\n ${isRemSen ? "│" : "   "} ${schData?.mctemp || schData?.mhtemp ? "│" : ""} └ (${isMotionActive(settings["${sLbl}Motion"]) ? "Active" : "None Active"})" : ""
		str += isMot && schData?.mctemp ? 	"\n ${isRemSen ? "│" : "   "} ${(schData?.mctemp || schData?.mhtemp) ? "├" : "└"} Mot. Cool Setpoint: (${schData?.mctemp}${tempScaleStr})" : ""
		str += isMot && schData?.mhtemp ? 	"\n ${isRemSen ? "│" : "   "} ${schData?.mdelayOn || schData?.mdelayOff ? "├" : "└"} Mot. Heat Setpoint: (${schData?.mhtemp}${tempScaleStr})" : ""
		str += isMot && schData?.mhvacm ? 	"\n ${isRemSen ? "│" : "   "} ${(schData?.mdelayOn || schData?.mdelayOff) ? "├" : "└"} Mot. HVAC Mode: (${schData?.mhvacm.toString().capitalize()})" : ""
		str += isMot && schData?.mdelayOn ? "\n ${isRemSen ? "│" : "   "} ${schData?.mdelayOff ? "├" : "└"} Mot. On Delay: (${getEnumValue(longTimeSecEnum(), schData?.mdelayOn)})" : ""
		str += isMot && schData?.mdelayOff ?"\n ${isRemSen ? "│" : "   "} └ Mot. Off Delay: (${getEnumValue(longTimeSecEnum(), schData?.mdelayOff)})" : ""

		/*//Fan Control
		str += isFanEn ? 						"${isTemp || isRemSen || isRestrict ? "\n │\n" : "\n"} ${isRemSen ? "├" : "└"} Fan Control Settings:" : ""
		str += isFanEn ?		 				"\n ${isRemSen ? "│" : "   "} ${(schData?.ftempl || schData?.ftemph) ? "├" : "└"} Fans: (${schData?.fan0.size()})" : ""
		str += isFanEn && schData?.ftemp && schData?.ftempl && schData?.ftemph ? "\n ${isRemSen ? "│" : "   "} └ Temp Range: (Low: ${schData?.ftempl}${tempScaleStr} | High: ${schData?.ftemph}${tempScaleStr})" : ""

		str += isFanEn && schData?.fmoton ? 	"\n ${isRemSen ? "│" : "   "} ${(schData?.fmoton || schData?.fmotOff) ? "├" : "└"} Mot. On Delay: (${getEnumValue(longTimeSecEnum(), schData?.fmoton)})" : ""
		str += isFanEn && schData?.fmotoff ? 	"\n ${isRemSen ? "│" : "   "} └ Mot. Off Delay: (${getEnumValue(longTimeSecEnum(), schData?.fmotoff)})" : ""*/

		//Remote Sensor Info
		str += isRemSen ?	"${isRemSen || isRestrict ? "\n │\n" : "\n"} └ Alternate Remote Sensor:" : ""
		//str += isRemSen ? 	"\n      ├ Temp Sensors: (${schData?.sen0.size()})" : ""
		settings["${sLbl}remSensor"]?.each { t ->
			str += "\n      ├ ${t?.label}: ${(t?.label.length() > 10) ? "\n      │ └ " : ""}(${getDeviceTemp(t)}°${getTemperatureScale()})"
		}
		str += isRemSen && schData?.sen0 ? "\n      └ Temp${(settings["${sLbl}remSensor"]?.size() > 1) ? " (avg):" : ":"} (${getDeviceTempAvg(settings["${sLbl}remSensor"])}${tempScaleStr})" : ""
		//log.debug "str: \n$str"
		if(str != "") { result[schNum] = str }
	}
	return (result?.size() >= 1) ? result : null
}

def getAbrevDay(vals) {
	def list = []
	if(vals) {
		//log.debug "days: $vals | (${vals?.size()})"
		def len = (vals?.toString().length() < 7) ? 3 : 2
		vals?.each { d ->
			list.push(d?.toString().substring(0, len))
		}
	}
	return list
}

def roundTemp(temp) {
	if(temp == null) { return null }
	def newtemp
	if( getTemperatureScale() == "C") {
		newtemp = Math.round(temp.round(1) * 2) / 2.0f
	} else {
		if(temp instanceof Integer) {
			//log.debug "roundTemp: ($temp) is Integer"
			newTemp = temp.toInteger()
		}
		else if(temp instanceof Double) {
			//log.debug "roundTemp: ($temp) is Double"
			newtemp = temp.round(0).toInteger()
		}
		else if(temp instanceof BigDecimal) {
			//log.debug "roundTemp: ($temp) is BigDecimal"
			newtemp = temp.toInteger()
		}
	}
	return newtemp
}

def updateScheduleStateMap() {
	if(autoType == "schMot" && isSchMotConfigured()) {
		def actSchedules = null
		def numAct = 0
		actSchedules = [:]
		atomicState?.scheduleList?.each { scdNum ->
			def sLbl = "schMot_${scdNum}_"
			def newScd = []
			def schActive = settings["${sLbl}SchedActive"]

			if(schActive) {
				actSchedules?."${scdNum}" = [:]
				newScd = cleanUpMap([
					lbl: settings["${sLbl}name"],
					m: settings["${sLbl}restrictionMode"],
					tf: settings["${sLbl}restrictionTimeFrom"],
					tfc: settings["${sLbl}restrictionTimeFromCustom"],
					tfo: settings["${sLbl}restrictionTimeFromOffset"],
					tt: settings["${sLbl}restrictionTimeTo"],
					ttc: settings["${sLbl}restrictionTimeToCustom"],
					tto: settings["${sLbl}restrictionTimeToOffset"],
					w: settings["${sLbl}restrictionDOW"],
					s1: deviceInputToList(settings["${sLbl}restrictionSwitchOn"]),
					s0: deviceInputToList(settings["${sLbl}restrictionSwitchOff"]),
//ERSERS
					ctemp: roundTemp(settings["${sLbl}CoolTemp"]),
					htemp: roundTemp(settings["${sLbl}HeatTemp"]),
					hvacm: settings["${sLbl}HvacMode"],
					sen0: settings["schMotRemoteSensor"] ? deviceInputToList(settings["${sLbl}remSensor"]) : null,
					m0: deviceInputToList(settings["${sLbl}Motion"]),
					mctemp: settings["${sLbl}Motion"] ? roundTemp(settings["${sLbl}MCoolTemp"]) : null,
					mhtemp: settings["${sLbl}Motion"] ? roundTemp(settings["${sLbl}MHeatTemp"]) : null,
					mhvacm: settings["${sLbl}Motion"] ? settings["${sLbl}MHvacMode"] : null,
					mdelayOn: settings["${sLbl}Motion"] ? settings["${sLbl}MDelayValOn"] : null,
					mdelayOff: settings["${sLbl}Motion"] ? settings["${sLbl}MDelayValOff"] : null
				])

				/*fan0: deviceInputToList(settings["${sLbl}Fans"]),
				ftemp: settings["${sLbl}FansUseTemp"],
				ftempl: settings["${sLbl}FansLowTemp"],
				ftemph: settings["${sLbl}FansHighTemp"],
				fmot: settings["${sLbl}FansMotion"],
				fmoton: settings["${sLbl}FansMDelayValOn"],
				fmotoff: settings["${sLbl}FansMDelayValOff"]*/
				numAct += 1
				actSchedules?."${scdNum}" = newScd
				//LogAction("updateScheduleMap [ ScheduleNum: $scdNum | PrefixLbl: $sLbl | SchedActive: $schActive | NewSchedData: $newScd ]", "info", true)
			}
		}
		atomicState.activeSchedData = actSchedules
		//atomicState.scheduleSchedActiveCount = numAct
	}
}


def deviceInputToList(items) {
	def list = []
	if(items) {
		items?.sort().each { d ->
			list.push(d?.displayName.toString())
		}
		return list
	}
	return null
}

def inputItemsToList(items) {
	def list = []
	if(items) {
		items?.each { d ->
			list.push(d)
		}
		return list
	}
	return null
}


def isSchMotConfigured() {
	if(settings?.schMotTstat) {
		return true
	}
	return false
}

def getLastschMotEvalSec() { return !atomicState?.lastschMotEval ? 100000 : GetTimeDiffSeconds(atomicState?.lastschMotEval).toInteger() }

def schMotCheck() {
	LogAction("schMotCheck...", "trace", false)
	try {
		if(atomicState?.disableAutomation) { return }
		def schWaitVal = settings?.schMotWaitVal?.toInteger() ?: 60
		if(schWaitVal > 60) { schWaitVal = 60 }
		if(getLastschMotEvalSec() < schWaitVal) {
			def schChkVal = ((schWaitVal - getLastschMotEvalSec()) < 30) ? 30 : (schWaitVal - getLastschMotEvalSec())
			scheduleAutomationEval(schChkVal)
			LogAction("Remote Sensor: Too Soon to Evaluate Actions...Scheduling Re-Evaluation in (${schChkVal} seconds)", "info", true)
			return
		}

		def execTime = now()
		atomicState?.lastEvalDt = getDtNow()
		atomicState?.lastschMotEval = getDtNow()

		// This order is important...
		// turn system on/off, then update schedule mode/temps, then remote sensors, then update fans

		if(settings?.schMotWaterOff) {
			if(isLeakWatConfigured()) { leakWatCheck() }
		}
		if(settings?.schMotContactOff) {
			if(isConWatConfigured()) { conWatCheck() }
		}
		if(settings?.schMotExternalTempOff) {
			if(isExtTmpConfigured()) {
				if(setting?.extTmpUseWeather) { getExtConditions() }
				extTmpTempCheck()
			}
		}
		if(settings?.schMotSetTstatTemp) {
			if(isTstatSchedConfigured()) { setTstatTempCheck() }
		}
		if(settings?.schMotRemoteSensor) {
			if(isRemSenConfigured()) {
				remSenCheck()
			}
		}
		if(settings?.schMotOperateFan) {
			if(isFanCtrlConfigured()) {
				fanCtrlCheck()
			}
		}

		storeExecutionHistory((now() - execTime), "schMotCheck")
	} catch (ex) {
		log.error "schMotCheck Exception:", ex
		parent?.sendExceptionData(ex.message, "schMotCheck", true, getAutoType())
	}
}

def storeLastEventData(evt) {
	if(evt) {
		atomicState?.lastEventData = ["name":evt.name, "displayName":evt.displayName, "value":evt.value, "date":formatDt(evt.date), "unit":evt.unit]
		//log.debug "LastEvent: ${atomicState?.lastEventData}"
	}
}

def storeExecutionHistory(val, method = null) {
	//log.debug "storeExecutionHistory($val, $method)"
	try {
		if(method) {
			LogAction("${method} Execution Time: (${val} milliseconds)", "trace", false)
		}
		atomicState?.lastExecutionTime = val ?: null
		def list = atomicState?.evalExecutionHistory ?: []
		def listSize = 30
		if(list?.size() < listSize) {
			list.push(val)
		}
		else if(list?.size() > listSize) {
			def nSz = (list?.size()-listSize) + 1
			def nList = list?.drop(nSz)
			nList?.push(val)
			list = nList
		}
		else if(list?.size() == listSize) {
			def nList = list?.drop(1)
			nList?.push(val)
			list = nList
		}
		if(list) { atomicState?.evalExecutionHistory = list }
	} catch (ex) {
		log.error "storeExecutionHistory Exception:", ex
		parent?.sendExceptionData(ex.message, "storeExecutionHistory", true, getAutoType())
	}
}

def getAverageValue(items) {
	def tmpAvg = []
	def val = 0
	if(!items) { return val }
	else if(items?.size() > 1) {
		tmpAvg = items
		if(tmpAvg && tmpAvg?.size() > 1) { val = (tmpAvg?.sum().toDouble() / tmpAvg?.size().toDouble()).round(0) }
	} else { val = item }
	return val.toInteger()
}

/************************************************************************************************
|								DYNAMIC NOTIFICATION PAGES								|
*************************************************************************************************/

def setNotificationPage(params) {
	def pName = params.pName
	def allowSpeech = false
	def allowAlarm = false
	def showSched = false
	if(params?.pName) {
		atomicState.curNotifPageData = params
		allowSpeech = params?.allowSpeech?.toBoolean(); showSched = params?.showSchedule?.toBoolean(); allowAlarm = params?.allowAlarm?.toBoolean()
	} else {
		pName = atomicState?.curNotifPageData?.pName; allowSpeech = atomicState?.curNotifPageData?.allowSpeech; showSched = atomicState?.curNotifPageData?.showSchedule; allowAlarm = atomicState?.curNotifPageData?.allowAlarm
	}
	dynamicPage(name: "setNotificationPage", title: "Configure Notification Options", uninstall: false) {
		section("Notification Preferences:") {
			input "${pName}NotificationsOn", "bool", title: "Enable Notifications?", description: (!settings["${pName}NotificationsOn"] ? "Enable Text, Voice, Ask Alexa, or Alarm Notifications..." : ""), required: false, defaultValue: false, submitOnChange: true,
						image: getAppImg("notification_icon.png")
		}
		if(settings["${pName}NotificationsOn"]) {
			def notifDesc = !location.contactBookEnabled ? "Enable Push Messages Below..." : "(Manager App Recipients are used by default)"
			section("${notifDesc}") {
				if(!location.contactBookEnabled) {
					input "${pName}UsePush", "bool", title: "Send Push Notitifications\n(Optional)", required: false, submitOnChange: true, defaultValue: false, image: getAppImg("notification_icon.png")
				} else {
					input("${pName}NotifRecips", "contact", title: "Select Recipients...\n(Optional)", required: false, multiple: true, submitOnChange: true, image: getAppImg("recipient_icon.png")) {
						input ("${pName}NotifPhones", "phone", title: "Phone Number to Send SMS to...\n(Optional)", submitOnChange: true, required: false)
					}
				}
			}
		}

/*
this is a parent only method today

		if(showSched && settings["${pName}NotificationsOn"]) {
			section(title: "Time Restrictions") {
				href "setNotificationTimePage", title: "Silence Notifications...", description: (getNotifSchedDesc(pName) ?: "Tap to configure..."), params: [pName: "${pName}"],
					state: (getNotifSchedDesc(pName) ? "complete" : null), image: getAppImg("quiet_time_icon.png")
			}
		}
*/
		if(allowSpeech && settings?."${pName}NotificationsOn") {
			section("Voice Notification Preferences:") {
				input "${pName}AllowSpeechNotif", "bool", title: "Enable Voice Notifications?", description: "Media players, Speech Devices, or Ask Alexa", required: false, defaultValue: (settings?."${pName}AllowSpeechNotif" ? true : false), submitOnChange: true, image: getAppImg("speech_icon.png")
				if(settings["${pName}AllowSpeechNotif"]) {
					if(pName == "leakWat") {
						if(!atomicState?."${pName}OffVoiceMsg" || !settings["${pName}UseCustomSpeechNotifMsg"]) { atomicState?."${pName}OffVoiceMsg" = "ATTENTION: %devicename% has been turned OFF because %wetsensor% has reported it is WET" }
						if(!atomicState?."${pName}OnVoiceMsg" || !settings["${pName}UseCustomSpeechNotifMsg"]) { atomicState?."${pName}OnVoiceMsg" = "Restoring %devicename% to %lastmode% Mode because ALL water sensors have been Dry again for (%ondelay%)" }
					}
					if(pName == "conWat") {
						if(!atomicState?."${pName}OffVoiceMsg" || !settings["${pName}UseCustomSpeechNotifMsg"]) { atomicState?."${pName}OffVoiceMsg" = "ATTENTION: %devicename% has been turned OFF because %opencontact% has been Opened for (%offdelay%)" }
						if(!atomicState?."${pName}OnVoiceMsg" || !settings["${pName}UseCustomSpeechNotifMsg"]) { atomicState?."${pName}OnVoiceMsg" = "Restoring %devicename% to %lastmode% Mode because ALL contacts have been Closed again for (%ondelay%)" }
					}
					if(pName == "extTmp") {
						if(!atomicState?."${pName}OffVoiceMsg" || !settings["${pName}UseCustomSpeechNotifMsg"]) { atomicState?."${pName}OffVoiceMsg" = "ATTENTION: %devicename% has been turned OFF because External Temp is above the temp threshold for (%offdelay%)" }
						if(!atomicState?."${pName}OnVoiceMsg" || !settings["${pName}UseCustomSpeechNotifMsg"]) { atomicState?."${pName}OnVoiceMsg" = "Restoring %devicename% to %lastmode% Mode because External Temp has been above the temp threshold for (%ondelay%)" }
					}
					input "${pName}SendToAskAlexaQueue", "bool", title: "Send to Ask Alexa Message Queue?", required: false, defaultValue: (settings?."${pName}AllowSpeechNotif" ? false : true), submitOnChange: true,
							image: askAlexaImgUrl()
					input "${pName}SpeechMediaPlayer", "capability.musicPlayer", title: "Select Media Player Devices", hideWhenEmpty: true, multiple: true, required: false, submitOnChange: true, image: getAppImg("media_player.png")
					input "${pName}SpeechDevices", "capability.speechSynthesis", title: "Select Speech Synthesis Devices", hideWhenEmpty: true, multiple: true, required: false, submitOnChange: true, image: getAppImg("speech2_icon.png")
					if(settings["${pName}SpeechMediaPlayer"]) {
						input "${pName}SpeechVolumeLevel", "number", title: "Default Volume Level?", required: false, defaultValue: 30, range: "0::100", submitOnChange: true, image: getAppImg("volume_icon.png")
						input "${pName}SpeechAllowResume", "bool", title: "Can Resume Playing Media?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("resume_icon.png")
					}
					def desc = ""
					if(pName in ["conWat", "extTmp", "leakWat"]) {
						if( (settings["${pName}SpeechMediaPlayer"] || settings["${pName}SpeechDevices"] || settings["${pName}SendToAskAlexaQueue"]) ) {
							switch(pName) {
								case "conWat":
									desc = "Contact Close"
									break
								case "extTmp":
									desc = "External Temperature Threshold"
									break
								case "leakWat":
									desc = "Water Dried"
									break
							}


							input "${pName}SpeechOnRestore", "bool", title: "Speak when restoring HVAC on (${desc})?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("speech_icon.png")
	// TODO There are more messages and errors than ON / OFF
							input "${pName}UseCustomSpeechNotifMsg", "bool", title: "Customize Notitification Message?", required: false, defaultValue: (settings?."${pName}AllowSpeechNotif" ? false : true), submitOnChange: true,
								image: getAppImg("speech_icon.png")
							if(settings["${pName}UseCustomSpeechNotifMsg"]) {
								getNotifVariables(pName)
								input "${pName}CustomOffSpeechMessage", "text", title: "Turn Off Message?", required: false, defaultValue: atomicState?."${pName}OffVoiceMsg" , submitOnChange: true, image: getAppImg("speech_icon.png")
								atomicState?."${pName}OffVoiceMsg" = settings?."${pName}CustomOffSpeechMessage"
								if(settings?."${pName}CustomOffSpeechMessage") {
									paragraph "Off Msg:\n" + voiceNotifString(atomicState?."${pName}OffVoiceMsg",pName)
								}
								input "${pName}CustomOnSpeechMessage", "text", title: "Restore On Message?", required: false, defaultValue: atomicState?."${pName}OnVoiceMsg", submitOnChange: true, image: getAppImg("speech_icon.png")
								atomicState?."${pName}OnVoiceMsg" = settings?."${pName}CustomOnSpeechMessage"
								if(settings?."${pName}CustomOnSpeechMessage") {
									paragraph "Restore On Msg:\n" + voiceNotifString(atomicState?."${pName}OnVoiceMsg",pName)
								}
							} else {
								atomicState?."${pName}OffVoiceMsg" = ""
								atomicState?."${pName}OnVoiceMsg" = ""
							}
						}
					}
				}
			}
		}
		if(allowAlarm && settings?."${pName}NotificationsOn") {
			section("Alarm/Siren Device Preferences:") {
				input "${pName}AllowAlarmNotif", "bool", title: "Enable Alarm|Siren?", required: false, defaultValue: (settings?."${pName}AllowAlarmNotif" ? true : false), submitOnChange: true,
						image: getAppImg("alarm_icon.png")
				if(settings["${pName}AllowAlarmNotif"]) {
					input "${pName}AlarmDevices", "capability.alarm", title: "Select Alarm/Siren Devices", multiple: true, required: settings["${pName}AllowAlarmNotif"], submitOnChange: true, image: getAppImg("alarm_icon.png")
				}
			}
		}
		//if(pName in ["conWat", "leakWat", "extTmp", "watchDog"] && settings["${pName}NotificationsOn"] && (settings["${pName}AllowSpeechNotif"] || settings["${pName}AllowAlarmNotif"])) {
		if(pName in ["conWat", "leakWat", "extTmp", "watchDog"] && settings["${pName}NotificationsOn"] && settings["${pName}AllowAlarmNotif"] && settings["${pName}AlarmDevices"]) {
			section("Notification Alert Options (1):") {
				input "${pName}_Alert_1_Delay", "enum", title: "First Alert Delay (in minutes)", defaultValue: null, required: true, submitOnChange: true, metadata: [values:longTimeSecEnum()],
						image: getAppImg("alert_icon2.png")
				if(settings?."${pName}_Alert_1_Delay") {
/*
TODO These are not in use.   They could only be used in alarming, but the other data needed is not available

					if(settings?."${pName}NotificationsOn" && (settings["${pName}UsePush"] || settings["${pName}NotifRecips"] || settings["${pName}NotifPhones"])) {
						input "${pName}_Alert_1_Send_Push", "bool", title: "Send Push Notification?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("notification_icon.png")
						if(settings["${pName}_Alert_1_Send_Push"]) {
							input "${pName}_Alert_1_Send_Custom_Push", "bool", title: "Custom Push Message?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("notification_icon.png")
							if(settings["${pName}_Alert_1_Send_Custom_Push"]) {
								input "${pName}_Alert_1_CustomPushMessage", "text", title: "Push Message to Send?", required: false, defaultValue: atomicState?."${pName}OffVoiceMsg" , submitOnChange: true, image: getAppImg("speech_icon.png")
							}
						}
					}
					if(settings?."${pName}AllowSpeechNotif") {
						input "${pName}_Alert_1_Use_Speech", "bool", title: "Send Voice Notification?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("speech_icon.png")
						if(settings?."${pName}_Alert_1_Use_Speech") {
							input "${pName}_Alert_1_Send_Custom_Speech", "bool", title: "Custom Speech Message?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("speech_icon.png")
							if(settings["${pName}_Alert_1_Send_Custom_Speech"]) {
								input "${pName}_Alert_1_CustomSpeechMessage", "text", title: "Push Message to Send?", required: false, defaultValue: atomicState?."${pName}OffVoiceMsg" , submitOnChange: true, image: getAppImg("speech_icon.png")
							}
						}
					}
					if(settings?."${pName}AllowAlarmNotif") {
*/
						//input "${pName}_Alert_1_Use_Alarm", "bool", title: "Use Alarm Device", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("alarm_icon.png")
						//if(settings?."${pName}_Alert_1_Use_Alarm" && settings?."${pName}AlarmDevices") {
							input "${pName}_Alert_1_AlarmType", "enum", title: "Alarm Type to use?", metadata: [values:alarmActionsEnum()], defaultValue: null, submitOnChange: true, required: true, image: getAppImg("alarm_icon.png")
							if(settings?."${pName}_Alert_1_AlarmType") {
								input "${pName}_Alert_1_Alarm_Runtime", "enum", title: "Turn off Alarm After (in seconds)?", metadata: [values:shortTimeEnum()], defaultValue: 10, required: true, submitOnChange: true,
										image: getAppImg("delay_time_icon.png")
							}
						//}
/*
					}
					if(settings["${pName}_Alert_1_Send_Custom_Speech"] || settings["${pName}_Alert_1_Send_Custom_Push"]) {
						if(pName in ["leakWat", "conWat", "extTmp"]) {
							getNotifVariables(pName)
						}
					}
*/
				}
			}
			if(settings["${pName}_Alert_1_Delay"]) {
				section("Notification Alert Options (2):") {
					input "${pName}_Alert_2_Delay", "enum", title: "Second Alert Delay (in minutes)", defaultValue: null, metadata: [values:longTimeSecEnum()], required: false, submitOnChange: true, image: getAppImg("alert_icon2.png")
					if(settings?."${pName}_Alert_2_Delay") {
/*
TODO These are not in use.   They could only be used in alarming, but the other data needed is not available
						if(settings?."${pName}NotificationsOn" && (settings["${pName}UsePush"] || settings["${pName}NotifRecips"] || settings["${pName}NotifPhones"])) {
							input "${pName}_Alert_2_Send_Push", "bool", title: "Send Push Notification?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("notification_icon.png")
							if(settings["${pName}_Alert_2_Send_Push"]) {
								input "${pName}_Alert_2_Send_Custom_Push", "bool", title: "Custom Push Message?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("notification_icon.png")
								if(settings["${pName}_Alert_2_Send_Custom_Push"]) {
									input "${pName}_Alert_2_CustomPushMessage", "text", title: "Push Message to Send?", required: false, defaultValue: atomicState?."${pName}OffVoiceMsg" , submitOnChange: true, image: getAppImg("speech_icon.png")
								}
							}
						}
						if(settings?."${pName}AllowSpeechNotif") {
							input "${pName}_Alert_2_Use_Speech", "bool", title: "Send Voice Notification?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("speech_icon.png")
							if(settings?."${pName}_Alert_2_Use_Speech") {
								input "${pName}_Alert_2_Send_Custom_Speech", "bool", title: "Custom Speech Message?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("speech_icon.png")
								if(settings["${pName}_Alert_2_Send_Custom_Speech"]) {
									input "${pName}_Alert_2_CustomSpeechMessage", "text", title: "Push Message to Send?", required: false, defaultValue: atomicState?."${pName}OffVoiceMsg" , submitOnChange: true, image: getAppImg("speech_icon.png")
								}
							}
						}
						if(settings?."${pName}AllowAlarmNotif") {
*/
							//input "${pName}_Alert_2_Use_Alarm", "bool", title: "Use Alarm Device?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("alarm_icon.png")
							//if(settings?."${pName}_Alert_2_Use_Alarm" && settings?."${pName}AlarmDevices") {
								input "${pName}_Alert_2_AlarmType", "enum", title: "Alarm Type to use?", metadata: [values:alarmActionsEnum()], defaultValue: null, submitOnChange: true, required: true, image: getAppImg("alarm_icon.png")
								if(settings?."${pName}_Alert_2_AlarmType") {
									input "${pName}_Alert_2_Alarm_Runtime", "enum", title: "Turn off Alarm After (in minutes)?", metadata: [values:shortTimeEnum()], defaultValue: 10, required: true, submitOnChange: true,
											image: getAppImg("delay_time_icon.png")
								}
							//}
/*
						}
						if(settings["${pName}_Alert_2_Send_Custom_Speech"] || settings["${pName}_Alert_2_Send_Custom_Push"]) {
							if(pName in ["leakWat", "conWat", "extTmp"]) {
								getNotifVariables(pName)
							}
						}
*/
					}
				}
			}
		}
	}
}

def getNotifVariables(pName) {
	def str = ""
	str += "\n • DeviceName: %devicename%"
	str += "\n • Last Mode: %lastmode%"
	str += (pName == "leakWat") ? "\n • Wet Water Sensor: %wetsensor%" : ""
	str += (pName == "conWat") ? "\n • Open Contact: %opencontact%" : ""
	str += (pName in ["conWat", "extTmp"]) ? "\n • Off Delay: %offdelay%" : ""
	str += "\n • On Delay: %ondelay%"
	str += (pName == "extTmp") ? "\n • Temp Threshold: %tempthreshold%" : ""
	paragraph "These Variables are accepted: ${str}"
}

//process custom tokens to generate final voice message (Copied from BigTalker)
def voiceNotifString(phrase, pName) {
	//log.trace "conWatVoiceNotifString..."
	try {
		if(phrase?.toLowerCase().contains("%devicename%")) { phrase = phrase?.toLowerCase().replace('%devicename%', (settings?."schMotTstat"?.displayName.toString() ?: "unknown")) }
		if(phrase?.toLowerCase().contains("%lastmode%")) { phrase = phrase?.toLowerCase().replace('%lastmode%', (atomicState?."${pName}RestoreMode".toString() ?: "unknown")) }
		if(pName == "leakWat" && phrase?.toLowerCase().contains("%wetsensor%")) {
			phrase = phrase?.toLowerCase().replace('%wetsensor%', (getWetWaterSensors(leakWatSensors) ? getWetWaterSensors(leakWatSensors)?.join(", ").toString() : "a selected leak sensor")) }
		if(pName == "conWat" && phrase?.toLowerCase().contains("%opencontact%")) {
			phrase = phrase?.toLowerCase().replace('%opencontact%', (getOpenContacts(conWatContacts) ? getOpenContacts(conWatContacts)?.join(", ").toString() : "a selected contact")) }
		if(pName == "extTmp" && phrase?.toLowerCase().contains("%tempthreshold%")) {
			phrase = phrase?.toLowerCase().replace('%tempthreshold%', "${extTmpDiffVal.toString()}(°${getTemperatureScale()})") }
		if(phrase?.toLowerCase().contains("%offdelay%")) { phrase = phrase?.toLowerCase().replace('%offdelay%', getEnumValue(longTimeSecEnum(), settings?."${pName}OffDelay").toString()) }
		if(phrase?.toLowerCase().contains("%ondelay%")) { phrase = phrase?.toLowerCase().replace('%ondelay%', getEnumValue(longTimeSecEnum(), settings?."${pName}OnDelay").toString()) }
	} catch (ex) {
		log.error "voiceNotifString Exception:", ex
		parent?.sendExceptionData(ex.message, "voiceNotifString", true, getAutoType())
	}
	return phrase
}

def getNotificationOptionsConf(pName) {
	LogAction("getNotificationOptionsConf pName: $pName", "trace", false)
	def res = (settings?."${pName}NotificationsOn" &&
			(getRecipientDesc(pName) ||
			(settings?."${pName}AllowSpeechNotif" && (settings?."${pName}SpeechDevices" || settings?."${pName}SpeechMediaPlayer")) ||
			(settings?."${pName}AllowAlarmNofif" && settings?."${pName}AlarmDevices")
		) ) ? true : false
	return res
}

def getNotifConfigDesc(pName) {
	LogAction("getNotifConfigDesc pName: $pName", "trace", false)
	def str = ""
	if(settings?."${pName}NotificationsOn") {
		str += ( getRecipientDesc(pName) || (settings?."${pName}AllowSpeechNotif" && (settings?."${pName}SpeechDevices" || settings?."${pName}SpeechMediaPlayer"))) ?
			"Notification Status:" : ""
		str += (settings?."${pName}NotifRecips") ? "${str != "" ? "\n" : ""} • Contacts: (${settings?."${pName}NotifRecips"?.size()})" : ""
		str += (settings?."${pName}UsePush") ? "\n • Push Messages: Enabled" : ""
		str += (settings?."${pName}NotifPhones") ? "\n • SMS: (${settings?."${pName}NotifPhones"?.size()})" : ""
		str += getVoiceNotifConfigDesc(pName) ? ("${(str != "") ? "\n\n" : "\n"}Voice Status:${getVoiceNotifConfigDesc(pName)}") : ""
		str += getAlarmNotifConfigDesc(pName) ? ("${(str != "") ? "\n\n" : "\n"}Alarm Status:${getAlarmNotifConfigDesc(pName)}") : ""
		str += getAlertNotifConfigDesc(pName) ? "\n${getAlertNotifConfigDesc(pName)}" : ""
	}
	return (str != "") ? "${str}" : null
}

def getVoiceNotifConfigDesc(pName) {
	def str = ""
	if(settings?."${pName}NotificationsOn" && settings["${pName}AllowSpeechNotif"]) {
		def speaks = getInputToStringDesc(settings?."${pName}SpeechDevices", true)
		def medias = getInputToStringDesc(settings?."${pName}SpeechMediaPlayer", true)
		str += settings["${pName}SendToAskAlexaQueue"] ? "\n • Send to Ask Alexa: (True)" : ""
		str += speaks ? "\n • Speech Devices:${speaks.size() > 1 ? "\n" : ""}${speaks}" : ""
		str += medias ? "\n • Media Players:${medias.size() > 1 ? "" : ""}${medias}" : ""
		str += (medias && settings?."${pName}SpeechVolumeLevel") ? "\n    ├ Volume: (${settings?."${pName}SpeechVolumeLevel"})" : ""
		str += (medias && settings?."${pName}SpeechAllowResume") ? "\n    └ Resume: (${settings?."${pName}SpeechAllowResume".toString().capitalize()})" : ""
		str += (settings?."${pName}UseCustomSpeechNotifMsg" && (medias || speaks)) ? "\n • Custom Message: (${settings?."${pName}UseCustomSpeechNotifMsg".toString().capitalize()})" : ""
	}
	return (str != "") ? "${str}" : null
}

def getAlarmNotifConfigDesc(pName) {
	def str = ""
	if(settings?."${pName}NotificationsOn" && settings["${pName}AllowAlarmNotif"]) {
		def alarms = getInputToStringDesc(settings["${pName}AlarmDevices"], true)
		str += alarms ? "\n • Alarm Devices:${alarms.size() > 1 ? "\n" : ""}${alarms}" : ""
	}
	return (str != "") ? "${str}" : null
}

def getAlertNotifConfigDesc(pName) {
	def str = ""
//TODO not sure we do all these...
	if(settings?."${pName}NotificationsOn" && (settings["${pName}_Alert_1_Delay"] || settings["${pName}_Alert_2_Delay"]) && (settings["${pName}AllowSpeechNotif"] || settings["${pName}AllowAlarmNotif"])) {
		str += settings["${pName}_Alert_1_Delay"] ? "\nAlert (1) Status:\n  • Delay: (${getEnumValue(longTimeSecEnum(), settings["${pName}_Alert_1_Delay"])})" : ""
		str += settings["${pName}_Alert_1_Send_Push"] ? "\n  • Send Push: (${settings["${pName}_Alert_1_Send_Push"]})" : ""
		str += settings["${pName}_Alert_1_Use_Speech"] ? "\n  • Use Speech: (${settings["${pName}_Alert_1_Use_Speech"]})" : ""
		str += settings["${pName}_Alert_1_Use_Alarm"] ? "\n  • Use Alarm: (${settings["${pName}_Alert_1_Use_Alarm"]})" : ""
		str += (settings["${pName}_Alert_1_Use_Alarm"] && settings["${pName}_Alert_1_AlarmType"]) ? "\n ├ Alarm Type: (${getEnumValue(alarmActionsEnum(), settings["${pName}_Alert_1_AlarmType"])})" : ""
		str += (settings["${pName}_Alert_1_Use_Alarm"] && settings["${pName}_Alert_1_Alarm_Runtime"]) ? "\n └ Alarm Runtime: (${getEnumValue(shortTimeEnum(), settings["${pName}_Alert_1_Alarm_Runtime"])})" : ""
		str += settings["${pName}_Alert_2_Delay"] ? "${settings["${pName}_Alert_1_Delay"] ? "\n" : ""}\nAlert (2) Status:\n  • Delay: (${getEnumValue(longTimeSecEnum(), settings["${pName}_Alert_2_Delay"])})" : ""
		str += settings["${pName}_Alert_2_Send_Push"] ? "\n  • Send Push: (${settings["${pName}_Alert_2_Send_Push"]})" : ""
		str += settings["${pName}_Alert_2_Use_Speech"] ? "\n  • Use Speech: (${settings["${pName}_Alert_2_Use_Speech"]})" : ""
		str += settings["${pName}_Alert_2_Use_Alarm"] ? "\n  • Use Alarm: (${settings["${pName}_Alert_2_Use_Alarm"]})" : ""
		str += (settings["${pName}_Alert_2_Use_Alarm"] && settings["${pName}_Alert_2_AlarmType"]) ? "\n ├ Alarm Type: (${getEnumValue(alarmActionsEnum(), settings["${pName}_Alert_2_AlarmType"])})" : ""
		str += (settings["${pName}_Alert_2_Use_Alarm"] && settings["${pName}_Alert_2_Alarm_Runtime"]) ? "\n └ Alarm Runtime: (${getEnumValue(shortTimeEnum(), settings["${pName}_Alert_2_Alarm_Runtime"])})" : ""
	}
	return (str != "") ? "${str}" : null
}

def getInputToStringDesc(inpt, addSpace = null) {
	def cnt = 0
	def str = ""
	if(inpt) {
		inpt.sort().each { item ->
			cnt = cnt+1
			str += item ? (((cnt < 1) || (inpt?.size() > 1)) ? "\n    ${item}" : "${addSpace ? "    " : ""}${item}") : ""
		}
	}
	//log.debug "str: $str"
	return (str != "") ? "${str}" : null
}

def isPluralString(obj) {
	return (obj?.size() > 1) ? "(s)" : ""
}

def getRecipientsNames(val) {
	def n = ""
	def i = 0
	if(val) {
		//def valLabel =
		log.debug "val: $val"
		val?.each { r ->
			i = i + 1
			n += i == val?.size() ? "${r}" : "${r},"
		}
	}
	return n?.toString().replaceAll("\\,", "\n")
}

def getRecipientDesc(pName) {
	return ((settings?."${pName}NotifRecips") || (settings?."${pName}NotifPhones" || settings?."${pName}NotifUsePush")) ? getRecipientsNames(settings?."${pName}NotifRecips") : null
}

def setDayModeTimePage(params) {
	def pName = params.pName
	if(params?.pName) {
		atomicState.cursetDayModeTimePageData = params
	} else {
		pName = atomicState?.cursetDayModeTimePageData?.pName
	}
	dynamicPage(name: "setDayModeTimePage", title: "Select Days, Times or Modes", uninstall: false) {
		def secDesc = settings["${pName}DmtInvert"] ? "Not" : "Only"
		def inverted = settings["${pName}DmtInvert"] ? true : false
		section("") {
			input "${pName}DmtInvert", "bool", title: "When Not in Any of These?...", defaultValue: false, submitOnChange: true, image: getAppImg("switch_icon.png")
		}
		section("${secDesc} During these Days, Times, or Modes:") {
			def timeReq = (settings?."${pName}StartTime" || settings."${pName}StopTime") ? true : false
			input "${pName}StartTime", "time", title: "Start time", required: timeReq, image: getAppImg("start_time_icon.png")
			input "${pName}StopTime", "time", title: "Stop time", required: timeReq, image: getAppImg("stop_time_icon.png")
			input "${pName}Days", "enum", title: "${inverted ? "Not": "Only"} These Days", multiple: true, required: false, options: timeDayOfWeekOptions(), image: getAppImg("day_calendar_icon2.png")
			input "${pName}Modes", "mode", title: "${inverted ? "Not": "Only"} in These Modes...", multiple: true, required: false, image: getAppImg("mode_icon.png")
		}
	}
}

def getDayModeTimeDesc(pName) {
	def startTime = settings?."${pName}StartTime"
	def stopInput = settings?."${pName}StopInput"
	def stopTime = settings?."${pName}StopTime"
	def dayInput = settings?."${pName}Days"
	def modeInput = settings?."${pName}Modes"
	def inverted = settings?."${pName}DmtInvert" ?: null
	def str = ""
	def days = getInputToStringDesc(dayInput)
	def modes = getInputToStringDesc(modeInput)
	str += ((startTime && stopTime) || modes || days) ? "${!inverted ? "When" : "When Not"}:" : ""
	str += (startTime && stopTime) ? "\n • Time: ${time2Str(settings?."${pName}StartTime")} - ${time2Str(settings?."${pName}StopTime")}"  : ""
	str += days ? "${(startTime || stopTime) ? "\n" : ""}\n • Day${isPluralString(dayInput)}: ${days}" : ""
	str += modes ? "${(startTime || stopTime || days) ? "\n" : ""}\n • Mode${isPluralString(modeInput)}: ${modes}" : ""
	str += (str != "") ? "\n\nTap to Modify..." : ""
	return str
}

def getDmtSectionDesc(autoType) {
	return settings["${autoType}DmtInvert"] ? "Do Not Act During these Days, Times, or Modes:" : "Only Act During these Days, Times, or Modes:"
}

/************************************************************************************************
|   					      AUTOMATION SCHEDULE CHECK 								|
*************************************************************************************************/

def autoScheduleOk(autoType) {
	try {
		def inverted = settings?."${autoType}DmtInvert" ? true : false
		def modeOk = true
		modeOk = (!settings?."${autoType}Modes" || ((isInMode(settings?."${autoType}Modes") && !inverted) || (!isInMode(settings?."${autoType}Modes") && inverted))) ? true : false
		//dayOk
		def dayOk = true
		def dayFmt = new SimpleDateFormat("EEEE")
		dayFmt.setTimeZone(getTimeZone())
		def today = dayFmt.format(new Date())
		def inDay = (today in settings?."${autoType}Days") ? true : false
		dayOk = (!settings?."${autoType}Days" || ((inDay && !inverted) || (!inDay && inverted))) ? true : false

		//scheduleTimeOk
		def timeOk = true
		if(settings?."${autoType}StartTime" && settings?."${autoType}StopTime") {
			def inTime = (timeOfDayIsBetween(settings?."${autoType}StartTime", settings?."${autoType}StopTime", new Date(), getTimeZone())) ? true : false
			timeOk = ((inTime && !inverted) || (!inTime && inverted)) ? true : false
		}

		//LogAction("autoScheduleOk( dayOk: $dayOk | modeOk: $modeOk | dayOk: ${dayOk} | timeOk: $timeOk | inverted: ${inverted})", "info", false)
		return (modeOk && dayOk && timeOk) ? true : false
	} catch (ex) {
		log.error "${autoType}-autoScheduleOk Exception:", ex
		parent?.sendExceptionData(ex.message, "${autoType}-autoScheduleOk", true, getAutoType())
	}
}

/************************************************************************************************
|					      SEND NOTIFICATIONS VIA PARENT APP								|
*************************************************************************************************/
def sendNofificationMsg(msg, msgType, recips = null, sms = null, push = null) {
	LogAction("sendNofificationMsg...($msg, $msgType, $recips, $sms, $push)", "trace", false)
	if(recips || sms || push) {
		parent?.sendMsg(msgType, msg, recips, sms, push)
		//LogAction("Send Push Notification to $recips...", "info", true)
	} else {
		parent?.sendMsg(msgType, msg)
	}
}

/************************************************************************************************
|							GLOBAL Code | Logging AND Diagnostic							    |
*************************************************************************************************/

def sendEventPushNotifications(message, type, pName) {
	//log.trace "sendEventPushNotifications...($message, $type, $pName)"
	if(settings["${pName}_Alert_1_Send_Push"] || settings["${pName}_Alert_2_Send_Push"]) {
//TODO this portion is never reached
		if(settings["${pName}_Alert_1_CustomPushMessage"]) {
			sendNofificationMsg(settings["${pName}_Alert_1_CustomPushMessage"].toString(), type, settings?."${pName}NotifRecips", settings?."${pName}NotifPhones", settings?."${pName}UsePush")
		} else {
			sendNofificationMsg(message, type, settings?."${pName}NotifRecips", settings?."${pName}NotifPhones", settings?."${pName}UsePush")
		}
	} else {
		sendNofificationMsg(message, type, settings?."${pName}NotifRecips", settings?."${pName}NotifPhones", settings?."${pName}UsePush")
	}
}

def sendEventVoiceNotifications(vMsg, pName, msgId, rmAAMsg=false, rmMsgId) {
	def allowNotif = settings?."${pName}NotificationsOn" ? true : false
	def allowSpeech = allowNotif && settings?."${pName}AllowSpeechNotif" ? true : false
	def ok2Notify = parent.getOk2Notify()

	LogAction("sendEventVoiceNotifications...($vMsg, $pName)   ok2Notify: $ok2Notify", "trace", false)
	if(allowNotif && allowSpeech) {
		if(ok2Notify && (settings["${pName}SpeechDevices"] || settings["${pName}SpeechMediaPlayer"])) {
			sendTTS(vMsg, pName)
		}
		if(settings["${pName}SendToAskAlexaQueue"]) {		// we queue to Alexa regardless of quiet times
			if(rmMsgId != null && rmAAMsg == true) {
				removeAskAlexaQueueMsg(rmMsgId)
			}
			if (vMsg && msgId != null) {
				addEventToAskAlexaQueue(vMsg, msgId)
			}
		}
	}
}

def addEventToAskAlexaQueue(vMsg, msgId) {
	if(parent?.getAskAlexaQueueEnabled() == true) {
		LogAction("sendEventToAskAlexaQueue: Adding this Message to the Ask Alexa Queue: ($vMsg)|${msgId}", "info", true)
		sendLocationEvent(name: "AskAlexaMsgQueue", value: "${app?.label}", isStateChange: true, descriptionText: "${vMsg}", unit: "${msgId}")
	}
}

def removeAskAlexaQueueMsg(msgId) {
	if(parent?.getAskAlexaQueueEnabled() == true) {
		LogAction("removeAskAlexaQueueMsg: Removing Message ID (${msgId}) from the Ask Alexa Queue", "info", true)
		sendLocationEvent(name: "AskAlexaMsgQueueDelete", value: "${app?.label}", isStateChange: true, unit: msgId)
	}
}


def scheduleAlarmOn(autoType) {
	LogAction("scheduleAlarmOn: autoType: $autoType a1DelayVal: ${getAlert1DelayVal(autoType)}", "debug", true)
	def timeVal = getAlert1DelayVal(autoType).toInteger()
	def ok2Notify = parent.getOk2Notify()

	log.debug "scheduleAlarmOn timeVal: $timeVal ok2Notify: $ok2Notify"
	if(canSchedule() && ok2Notify) {
		if(timeVal > 0) {
			runIn(timeVal, "alarm0FollowUp", [data: [autoType: autoType]])
			LogAction("scheduleAlarmOn: Scheduling Alarm Followup 0...in timeVal: $timeVal", "info", true)
			atomicState."${autoType}AlarmActive" = true
		} else { LogAction("scheduleAlarmOn: Did not schedule ANY operation timeVal: $timeVal", "error", true) }
	} else { LogAction("scheduleAlarmOn: Could not schedule operation timeVal: $timeVal", "error", true) }
}

def alarm0FollowUp(val) {
	def autoType = val.autoType
	LogAction("alarm0FollowUp: autoType: $autoType 1 OffVal: ${getAlert1AlarmEvtOffVal(autoType)}", "debug", true)
	def timeVal = getAlert1AlarmEvtOffVal(autoType).toInteger()
	log.debug "alarm0FollowUp timeVal: $timeVal"
	if(canSchedule() && timeVal > 0 && sendEventAlarmAction(1, autoType)) {
		runIn(timeVal, "alarm1FollowUp", [data: [autoType: autoType]])
		LogAction("alarm0FollowUp: Scheduling Alarm Followup 1...in timeVal: $timeVal", "info", true)
	} else { LogAction ("alarm0FollowUp: Could not schedule operation timeVal: $timeVal", "error", true) }
}

def alarm1FollowUp(val) {
	def autoType = val.autoType
	LogAction("alarm1FollowUp  autoType: $autoType  a2DelayVal: ${getAlert2DelayVal(autoType)}", "debug", true)
	def aDev = settings["${autoType}AlarmDevices"]
	if(aDev) {
		aDev?.off()
		storeLastAction("Set Alarm OFF", getDtNow())
		LogAction("alarm1FollowUp: Turning OFF ${aDev}", "info", true)
	}
	def timeVal = getAlert2DelayVal(autoType).toInteger()
	//if(canSchedule() && (settings["${autoType}_Alert_2_Use_Alarm"] && timeVal > 0)) {
	if(canSchedule() && timeVal > 0) {
		runIn(timeVal, "alarm2FollowUp", [data: [autoType: autoType]])
		LogAction("alarm1FollowUp: Scheduling Alarm Followup 2...in timeVal: $timeVal", "info", true)
	} else { LogAction ("alarm1FollowUp: Could not schedule operation timeVal: $timeVal", "error", true) }
}

def alarm2FollowUp(val) {
	def autoType = val.autoType
	LogAction("alarm2FollowUp: autoType: $autoType 2 OffVal: ${getAlert2AlarmEvtOffVal(autoType)}", "debug", true)
	def timeVal = getAlert2AlarmEvtOffVal(autoType)
	if(canSchedule() && timeVal > 0 && sendEventAlarmAction(2, autoType)) {
		runIn(timeVal, "alarm3FollowUp", [data: [autoType: autoType]])
		LogAction("alarm2FollowUp: Scheduling Alarm Followup 3...in timeVal: $timeVal", "info", true)
	} else { LogAction ("alarm2FollowUp: Could not schedule operation timeVal: $timeVal", "error", true) }
}

def alarm3FollowUp(val) {
	def autoType = val.autoType
	LogAction("alarm3FollowUp: autoType: $autoType", "debug", true)
	def aDev = settings["${autoType}AlarmDevices"]
	if(aDev) {
		aDev?.off()
		storeLastAction("Set Alarm OFF", getDtNow())
		LogAction("alarm3FollowUp: Turning OFF ${aDev}", "info", true)
	}
	atomicState."${autoType}AlarmActive" = false
}

def alarmEvtSchedCleanup(autoType) {
	if(atomicState?."${autoType}AlarmActive") {
		LogAction("Cleaning Up Alarm Event Schedules... autoType: $autoType", "info", true)
		def items = ["alarm0FollowUp","alarm1FollowUp", "alarm2FollowUp", "alarm3FollowUp"]
		items.each {
			unschedule("$it")
		}
		def val = [ autoType: autoType ]
		alarm3FollowUp(val)
	}
}

def sendEventAlarmAction(evtNum, autoType) {
	LogAction("sendEventAlarmAction  evtNum: $evtNum  autoType: $autoType", "info", true)
	try {
		def resval = false
		def allowNotif = settings?."${autoType}NotificationsOn" ? true : false
		def allowAlarm = allowNotif && settings?."${autoType}AllowAlarmNotif" ? true : false
		def aDev = settings["${autoType}AlarmDevices"]
		if(allowNotif && allowAlarm && aDev) {
			//if(settings["${autoType}_Alert_${evtNum}_Use_Alarm"]) {
				resval = true
				def alarmType = settings["${autoType}_Alert_${evtNum}_AlarmType"].toString()
				switch (alarmType) {
					case "both":
						atomicState?."${autoType}alarmEvt${evtNum}StartDt" = getDtNow()
						aDev?.both()
						storeLastAction("Set Alarm BOTH ON", getDtNow())
						break
					case "siren":
						atomicState?."${autoType}alarmEvt${evtNum}StartDt" = getDtNow()
						aDev?.siren()
						storeLastAction("Set Alarm SIREN ON", getDtNow())
						break
					case "strobe":
						atomicState?."${autoType}alarmEvt${evtNum}StartDt" = getDtNow()
						aDev?.strobe()
						storeLastAction("Set Alarm STROBE ON", getDtNow())
						break
					default:
						resval = false
						break
				}
			//}
		}
	} catch (ex) {
		log.error "sendEventAlarmAction Exception: ($evtNum) - ", ex
		parent?.sendExceptionData(ex.message, "sendEventAlarmAction", true, getAutoType())
	}
	return resval
}

def alarmAlertEvt(evt) {
	log.trace "alarmAlertEvt: ${evt.displayName} Alarm State is Now (${evt.value})"
}

def getAlert1DelayVal(autoType) { return !settings["${autoType}_Alert_1_Delay"] ? 300 : (settings["${autoType}_Alert_1_Delay"].toInteger()) }
def getAlert2DelayVal(autoType) { return !settings["${autoType}_Alert_2_Delay"] ? 300 : (settings["${autoType}_Alert_2_Delay"].toInteger()) }

def getAlert1AlarmEvtOffVal(autoType) { return !settings["${autoType}_Alert_1_Alarm_Runtime"] ? 10 : (settings["${autoType}_Alert_1_Alarm_Runtime"].toInteger()) }
def getAlert2AlarmEvtOffVal(autoType) { return !settings["${autoType}_Alert_2_Alarm_Runtime"] ? 10 : (settings["${autoType}_Alert_2_Alarm_Runtime"].toInteger()) }

/*
def getAlarmEvt1RuntimeDtSec() { return !atomicState?.alarmEvt1StartDt ? 100000 : GetTimeDiffSeconds(atomicState?.alarmEvt1StartDt).toInteger() }
def getAlarmEvt2RuntimeDtSec() { return !atomicState?.alarmEvt2StartDt ? 100000 : GetTimeDiffSeconds(atomicState?.alarmEvt2StartDt).toInteger() }
*/

void sendTTS(txt, pName) {
	log.trace "sendTTS(data: ${txt})"
	try {
		def msg = txt.toString().replaceAll("\\[|\\]|\\(|\\)|\\'|\\_", "")
		def spks = settings?."${pName}SpeechDevices"
		def meds = settings?."${pName}SpeechMediaPlayer"
		def res = settings?."${pName}SpeechAllowResume"
		def vol = settings?."${pName}SpeechVolumeLevel"
		log.debug "msg: $msg | speaks: $spks | medias: $meds | resume: $res | volume: $vol"
		if(settings?."${pName}AllowSpeechNotif") {
			if(spks) {
				spks*.speak(msg)
			}
			if(meds) {
				meds?.each {
					if(res) {
						def currentStatus = it.latestValue('status')
						def currentTrack = it.latestState("trackData")?.jsonValue
						def currentVolume = it.latestState("level")?.integerValue ? it.currentState("level")?.integerValue : 0
						if(vol) {
							it?.playTextAndResume(msg, vol?.toInteger())
						} else {
							it?.playTextAndResume(msg)
						}
					}
					else {
						it?.playText(msg)
					}
				}
			}
		}
	} catch (ex) {
		log.error "sendTTS Exception:", ex
		parent?.sendExceptionData(ex.message, "sendTTS", true, getAutoType())
	}
}

def scheduleTimeoutRestore(pName) {
	def timeOutVal = settings["${pName}OffTimeout"]?.toInteger()
	if(timeOutVal && !atomicState?."${pName}timeOutScheduled") {
		runIn(timeOutVal.toInteger(), "restoreAfterTimeOut", [data: [pName:pName]])
		LogAction("Mode Restoration Timeout Scheduled for ${pName} (${getEnumValue(longTimeSecEnum(), settings?."${pName}OffTimeout")})", "info", true)
		atomicState."${pName}timeOutScheduled" = true
	}
}

def unschedTimeoutRestore(pName) {
	def timeOutVal = settings["${pName}OffTimeout"]?.toInteger()
	if(timeOutVal && atomicState?."${pName}timeOutScheduled") {
		unschedule("restoreAfterTimeOut")
		LogAction("The Scheduled Mode Restoration Timeout for ${pName} has been cancelled because all Triggers are now clear...", "info", true)
	}
	atomicState."${pName}timeOutScheduled" = false
}

def restoreAfterTimeOut(val) {
	def pName = val?.pName.value
	if(pName && settings?."${pName}OffTimeout") {
		switch(pName) {
			case "conWat":
				atomicState."${pName}timeOutScheduled" = false
				conWatCheck(true)
				break
			case "leakWat":
				//leakWatCheck(true)
				break
			case "extTmp":
				atomicState."${pName}timeOutScheduled" = false
				extTmpTempCheck(true)
				break
			default:
				LogAction("restoreAfterTimeOut no pName match ${pName}", "error", true)
				break
		}
	}
}

def checkThermostatDupe(tstatOne, tstatTwo) {
	def result = false
	if(tstatOne && tstatTwo) {
		def pTstat = tstatOne?.deviceNetworkId.toString()
		def mTstatAr = []
		tstatTwo?.each { ts ->
			mTstatAr << ts?.deviceNetworkId.toString()
		}
		if(pTstat in mTstatAr) { return true }
	}
	return result
}

def checkModeDuplication(modeOne, modeTwo) {
	def result = false
	if(modeOne && modeTwo) {
		 modeOne?.each { dm ->
			if(dm in modeTwo) {
				result = true
			}
		}
	}
	return result
}

private getDeviceSupportedCommands(dev) {
	return dev?.supportedCommands.findAll { it as String }
}

def checkFanSpeedSupport(dev) {
	def req = ["lowSpeed", "medSpeed", "highSpeed"]
	def devCnt = 0
	def devData = getDeviceSupportedCommands(dev)
	devData.each { cmd ->
		if(cmd.name in req) { devCnt = devCnt+1 }
	}
	def speed = dev?.currentValue("currentState") ?: null
	//log.debug "checkFanSpeedSupport (speed: $speed | devCnt: $devCnt)"
	return (speed && devCnt == 3) ? true : false
}

def getTstatCapabilities(tstat, autoType, dyn = false) {
	try {
		def canCool = true
		def canHeat = true
		def hasFan = true
		if(tstat?.currentCanCool) { canCool = tstat?.currentCanCool.toBoolean() }
		if(tstat?.currentCanHeat) { canHeat = tstat?.currentCanHeat.toBoolean() }
		if(tstat?.currentHasFan) { hasFan = tstat?.currentHasFan.toBoolean() }

		atomicState?."${autoType}${dyn ? "_${tstat?.deviceNetworkId}_" : ""}TstatCanCool" = canCool
		atomicState?."${autoType}${dyn ? "_${tstat?.deviceNetworkId}_" : ""}TstatCanHeat" = canHeat
		atomicState?."${autoType}${dyn ? "_${tstat?.deviceNetworkId}_" : ""}TstatHasFan" = hasFan
	} catch (ex) {
		log.error "getTstatCapabilities Exception:", ex
		parent?.sendExceptionData("${tstat} - ${autoType} | ${ex.message}", "getTstatCapabilities", true, getAutoType())
	}
}

def getSafetyTemps(tstat) {
	def minTemp = tstat?.currentState("safetyTempMin")?.doubleValue
	def maxTemp = tstat?.currentState("safetyTempMax")?.doubleValue
	if(minTemp == 0) { minTemp = null }
	if(maxTemp == 0) { maxTemp = null }
	if(minTemp || maxTemp) {
		return ["min":minTemp, "max":maxTemp]
	}
	return null
}

def getComfortHumidity(tstat) {
	def maxHum = tstat?.currentValue("comfortHumidityMax") ?: 0
	if(maxHum) {
		//return ["min":minHumidity, "max":maxHumidity]
		return maxHum
	}
	return null
}

def getComfortDewpoint(tstat) {
	def maxDew = tstat?.currentState("comfortDewpointMax")?.doubleValue ?: 0.0
	if(maxDew) {
		//return ["min":minDew, "max":maxDew]
		return maxDew.toDouble()
	}
	return null
}

def getSafetyTempsOk(tstat) {
	def sTemps = getSafetyTemps(tstat)
	//log.debug "sTempsOk: $sTemps"
	if(sTemps) {
		def curTemp = tstat?.currentTemperature?.toDouble()
		//log.debug "curTemp: ${curTemp}"
		if( ((sTemps?.min.toDouble() != 0) && (curTemp < sTemps?.min.toDouble())) || ((sTemps?.max?.toDouble() != 0) && (curTemp > sTemps?.max?.toDouble())) ) {
			return false
		}
	} // else { log.debug "getSafetyTempsOk: no safety Temps" }
	return true
}

def getGlobalDesiredHeatTemp() {
	return parent?.settings?.locDesiredHeatTemp?.toDouble() ?: null
}

def getGlobalDesiredCoolTemp() {
	return parent?.settings?.locDesiredCoolTemp?.toDouble() ?: null
}

def getClosedContacts(contacts) {
	if(contacts) {
		def cnts = contacts?.findAll { it?.currentContact == "closed" }
		return cnts ?: null
	}
	return null
}

def getOpenContacts(contacts) {
	if(contacts) {
		def cnts = contacts?.findAll { it?.currentContact == "open" }
		return cnts ?: null
	}
	return null
}

def getDryWaterSensors(sensors) {
	if(sensors) {
		def cnts = sensors?.findAll { it?.currentWater == "dry" }
		return cnts ?: null
	}
	return null
}

def getWetWaterSensors(sensors) {
	if(sensors) {
		def cnts = sensors?.findAll { it?.currentWater == "wet" }
		return cnts ?: null
	}
	return null
}

def isContactOpen(con) {
	def res = false
	if(con) {
		if(con?.currentSwitch == "on") { res = true }
	}
	return res
}

def isSwitchOn(dev) {
	def res = false
	if(dev) {
		dev?.each { d ->
			if(d?.currentSwitch == "on") { res = true }
		}
	}
	return res
}

def isPresenceHome(presSensor) {
	def res = false
	if(presSensor) {
		presSensor?.each { d ->
			if(d?.currentPresence == "present") { res = true }
		}
	}
	return res
}

def getTstatPresence(tstat) {
	def pres = "not present"
	if(tstat) { pres = tstat?.currentPresence }
	return pres
}

def setTstatMode(tstat, mode) {
	def result = false
	try {
		if(mode) {
			if(mode == "auto") { tstat.auto(); result = true }
			else if(mode == "heat") { tstat.heat(); result = true }
			else if(mode == "cool") { tstat.cool(); result = true }
			else if(mode == "off") { tstat.off(); result = true }

			if(result) { LogAction("setTstatMode: '${tstat?.label}' Mode has been set to (${mode.toString().toUpperCase()})", "info", false) }
			else { LogAction("setTstatMode() | Invalid or Missing Mode received: ${mode}", "error", true) }
		} else {
			LogAction("setTstatMode() | Invalid or Missing Mode received: ${mode}", "warn", true)
		}
	}
	catch (ex) {
		log.error "setTstatMode() Exception:", ex
		parent?.sendExceptionData(ex.message, "setTstatMode", true, getAutoType())
	}
	return result
}

def setMultipleTstatMode(tstats, mode) {
	def result = false
	try {
		if(tstats && md) {
			tstats?.each { ts ->
				if(setTstatMode(ts, mode)) {
					LogAction("Setting ${ts} Mode to (${mode})", "info", true)
					result = true
				} else {
					return false
				}
			}
		}
	} catch (ex) {
		log.error "setMultipleTstatMode() Exception:", ex
		parent?.sendExceptionData(ex.message, "setMultipleTstatMode", true, getAutoType())
	}
	return result
}

def setTstatAutoTemps(tstat, coolSetpoint, heatSetpoint) {
	LogAction("setTstatAutoTemps: tstat: ${tstat?.displayName}  coolSetpoint: ${coolSetpoint}   heatSetpoint: ${heatSetpoint}°${getTemperatureScale()} ", "info", true)
	def retVal = false
	if(tstat) {
		def hvacMode = tstat?.currentThermostatMode.toString()
		def curCoolSetpoint = getTstatSetpoint(tstat, "cool")
		def curHeatSetpoint = getTstatSetpoint(tstat, "heat")
		def diff = getTemperatureScale() == "C" ? 2.0 : 3.0

		def reqCool =  coolSetpoint?.toDouble() ?: null
		def reqHeat =  heatSetpoint?.toDouble() ?: null

		if(hvacMode in ["auto"]) {
			if(!reqCool && reqHeat) { reqCool = (double) (curCoolSetpoint > (reqHeat + diff)) ? curCoolSetpoint : (reqHeat + diff) }
			if(!reqHeat && reqCool) { reqHeat = (double) (curHeatSetpoint < (reqCool - diff)) ? curHeatSetpoint : (reqCool - diff) }
			if((reqCool && reqHeat) && (reqCool >= (reqHeat + diff))) {
				def heatFirst
				if(reqHeat <= curHeatSetpoint) { heatFirst = true }
					else if(reqCool >= curCoolSetpoint) { heatFirst = false }
					else if(reqHeat > curHeatSetpoint) { heatFirst = false }
					else { heatFirst = true }
				if(heatFirst) {
					LogAction("setTstatAutoTemps() | Setting tstat: ${tstat?.displayName} mode: ${hvacMode} heatSetpoint: ${reqHeat}   coolSetpoint: ${reqCool}°${getTemperatureScale()} ", "info", true)
					if(reqHeat != curHeatSetpoint) { tstat?.setHeatingSetpoint(reqHeat); retVal = true }
					if(reqCool != curCoolSetpoint) { tstat?.setCoolingSetpoint(reqCool); retVal = true }
				} else {
					LogAction("setTstatAutoTemps() | Setting tstat: ${tstat?.displayName} mode: ${hvacMode} coolSetpoint: ${reqCool}   heatSetpoint: ${reqHeat}°${getTemperatureScale()} ", "info", true)
					if(reqCool != curCoolSetpoint) { tstat?.setCoolingSetpoint(reqCool); retVal = true }
					if(reqHeat != curHeatSetpoint) { tstat?.setHeatingSetpoint(reqHeat); retVal = true }
				}
			} else { LogAction("setTstatAutoTemps() | Setting tstat: ${tstat?.displayName} mode: ${hvacMode} missing cool or heat set points ${reqCool} ${reqHeat} or not separated by ${diff}", "info", true) }

		} else if(hvacMode in ["cool"] && reqCool) {
			if(reqCool != curCoolSetpoint) { tstat?.setCoolingSetpoint(reqCool); retVal = true }

		} else if(hvacMode in ["heat"] && reqHeat) {
			if(reqHeat != curHeatSetpoint) { tstat?.setHeatingSetpoint(reqHeat); retVal = true }

		} else { LogAction("setTstatAutoTemps() | thermostat ${tstat?.displayName} mode is not AUTO COOl or HEAT", "info", true) }
	}
	return retVal
}


/******************************************************************************
*					Keep These Methods						  *
*******************************************************************************/
def switchEnumVals() { return [0:"Off", 1:"On", 2:"On/Off"] }

def longTimeMinEnum() {
	def vals = [
		1:"1 Minute", 2:"2 Minutes", 3:"3 Minutes", 4:"4 Minutes", 5:"5 Minutes", 10:"10 Minutes", 15:"15 Minutes", 20:"20 Minutes", 25:"25 Minutes", 30:"30 Minutes",
		45:"45 Minutes", 60:"1 Hour", 120:"2 Hours", 240:"4 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"
	]
	return vals
}

def longTimeSecEnum() {
	def vals = [
		0:"Off", 60:"1 Minute", 120:"2 Minutes", 180:"3 Minutes", 240:"4 Minutes", 300:"5 Minutes", 600:"10 Minutes", 900:"15 Minutes", 1200:"20 Minutes", 1500:"25 Minutes",
		1800:"30 Minutes", 2700:"45 Minutes", 3600:"1 Hour", 7200:"2 Hours", 14400:"4 Hours", 21600:"6 Hours", 43200:"12 Hours", 86400:"24 Hours", 10:"10 Seconds(Testing)"
	]
	return vals
}

def shortTimeEnum() {
	def vals = [
		1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds",
		8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 15:"15 Seconds", 30:"30 Seconds", 60:"60 Seconds"
	]
	return vals
}

def smallTempEnum() {
	def tempUnit = getTemperatureScale()
	def vals = [
		1:"1°${tempUnit}", 2:"2°${tempUnit}", 3:"3°${tempUnit}", 4:"4°${tempUnit}", 5:"5°${tempUnit}", 6:"6°${tempUnit}", 7:"7°${tempUnit}",
		8:"8°${tempUnit}", 9:"9°${tempUnit}", 10:"10°${tempUnit}"
	]
	return vals
}

def switchRunEnum() {
	def pName = schMotPrefix()
	def hasFan = atomicState?."${pName}TstatHasFan" ? true : false
	def vals = [
		1:"Heating/Cooling", 2:"With Fan Only"
	]
	if(!hasFan) {
		vals = [1:"Heating/Cooling"]
	}
	return vals
}

def fanModeTrigEnum() {
	def pName = schMotPrefix()
	def canCool = atomicState?."${pName}TstatCanCool" ? true : false
	def canHeat = atomicState?."${pName}TstatCanHeat" ? true : false
	def hasFan = atomicState?."${pName}TstatHasFan" ? true : false
	def vals = ["auto":"Auto", "cool":"Cool", "heat":"Heat", "any":"Any Mode"]
	if(!canHeat) {
		vals = ["cool":"Cool", "any":"Any Mode"]
	}
	if(!canCool) {
		vals = ["heat":"Heat", "any":"Any Mode"]
	}
	return vals
}

def tModeHvacEnum(canHeat, canCool) {
	def vals = ["auto":"Auto", "cool":"Cool", "heat":"Heat"]
	if(!canHeat) {
		vals = ["cool":"Cool"]
	}
	if(!canCool) {
		vals = ["heat":"Heat"]
	}
	return vals
}

def alarmActionsEnum() {
	def vals = ["siren":"Siren", "strobe":"Strobe", "both":"Both (Siren/Strobe)"]
	return vals
}

def getEnumValue(enumName, inputName) {
	def result = "unknown"
	if(enumName) {
		enumName?.each { item ->
			if(item?.key.toString() == inputName?.toString()) {
				result = item?.value
			}
		}
	}
	return result
}

def getSunTimeState() {
	def tz = TimeZone.getTimeZone(location.timeZone.ID)
	def sunsetTm = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", location?.currentValue('sunsetTime')).format('h:mm a', tz)
	def sunriseTm = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", location?.currentValue('sunriseTime')).format('h:mm a', tz)
	atomicState.sunsetTm = sunsetTm
	atomicState.sunriseTm = sunriseTm
}

def parseDt(format, dt) {
    def result
    def newDt = Date.parse("$format", dt)
    result = formatDt(newDt)
    //log.debug "result: $result"
    return result
}

///////////////////////////////////////////////////////////////////////////////
/******************************************************************************
|				Application Help and License Info Variables		  			  |
*******************************************************************************/
///////////////////////////////////////////////////////////////////////////////
def appName() 		{ return "${parent ? "Nest Automations" : "Nest Manager"}${appDevName()}" }
def appAuthor() 	{ return "Anthony S." }
def appNamespace() 	{ return "tonesto7" }
def gitBranch()     { return "master" }
def betaMarker()    { return false }
def appDevType()    { return false }
def appDevName()    { return appDevType() ? " (Dev)" : "" }
def appInfoDesc() 	{
	def cur = atomicState?.appData?.updater?.versions?.app?.ver.toString()
	def beta = betaMarker() ? "" : ""
	def str = ""
	str += "${textAppName()}"
	str += isAppUpdateAvail() ? "\n• ${textVersion()} (Lastest: v${cur})${beta}" : "\n• ${textVersion()}${beta}"
	str += "\n• ${textModified()}"
	return str
}
def textAppName()   { return "${appName()}" }
def textVersion()   { return "Version: ${appVersion()}" }
def textModified()  { return "Updated: ${appVerDate()}" }
def textAuthor()    { return "${appAuthor()}" }
def textNamespace() { return "${appNamespace()}" }
def textVerInfo()   { return "${appVerInfo()}" }
def textDonateLink(){ return "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=2CJEVN439EAWS" }
def stIdeLink()     { return "https://graph.api.smartthings.com" }
def textCopyright() { return "Copyright© 2016 - Anthony S." }
def textDesc()      { return "This SmartApp is used to integrate you're Nest devices with SmartThings as well as allow you to create child automations triggered by user selected actions..." }
def textHelp()      { return "" }
def textLicense() {
	return "Licensed under the Apache License, Version 2.0 (the 'License'); "+
		"you may not use this file except in compliance with the License. "+
		"You may obtain a copy of the License at"+
		"\n\n"+
		"    http://www.apache.org/licenses/LICENSE-2.0"+
		"\n\n"+
		"Unless required by applicable law or agreed to in writing, software "+
		"distributed under the License is distributed on an 'AS IS' BASIS, "+
		"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
		"See the License for the specific language governing permissions and "+
		"limitations under the License."
}

def askAlexaImgUrl() { return "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/ask-alexa.src/AskAlexa512.png" }
