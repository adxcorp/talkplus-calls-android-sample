package com.neptune.talkplus_calls_android_sample

import com.neptune.talkplus_calls_android_sample.data.model.base.WrappedFailResult
import io.talkplus.entity.channel.TPChannel
import io.talkplus.entity.user.TPUser

sealed class CallUiState {
    data class Login(val tpUser: TPUser) : CallUiState()
    data class JoinChannel(val tpChannel: TPChannel) : CallUiState()
    data class EnablePush(val tpUser: TPUser) : CallUiState()
    data object RegisterToken : CallUiState()

//    data class GetMessages(val getMessagesResult: GetMessageResult) : ChatUiState()
//    data class SendMessage(val tpMessage: TPMessage) : ChatUiState()
//    data class ReceiveMessage(val receiveCallbackResult: ReceiveCallbackResult) : ChatUiState()
//    data class GetPeerMutedUsers(val getMutedPeersResult: GetMutedPeersResult) : ChatUiState()
//    data class PeerMuteUser(val getMuteUserResult: ChannelMemberResult) : ChatUiState()
//    data class UnPeerMuteUser(val getMuteUserResult: ChannelMemberResult) : ChatUiState()
//    data class GetMutedUser(val getMutedUserResult: ReceiveCallbackResult) : ChatUiState()
//    data class GetUnMutedUser(val getUnMutedUserResult: ReceiveCallbackResult) : ChatUiState()
//    data class GetBannedUser(val getBannedUserResult: ReceiveCallbackResult) : ChatUiState()
//    data class GetUnBannedUser(val getUnBannedUserResult: ReceiveCallbackResult) : ChatUiState()
    data class Failed(val failResult: WrappedFailResult) : CallUiState()
//    object LeaveChannel : ChatUiState()
}
