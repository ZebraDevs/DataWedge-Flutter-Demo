package com.darryncampbell.datawedgeflutter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar

//  This sample implementation is heavily based on the flutter demo at
//  https://github.com/flutter/flutter/blob/master/examples/platform_channel/android/app/src/main/java/com/example/platformchannel/MainActivity.java

class DWSetup(private val flutterActivity: FlutterActivity) {
    private val COMMAND_CHANNEL = "com.darryncampbell.datawedgeflutter/command"
    private val SCAN_CHANNEL = "com.darryncampbell.datawedgeflutter/scan"
    private val PROFILE_INTENT_ACTION = "com.darryncampbell.datawedgeflutter.SCAN"
    private val PROFILE_INTENT_BROADCAST = "2"

    private val dwInterface = DWInterface()

    private val LOGTAG = "DWSetup"

    fun setupHandlers(flutterEngine: FlutterEngine) {
        Log.i(LOGTAG, "setupHandlers setStreamHandler")
        EventChannel(flutterEngine.dartExecutor, SCAN_CHANNEL).setStreamHandler(
            object : StreamHandler {
                private var dataWedgeBroadcastReceiver: BroadcastReceiver? = null
                override fun onListen(arguments: Any?, events: EventSink?) {
                    dataWedgeBroadcastReceiver = createDataWedgeBroadcastReceiver(events)
                    val intentFilter = IntentFilter()
                    intentFilter.addAction(PROFILE_INTENT_ACTION)
                    intentFilter.addAction(DWInterface.DATAWEDGE_RETURN_ACTION)
                    intentFilter.addCategory(DWInterface.DATAWEDGE_RETURN_CATEGORY)
                    try {
                        flutterActivity.registerReceiver(
                            dataWedgeBroadcastReceiver, intentFilter, RECEIVER_EXPORTED
                        )
                    } catch (e: Exception) {
                        Log.i(LOGTAG, "Unable to register receiver $e")
                    }
                }

                override fun onCancel(arguments: Any?) {
                    flutterActivity.unregisterReceiver(dataWedgeBroadcastReceiver)
                    dataWedgeBroadcastReceiver = null
                }
            }
        )

        Log.i(LOGTAG, "setupHandlers setMethodCallHandler")
        MethodChannel(
            flutterEngine.dartExecutor,
            COMMAND_CHANNEL
        ).setMethodCallHandler { call, result ->
            Log.i(LOGTAG, "setMethodCallHandler " + call.method)
            if (call.method == "sendDataWedgeCommandStringParameter") {
                val arguments = JSONObject(call.arguments.toString())
                val command: String = arguments.get("command") as String
                val parameter: String = arguments.get("parameter") as String
                dwInterface.sendCommandString(
                    flutterActivity.applicationContext,
                    command,
                    parameter
                )
                //  result.success(0);  //  DataWedge does not return responses
            } else if (call.method == "createDataWedgeProfile") {
                createDataWedgeProfile(call.arguments.toString())
            } else {
                result.notImplemented()
            }
        }
        Log.i(LOGTAG, "setupHandlers completed")
    }

    private fun createDataWedgeBroadcastReceiver(events: EventSink?): BroadcastReceiver {
        Log.i(LOGTAG, "createDataWedgeBroadcastReceiver")
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.equals(PROFILE_INTENT_ACTION)) {
                    //  A barcode has been scanned
                    val scanData =
                        intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING)
                    val symbology =
                        intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_LABEL_TYPE)
                    val date = Calendar.getInstance().time
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val dateTimeString = df.format(date)
                    val currentScan =
                        Scan(scanData.toString(), symbology.toString(), dateTimeString)
                    events?.success(currentScan.toJson())
                }
                //  Could handle return values from DW here such as RETURN_GET_ACTIVE_PROFILE
                //  or RETURN_ENUMERATE_SCANNERS
            }
        }
    }

    private fun createDataWedgeProfile(profileName: String) {
        Log.i(LOGTAG, "createDataWedgeProfile " + profileName)
        //  Create and configure the DataWedge profile associated with this application
        //  For readability's sake, I have not defined each of the keys in the DWInterface file
        dwInterface.sendCommandString(
            flutterActivity,
            DWInterface.DATAWEDGE_SEND_CREATE_PROFILE,
            profileName
        )
        val profileConfig = Bundle()
        profileConfig.putString("PROFILE_NAME", profileName)
        profileConfig.putString("PROFILE_ENABLED", "true") //  These are all strings
        profileConfig.putString("CONFIG_MODE", "UPDATE")
        val barcodeConfig = Bundle()
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
        barcodeConfig.putString(
            "RESET_CONFIG",
            "true"
        ) //  This is the default but never hurts to specify
        val barcodeProps = Bundle()
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps)
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig)
        val appConfig = Bundle()
        appConfig.putString(
            "PACKAGE_NAME",
            flutterActivity.packageName
        )      //  Associate the profile with this app
        appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))
        profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig))
        dwInterface.sendCommandBundle(
            flutterActivity,
            DWInterface.DATAWEDGE_SEND_SET_CONFIG,
            profileConfig
        )
        //  You can only configure one plugin at a time in some versions of DW, now do the intent output
        profileConfig.remove("PLUGIN_CONFIG")
        val intentConfig = Bundle()
        intentConfig.putString("PLUGIN_NAME", "INTENT")
        intentConfig.putString("RESET_CONFIG", "true")
        val intentProps = Bundle()
        intentProps.putString("intent_output_enabled", "true")
        intentProps.putString("intent_action", PROFILE_INTENT_ACTION)
        intentProps.putString("intent_delivery", PROFILE_INTENT_BROADCAST)  //  "2"
        intentConfig.putBundle("PARAM_LIST", intentProps)
        profileConfig.putBundle("PLUGIN_CONFIG", intentConfig)
        dwInterface.sendCommandBundle(
            flutterActivity,
            DWInterface.DATAWEDGE_SEND_SET_CONFIG,
            profileConfig
        )
    }
}
