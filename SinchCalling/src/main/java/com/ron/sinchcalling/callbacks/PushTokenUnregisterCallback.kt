package com.ron.sinchcalling.callbacks

interface PushTokenUnregisterCallback {
    fun onPushTokenUnregistered()
    fun onPushTokenUnRegistrationFailed(error: String?)
}