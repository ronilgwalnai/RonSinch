package com.ron.sinchcalling.helpers

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.view.View

internal fun String.usernameFromCall(): String {
    val name =
        try {
            val tempList = this.split("|")
            if (tempList.isEmpty()) {
                this
            } else {
                tempList[0]
            }

        } catch (_: Exception) {
            this
        }
    return name

}


internal fun View.ronVisible(value: Boolean) = if (value) {
    this.visibility = View.VISIBLE
} else {
    this.visibility = View.GONE

}


