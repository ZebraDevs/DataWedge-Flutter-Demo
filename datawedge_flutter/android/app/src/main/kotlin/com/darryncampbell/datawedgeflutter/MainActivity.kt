package com.darryncampbell.datawedgeflutter

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

//  This sample implementation is heavily based on the flutter demo at
//  https://github.com/flutter/flutter/blob/master/examples/platform_channel/android/app/src/main/java/com/example/platformchannel/MainActivity.java

class MainActivity : FlutterActivity() {

    private val dwSetup = DWSetup(this)

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        dwSetup.setupHandlers(flutterEngine)
        super.configureFlutterEngine(flutterEngine)
    }
}
