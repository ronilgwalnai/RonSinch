package com.ron.sinchcalling.helpers

import android.content.Context
import android.os.PowerManager

internal class RonProximitySensor(context: Context) {
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "${context.applicationInfo.packageName}:ProximityWakeLock"
        )
    }

    fun acquireWakeLock() {
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    fun releaseWakeLock() {
        if (wakeLock?.isHeld==true){
        wakeLock?.release()
        }
    }
}
