package com.ron.sinchcalling.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ron.sinchcalling.R
import com.sinch.android.rtc.calling.Call
import java.util.Date

class RonNotificationUtils(private val context: Context) {
    private val notificationManager: NotificationManager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun createNotification(
        call: Call,
        baseIntent: Intent
    ): Notification {
        val model = SharedPrefUtils(context).getUserModel()
        val logo: Int
        val title =
            if (call.details.isVideoOffered) {
                logo = model?.videoNotificationLogo ?: androidx.core.R.drawable.ic_call_answer_video
                context.getString(R.string.incoming_video_call_notification_title)
            } else {
                logo = model?.audioNotificationLogo
                    ?: androidx.core.R.drawable.ic_call_answer
                context.getString(R.string.incoming_call_notification_title)
            }
        createNotificationChannelIfNeeded()
        call.addCallListener(NotificationCancellationListener(notificationManager))

        return NotificationCompat.Builder(
            context,
            this.context.packageName + IConstants.NotificationConstants.DEF_CHANNEL_ID
        )
            .setContentTitle(title)
            .setContentText(call.remoteUserId.usernameFromCall())
            .setSmallIcon(logo)
//            .setSmallIcon(R.drawable.ic_user)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_CALL).setContentIntent(
                createNotificationPendingIntent(
                    baseIntent
                )
            ).setPriority(NotificationCompat.PRIORITY_HIGH).setFullScreenIntent(
                createNotificationPendingIntent(
                    baseIntent
                ), true
            ).addAction(
                R.drawable.button_accept,
                model?.acceptButton ?: context.getString(R.string.hint_accept),
                createNotificationPendingIntent(baseIntent.apply {
                    putExtra(IConstants.NotificationConstants.actionButtons, true)
                })
            ).addAction(
                R.drawable.button_decline,
                model?.rejectButton ?: context.getString(R.string.hint_reject),
                createNotificationPendingIntent(baseIntent.apply {
                    putExtra(IConstants.NotificationConstants.actionButtons, false)
                })
            ).setOngoing(true).build().apply {
                flags = flags or Notification.FLAG_INSISTENT
            }.also {
                notificationManager.notify(Date().time.toInt(), it)
            }

    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager.getNotificationChannel(
                this.context.packageName + IConstants.NotificationConstants.DEF_CHANNEL_ID
            ) != null
        ) {
            return
        }
        NotificationChannel(
            this.context.packageName + IConstants.NotificationConstants.DEF_CHANNEL_ID,
            IConstants.NotificationConstants.DEF_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = IConstants.NotificationConstants.DEF_CHANNEL_DESC
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            shouldVibrate()
            shouldShowLights()
            enableLights(true)
            lightColor = Color.GREEN
            canShowBadge()
            setSound(null, null)

        }.let {
            notificationManager.createNotificationChannel(it)
        }
    }

    private fun createNotificationPendingIntent(source: Intent): PendingIntent =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                source,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(), source, PendingIntent.FLAG_IMMUTABLE
            )
        }


}