package com.neptune.talkpluscallsandroid.webrtc.core

import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.neptune.talkpluscallsandroid.webrtc.events.SignalingClientListener
import com.neptune.talkpluscallsandroid.webrtc.model.EndCallStatus
import com.neptune.talkpluscallsandroid.webrtc.model.SignalingMessageType
import com.neptune.talkpluscallsandroid.webrtc.model.WebRTCMessageType
import events.DirectCallListener
import io.talkplus.internal.api.TalkPlusImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

//

class SignalingClient(
    private val signalingClientListener: SignalingClientListener
) {
    init {
        connect()
    }
     fun connect() {
        Log.d(TAG, "start connect")
        TalkPlusImpl.sendType.observe(ProcessLifecycleOwner.get()) { payload: Map<String, Any> ->
            when (payload["type"]) {
                WebRTCMessageType.OFFER.type -> {
                    offerReceive(
                        payload["sessionDescription"].toString(),
                        payload["uuid"].toString()
                    )
                }

                WebRTCMessageType.ANSWER.type -> {
                    answerReceive(
                        payload["sessionDescription"].toString(),
                        payload["uuid"].toString()
                    )
                }

                WebRTCMessageType.END_CALL.type -> handleReasonCode(
                    reasonCode = payload["endReasonCode"].toString().toInt(),
                    reasonMessage = payload["endReasonMessage"].toString()
                )

                WebRTCMessageType.CANDIDATE.type -> {
                    val iceCandidate: IceCandidate = IceCandidate(
                        payload["sdpMid"].toString(),
                        payload["sdpMLineIndex"].toString().toInt(),
                        payload["candidate"].toString()
                    )
                    iceCandidateReceive(iceCandidate)
                }
            }
        }
    }

    fun offerReceive(sessionDescription: String, uuid: String) {
        Log.d(TAG, "offerReceive $uuid")
        signalingClientListener.onOfferReceived(
            SessionDescription(
                SessionDescription.Type.OFFER,
                sessionDescription
            ), uuid
        )
    }

    private fun answerReceive(sessionDescription: String, uuid: String) {
        Log.d(TAG, "answerReceive $uuid")
        signalingClientListener.onAnswerReceived(
            SessionDescription(
                SessionDescription.Type.ANSWER,
                sessionDescription
            ), uuid
        )
    }

    private fun iceCandidateReceive(iceCandidate: IceCandidate) {
        signalingClientListener.onIceCandidateReceived(iceCandidate)
    }

    private fun handleReasonCode(
        reasonCode: Int,
        reasonMessage: String
    ) {
        signalingClientListener.onCallEnded(
            reasonCode = reasonCode,
            reasonMessage = reasonMessage
        )
    }

     fun sendIceCandidate(
        candidate: IceCandidate,
        targetUserId: String,
        channelId: String,
        uuid: String
    ) {
        val iceCandidatePayload = SignalingMessageType.CandidatePayload(
            candidate.sdp,
            candidate.sdpMid,
            candidate.sdpMLineIndex
        )

        val iceCandidateRequest = SignalingMessageType.IceCandidateRequest(
            WebRTCMessageType.CANDIDATE.type,
            channelId,
            targetUserId,
            uuid,
            iceCandidatePayload
        )
        TalkPlusImpl.sendMessage(Gson().toJson(iceCandidateRequest))
    }

    // ced30560-b557-4482-bbc0-0f816785107f


    companion object {
        private const val TAG = "SignallingClient!!"
    }
}