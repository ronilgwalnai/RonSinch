package com.ron.sinchcalling

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.ron.sinchcalling.activities.RonSinchCallActivity
import com.ron.sinchcalling.callbacks.PushTokenRegisterCallback
import com.ron.sinchcalling.callbacks.PushTokenUnregisterCallback
import com.ron.sinchcalling.callbacks.UserRegisterCallbacks
import com.ron.sinchcalling.helpers.IConstants
import com.ron.sinchcalling.helpers.RonJwt
import com.ron.sinchcalling.helpers.SharedPrefUtils
import com.ron.sinchcalling.models.UserModel
import com.ron.sinchcalling.services.RonSinchService
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.PushConfiguration
import com.sinch.android.rtc.PushTokenRegistrationCallback
import com.sinch.android.rtc.PushTokenUnregistrationCallback
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.SinchPush
import com.sinch.android.rtc.SinchPush.queryPushNotificationPayload
import com.sinch.android.rtc.UserController
import com.sinch.android.rtc.UserRegistrationCallback

class RonSinch(private val context: Context) {
    private val preference by lazy { SharedPrefUtils(context) }

    companion object {
        fun isSinchPayload(payload: Map<String, String>): Boolean {
            return SinchPush.isSinchPushPayload(payload)
        }
    }

    fun registerUser(
        model: UserModel,
        userRegisterCallbacks: UserRegisterCallbacks? = null,
        pushTokenRegisterCallback: PushTokenRegisterCallback? = null
    ) {
        preference.setUserModel(model)

        UserController.builder().context(context).userId(model.userID).applicationKey(model.key)
            .environmentHost(model.environment).pushConfiguration(
                PushConfiguration.fcmPushConfigurationBuilder().senderID(model.fcmSenderID)
                    .registrationToken(model.fcmToken).build()
            ).build().also { it ->
                it.registerUser(object : UserRegistrationCallback {
                    override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
                        val jwt: String = RonJwt.create(
                            model.key, model.secret, model.userID
                        )
                        clientRegistration.register(jwt)
                    }

                    override fun onUserRegistered() {
                        userRegisterCallbacks?.onUserRegistered()

                    }

                    override fun onUserRegistrationFailed(error: SinchError) {
                        userRegisterCallbacks?.onUserRegistrationFailed(error.message)
                    }
                }, object : PushTokenRegistrationCallback {
                    override fun onPushTokenRegistered() {
                        pushTokenRegisterCallback?.onPushTokenRegistered()
                    }

                    override fun onPushTokenRegistrationFailed(error: SinchError) {
                        pushTokenRegisterCallback?.onPushTokenRegistrationFailed(error.message)
                    }
                })
            }

    }

    fun signOut(model: UserModel, pushTokenUnregisterCallback: PushTokenUnregisterCallback) {
        UserController.builder().context(context).applicationKey(model.key).userId(model.userID)
            .environmentHost(model.environment).build()
            .unregisterPushToken(object : PushTokenUnregistrationCallback {
                override fun onPushTokenUnregistered() {
                    preference.setUserModel(null)
                    pushTokenUnregisterCallback.onPushTokenUnregistered()
                }

                override fun onPushTokenUnregistrationFailed(error: SinchError) {
                    pushTokenUnregisterCallback.onPushTokenUnRegistrationFailed(error.message)
                }
            })
    }

    fun handelCall(data: MutableMap<String, String>) {
        preference.getUserModel()?.let {
            context.startService(Intent(context, RonSinchService::class.java).apply {
                putExtra(IConstants.Calls.payload, preference.getUserModel())
            })
        }
        context.bindService(
            Intent(context, RonSinchService::class.java).apply {
                putExtra(IConstants.Calls.payload, SharedPrefUtils(context).getUserModel())
            },
            object : ServiceConnection {
                override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                    if (p1 is RonSinchService.SinchClientServiceBinder) {
                        val client: SinchClient? = p1.sinchClient
                        if (client != null) {
                            val result = queryPushNotificationPayload(
                                context,
                                data
                            )
                            client.relayRemotePushNotification(result)
                        }
                    }
                    context.unbindService(this)
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    Log.e("onServiceDisconnected", "onServiceDisconnected: ")
                }
            },
            Context.BIND_AUTO_CREATE
        )
    }


    fun placeVoiceCall(callerID: String, min: Int? = null, seconds: Int? = null) {
        context.startActivity(Intent(context, RonSinchCallActivity::class.java).apply {
            putExtra(IConstants.Calls.type, IConstants.Calls.audioCall)
            putExtra(IConstants.Calls.callerID, callerID)
            seconds?.let {
                putExtra(IConstants.Calls.seconds, it)
            }
            min?.let {
                putExtra(IConstants.Calls.min, it)
            }
            putExtra(IConstants.Calls.caller, true)
        })
    }

    fun placeVideoCall(callerID: String, min: Int? = null, seconds: Int? = null) {
        context.startActivity(Intent(context, RonSinchCallActivity::class.java).apply {
            putExtra(IConstants.Calls.type, IConstants.Calls.videoCall)
            putExtra(IConstants.Calls.callerID, callerID)
            seconds?.let {
                putExtra(IConstants.Calls.seconds, it)
            }
            min?.let {
                putExtra(IConstants.Calls.min, it)
            }
            putExtra(IConstants.Calls.caller, true)
        })
    }


}