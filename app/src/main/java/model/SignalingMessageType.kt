package com.neptune.talkpluscallsandroid.webrtc.model

// TODO 파라미터 클래스로 묶기

sealed class SignalingMessageType {
	data class OfferAnswerPayload(
		val type: Any,
		val sdp: String
	) : SignalingMessageType()

	data class CandidatePayload(val candidate: String, val sdpMid: String, val sdpMLineIndex: Int) : SignalingMessageType()

	data class OfferAnswerRequest(
		private val type: String = WebRTCMessageType.OFFER.type,
		private val channelId: String,
		private val calleeId: String,
		private val callerId: String,
		private val uuid: String,
		private val payload: SignalingMessageType
	) : SignalingMessageType()

	data class EndCallRequest(
		private val type: String,
		private val channelId: String,
		private val calleeId: String,
		private val callerId: String,
		private val uuid: String,
		private val endReasonCode: Int,
		private val endReasonMessage: String
	) : SignalingMessageType()

	data class IceCandidateRequest(
		private val type: String = WebRTCMessageType.CANDIDATE.type,
		private val channelId: String,
		private val calleeId: String,
		private val uuid: String,
		private val payload: SignalingMessageType
	) : SignalingMessageType()
}