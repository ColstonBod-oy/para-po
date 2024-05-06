package com.enigma.parapo.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.enigma.parapo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        auth = Firebase.auth
        val currentUser = auth.currentUser

        Handler().postDelayed({
            if (currentUser != null)
                startActivity(Intent(this, MainActivity::class.java))
            else
                startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        }, 1500)
    }
}