package com.neptune.talkpluscallsandroid.webrtc.model

enum class WebRTCMessageType(val type: String) {
    ANSWER("answer"),
    OFFER("offer"),
    CANDIDATE("candidate"),
    END_CALL("endCall")
}