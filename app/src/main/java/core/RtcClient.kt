package com.neptune.talkpluscallsandroid.webrtc.core

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.neptune.talkplus_calls_android_sample.TPFirebaseMessagingService
import com.neptune.talkpluscallsandroid.webrtc.events.PeerConnectionObserver
import com.neptune.talkpluscallsandroid.webrtc.events.SignalingClientListener
import com.neptune.talkpluscallsandroid.webrtc.model.RTCConnectionConfig
import com.neptune.talkpluscallsandroid.webrtc.model.SignalingMessageType
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import com.neptune.talkpluscallsandroid.webrtc.model.WebRTCMessageType
import events.DirectCallListener
import io.talkplus.TalkPlus
import io.talkplus.internal.api.TalkPlusImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import model.EndCallInfo
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * WebRTC 라이브러리의 기본 객체들을 초기화하고, 핸들링 합니다.
 * @author ggone212(정지원)
 */

// TODO : 예외처리 스펙 정하기 -> error, exception, throw


// c527354a-e92a-4082-80f7-4d45844f53e3

internal class RtcClient(
    private val context: Context,
    private var talkplusCall: TalkPlusCall,
    private val rtcConnectionConfig: RTCConnectionConfig,
    private val directCallListener: DirectCallListener
) {
    private var isNotification = true

    init {
        initPeerConnectionFactory()
    }

    fun setTalkPlusCall(talkPlusCall: TalkPlusCall) {
        this.talkplusCall = talkPlusCall
    }

    /**
     * EglBase
     * 렌더링 API와 기본 네이티브 플랫폼 윈도우 시스템 간의 인터페이스 역할을 하는 기술
     * WebRTC에서 주로 비디오 프레임 렌더링에 사용됨.
     * **/

    private val rootEglBase: EglBase = EglBase.create()
    private var type: String = ""

    private var videoCapturer: VideoCapturer? = getVideoCapturer()
    private var peerConnectionFactory: PeerConnectionFactory? = buildPeerConnectionFactory()
    private var peerConnection: PeerConnection? = buildPeerConnection()

    private val signallingClient: SignalingClient = SignalingClient(createSignallingClientListener())
    private var remoteSurfaceView: SurfaceViewRenderer? = null
    private var localSurfaceView: SurfaceViewRenderer? = null

    private var localVideoSource: VideoSource = peerConnectionFactory?.createVideoSource(false) ?: error("failed createVideoSource")
    private var audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints()) ?: error("failed createAudioSource")

    private var localAudioTrack: AudioTrack =
        peerConnectionFactory?.createAudioTrack("local_track" + "_audio", audioSource) ?: error("failed createAudioTrack")

    private var localVideoTrack: VideoTrack =
        peerConnectionFactory?.createVideoTrack("local_track", localVideoSource) ?: error("failed createVideoTrack")

    private val sendCandidate: ArrayList<IceCandidate> = arrayListOf()
    private val receiveCandidate: ArrayList<IceCandidate> = arrayListOf()

    private val mainDispatcher = CoroutineScope(Dispatchers.Main)

    private fun initPeerConnectionFactory() {
        val options: PeerConnectionFactory.InitializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        val peerConnectOptions: PeerConnectionFactory.Options = PeerConnectionFactory.Options().apply {
            disableEncryption = false // true일 경우 암호화 비활성화
            disableNetworkMonitor = true // true일 경우 네트워크 모니터링 비활성화
        }
        return PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(peerConnectOptions)
            .createPeerConnectionFactory()
    }

    private fun buildPeerConnection(): PeerConnection = with(rtcConnectionConfig) {
        Log.d(TAG, "buildPeerConnection")
        val icesServers: ArrayList<PeerConnection.IceServer> = arrayListOf()

        stunServerUris.forEach { stunServerUri ->
            icesServers.add(PeerConnection.IceServer.builder(stunServerUri).createIceServer())
        }

        turnServerUris.forEach { turnServerUri ->
            icesServers.add(
                PeerConnection.IceServer.builder(turnServerUri)
                    .setUsername(turnUsername)
                    .setPassword(turnPassword)
                    .createIceServer()
            )
        }
        return peerConnectionFactory?.createPeerConnection(icesServers, peerConnectionObserver()) ?: error("failed createPeerConnection")
    }

    fun setLocalVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        initSurfaceView(surfaceViewRenderer)
        startLocalVideoCapture(surfaceViewRenderer)
        localSurfaceView = surfaceViewRenderer
        Log.d("view state : ", "setLocalVideo $localSurfaceView")
    }

    fun setRemoteVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        initSurfaceView(surfaceViewRenderer)
        remoteSurfaceView = surfaceViewRenderer
        Log.d("view state : ", "setRemoteVideo $remoteSurfaceView")
    }

    private fun initSurfaceView(surfaceViewRenderer: SurfaceViewRenderer) {
        Log.d(TAG, "initSurfaceView")
        with(surfaceViewRenderer) {
            setMirror(true)
            setEnableHardwareScaler(true)
            init(rootEglBase.eglBaseContext, null)
        }
    }

    private fun startLocalVideoCapture(localVideoOutput: SurfaceViewRenderer) {
        Log.d(TAG, "startLocalVideoCapture")
        val surfaceTextureHelper: SurfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)

        Log.d(TAG, Thread.currentThread().name)

        with(videoCapturer) {
            this!!.initialize(
                surfaceTextureHelper,
                localVideoOutput.context,
                localVideoSource.capturerObserver
            )
            startCapture(640, 480, 60) // 320, 240, 60
        }

        with(peerConnection) {
            this?.addTrack(localVideoTrack)
            this?.addTrack(localAudioTrack)
        }

        localVideoTrack.addSink(localVideoOutput)
    }

    private fun getVideoCapturer(): VideoCapturer {
        val enumerator: Camera2Enumerator = Camera2Enumerator(context)
        val deviceNames: Array<String> = enumerator.deviceNames

        deviceNames.forEach { deviceName ->
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        throw IllegalStateException("Front facing camera not found")
    }

    // createOffer
    fun makeCall(talkplusCall: TalkPlusCall) {
        Log.d(TAG, "createOffer")
        type = "offer"
        val mediaConstraints: MediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        with(peerConnection) {
            this?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription)  {
                    sendOffer(
                        sessionDescription = sessionDescription,
                        talkplusCall = talkplusCall
                    )
                    setLocalDescription(sessionDescription)
                }

                override fun onSetSuccess() { Log.d(TAG, "createOffer onCreateFailure") }
                override fun onCreateFailure(reason: String) { Log.d(TAG, "createOffer onCreateFailure") }
                override fun onSetFailure(reason: String) { Log.d(TAG, "createOffer onSetFailure") }
            }, mediaConstraints)
        }
    }

    private fun sendOffer(
        sessionDescription: SessionDescription,
        talkplusCall: TalkPlusCall
    ) {
        Log.d(TAG, "suspend sendOffer")
        val sendOfferPayload: SignalingMessageType = SignalingMessageType.OfferAnswerPayload(
            type = sessionDescription.type.toString().lowercase(),
            sdp = sessionDescription.description
        )

        val sendOffer: SignalingMessageType = SignalingMessageType.OfferAnswerRequest(
            type = WebRTCMessageType.OFFER.type,
            channelId = talkplusCall.channelId,
            calleeId = talkplusCall.calleeId,
            callerId = talkplusCall.callerId,
            uuid = talkplusCall.uuid,
            payload = sendOfferPayload
        )
        Log.d(TAG, sendOffer.toString())
        TalkPlusImpl.sendMessage(Gson().toJson(sendOffer))
    }

    // createAnswer
    fun acceptCall() {
        type = "answer"
        if (isNotification) {
            signallingClient.offerReceive(
                talkplusCall.sdp,
                talkplusCall.uuid
            )
        }
        Log.d(TAG, "createAnswer")
        val mediaConstraints: MediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        with(peerConnection) {
            this?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sessionDescription: SessionDescription) {
                    Log.d(TAG, "createAnswer onCreateSuccess")
                    sendAnswer(
                        sessionDescription = sessionDescription,
                        talkplusCall = talkplusCall
                    )
                    setLocalDescription(sessionDescription)
                }

                override fun onSetSuccess() { Log.d(TAG, "createAnswer onSetSuccess") }
                override fun onCreateFailure(reason: String) { Log.d(TAG, "createAnswer onCreateFailure $reason") }
                override fun onSetFailure(reason: String) { Log.d(TAG, "createAnswer onSetFailure") }
            }, mediaConstraints)
        }
    }

    private fun sendAnswer(
        sessionDescription: SessionDescription,
        talkplusCall: TalkPlusCall
    ) {
        val sendAnswerPayload: SignalingMessageType = SignalingMessageType.OfferAnswerPayload(
            type = sessionDescription.type.toString().lowercase(),
            sdp = sessionDescription.description
        )

        val sendAnswer: SignalingMessageType = SignalingMessageType.OfferAnswerRequest(
            type = WebRTCMessageType.ANSWER.type,
            channelId = talkplusCall.channelId,
            calleeId = talkplusCall.calleeId,
            callerId = talkplusCall.callerId,
            uuid = talkplusCall.uuid,
            payload = sendAnswerPayload
        )
        TalkPlusImpl.sendMessage(Gson().toJson(sendAnswer))
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) { Log.d(TAG, "onRemoteSessionReceived : $sessionDescription") }
            override fun onSetSuccess() { Log.d(TAG, "onRemoteSessionReceived : onSetSuccess") }
            override fun onCreateFailure(reason: String) { Log.d(TAG, "onRemoteSessionReceived : onCreateFailure $reason") }
            override fun onSetFailure(reason: String) { Log.d(TAG, "onRemoteSessionReceived : onSetFailure $reason") }
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate) {
        peerConnection?.let { peer ->
            if (!peer.addIceCandidate(iceCandidate)) {
                receiveCandidate.add(iceCandidate)
            }
        } ?: error("peerConnection is null")
    }

    fun enableVideo(videoEnabled: Boolean) {
        localVideoTrack.setEnabled(videoEnabled)
    }

    fun enableAudio(audioEnabled: Boolean) {
        localAudioTrack.setEnabled(audioEnabled)
    }

    // 내가 종료한 경우
    fun endCall(
        talkplusCall: TalkPlusCall,
        endReasonCode: Int,
        endReasonMessage: String
    ) {
        deAllocation()
        allocation()
        val endCallRequest: SignalingMessageType = SignalingMessageType.EndCallRequest(
            type = WebRTCMessageType.END_CALL.type,
            channelId = talkplusCall.channelId,
            calleeId = talkplusCall.calleeId,
            callerId = talkplusCall.callerId,
            uuid = talkplusCall.uuid,
            endReasonCode = endReasonCode,
            endReasonMessage = endReasonMessage
        )
        type = ""
        TalkPlusImpl.sendMessage(Gson().toJson(endCallRequest))
    }

    private fun deAllocation() {
        val receiveCandidates: Array<IceCandidate?> = arrayOfNulls(receiveCandidate.size)
        receiveCandidate.forEachIndexed { index, candidate -> receiveCandidates[index] = candidate }
        peerConnection?.removeIceCandidates(receiveCandidates)
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory = null
        videoCapturer = null
        remoteSurfaceView = null
        receiveCandidate.clear()
    }

    private fun allocation() {
        initPeerConnectionFactory()
        videoCapturer = getVideoCapturer()
        peerConnectionFactory = buildPeerConnectionFactory()
        peerConnection = buildPeerConnection()
        localVideoSource = peerConnectionFactory?.createVideoSource(false) ?: error("failed createVideoSource")
        audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints()) ?: error("failed createAudioSource")
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_track" + "_audio", audioSource) ?: error("failed createAudioTrack")
        localVideoTrack = peerConnectionFactory?.createVideoTrack("local_track", localVideoSource) ?: error("failed createVideoTrack")
    }

    private fun PeerConnection.setLocalDescription(sessionDescription: SessionDescription) {
        Log.d(TAG, "suspend setLocalDescription")
        setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) { Log.d(TAG, "setLocalDescription onCreateSuccess") }
            override fun onSetSuccess() { Log.d(TAG, "setLocalDescription onSetSuccess") }
            override fun onCreateFailure(reason: String) { Log.d(TAG, "offer setLocalDescription onCreateFailure") }
            override fun onSetFailure(reason: String) { Log.d(TAG, "offer setLocalDescription onSetFailure") }
        }, sessionDescription)
    }

    private fun createSignallingClientListener(): SignalingClientListener {
        return object : SignalingClientListener {
            override fun onConnectionEstablished() {
                Log.d(TAG, "onConnectionEstablished")
            }

            override fun onOfferReceived(description: SessionDescription, uuid: String) {
                Log.d(TAG, "onOfferReceived")
                onRemoteSessionReceived(description)

                // current : false
                if (!isNotification) {
                    mainDispatcher.launch {
                        directCallListener.inComing(
                            talkplusCall.copy(
                                sdp = description.description,
                                uuid = uuid
                            ))
                    }
                }
                isNotification = false
            }

            override fun onAnswerReceived(description: SessionDescription, uuid: String) {
                Log.d(TAG, "onAnswerReceived")
                onRemoteSessionReceived(description)
                sendCandidate.forEach { iceCandidate ->
                    signallingClient.sendIceCandidate(
                        candidate = iceCandidate,
                        targetUserId = talkplusCall.calleeId,
                        channelId = talkplusCall.channelId,
                        uuid = uuid
                    )
                }
            }

            override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
                Log.d(TAG, "onIceCandidateReceived : $iceCandidate")
                receiveCandidate.add(iceCandidate)
                addIceCandidate(iceCandidate)
            }

            override fun onCallEnded(
                reasonCode: Int,
                reasonMessage: String
            ) {
                val endCallInfo: EndCallInfo = EndCallInfo(
                    channelId = talkplusCall.channelId,
                    callerId = talkplusCall.callerId,
                    calleeId = talkplusCall.calleeId,
                    endReasonCode = reasonCode,
                    endReasonMessage = reasonMessage
                )
                CoroutineScope(Dispatchers.Main).launch {
                    directCallListener.ended(endCallInfo)
                }
                deAllocation()
                allocation()
            }
        }
    }

    private fun peerConnectionObserver(): PeerConnectionObserver = object : PeerConnectionObserver() {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            when (type) {
                "offer" -> sendCandidate.add(iceCandidate)
                "answer" -> signallingClient.sendIceCandidate(
                    candidate = iceCandidate,
                    targetUserId = talkplusCall.callerId,
                    channelId = talkplusCall.channelId,
                    uuid = talkplusCall.uuid
                )
            }
        }

        override fun onIceGatheringChange(gatheringChange: PeerConnection.IceGatheringState) {
            Log.d(TAG, "onIceGatheringChange ${gatheringChange.name}")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
            super.onAddTrack(rtpReceiver, mediaStreams)
            Log.d(TAG, mediaStreams[0].videoTracks.size.toString())

            Log.d(TAG + "view state : ", remoteSurfaceView.toString())

            if (mediaStreams[0].videoTracks.size != 0) {
                mediaStreams[0].videoTracks[0].addSink(remoteSurfaceView)
            }
        }

        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
            Log.d(TAG, iceConnectionState.name.toString())
            when (iceConnectionState) {
                PeerConnection.IceConnectionState.CONNECTED -> mainDispatcher.launch { directCallListener.connected(talkplusCall) }
                PeerConnection.IceConnectionState.FAILED -> mainDispatcher.launch { directCallListener.failed(talkplusCall) }
                PeerConnection.IceConnectionState.DISCONNECTED -> mainDispatcher.launch { directCallListener.disConnect(talkplusCall) }
                else -> mainDispatcher.launch { directCallListener.stateChanged(talkplusCall, iceConnectionState) }
            }
        }
    }

    companion object {
        private const val TAG = "RTCClient!!"
    }
}