package com.ron.sinch.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {
//    private var ronSinch: RonSinch? = null
    override fun onMessageReceived(data: RemoteMessage) {
        Log.d("onMessageReceived", ": $data")
//        if (RonSinch.isSinchPayload(data.data)) {
//            if (ronSinch == null) {
//                ronSinch = RonSinch(this)
//            }
//            ronSinch?.handelCall(data.data)
//        }
    }

    override fun onNewToken(p0: String) {

    }
}