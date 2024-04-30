package com.neptune.talkplus_calls_android_sample.data.repository.auth

import com.neptune.talkplus_calls_android_sample.Constant.TEST_CHANNEL_ID
import com.neptune.talkplus_calls_android_sample.data.model.base.WrappedFailResult
import com.neptune.talkplus_calls_android_sample.data.model.base.Result
import io.talkplus.entity.user.TPUser
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.flow.Flow
import io.talkplus.entity.channel.TPChannel

class AuthenticationRepository(private val authRepositoryImpl: AuthenticationRepositoryImpl = AuthenticationRepositoryImpl()) {
    fun login(tpLoginParams: TPLoginParams): Flow<Result<TPUser, WrappedFailResult>> = authRepositoryImpl.login(tpLoginParams)
    fun joinChannel(channelId: String = TEST_CHANNEL_ID): Flow<Result<TPChannel, WrappedFailResult>> = authRepositoryImpl.joinChannel(channelId)
}

