package com.neptune.talkplus_calls_android_sample

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
import com.neptune.talkplus_calls_android_sample.extensions.closeNotification
import com.neptune.talkplus_calls_android_sample.extensions.intentSerializable
import com.neptune.talkplus_calls_android_sample.extensions.requirePermission
import com.neptune.talkplus_calls_android_sample.extensions.showToast
import com.neptune.talkpluscallsandroid.webrtc.model.EndCallStatus
import com.neptune.talkpluscallsandroid.webrtc.model.RTCConnectionConfig
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import core.TPWebRTCClient
import events.DirectCallListener
import io.talkplus.TalkPlus
import io.talkplus.entity.user.TPNotificationPayload
import io.talkplus.entity.user.TPRtcConfiguration
import kotlinx.coroutines.launch
import model.EndCallInfo
import org.webrtc.PeerConnection
import java.lang.Exception

class CallActivity : AppCompatActivity() {
    private val binding: ActivityCallBinding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val callViewModel: CallViewModel by lazy { ViewModelProvider(this)[CallViewModel::class.java] }
    private var alert: AlertDialog? = null
    private lateinit var tpWebRTCClient: TPWebRTCClient
    lateinit var rtcConnectionConfig: RTCConnectionConfig

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
            is CallUiState.Login -> callViewModel.enablePushNotification()
            is CallUiState.JoinChannel -> {
                TalkPlus.getWebRtcConfiguration(object : TalkPlus.CallbackListener<TPRtcConfiguration> {
                    override fun onSuccess(tpRtcConfiguration: TPRtcConfiguration) {
                        Log.d(TAG, "getWebRtcConfiguration")
                        val rtcConnectionConfig = RTCConnectionConfig(
                            turnPassword = tpRtcConfiguration.turnPassword,
                            turnUsername = tpRtcConfiguration.turnUsername,
                            stunServerUris = tpRtcConfiguration.stunServerUris,
                            turnServerUris = tpRtcConfiguration.turnServerUris
                        )
                        this@CallActivity.rtcConnectionConfig = rtcConnectionConfig
                        tpWebRTCClient = setTpWebRTCClient()
                        startConnect()
                    }

                    override fun onFailure(p0: Int, p1: Exception?) {
                        Log.d(TAG + "jj", p1?.message.toString())
                    }
                })
            }
            is CallUiState.EnablePush -> callViewModel.getFCMToken()
            is CallUiState.RegisterToken -> callViewModel.joinChannel()
            is CallUiState.Failed -> showToast(callUiState.failResult.toString())
        }
    }

    private fun setClickListener() = with(binding) {
        ivAudio.setOnClickListener { toggleAudio() }
        ivVideo.setOnClickListener { toggleVideo() }
        ivEndCall.setOnClickListener {
            if (callViewModel.isConnected) {
                tpWebRTCClient.endCall()
            } else {
                tpWebRTCClient.cancel()
            }
            reLoadSurfaceView()
            showToast("상대방과의 통화가 끊어졌습니다.")
        }
        surfaceLocal.setOnClickListener {
            tpWebRTCClient.makeCall(callViewModel.talkPlusCall)
        }
    }

    private fun toggleVideo() {
        when (callViewModel.isEnableLocalVideo) {
            true -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_off)
            false -> binding.ivVideo.setBackgroundResource(R.drawable.ic_video_on)
        }
        tpWebRTCClient.enableVideo(!callViewModel.isEnableLocalVideo)
        callViewModel.setLocalVideo(!callViewModel.isEnableLocalVideo)
    }

    private fun toggleAudio() {
        when (callViewModel.isEnableLocalAudio) {
            true -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_on)
            false -> binding.ivAudio.setBackgroundResource(R.drawable.ic_mic_off)
        }
        tpWebRTCClient.enableAudio(!callViewModel.isEnableLocalAudio)
        callViewModel.setLocalAudio(!callViewModel.isEnableLocalAudio)
    }

    private fun checkVideoCallPermissions() {
        when (checkPermissionsGranted(permissions)){
            true -> {
                Log.d(TAG, "checkVideoCallPermissions")
                setTalkPlusCall()
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
                            uuid = payload.uuid,
                            sdp = payload.sdp
                        )
                    )
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
        login()
    }

    private fun login() {
        Log.d(TAG, "start login")
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

    private fun setTpWebRTCClient(): TPWebRTCClient {
        return TPWebRTCClient(
            callViewModel.talkPlusCall,
            directCallListener,
            rtcConnectionConfig
        )
    }

    private fun startConnect() {
        with(tpWebRTCClient) {
            setLocalVideo(binding.surfaceLocal)
            setRemoteVideo(binding.surfaceRemote)
        }

        if (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            closeNotification()
            tpWebRTCClient.acceptCall()
        } else {
            tpWebRTCClient.makeCall(callViewModel.talkPlusCall)
        }
    }

    private fun showAcceptDialog() {
        alert = AlertDialog.Builder(this)
            .setTitle("통화 요청")
            .setMessage("통화 요청이 왔습니다.")
            .setPositiveButton("수락") { _, _ -> tpWebRTCClient.acceptCall() }
            .setNegativeButton("거절") { _, _ ->
                tpWebRTCClient.decline()
                reLoadSurfaceView()
            }
            .create()
    }

    private val directCallListener: DirectCallListener = object : DirectCallListener {
        override fun inComing(talkPlusCall: TalkPlusCall) {
            Handler(Looper.getMainLooper()).postDelayed({
                callViewModel.talkPlusCall = talkPlusCall.copy(uuid = TPFirebaseMessagingService.uuid)
                tpWebRTCClient.talkPlusCall = talkPlusCall.copy(uuid = TPFirebaseMessagingService.uuid)
                Log.d(TAG + "inComing! : ", tpWebRTCClient.talkPlusCall.toString())
                showAcceptDialog()
                alert?.show()
            }, 500)
        }

        // 51ef4b12-43ee-43f5-b23d-cac4f8b3949d
        // c2261fae-9feb-40c5-805f-0ec285a93d5a
        // 2d64a82f-e60d-40fd-81d7-b5ce51db7130


        override fun connected(talkPlusCall: TalkPlusCall) {
            callViewModel.isConnected = true
            binding.pbLoading.visibility = View.GONE
            binding.surfaceRemote.setBackgroundColor(Color.TRANSPARENT)
            setSpeakerPhone()
        }

        override fun ended(endCallInfo: EndCallInfo) {
            callViewModel.isConnected = false
            when (endCallInfo.endReasonCode) {
                EndCallStatus.UNKNOWN.code -> { showToast("비정상 종료")}
                EndCallStatus.COMPLETED.code -> {
                    reLoadSurfaceView()
                    showToast("상대방과의 통화가 끊어졌습니다.")
                }
                EndCallStatus.DECLINED.code -> {
                    reLoadSurfaceView()
                    showToast("callee가 거절")
                }
                EndCallStatus.CANCELED.code -> {
                    reLoadSurfaceView()
                    showToast("caller가 거절")
                    alert?.cancel()
                }
            }
        }

        override fun failed(talkPlusCall: TalkPlusCall) {

        }

        override fun stateChanged(talkPlusCall: TalkPlusCall, state: PeerConnection.IceConnectionState) {

        }

        override fun disConnect(talkPlusCall: TalkPlusCall) {

        }

        override fun error() {

        }
    }

    private fun reLoadSurfaceView() {
        binding.surfaceRemote.apply {
            clearImage()
            release()
            setBackgroundColor(Color.BLACK)
        }

        binding.surfaceLocal.apply {
            clearImage()
            release()
        }

        binding.pbLoading.visibility = View.VISIBLE

        tpWebRTCClient.setRemoteVideo(binding.surfaceRemote)
        tpWebRTCClient.setLocalVideo(binding.surfaceLocal)
    }

    private fun setSpeakerPhone() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onStop() {
        super.onStop()
        TPFirebaseMessagingService.isCalling = false
    }

    override fun onDestroy() {
        super.onDestroy()
        TPFirebaseMessagingService.isCalling = false
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