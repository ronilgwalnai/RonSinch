package com.ron.sinchcalling.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.ron.sinchcalling.R
import com.ron.sinchcalling.databinding.ActivityRonSinchCallBinding
import com.ron.sinchcalling.helpers.RonConstants
import com.ron.sinchcalling.helpers.RonNotificationUtils
import com.ron.sinchcalling.helpers.RonProximitySensor
import com.ron.sinchcalling.helpers.ronVisible
import com.ron.sinchcalling.models.RonSinchCallResult
import com.ron.sinchcalling.models.UserCallModel
import com.ron.sinchcalling.services.RonSinchService
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.MediaConstraints
import com.sinch.android.rtc.video.VideoCallListener
import com.sinch.android.rtc.video.VideoController
import com.sinch.android.rtc.video.VideoScalingType


internal class RonSinchCallActivity : AppCompatActivity() {
    private var seconds: Int? = null
    private var minutes: Int? = null
    private var timeProvided: Int? = null
    private var resultModel = RonSinchCallResult()
    private lateinit var screenManager: RonProximitySensor

    private var permissionStatus: MutableLiveData<Boolean> = MutableLiveData()
    private val callType by lazy {
        intent.getStringExtra(RonConstants.Calls.type) ?: RonConstants.Calls.audioCall
    }
    private var speakerEnabled = false
    private var onMute = false
    private var actionButtons: Boolean? = null
    private val userCallModel by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(RonConstants.Calls.payload, UserCallModel::class.java)
        } else {
            intent.getSerializableExtra(RonConstants.Calls.payload) as UserCallModel
        }
    }
    private val callerID by lazy { userCallModel?.callerID }
    private val caller by lazy { intent.getBooleanExtra(RonConstants.Calls.caller, false) }
    private var sinchClient: SinchClient? = null
    private var ongoingCall: Call? = null
    private val binding by lazy { ActivityRonSinchCallBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        screenManager = RonProximitySensor(this)
        checkPermissions()
        if (intent.hasExtra(RonConstants.NotificationConstants.actionButtons)) {
            actionButtons =
                intent.getBooleanExtra(RonConstants.NotificationConstants.actionButtons, true)
        }
        if (intent.hasExtra(RonConstants.Calls.min)) {
            minutes = intent.getIntExtra(RonConstants.Calls.min, 0)
        }
        if (intent.hasExtra(RonConstants.Calls.seconds)) {
            seconds = intent.getIntExtra(RonConstants.Calls.seconds, 0)
        }
        timeProvided = (seconds ?: 0) + ((minutes ?: 0) * 60)


        if (sinchClient == null) {
            permissionStatus.observe(this@RonSinchCallActivity) {
                if (it) {
                    bindService(
                        Intent(application, RonSinchService::class.java),
                        serviceConnector,
                        Context.BIND_AUTO_CREATE
                    )
                }
            }
        }
        binding.imgRejectCall.setOnClickListener {
            rejectCall()
        }
        binding.imgAcceptCall.setOnClickListener {
            acceptCall()
        }
        binding.hangup.setOnClickListener {
            rejectCall()
        }
        binding.speaker.setOnClickListener {
            handelSpeaker()
        }
        binding.mute.setOnClickListener {

            handelMute()
        }
    }

    private val enableStates =
        arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf(-android.R.attr.state_pressed))
    private val disableStates =
        arrayOf(intArrayOf(-android.R.attr.state_pressed), intArrayOf(android.R.attr.state_pressed))

    private var enableColors = intArrayOf(
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#00FF00")
    )

    private var enableColorStateList = ColorStateList(enableStates, enableColors)
    private var disableColorStateList = ColorStateList(disableStates, enableColors)

    private fun handelSpeaker() {
        if (speakerEnabled) {
            screenManager.releaseWakeLock()
            sinchClient?.audioController?.enableSpeaker()
            binding.speaker.backgroundTintList = enableColorStateList
        } else {
            screenManager.acquireWakeLock()
            sinchClient?.audioController?.disableSpeaker()
            binding.speaker.backgroundTintList = disableColorStateList
        }
        speakerEnabled = !speakerEnabled

    }

    private fun handelMute() {
        if (onMute) {
            binding.mute.setImageResource(R.drawable.ic_mic_lib)
            sinchClient?.audioController?.unmute()
        } else {
            binding.mute.setImageResource(R.drawable.ic_mute_lib)
            sinchClient?.audioController?.mute()
        }
        onMute = !onMute

    }


    private fun establishCall(id: String?) {

        id?.let {
            binding.actionsLayout.ronVisible(true)
            binding.bottomLayout.ronVisible(false)
            binding.callState.text = "Dialling call"
            binding.callerName.text = userCallModel?.receiverName
            when (callType) {
                RonConstants.Calls.videoCall -> {
                    ongoingCall = sinchClient?.callController?.callUser(
                        it,
                        MediaConstraints(true),
                        HashMap<String, String>().also {
                            it["receiverName"] = userCallModel?.receiverName ?: ""
                            it["callerName"] = userCallModel?.callerName ?: ""
                        })
                    ongoingCall?.addCallListener(ongoingVideoCallListener)
                }

                RonConstants.Calls.audioCall -> {
                    ongoingCall = sinchClient?.callController?.callUser(
                        it,
                        MediaConstraints(false),
                        HashMap<String, String>().also {
                            it["receiverName"] = userCallModel?.receiverName ?: ""
                            it["callerName"] = userCallModel?.callerName ?: ""
                        })
                    ongoingCall?.addCallListener(ongoingVoiceCallListener)
                }

                else -> {
                    ongoingCall = sinchClient?.callController?.callUser(
                        it,
                        MediaConstraints(false),
                        HashMap<String, String>().also {
                            it["receiverName"] = userCallModel?.receiverName ?: ""
                            it["callerName"] = userCallModel?.callerName ?: ""
                        })
                    ongoingCall?.addCallListener(ongoingVoiceCallListener)
                }
            }
            onMute = true
            handelMute()
        }
    }


    private fun startListeningIncomingCall() {
        if (sinchClient == null) {
            Log.d("classTAG", "startListeningIncomingCall: NULL")
        }
        ongoingCall = sinchClient?.callController?.getCall(callerID ?: "")
        if (!caller) {
            binding.bottomLayout.ronVisible(true)
        }
        binding.actionsLayout.ronVisible(false)
        if (actionButtons == true) {
            acceptCall()
        } else if (actionButtons == false) {
            rejectCall()
        }
        binding.callerName.text = userCallModel?.callerName
        binding.callState.text = callType
        if (ongoingCall?.details?.isVideoOffered == true) {
            binding.audioInfoLayout.ronVisible(true)
            ongoingCall?.addCallListener(ongoingVideoCallListener)
        } else {
            ongoingCall?.addCallListener(ongoingVoiceCallListener)
        }
    }

    private fun rejectCall() {
        if (caller) {
            resultModel.callEndBy = RonConstants.UserType.caller
        } else {
            resultModel.callEndBy = RonConstants.UserType.receiver
        }
        RonNotificationUtils.stopRingTone()
        ongoingCall?.hangup()
    }

    private fun acceptCall() {
        RonNotificationUtils.stopRingTone()
        onMute = true
        handelMute()
        ongoingCall?.answer()
    }


    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(RonConstants.Broadcast.connectionEstablished)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra(RonConstants.Broadcast.type)) {
                permissionStatus.observe(this@RonSinchCallActivity) {
                    if (it) {
                        establishCall(callerID)
                    }
                }
            }
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    fun disconnectService() {
        binding.duration.ronVisible(false)
        binding.callState.text = getString(R.string.call_ended)
        screenManager.releaseWakeLock()
        RonNotificationUtils.stopRingTone()
        stopService(Intent(this, RonSinchService::class.java))
        setResult(Activity.RESULT_OK, Intent().also {
            it.putExtra("result", resultModel)
        })
        finishAndRemoveTask()

    }


    private val serviceConnector = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            if (p1 is RonSinchService.SinchClientServiceBinder) {
                handelUI()
                sinchClient = p1.sinchClient
                if (sinchClient?.isStarted == true && caller) {
                    establishCall(callerID)

                } else {
                    startListeningIncomingCall()
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.e("onServiceDisconnected", ": ${p0.toString()}")

        }
    }

    private fun handelUI() {
        if (callType == RonConstants.Calls.audioCall) {
            speakerEnabled = false
            binding.audioInfoLayout.ronVisible(true)
            binding.localView.ronVisible(false)
            binding.remoteView.ronVisible(false)
        } else {
            binding.speaker.ronVisible(false)
            speakerEnabled = true
            binding.imgAcceptCall.setImageResource(R.drawable.ic_video_answer_lib)
            binding.localView.ronVisible(true)
            binding.remoteView.ronVisible(true)
        }
        handelSpeaker()
    }


    private val ongoingVoiceCallListener = object : CallListener {
        override fun onCallProgressing(p0: Call) {
//            binding.callerName.text = "${ongoingCall?.remoteUserId?.usernameFromCall()}"
            binding.callState.text = getString(R.string.ringing)
            binding.duration.ronVisible(false)


        }

        override fun onCallEstablished(p0: Call) {
            RonNotificationUtils.stopRingTone()
            binding.callState.text = getString(R.string.connected)
            binding.bottomLayout.ronVisible(false)
            binding.actionsLayout.ronVisible(true)
            binding.duration.ronVisible(true)
            startCallTimer()

        }

        override fun onCallEnded(p0: Call) {
            disconnectService()

        }

    }

    private val ongoingVideoCallListener = object : VideoCallListener {
        override fun onCallProgressing(p0: Call) {
//            binding.callerName.text = "${ongoingCall?.remoteUserId?.usernameFromCall()}"
            binding.audioInfoLayout.ronVisible(true)
            binding.callState.text = getString(R.string.ringing)
        }

        override fun onCallEstablished(p0: Call) {
            sinchClient?.audioController?.enableSpeaker()
            binding.bottomLayout.ronVisible(false)
            binding.actionsLayout.ronVisible(true)
            binding.audioInfoLayout.ronVisible(false)
            startCallTimer()
        }

        override fun onCallEnded(p0: Call) {
            binding.audioInfoLayout.ronVisible(true)
            binding.localView.removeAllViews()
            binding.remoteView.removeAllViews()
            disconnectService()

        }


        override fun onVideoTrackAdded(p0: Call) {
            val vc: VideoController? = sinchClient?.videoController
            vc?.let {
                binding.localView.ronVisible(true)
                binding.remoteView.ronVisible(true)
                binding.localView.addView(it.localView)
                binding.remoteView.addView(it.remoteView)
                it.setResizeBehaviour(VideoScalingType.ASPECT_FILL)
            }
        }

        override fun onVideoTrackPaused(p0: Call) {
            //PAUSED
        }

        override fun onVideoTrackResumed(p0: Call) {
            //RESUMED
        }
    }

    private val callTimerHandler = Handler(Looper.getMainLooper())
    private var callRunnableHandler: Runnable? = null
    private fun startCallTimer() {
        callRunnableHandler = object : Runnable {
            override fun run() {
                ongoingCall?.details?.duration?.let {
                    resultModel.callDurationInSec = it
                    splitToComponentTimes(it)
                    if (timeProvided != null && (timeProvided ?: 0) > 1) {
                        val counter = timeProvided!! - it
                        if (counter <= 0) {
                            rejectCall()
                        }
                    }

                }
                callTimerHandler.postDelayed(this, 1000)
            }
        }
        callRunnableHandler?.let {
            callTimerHandler.post(it)
        }
    }

    @SuppressLint("SetTextI18n")
    fun splitToComponentTimes(counter: Int) {
        val longVal: Int = counter
        val hours = longVal / 3600
        var remainder = longVal - hours * 3600
        val minutes = remainder / 60
        remainder -= minutes * 60
        val secs = remainder
        if (hours > 0) {
            binding.duration.text =
                "${String.format("%02d", hours)}:${
                    String.format(
                        "%02d",
                        minutes
                    )
                }:${String.format("%02d", secs)}"
        } else if (minutes > 0) {
            binding.duration.text =
                "${String.format("%02d", minutes)}:${String.format("%02d", secs)}"

        } else {
//            binding.duration.text = "0:$secs"
            binding.duration.text = String.format("00:%02d", secs)
        }
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE
            )
        }
        if (!hasPermissions(this, *permissions)) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                1000
            )
        } else {
            permissionStatus.postValue(true)
        }
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (hasPermissions(this, *permissions)) {
                permissionStatus.postValue(true)
            } else {
                permissionStatus.postValue(false)
            }
        }
    }

    override fun onDestroy() {
        rejectCall()
        super.onDestroy()
    }

}


