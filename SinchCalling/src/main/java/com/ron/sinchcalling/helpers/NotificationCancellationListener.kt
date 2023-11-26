package com.ron.sinchcalling.helpers

import android.app.NotificationManager
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener

class NotificationCancellationListener(private val notificationManager: NotificationManager) :
    CallListener {


    override fun onCallEnded(call: Call) {
        notificationManager.cancelAll()
    }

    override fun onCallEstablished(call: Call) {
        notificationManager.cancelAll()
    }

    override fun onCallProgressing(call: Call) {
    }

}