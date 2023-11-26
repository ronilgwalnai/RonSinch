package com.ron.sinchcalling.callbacks

interface UserRegisterCallbacks {
    fun onUserRegistered()
    fun onUserRegistrationFailed(error: String?)
}