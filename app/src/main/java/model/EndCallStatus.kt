package com.neptune.talkpluscallsandroid.webrtc.model

enum class EndCallStatus(val code: Int) {
    UNKNOWN(0),
    COMPLETED(1),
    DECLINED(2),
    CANCELED(3);
}
