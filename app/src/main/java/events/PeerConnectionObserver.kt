package com.neptune.talkpluscallsandroid.webrtc.events;

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver

open class PeerConnectionObserver : PeerConnection.Observer {
    override fun onSignalingChange(signallingState: PeerConnection.SignalingState) { }
    override fun onIceConnectionChange(iceConnectionState : PeerConnection.IceConnectionState) { }
    override fun onIceConnectionReceivingChange(receivingChange: Boolean) { }
    override fun onIceGatheringChange(gatheringChange: PeerConnection.IceGatheringState) { }
    override fun onIceCandidate(iceCandidate: IceCandidate) { }
    override fun onIceCandidatesRemoved(candidateRemoved: Array<IceCandidate>) { }
    override fun onAddStream(mediaStream: MediaStream) { }
    override fun onRemoveStream(removeStream: MediaStream) { }
    override fun onDataChannel(dataChannel: DataChannel) { }
    override fun onRenegotiationNeeded() { }
    override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) { }
}
