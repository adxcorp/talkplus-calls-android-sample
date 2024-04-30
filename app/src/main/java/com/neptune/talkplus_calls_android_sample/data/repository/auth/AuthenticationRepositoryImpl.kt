package com.neptune.talkplus_calls_android_sample.data.repository.auth

import com.neptune.talkplus_calls_android_sample.data.model.base.WrappedFailResult
import io.talkplus.TalkPlus
import io.talkplus.TalkPlus.CallbackListener
import io.talkplus.entity.channel.TPChannel
import io.talkplus.entity.user.TPUser
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.neptune.talkplus_calls_android_sample.data.model.base.Result
import java.lang.Exception

class AuthenticationRepositoryImpl {
    fun login(tpLoginParams: TPLoginParams): Flow<Result<TPUser, WrappedFailResult>> = callbackFlow {
        TalkPlus.login(tpLoginParams, object : CallbackListener<TPUser> {
            override fun onSuccess(tpUers: TPUser) { trySend(Result.Success(tpUers)) }
            override fun onFailure(errorCode: Int, exception: Exception) { trySend(Result.Failure(WrappedFailResult(LOGIN, errorCode, exception))) }
        })
        awaitClose { cancel() }
    }

    fun joinChannel(channelId: String = ""): Flow<Result<TPChannel, WrappedFailResult>> = callbackFlow {
        TalkPlus.joinChannel(channelId, object : CallbackListener<TPChannel> {
            override fun onSuccess(tpChannel: TPChannel) { trySend(Result.Success(tpChannel)) }
            override fun onFailure(errorCode: Int, exception: Exception) {
                trySend(Result.Failure(WrappedFailResult(JOIN_CHANNEL, errorCode, exception)))
            }
        })
        awaitClose { cancel() }
    }

    companion object {
        private const val LOGIN = "login"
        private const val JOIN_CHANNEL = "joinChannel"
    }
}