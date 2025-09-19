package com.darryncampbell.datawedgeflutter

import org.json.JSONObject;

class Scan(val data: String, val symbology: String, val dateTime: String) {
    fun toJson(): String {
        return JSONObject(
            mapOf(
                "scanData" to this.data,
                "symbology" to this.symbology,
                "dateTime" to this.dateTime
            )
        ).toString();
    }
}

