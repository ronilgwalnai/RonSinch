package com.ron.sinchcalling.models

import com.ron.sinchcalling.helpers.RonConstants
import java.io.Serializable

data class RonSinchCallResult(
    var callDurationInSec: Int = 0,
    var callEndBy: String = RonConstants.UserType.receiver,
) : Serializable
