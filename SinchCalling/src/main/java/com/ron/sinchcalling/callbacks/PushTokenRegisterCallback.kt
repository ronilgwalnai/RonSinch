package com.ron.sinchcalling.callbacks

interface PushTokenRegisterCallback {
    fun onPushTokenRegistered()
    fun onPushTokenRegistrationFailed(error: String?)
}