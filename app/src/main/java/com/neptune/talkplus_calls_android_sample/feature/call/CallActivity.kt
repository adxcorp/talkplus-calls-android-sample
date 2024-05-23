package com.neptune.talkplus_calls_android_sample.feature.call

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neptune.talkplus_calls_android_sample.R
import com.neptune.talkplus_calls_android_sample.background.TPFirebaseMessagingService
import com.neptune.talkplus_calls_android_sample.commons.Constant.TEST_CHANNEL_ID
import com.neptune.talkplus_calls_android_sample.databinding.ActivityCallBinding
import com.neptune.talkplus_calls_android_sample.extensions.closeNotification
import com.neptune.talkplus_calls_android_sample.extensions.intentSerializable
import com.neptune.talkplus_calls_android_sample.extensions.showToast
import com.neptune.talkpluscallsandroid.webrtc.core.TPWebRTCClient
import com.neptune.talkpluscallsandroid.webrtc.event.DirectCallListener
import com.neptune.talkpluscallsandroid.webrtc.event.OnCallResult
import com.neptune.talkpluscallsandroid.webrtc.model.EndCallInfo
import com.neptune.talkpluscallsandroid.webrtc.model.EndCallStatus
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCallParams
import io.talkplus.entity.user.TPNotificationPayload
import io.talkplus.entity.user.TPRtcConfiguration
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection

class CallActivity : AppCompatActivity() {
    private val binding: ActivityCallBinding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val callViewModel: CallViewModel by lazy { ViewModelProvider(this)[CallViewModel::class.java] }
    private var alert: AlertDialog? = null
    private lateinit var tpWebRTCClient: TPWebRTCClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setTalkPlusCallParams()
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

    private fun handleCallUiState(callUiState: CallUiState): Unit = with(callViewModel) {
        when (callUiState) {
            is CallUiState.Login -> enablePushNotification()
            is CallUiState.JoinChannel -> getCallConfiguration()
            is CallUiState.GetCallConfiguration -> setRtcClient(callUiState.tpRtcConfiguration)
            is CallUiState.EnablePush -> getFCMToken()
            is CallUiState.RegisterToken -> joinChannel()
            is CallUiState.Failed -> showToast(callUiState.failResult.toString())
        }
    }

    private fun setClickListener() = with(binding) {
        surfaceLocal.setOnClickListener { makeCall() }
        ivAudio.setOnClickListener { toggleAudio() }
        ivVideo.setOnClickListener { toggleVideo() }
        ivEndCall.setOnClickListener {
            when (callViewModel.isConnected) {
                true -> endCall()
                false -> cancel()
            }
            showToast("상대방과의 통화가 끊어졌습니다.")
            reLoadSurfaceView()
        }
    }

    private fun login() {
        callViewModel.login(
            userId = callViewModel.talkPlusCallParams.callerId,
            userName = callViewModel.talkPlusCallParams.callerId
        )
    }

