package com.devlomi.fireapp.activities.intro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.AgreePrivacyPolicyActivity
import com.devlomi.fireapp.activities.main.MainActivity
import com.devlomi.fireapp.activities.setup.EnterUsernameActivity
import com.devlomi.fireapp.activities.setup.SetupUserActivity.Companion.start
import com.devlomi.fireapp.utils.SharedPreferencesManager

class IntroActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        startNextActivity()
    }

    private fun startNextActivity() {
        if (!SharedPreferencesManager.hasAgreedToPrivacyPolicy()) {
            startPrivacyPolicyActivity()
        } else if (SharedPreferencesManager.isUserInfoSaved()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else if (!SharedPreferencesManager.hasEnteredUsername()) {
            val intent = Intent(this, EnterUsernameActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val username = SharedPreferencesManager.getUserName()
            val localPhotoPath = SharedPreferencesManager.getLocalPhotoPathSetup()
            val backupUri = SharedPreferencesManager.getLocalBackupPath()
            val dbUri = SharedPreferencesManager.getDbFileUri()
            start(this, username, localPhotoPath, backupUri, dbUri)
            finish()
        }
    }

    private fun startPrivacyPolicyActivity() {
        val intent = Intent(this, AgreePrivacyPolicyActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}