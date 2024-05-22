package com.neptune.talkplus_calls_android_sample.data.repository.auth

import com.neptune.talkplus_calls_android_sample.commons.Constant.TEST_CHANNEL_ID
import com.neptune.talkplus_calls_android_sample.data.model.base.WrappedFailResult
import com.neptune.talkplus_calls_android_sample.data.model.base.Result
import io.talkplus.entity.user.TPUser
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.flow.Flow
import io.talkplus.entity.channel.TPChannel
import io.talkplus.entity.user.TPRtcConfiguration

class AuthenticationRepository(private val authRepositoryImpl: AuthenticationRepositoryImpl = AuthenticationRepositoryImpl()) {
    fun login(tpLoginParams: TPLoginParams): Flow<Result<TPUser, WrappedFailResult>> = authRepositoryImpl.login(tpLoginParams)
    fun joinChannel(channelId: String = TEST_CHANNEL_ID): Flow<Result<TPChannel, WrappedFailResult>> = authRepositoryImpl.joinChannel(channelId)
    fun enablePushNotification(): Flow<Result<TPUser, WrappedFailResult>> = authRepositoryImpl.enablePushNotification()
    fun registerFcmToken(fcmToken: String): Flow<Result<Unit, WrappedFailResult>> = authRepositoryImpl.registerFcmToken(fcmToken)
    fun getCallConfiguration(): Flow<Result<TPRtcConfiguration, WrappedFailResult>> = authRepositoryImpl.getConnectionConfig()
}