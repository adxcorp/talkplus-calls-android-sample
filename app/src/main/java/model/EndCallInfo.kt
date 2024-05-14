package model

data class EndCallInfo(
    val channelId: String,
    val callerId: String,
    val calleeId: String,
    val endReasonCode: Int,
    val endReasonMessage: String
)