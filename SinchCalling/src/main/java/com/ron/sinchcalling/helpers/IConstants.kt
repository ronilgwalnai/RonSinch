package com.ron.sinchcalling.helpers

interface IConstants {

    object Preferences {
        const val userModel = "UserModel"
    }

    object NotificationConstants {
        const val ContentIntent: Int = 1
        const val FullScreenIntent: Int = 2
        const val acceptButtonIntent: Int = 3
        const val rejectButtonIntent: Int = 4
        const val actionButtons = "actionButtons"
        const val DEF_CHANNEL_ID = "Calling"
        const val DEF_CHANNEL_DESC = "this channel is design for calling "
    }

    object Broadcast {
        const val clientStarted = "clientStarted"
        const val connectionEstablished = "connectionEstablished"
        const val incomingCall = "incomingCall"
        const val type = "broadcastType"
    }

    object Calls {
        const val payload = "AudioCall"
        const val incomingCall = "incomingCall"
        const val audioCall = "Audio Call"
        const val videoCall = "Video Call"
        const val outgoingCall = "outgoingCall"
        const val type = "callType"
        const val caller = "caller"
        const val callerID = "callerID"
        const val min = "min"
        const val seconds = "seconds"
    }

}