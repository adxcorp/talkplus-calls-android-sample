package com.neptune.talkplus_calls_android_sample.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neptune.talkplus_calls_android_sample.extensions.closeNotification
import com.neptune.talkplus_calls_android_sample.feature.call.CallActivity
import com.neptune.talkplus_calls_android_sample.feature.call.DeclineActivity
import io.talkplus.TalkPlus
import io.talkplus.entity.user.TPNotificationPayload
import org.json.JSONObject

class TPFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, remoteMessage.data.toString())
        if (remoteMessage.data.containsKey("talkplus")) {
            val pushNotificationResponse: JSONObject = remoteMessage.data["talkplus"]?.let { JSONObject(it) } ?: return
            when (pushNotificationResponse.getString("type")) {
                "offer" -> {
                    if (!isCalling) {
                        inComing(pushNotificationResponse.getString("notificationLink"))
                        isCalling = true
                    }
                }
                "endCall" -> closeNotification()
            }
        }
    }

    private fun inComing(notificationLink: String) {
        TalkPlus.getNotificationPayload(notificationLink, object : TalkPlus.CallbackListener<TPNotificationPayload> {
            override fun onSuccess(tpNotificationPayload: TPNotificationPayload) { showNotification(tpNotificationPayload) }
            override fun onFailure(errorCode: Int, e: Exception) { Log.d(TAG, "$errorCode ${e.message}") }
        })
    }

    private fun showNotification(tpNotificationPayload: TPNotificationPayload) {
        createNotificationChannel()

        val acceptIntent: Intent = Intent(this, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(CallActivity.INTENT_EXTRA_NOTIFICATION_PAYLOAD, tpNotificationPayload)
        }

        val declineIntent: Intent = Intent(this, DeclineActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(DeclineActivity.INTENT_EXTRA_NOTIFICATION_PAYLOAD, tpNotificationPayload)
        }

        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TalkPlus")
            .setContentText("Call")
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(setNotificationAction(title = "수락", type = acceptIntent))
            .addAction(setNotificationAction(title = "거절", type = declineIntent))
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TalkPlusCalls"
            val descriptionText = "Notification for the Incoming calls."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
            val notificationManager: NotificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setNotificationAction(
        title: String,
        type: Intent,
        code: Int = 0
    ): NotificationCompat.Action {
        return NotificationCompat.Action(
            androidx.core.R.drawable.notification_bg,
            title,
            PendingIntent.getActivity(
                this,
                code,
                type,
                PendingIntent.FLAG_IMMUTABLE + PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }

    companion object {
        private const val TAG = "MyFirebaseMessagingService!!"

        const val CHANNEL_ID: String = "TalkPlusCallas"
        const val NOTIFICATION_ID = 1

        var isCalling = false
    }
}