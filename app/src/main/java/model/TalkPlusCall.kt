package com.neptune.talkpluscallsandroid.webrtc.model

import java.util.UUID

data class TalkPlusCall(
    val callerId: String = "",
    val calleeId: String = "",
    val channelId: String = "",
    val uuid: String = UUID.randomUUID().toString(),
    val sdp: String = ""
)