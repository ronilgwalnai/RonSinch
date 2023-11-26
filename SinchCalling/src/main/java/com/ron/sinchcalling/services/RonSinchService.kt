package com.ron.sinchcalling.services

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.ron.sinchcalling.activities.RonSinchCallActivity
import com.ron.sinchcalling.callbacks.RonSinchClientListener
import com.ron.sinchcalling.helpers.IConstants
import com.ron.sinchcalling.helpers.RonJwt
import com.ron.sinchcalling.helpers.RonNotificationUtils
import com.ron.sinchcalling.helpers.SharedPrefUtils
import com.ron.sinchcalling.models.UserModel
import com.sinch.android.rtc.ClientRegistration
import com.sinch.android.rtc.PushConfiguration
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.SinchClientListener
import com.sinch.android.rtc.SinchError
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallController
import com.sinch.android.rtc.calling.CallControllerListener

@SuppressLint("AnnotateVersionCheck")
class RonSinchService : Service() {
    private var sinchClientInstance: SinchClient? = null
    private var ronSinchClientListener: RonSinchClientListener? = null
    private val preferences by lazy { SharedPrefUtils(this) }
    private val model: UserModel? by lazy { preferences.getUserModel() }
    private val systemVersionDisallowsExplicitActivityStart: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onBind(p0: Intent?): IBinder {
        return SinchClientServiceBinder()
    }

    inner class SinchClientServiceBinder : Binder() {
        val sinchClient: SinchClient? get() = sinchClientInstance
    }

    override fun onCreate() {
        super.onCreate()
        registerSinchClientIfNecessary()

    }

    private fun registerSinchClientIfNecessary() {
        if (sinchClientInstance != null && sinchClientInstance?.isStarted == true) {
            return
        }
        if (model != null) {
            val userID = model?.userID
            val userName = model?.userName
            sinchClientInstance = SinchClient.builder().context(this)
                .environmentHost(model?.environment ?: "")
                .applicationKey(model?.key ?: "").userId(userID ?: "")
                .enableVideoCalls(model?.enableVideoCalls ?: true)
                .pushNotificationDisplayName(userName ?: "")
                .pushConfiguration(
                    PushConfiguration.fcmPushConfigurationBuilder()
                        .senderID(model?.fcmSenderID ?: "")
                        .registrationToken(model?.fcmToken ?: "")
                        .build()
                )
                .build().apply {
                    addSinchClientListener(sinchClientListener)
                    start()
                }
        }
        sinchClientInstance?.callController?.addCallControllerListener(callController)

    }


    private val sinchClientListener = object : SinchClientListener {
        override fun onClientFailed(client: SinchClient, error: SinchError) {
            ronSinchClientListener?.onClientFailed(error.message)
        }

        override fun onClientStarted(client: SinchClient) {
            val tempIntent = Intent(IConstants.Broadcast.connectionEstablished)
            tempIntent.putExtra(IConstants.Broadcast.type, IConstants.Broadcast.clientStarted)
            sendBroadcast(tempIntent)
            ronSinchClientListener?.onClientStarted()
        }

        override fun onCredentialsRequired(clientRegistration: ClientRegistration) {
            val jwt: String = RonJwt.create(
                model?.key, model?.secret, model?.userID
            )
            clientRegistration.register(jwt)
        }

        override fun onLogMessage(level: Int, area: String, message: String) {
            ronSinchClientListener?.onLogMessage(level, area, message)
        }

        override fun onPushTokenRegistered() {
            ronSinchClientListener?.onPushTokenRegistered()
        }

        override fun onPushTokenRegistrationFailed(error: SinchError) {
            ronSinchClientListener?.onPushTokenRegistrationFailed(error.message)
        }

        override fun onPushTokenUnregistered() {
            ronSinchClientListener?.onPushTokenUnregistered()

        }

        override fun onPushTokenUnregistrationFailed(error: SinchError) {
            ronSinchClientListener?.onPushTokenUnRegistrationFailed(error.message)
        }

        override fun onUserRegistered() {
            ronSinchClientListener?.onUserRegistered()
        }

        override fun onUserRegistrationFailed(error: SinchError) {
            ronSinchClientListener?.onUserRegistrationFailed(error.message)
        }
    }


    private val callController = object : CallControllerListener {
        override fun onIncomingCall(callController: CallController, call: Call) {
            val callType = if (call.details.isVideoOffered) {
                IConstants.Calls.videoCall
            } else {
                IConstants.Calls.audioCall

            }
            val mainActivityIntent =
                Intent(this@RonSinchService, RonSinchCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(IConstants.Calls.callerID, call.callId)
                    putExtra(IConstants.Calls.caller, false)
                    putExtra(IConstants.Calls.type, callType)

                }
            if (systemVersionDisallowsExplicitActivityStart && !checkIfInForeground()) {
                RonNotificationUtils(this@RonSinchService).createNotification(
                    call,
                    mainActivityIntent
                )

            } else {
                startActivity(mainActivityIntent)
                val tempIntent = Intent(IConstants.Broadcast.connectionEstablished)
                tempIntent.putExtra(
                    IConstants.Broadcast.type,
                    IConstants.Broadcast.incomingCall
                )
                sendBroadcast(tempIntent)
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_NOT_STICKY

    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {
        if (sinchClientInstance != null && sinchClientInstance?.isStarted == true) {
            sinchClientInstance?.terminateGracefully()
        }
        super.onDestroy()
    }

    private fun checkIfInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appProcesses: List<ActivityManager.RunningAppProcessInfo> =
            activityManager.runningAppProcesses ?: return false
        return appProcesses.any { appProcess: ActivityManager.RunningAppProcessInfo ->
            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName
        }
    }

}