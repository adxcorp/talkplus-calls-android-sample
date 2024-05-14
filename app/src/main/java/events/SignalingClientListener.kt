package com.neptune.talkpluscallsandroid.webrtc.events

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalingClientListener {
	fun onConnectionEstablished()
	fun onOfferReceived(description: SessionDescription)
	fun onAnswerReceived(description: SessionDescription)
	fun onIceCandidateReceived(iceCandidate: IceCandidate)
	fun onCallEnded(reasonCode: Int, reasonMessage: String)
}

