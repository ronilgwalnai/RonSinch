package com.ron.sinchcalling.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ron.sinchcalling.R
import com.ron.sinchcalling.databinding.ActivityRonSinchCallBinding
import com.ron.sinchcalling.helpers.IConstants
import com.ron.sinchcalling.helpers.usernameFromCall
import com.ron.sinchcalling.helpers.visible
import com.ron.sinchcalling.services.RonSinchService
import com.sinch.android.rtc.SinchClient
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallListener
import com.sinch.android.rtc.calling.MediaConstraints
import com.sinch.android.rtc.video.VideoCallListener
import com.sinch.android.rtc.video.VideoController
import com.sinch.android.rtc.video.VideoScalingType


class RonSinchCallActivity : AppCompatActivity() {
    private var seconds: Int? = null
    private var minutes: Int? = null
    private var timeProvided: Int? = null
    private val callType by lazy {
        intent.getStringExtra(IConstants.Calls.type) ?: IConstants.Calls.audioCall
    }
    private var speakerEnabled = false
    private var onMute = false
    private var actionButtons: Boolean? = null
    private val callerID by lazy { intent.getStringExtra(IConstants.Calls.callerID) }
    val caller by lazy { intent.getBooleanExtra(IConstants.Calls.caller, false) }
    private var sinchClient: SinchClient? = null
    private var ongoingCall: Call? = null
    private val binding by lazy { ActivityRonSinchCallBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (intent.hasExtra(IConstants.NotificationConstants.actionButtons)) {
            actionButtons =
                intent.getBooleanExtra(IConstants.NotificationConstants.actionButtons, true)
        }
        if (intent.hasExtra(IConstants.Calls.min)) {
            minutes = intent.getIntExtra(IConstants.Calls.min, 0)
        }
        if (intent.hasExtra(IConstants.Calls.seconds)) {
            seconds = intent.getIntExtra(IConstants.Calls.seconds, 0)
        }
        timeProvided = (seconds ?: 0) + ((minutes ?: 0) * 60)


        if (sinchClient == null) {
            bindService(
                Intent(application, RonSinchService::class.java),
                serviceConnector,
                Context.BIND_AUTO_CREATE
            )
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
            sinchClient?.audioController?.enableSpeaker()
            binding.speaker.backgroundTintList = enableColorStateList
        } else {
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
            binding.actionsLayout.visible(true)
            binding.bottomLayout.visible(false)
            binding.callState.text = "Dialling call"
            binding.callerName.text = it.usernameFromCall()
            when (callType) {
                IConstants.Calls.videoCall -> {
                    ongoingCall = sinchClient?.callController?.callUser(it, MediaConstraints(true))
                    ongoingCall?.addCallListener(ongoingVideoCallListener)
                }

                IConstants.Calls.audioCall -> {
                    ongoingCall = sinchClient?.callController?.callUser(it, MediaConstraints(false))
                    ongoingCall?.addCallListener(ongoingVoiceCallListener)
                }

                else -> {
                    ongoingCall = sinchClient?.callController?.callUser(it, MediaConstraints(false))
                    ongoingCall?.addCallListener(ongoingVoiceCallListener)
                }
            }
        }
    }


    private fun startListeningIncomingCall() {
        if (sinchClient == null) {
            Log.d("classTAG", "startListeningIncomingCall: NULL")
        }
        ongoingCall = sinchClient?.callController?.getCall(callerID ?: "")
        if (!caller) {
            binding.bottomLayout.visible(true)
        }
        binding.actionsLayout.visible(false)
        if (actionButtons == true) {
            acceptCall()
        } else if (actionButtons == false) {
            rejectCall()
        }
        binding.callerName.text = ongoingCall?.remoteUserId?.usernameFromCall()
        binding.callState.text = callType
        if (ongoingCall?.details?.isVideoOffered == true) {
            binding.audioInfoLayout.visible(true)
            ongoingCall?.addCallListener(ongoingVideoCallListener)
        } else {
            ongoingCall?.addCallListener(ongoingVoiceCallListener)
        }
    }

    private fun rejectCall() {
        ongoingCall?.hangup()
    }

    private fun acceptCall() {
        ongoingCall?.answer()
    }


    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(IConstants.Broadcast.connectionEstablished)
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
            if (intent.hasExtra(IConstants.Broadcast.type)) {
                Log.e("onReceive", ": ")
                establishCall(callerID)
            }
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    fun disconnectService() {

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
        if (callType == IConstants.Calls.audioCall) {
            speakerEnabled = false
            binding.audioInfoLayout.visible(true)
            binding.localView.visible(false)
            binding.remoteView.visible(false)
        } else {
            binding.speaker.visible(false)
            speakerEnabled = true
            binding.imgAcceptCall.setImageResource(R.drawable.ic_video_answer_lib)
            binding.localView.visible(true)
            binding.remoteView.visible(true)
        }
        handelSpeaker()
    }


    private val ongoingVoiceCallListener = object : CallListener {
        override fun onCallProgressing(p0: Call) {
            binding.callerName.text = "${ongoingCall?.remoteUserId?.usernameFromCall()}"
            binding.callState.text = getString(R.string.ringing)
            binding.duration.visible(false)


        }

        override fun onCallEstablished(p0: Call) {
            binding.callState.text = getString(R.string.connected)
            binding.bottomLayout.visible(false)
            binding.actionsLayout.visible(true)
            binding.duration.visible(true)
            startCallTimer()

        }

        override fun onCallEnded(p0: Call) {
            binding.callState.text = getString(R.string.call_ended)
            disconnectService()
            binding.duration.visible(false)

        }

    }

    private val ongoingVideoCallListener = object : VideoCallListener {
        override fun onCallProgressing(p0: Call) {
            binding.callerName.text = "${ongoingCall?.remoteUserId?.usernameFromCall()}"
            binding.audioInfoLayout.visible(true)
            binding.callState.text = getString(R.string.ringing)
        }

        override fun onCallEstablished(p0: Call) {
            sinchClient?.audioController?.enableSpeaker()
            binding.bottomLayout.visible(false)
            binding.actionsLayout.visible(true)
            binding.audioInfoLayout.visible(false)
            startCallTimer()
        }

        override fun onCallEnded(p0: Call) {
            binding.audioInfoLayout.visible(true)
            binding.duration.visible(false)
            binding.callState.text = getString(R.string.call_ended)
            binding.localView.removeAllViews()
            binding.remoteView.removeAllViews()
            disconnectService()

        }


        override fun onVideoTrackAdded(p0: Call) {
            val vc: VideoController? = sinchClient?.videoController
            vc?.let {
                binding.localView.visible(true)
                binding.remoteView.visible(true)
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
                    binding.duration.text = "Sec : $it"
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

    fun splitToComponentTimes(counter: Int) {
        val longVal: Int = counter
        val hours = longVal / 3600
        var remainder = longVal - hours * 3600
        val minutes = remainder / 60
        remainder -= minutes * 60
        val secs = remainder
        if (hours > 0) {
            binding.duration.text = "$hours:$minutes:$secs"
        } else if (minutes > 0) {
            binding.duration.text = "$minutes:$secs"
        } else {
            binding.duration.text = "0:$secs"
        }
    }

}


