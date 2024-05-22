package com.neptune.talkplus_calls_android_sample.feature.call

import com.neptune.talkplus_calls_android_sample.data.model.base.WrappedFailResult
import io.talkplus.entity.channel.TPChannel
import io.talkplus.entity.user.TPRtcConfiguration
import io.talkplus.entity.user.TPUser

sealed class CallUiState {
    data class Login(val tpUser: TPUser) : CallUiState()
    data class JoinChannel(val tpChannel: TPChannel) : CallUiState()
    data class EnablePush(val tpUser: TPUser) : CallUiState()
    data class GetCallConfiguration(val tpRtcConfiguration: TPRtcConfiguration) : CallUiState()
    data class Failed(val failResult: WrappedFailResult) : CallUiState()
    data object RegisterToken : CallUiState()
}