package com.neptune.talkplus_calls_android_sample

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.neptune.talkplus_calls_android_sample.data.model.base.Result
import com.neptune.talkplus_calls_android_sample.data.repository.auth.AuthenticationRepository
import com.neptune.talkpluscallsandroid.webrtc.model.RTCConnectionConfig
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import io.talkplus.entity.user.TPNotificationPayload
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
private const val TAG = "CallViewModel!!"

class CallViewModel(
    private val authRepository: AuthenticationRepository = AuthenticationRepository()
) : ViewModel() {
    var isEnableLocalAudio: Boolean = true
        private set

    var isEnableLocalVideo: Boolean = true
        private set

    var sdp: String = ""
        private set

    lateinit var talkPlusCall: TalkPlusCall
        private set

    lateinit var connectionConfig: RTCConnectionConfig
        private set

    private var _callState = MutableSharedFlow<CallUiState>()
    val callState: SharedFlow<CallUiState>
        get() = _callState.asSharedFlow()

    fun login(
        userId: String,
        userName: String
    ) {
        val params: TPLoginParams = TPLoginParams.Builder(userId, TPLoginParams.LoginType.ANONYMOUS)
            .setUserName(userName)
            .build()

        viewModelScope.launch {
            authRepository.login(params).collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> {
                        _callState.emit(CallUiState.Login(callbackResult.successData))
                        setConnectionConfig()
                    }
                    is Result.Failure -> _callState.emit(CallUiState.Failed(callbackResult.failResult))
                }
            }
        }
    }

    fun joinChannel() {
        viewModelScope.launch {
            authRepository.joinChannel().collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> _callState.emit(CallUiState.JoinChannel(callbackResult.successData))
                    is Result.Failure -> _callState.emit(CallUiState.Failed(callbackResult.failResult))
                }
            }
        }
    }

    fun enablePushNotification() {
        viewModelScope.launch {
            authRepository.enablePushNotification().collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> _callState.emit(CallUiState.EnablePush(callbackResult.successData))
                    is Result.Failure -> _callState.emit(CallUiState.Failed(callbackResult.failResult))
                }
            }
        }
    }

    private fun registerFcmToken(fcmToken: String) {
        viewModelScope.launch {
            authRepository.registerFcmToken(fcmToken).collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> _callState.emit(CallUiState.RegisterToken)
                    is Result.Failure -> _callState.emit(CallUiState.Failed(callbackResult.failResult))
                }
            }
        }
    }

    private fun setConnectionConfig() {
        viewModelScope.launch {
            authRepository.getConnectionConfig().collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> {
                        val tpRtcConfiguration = callbackResult.successData
                        connectionConfig = RTCConnectionConfig(
                            turnServerUris = tpRtcConfiguration.turnServerUris,
                            stunServerUris = tpRtcConfiguration.stunServerUris,
                            turnPassword = tpRtcConfiguration.turnPassword,
                            turnUsername = tpRtcConfiguration.turnUsername
                        )
                    }
                    is Result.Failure -> _callState.emit(CallUiState.Failed(callbackResult.failResult))
                }
            }
        }
    }

    fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            registerFcmToken(task.result)
            return@OnCompleteListener
        })
    }

    fun setLocalAudio(isEnable: Boolean) { isEnableLocalAudio = isEnable }
    fun setLocalVideo(isEnable: Boolean) { isEnableLocalVideo = isEnable }
    fun setTalkplusCall(talkPlusCall: TalkPlusCall) { this.talkPlusCall = talkPlusCall }
    fun setSdp(sdp: String) { this.sdp = sdp }
}