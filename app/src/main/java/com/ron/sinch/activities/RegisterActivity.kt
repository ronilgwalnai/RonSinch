package com.ron.sinch.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.ron.sinch.databinding.ActivityRegisterBinding
import com.ron.sinch.databinding.ProgressDialogueBinding
import com.ron.sinchcalling.RonSinch
import com.ron.sinchcalling.callbacks.UserRegisterCallbacks
import com.ron.sinchcalling.models.RonSinchUserModel

class RegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }
    private val progressBar by lazy {
        AlertDialog.Builder(this).setView(ProgressDialogueBinding.inflate(layoutInflater).root)
            .create().also {
                it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.register.setOnClickListener {
            if (binding.etTargetID.text.toString().length < 5) {
                Toast.makeText(
                    this,
                    "UserID must be valid(length greater then 5)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (binding.etUserName.text.toString().length < 5) {
                Toast.makeText(
                    this,
                    "etUserName must be valid(length greater then 5)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            registerUser(binding.etTargetID.text.toString(), binding.etUserName.text.toString())

        }
    }

    private fun registerUser(userId: String, userName: String) {
        progressBar.show()
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            RonSinch(this).registerUser(
                RonSinchUserModel(
                    secret = "yKEcNvop6UaLec7WIJii5g==",
                    userID = userId,
                    fcmSenderID = "873551401245",
                    environment = "ocra.api.sinch.com",
                    fcmToken = it,
                    key = "0feeb639-ba6b-45aa-af22-a081e8967bda",
                    userName = userName
                ), object : UserRegisterCallbacks {
                    override fun onUserRegistered() {
                        val sp =
                          getSharedPreferences(this@RegisterActivity.packageName, MODE_PRIVATE)
                        sp.edit().putString("userId", userId).apply()
                        Toast.makeText(
                            this@RegisterActivity,
                            "User Registration Successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.dismiss()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finishAffinity()
                    }

                    override fun onUserRegistrationFailed(error: String?) {
                        Toast.makeText(this@RegisterActivity, "$error", Toast.LENGTH_SHORT).show()
                        progressBar.dismiss()
                    }
                }
            )
        }

    }
}