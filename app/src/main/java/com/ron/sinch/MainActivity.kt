package com.ron.sinch

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.ron.sinch.databinding.ActivityMainBinding
import com.ron.sinchcalling.RonSinch
import com.ron.sinchcalling.models.RonSinchCallResult
import com.ron.sinchcalling.models.RonSinchUserModel
import com.ron.sinchcalling.models.UserCallModel
import java.util.Random

class MainActivity : AppCompatActivity() {
    private var sp: SharedPreferences? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        sp = getSharedPreferences("offline", MODE_PRIVATE)
        val yourUserID = findViewById<TextView>(R.id.your_user_id)
        val userID: String? = getUserId()
        initListners()
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            RonSinch(this).registerUser(
                RonSinchUserModel(
                    secret = "yKEcNvop6UaLec7WIJii5g==",
                    userID = userID ?: "",
                    fcmSenderID = "873551401245",
                    environment = "ocra.api.sinch.com",
                    fcmToken = it,
                    key = "0feeb639-ba6b-45aa-af22-a081e8967bda",
                    userName = "Ronil-$userID" ?: ""
                )
            )
        }

//        RonSinch(this).signOut(object :PushTokenUnregisterCallback{
//            override fun onPushTokenUnregistered() {
//                Log.e("onPushTokenUnregistered", ": ", )
//            }
//
//            override fun onPushTokenUnRegistrationFailed(error: String?) {
//                Log.e("onPushTokenUnregistered", ":ERROR ->  $error", )
//            }
//        })
        yourUserID.text = "Your User ID :$userID"

    }

    private fun initListners() {
        binding.btnVoiceCall.setOnClickListener {
            RonSinch(this).placeVoiceCall(
                UserCallModel(
                    binding.etTargetID.text.toString(),
                    receiverName = "Temp name ${binding.etTargetID.text.toString()}",
                ),
                launcher = launcher,
                seconds = 11
            )
        }
        binding.btnVideoCall.setOnClickListener {
            RonSinch(this).placeVideoCall(
                UserCallModel(
                    binding.etTargetID.text.toString(),
                    receiverName = "chota Don"
                ), seconds = 111
            )
        }
    }

    private fun generateUserID(): String? {
        val builder = StringBuilder()
        val random = Random()
        while (builder.length < 5) {
            val nextInt = random.nextInt(10)
            if (builder.isEmpty() && nextInt == 0) {
                continue
            }
            builder.append(nextInt)
        }
        saveUserId(builder.toString())
        return builder.toString()
    }

    private fun saveUserId(userId: String) {
        val edit = sp!!.edit()
        edit.putString("userId", userId)
        edit.apply()
    }

    private fun getUserId(): String? {
        val spValue = sp!!.getString("userId", "")
        return if (spValue!!.isEmpty()) {
            generateUserID()
        } else {
            spValue
        }
    }


    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("result", RonSinchCallResult::class.java)
            } else {
                result.data?.getSerializableExtra("result") as RonSinchCallResult
            }

            Log.e("RonSinchCallResul", ": $data")
        }


    }

}