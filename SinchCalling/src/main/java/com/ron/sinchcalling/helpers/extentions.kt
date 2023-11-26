package com.ron.sinchcalling.helpers

import android.view.View

fun String.usernameFromCall(): String {
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


fun View.visible(value: Boolean) = if (value) {
    this.visibility = View.VISIBLE
} else {
    this.visibility = View.GONE

}