    private fun setTalkPlusCallParams() = with(intent) {
        when (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            true -> {
                intentSerializable(
                    INTENT_EXTRA_NOTIFICATION_PAYLOAD,
                    TPNotificationPayload::class.java
                )?.let { payload ->
                    callViewModel.setTalkplusCall(
                        TalkPlusCallParams(
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
                    TalkPlusCallParams(
                        callerId = getStringExtra(INTENT_EXTRA_CALLER_ID) ?: "",
                        calleeId = getStringExtra(INTENT_EXTRA_CALLEE_ID) ?: "",
                        channelId = TEST_CHANNEL_ID
                    )
                )
            }
        }
        login()
    }

    private fun startConnect() {
        with(tpWebRTCClient) {
            setLocalVideo(binding.surfaceLocal)
            setRemoteVideo(binding.surfaceRemote)
        }

        when (intent.hasExtra(INTENT_EXTRA_NOTIFICATION_PAYLOAD)) {
            true -> {
                closeNotification()
                acceptCall()
            }
            false -> makeCall()
        }
    }

    private fun showAcceptDialog() {
        alert = AlertDialog.Builder(this)
            .setTitle("통화 요청")
            .setMessage("통화 요청이 왔습니다.")
            .setPositiveButton("수락") { _, _ -> acceptCall() }
            .setNegativeButton("거절") { _, _ ->
                decline()
                reLoadSurfaceView()
            }.create()
    }

    private val directCallListener: DirectCallListener = object : DirectCallListener {
        override fun inComing(talkPlusCallParams: TalkPlusCallParams) {
            tpWebRTCClient.setTalkPlusCallParamsParams(talkPlusCallParams.copy(uuid = talkPlusCallParams.uuid))
            showAcceptDialog()
            alert?.show()
        }

        override fun connected(talkPlusCallParams: TalkPlusCallParams) {
            callViewModel.isConnected = true
            binding.pbLoading.visibility = View.GONE
        }

        override fun ended(endCallInfo: EndCallInfo) {
            callViewModel.isConnected = false
            when (endCallInfo.endReasonCode) {
                EndCallStatus.UNKNOWN.code -> showToast("비정상 종료")
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

        override fun failed(talkPlusCallParams: TalkPlusCallParams) {}
        override fun stateChanged(talkPlusCallParams: TalkPlusCallParams, state: PeerConnection.IceConnectionState) {}
        override fun disConnect(talkPlusCallParams: TalkPlusCallParams) {}
        override fun error(talkPlusCallParams: TalkPlusCallParams, message: String) {}
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

    private fun reLoadSurfaceView() {
        binding.surfaceRemote.apply {
            clearImage()
            release()
        }

        binding.surfaceLocal.apply {
            clearImage()
            release()
        }

        binding.pbLoading.visibility = View.VISIBLE

        tpWebRTCClient.setRemoteVideo(binding.surfaceRemote)
        tpWebRTCClient.setLocalVideo(binding.surfaceLocal)
    }

    private fun setRtcClient(tpRtcConfiguration: TPRtcConfiguration) {
        tpWebRTCClient = TPWebRTCClient(tpRtcConfiguration).apply {
            this.setTalkPlusCallParamsParams(callViewModel.talkPlusCallParams)
            setDirectCallListener(directCallListener)
        }
        startConnect()
    }

    override fun onStop() {
        super.onStop()
        TPFirebaseMessagingService.isCalling = false
    }

    private fun makeCall() {
        tpWebRTCClient.makeCall(callViewModel.talkPlusCallParams, object : OnCallResult {
            override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) { showToast(talkPlusCallParams.toString()) }
            override fun onFailure(reason: String) { showToast(reason) }
        })
    }

    private fun acceptCall() {
        tpWebRTCClient.acceptCall(object : OnCallResult {
            override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) { showToast(talkPlusCallParams.toString()) }
            override fun onFailure(reason: String) { showToast(reason) }
        })
    }

    private fun endCall() {
        tpWebRTCClient.endCall(object : OnCallResult {
            override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) { showToast(talkPlusCallParams.toString()) }
            override fun onFailure(reason: String) { showToast(reason) }
        })
    }

    private fun cancel() {
        tpWebRTCClient.cancel(object : OnCallResult {
            override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) { showToast(talkPlusCallParams.toString()) }
            override fun onFailure(reason: String) { showToast(reason) }
        })
    }

    private fun decline() {
        tpWebRTCClient.decline(object : OnCallResult {
            override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) { showToast(talkPlusCallParams.toString()) }
            override fun onFailure(reason: String) { showToast(reason) }
        })
    }

    companion object {
        private const val TAG = "CallActivity!!"

        const val INTENT_EXTRA_CALLEE_ID = "extra_callee_id"
        const val INTENT_EXTRA_CALLER_ID = "extra_caller_id"
        const val INTENT_EXTRA_NOTIFICATION_PAYLOAD = "extra_notification_payload"
    }
}