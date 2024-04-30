package com.neptune.talkplus_calls_android_sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.talkplus_calls_android_sample.data.model.base.Result
import com.neptune.talkplus_calls_android_sample.data.repository.auth.AuthenticationRepository
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import io.talkplus.entity.user.TPNotificationPayload
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CallViewModel(
    private val authRepository: AuthenticationRepository = AuthenticationRepository()
) : ViewModel() {
    var isEnableLocalAudio: Boolean = true
        private set

    var isEnableLocalVideo: Boolean = true
        private set

    lateinit var talkPlusCall: TalkPlusCall
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
                    is Result.Success -> _callState.emit(CallUiState.Login(callbackResult.successData))
                    is Result.Failure -> _callState.emit(CallUiState.Fail(callbackResult.failResult))
                }
            }
        }
    }

//    fun joinChannel() {
//        viewModelScope.launch {
//            authRepository.joinChannel().collect { callbackResult ->
//                when (callbackResult) {
//                    is Result.Success -> _callState.emit(CallUiState.Join(callbackResult.successData))
//                    is Result.Failure -> _callState.emit(CallUiState.Fail(callbackResult.failResult))
//                }
//            }
//        }
//    }

    fun setLocalAudio(isEnable: Boolean) { isEnableLocalAudio = isEnable }
    fun setLocalVideo(isEnable: Boolean) { isEnableLocalVideo = isEnable }
    fun setTalkplusCall(talkPlusCall: TalkPlusCall) { this.talkPlusCall = talkPlusCall }
}