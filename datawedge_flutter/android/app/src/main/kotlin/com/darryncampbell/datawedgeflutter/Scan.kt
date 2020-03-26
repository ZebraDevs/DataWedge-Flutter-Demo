package com.darryncampbell.datawedgeflutter

class Scan(val data: String, val symbology: String, val dateTime: String)
{
    fun toJson(): String{
        return "{\"scanData\":\"$data\",\"symbology\":\"$symbology\",\"dateTime\":\"$dateTime\"}"
    }
}

