package com.neptune.talkpluscallsandroid.webrtc.events

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalingClientListener {
	fun onConnectionEstablished()
	fun onOfferReceived(description: SessionDescription, uuid: String)
	fun onAnswerReceived(description: SessionDescription, uuid: String)
	fun onIceCandidateReceived(iceCandidate: IceCandidate)
	fun onCallEnded(reasonCode: Int, reasonMessage: String)
}
