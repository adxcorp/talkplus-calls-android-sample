package com.neptune.talkplus_calls_android_sample.data.repository.auth

import android.util.Log
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
import io.talkplus.entity.user.TPRtcConfiguration
import java.lang.Exception

private const val TAG = "Impl!!"

class AuthenticationRepositoryImpl {
    fun login(tpLoginParams: TPLoginParams): Flow<Result<TPUser, WrappedFailResult>> = callbackFlow {
        TalkPlus.login(tpLoginParams, object : CallbackListener<TPUser> {
            override fun onSuccess(tpUer: TPUser) { trySend(Result.Success(tpUer)) }
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

    fun enablePushNotification(): Flow<Result<TPUser, WrappedFailResult>> = callbackFlow {
        TalkPlus.enablePushNotification(object : CallbackListener<TPUser> {
            override fun onSuccess(tpUser: TPUser) {
                Log.d(TAG, "success enablePushNotification")
                trySend(Result.Success(tpUser))
            }
            override fun onFailure(errorCode: Int, exception: Exception) {
                trySend(Result.Failure(WrappedFailResult(ENABLE_PUSH_NOTIFICATION, errorCode, exception)))
            }
        })
        awaitClose { cancel() }
    }

    fun registerFcmToken(fcmToken: String): Flow<Result<Unit, WrappedFailResult>> = callbackFlow {
        TalkPlus.registerFCMToken(fcmToken, object : CallbackListener<Void?> {
            override fun onSuccess(t: Void?) {
                Log.d(TAG, "success registerFcmToken")
                trySend(Result.Success(Unit))
            }
            override fun onFailure(errorCode: Int, exception: Exception) {
                trySend(Result.Failure(WrappedFailResult(REGISTER_TOKEN, errorCode, exception)))
            }
        })
        awaitClose { cancel() }
    }

    fun getConnectionConfig(): Flow<Result<TPRtcConfiguration, WrappedFailResult>> = callbackFlow {
        TalkPlus.getWebRtcConfiguration(object : CallbackListener<TPRtcConfiguration> {
            override fun onSuccess(tpRtcConfiguration: TPRtcConfiguration) { trySend(Result.Success(tpRtcConfiguration)) }
            override fun onFailure(errorCode: Int, exception: Exception) {
                trySend(Result.Failure(WrappedFailResult(REGISTER_TOKEN, errorCode, exception)))
            }
        })
        awaitClose { cancel() }
    }

    companion object {
        private const val LOGIN = "login"
        private const val JOIN_CHANNEL = "joinChannel"
        private const val ENABLE_PUSH_NOTIFICATION = "enablePushNotification"
        private const val REGISTER_TOKEN = "registerToken"
    }
}