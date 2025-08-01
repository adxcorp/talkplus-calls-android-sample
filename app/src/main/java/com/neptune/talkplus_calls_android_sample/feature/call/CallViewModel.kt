package com.neptune.talkplus_calls_android_sample.feature.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.neptune.talkplus_calls_android_sample.data.model.base.Result
import com.neptune.talkplus_calls_android_sample.data.repository.auth.AuthenticationRepository
import com.neptune.talkpluscallsandroid.webrtc.model.KlatCallParam
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CallViewModel(private val authRepository: AuthenticationRepository = AuthenticationRepository()) : ViewModel() {
    var isEnableLocalAudio: Boolean = true
        private set

    var isEnableLocalVideo: Boolean = true
        private set

    private var _callState = MutableSharedFlow<CallUiState>()
    val callState: SharedFlow<CallUiState>
        get() = _callState.asSharedFlow()

    var isConnected = false

    fun login() {
        val (id, userName) = if (KlatCallParam.talkPlusCallParams.sdp.isEmpty()) {
            KlatCallParam.talkPlusCallParams.callerId to KlatCallParam.talkPlusCallParams.callerId
        } else {
            KlatCallParam.talkPlusCallParams.calleeId to KlatCallParam.talkPlusCallParams.calleeId
        }

        val params: TPLoginParams = TPLoginParams.Builder(id, TPLoginParams.LoginType.ANONYMOUS)
            .setUserName(userName)
            .build()

        viewModelScope.launch {
            authRepository.login(params).collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> _callState.emit(CallUiState.Login(callbackResult.successData))
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

    fun getCallConfiguration() {
        viewModelScope.launch {
            authRepository.getCallConfiguration().collect { callbackResult ->
                when (callbackResult) {
                    is Result.Success -> _callState.emit(CallUiState.GetCallConfiguration(callbackResult.successData))
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
}