package com.neptune.talkpluscallsandroid.webrtc.model

data class RTCConnectionConfig(
    val turnPassword: String,
    val turnUsername: String,
    val turnServerUris: List<String>,
    val stunServerUris: List<String>
) 