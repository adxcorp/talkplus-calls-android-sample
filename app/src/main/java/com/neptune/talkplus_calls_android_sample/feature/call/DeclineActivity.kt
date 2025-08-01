package com.neptune.talkplus_calls_android_sample.feature.call

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.neptune.talkplus_calls_android_sample.R
import com.neptune.talkplus_calls_android_sample.background.TPFirebaseMessagingService
import com.neptune.talkplus_calls_android_sample.commons.Constant.TEST_CHANNEL_ID
import com.neptune.talkplus_calls_android_sample.commons.extensions.intentSerializable
import com.neptune.talkpluscallsandroid.webrtc.model.SignalingMessageType
import com.neptune.talkpluscallsandroid.webrtc.model.WebRTCMessageType
import io.talkplus.TalkPlus
import io.talkplus.entity.channel.TPChannel
import io.talkplus.entity.user.TPNotificationPayload
import io.talkplus.entity.user.TPUser
import io.talkplus.internal.api.TalkPlusImpl
import io.talkplus.params.TPLoginParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class DeclineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decline)

        val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(TPFirebaseMessagingService.NOTIFICATION_ID)
        TPFirebaseMessagingService.isCalling = false
        endCall()
    }

    private fun endCall() {
        intent.intentSerializable(
            INTENT_EXTRA_NOTIFICATION_PAYLOAD,
            TPNotificationPayload::class.java
        )?.let { payload ->
            val endCallRequest: SignalingMessageType = SignalingMessageType.EndCallRequest(
                type = WebRTCMessageType.END_CALL.type,
                channelId = payload.channelId,
                calleeId = payload.calleeId,
                callerId = payload.callerId,
                uuid = payload.uuid,
                endReasonCode = 2,
                endReasonMessage = "Callee Canceled"
            )

            val params: TPLoginParams = TPLoginParams.Builder(payload.calleeId, TPLoginParams.LoginType.ANONYMOUS)
                .setUserName(payload.calleeId)
                .build()

            TalkPlus.login(params, object : TalkPlus.CallbackListener<TPUser> {
                override fun onSuccess(tpUer: TPUser) {
                    TalkPlus.joinChannel(TEST_CHANNEL_ID, object : TalkPlus.CallbackListener<TPChannel> {
                        override fun onSuccess(p0: TPChannel?) {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(1000)
                                TalkPlusImpl.sendMessage(Gson().toJson(endCallRequest))
                                finish()
                            }
                        }

                        override fun onFailure(p0: Int, p1: Exception?) {

                        }
                    })
                }
                override fun onFailure(errorCode: Int, exception: Exception) {
                    Log.d(TAG, "$errorCode ${exception.message.toString()}")
                }
            })
        }
    }

    companion object {
        private const val TAG = "DeclineActivity!!"
        const val INTENT_EXTRA_NOTIFICATION_PAYLOAD = "payload"
        const val INTENT_EXTRA_CALLEE_ID = "calleeId"
    }
}