package com.neptune.talkplus_calls_android_sample

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neptune.talkplus_calls_android_sample.Constant.TEST_CHANNEL_ID
import com.neptune.talkplus_calls_android_sample.databinding.ActivityCallBinding
import com.neptune.talkplus_calls_android_sample.extensions.checkPermissionsGranted
import com.neptune.talkplus_calls_android_sample.extensions.intentSerializable
import com.neptune.talkplus_calls_android_sample.extensions.requirePermission
import com.neptune.talkplus_calls_android_sample.extensions.showToast
import com.neptune.talkpluscallsandroid.webrtc.core.RtcClient
import com.neptune.talkpluscallsandroid.webrtc.core.SignalingClient
import com.neptune.talkpluscallsandroid.webrtc.events.PeerConnectionObserver
import com.neptune.talkpluscallsandroid.webrtc.events.SignalingClientListener
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import io.talkplus.entity.user.TPNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity() {
    private val binding: ActivityCallBinding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val rtcClient: RtcClient by lazy { setRtcClient() }
    private val callViewModel: CallViewModel by lazy { ViewModelProvider(this)[CallViewModel::class.java] }
    private val signallingClient: SignalingClient by lazy { SignalingClient(createSignallingClientListener()) }
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var sdp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        checkVideoCallPermissions()
        observeCallUiState()
        setClickListener()
    }

    private fun observeCallUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                callViewModel.callState.collect { callUiState ->
                    handleCallUiState(callUiState)
                }
            }
        }
    }

    private fun handleCallUiState(callUiState: CallUiState) {
        when (callUiState) {
            // auth
            is CallUiState.Login -> callViewModel.enablePushNotification()
            is CallUiState.JoinChannel -> startConnect()
            is CallUiState.EnablePush -> callViewModel.getFCMToken()
            is CallUiState.RegisterToken -> callViewModel.joinChannel()
            is CallUiState.Failed -> {
                Log.d(TAG, callUiState.failResult.toString())
                showToast(callUiState.failResult.toString())
            }

            // call

        }
    }

    private fun setClickListener() = with(binding) {
        ivAudio.setOnClickListener { toggleAudio() }
        ivVideo.setOnClickListener { toggleVideo() }
        ivEndCall.setOnClickListener {
            rtcClient.endCall(
                talkplusCall = callViewModel.talkPlusCall,
                endReasonCode = 3,
                endReasonMessage = "call canceled by caller"
            )
        }
        btnOffer.setOnClickListener {
            rtcClient.allocation()
            rtcClient.makeCall(callViewModel.talkPlusCall)
        }
        btnAnswer.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                offerReceive(sdp)
                acceptCall(callViewModel.talkPlusCall)
            }
        }
    }

    private fun toggleVideo() {
        when (callViewModel.isEnableLocalVideo) {
            true -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_off)
            false -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_on)
        }
        callViewModel.setLocalVideo(!callViewModel.isEnableLocalVideo)
    }

    private fun toggleAudio() {
        when (callViewModel.isEnableLocalAudio) {
            true -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_on)
            false -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_off)
        }
        callViewModel.setLocalAudio(!callViewModel.isEnableLocalAudio)
    }

    private fun checkVideoCallPermissions() {
        when (checkPermissionsGranted(permissions)){
            true -> {
                setTalkPlusCall()
                login()
            }
            false -> requestVideoCallPermission()
        }
    }

    private fun setTalkPlusCall() = with(intent) {
        when (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            true -> {
                intentSerializable(
                    INTENT_EXTRA_NOTIFICATION_PAYLOAD,
                    TPNotificationPayload::class.java
                )?.let { payload ->
                    callViewModel.setTalkplusCall(
                        TalkPlusCall(
                            callerId = payload.callerId,
                            calleeId = payload.calleeId,
                            channelId = payload.channelId,
                            uuid = payload.uuid
                        )
                    )
                    callViewModel.setSdp(payload.sdp)
                } ?: showToast("payload is null")
            }
            false -> {
                callViewModel.setTalkplusCall(
                    TalkPlusCall(
                        callerId = getStringExtra(INTENT_EXTRA_CALLER_ID) ?: "",
                        calleeId = getStringExtra(INTENT_EXTRA_CALLEE_ID) ?: "",
                        channelId = TEST_CHANNEL_ID
                    )
                )
            }
        }
    }

    private fun login() {
        callViewModel.login(
            userId = callViewModel.talkPlusCall.callerId,
            userName = callViewModel.talkPlusCall.callerId
        )
    }

    private fun requestVideoCallPermission(dialogShown: Boolean = false) {
        when (requirePermission(permissions) && !dialogShown) {
            true -> showPermissionDialog()
            false -> requestPermission()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("카메라, 오디오 권한 요청")
            .setMessage("영상통화를 위해 카메라, 오디오 기능이 필요합니다.")
            .setPositiveButton("수락") { dialog, _ ->
                dialog.dismiss()
                requestVideoCallPermission(true)
            }
            .setNegativeButton("거절") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "카메라, 오디오 권한이 거절됐습니다.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, CAMERA_AUDIO_PERMISSION_REQUEST_CODE)
    }

    private fun setRtcClient(): RtcClient  {
        return RtcClient(
            this@CallActivity,
            peerConnectionObserver(),
            callViewModel.connectionConfig
        )
    }

    private fun peerConnectionObserver(): PeerConnectionObserver = object : PeerConnectionObserver() {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            // offer, answer로 분기
            when (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
                true -> signallingClient.sendIceCandidate(
                    candidate = iceCandidate,
                    targetUserId = callViewModel.talkPlusCall.callerId,
                    channelId = TEST_CHANNEL_ID,
                    uuid = callViewModel.talkPlusCall.uuid
                )
                false -> rtcClient.sendCandidate.add(iceCandidate)
            }
        }

        override fun onIceGatheringChange(gatheringChange: PeerConnection.IceGatheringState) {
            Log.d(TAG, "onIceGatheringChange ${gatheringChange.name}")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
            super.onAddTrack(rtpReceiver, mediaStreams)
            if (mediaStreams[0].videoTracks.size != 0) {
                mediaStreams[0].videoTracks[0].addSink(binding.surfaceRemote)
            }
        }

        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
            Log.d(TAG, iceConnectionState.name.toString())
            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                setSpeakerPhone()
            }
        }
    }

    private fun createSignallingClientListener(): SignalingClientListener {
        return object : SignalingClientListener {
            override fun onConnectionEstablished() {
                Log.d(TAG, "onConnectionEstablished")
            }

            override fun onOfferReceived(description: SessionDescription) {
                Log.d(TAG, "onOfferReceived")
                rtcClient.onRemoteSessionReceived(description)
                sdp = description.description
            }

            override fun onAnswerReceived(description: SessionDescription) {
                Log.d(TAG, "onAnswerReceived")
                rtcClient.onRemoteSessionReceived(description)
                rtcClient.sendCandidate.forEach { iceCandidate ->
                    signallingClient.sendIceCandidate(
                        candidate = iceCandidate,
                        targetUserId = callViewModel.talkPlusCall.calleeId,
                        channelId = TEST_CHANNEL_ID,
                        uuid = callViewModel.talkPlusCall.uuid
                    )
                }
            }

            override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
                Log.d(TAG, "onIceCandidateReceived : $iceCandidate")
                rtcClient.receiveCandidate.add(iceCandidate)
                rtcClient.addIceCandidate(iceCandidate)
            }

            override fun onCallEnded() {
                finish()
            }

            override fun onCallCanceled() {
                Log.d(TAG, "onCallCanceled")
                rtcClient.deAllocation()
                rtcClient.allocation()
            }

            override fun onCallDeclined() {
                finish()
            }
        }
    }

    private fun startConnect() {
        Log.d(TAG, "makeCall()")
        signallingClient.connect()
        with(rtcClient) {
            initSurfaceView(binding.surfaceRemote)
            initSurfaceView(binding.surfaceLocal)
            startLocalVideoCapture(binding.surfaceLocal)
        }

        if (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            // TODO 확장함수 호ㅏ
            val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(TPFirebaseMessagingService.NOTIFICATION_ID)
            with(callViewModel) {
                // TODO 추상화
                CoroutineScope(Dispatchers.IO).launch {
                    offerReceive(sdp)
                    acceptCall(talkPlusCall)
                }
            }
        } else {
            rtcClient.makeCall(callViewModel.talkPlusCall)
        }
    }

    private suspend fun offerReceive(sdp: String) {
        signallingClient.offerReceive(sdp)
    }

    private suspend fun acceptCall(talkPlusCall: TalkPlusCall) {
        rtcClient.acceptCall(talkPlusCall)
    }

    private fun setSpeakerPhone() {
        audioManager.isSpeakerphoneOn = true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    companion object {
        private const val TAG = "CallActivity!!"

        const val INTENT_EXTRA_CALLEE_ID = "extra_callee_id"
        const val INTENT_EXTRA_CALLER_ID = "extra_caller_id"
        const val INTENT_EXTRA_NOTIFICATION_PAYLOAD = "extra_notification_payload"

        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        private const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

        private const val CAMERA_AUDIO_PERMISSION_REQUEST_CODE = 1

        private val permissions: Array<String> = arrayOf(
            CAMERA_PERMISSION,
            AUDIO_PERMISSION,
            NOTIFICATION_PERMISSION
        )
    }
}