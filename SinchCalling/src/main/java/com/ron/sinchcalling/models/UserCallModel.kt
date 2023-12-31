package com.ron.sinchcalling.models

import java.io.Serializable

data class UserCallModel(
    var callerID: String,
    var receiverName: String? = callerID,
    var callerName: String? = null
) : Serializable
