package com.example.madproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class loginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

        btnSignIn.setOnClickListener {
            val intent = Intent(this, profileActivity::class.java)
            startActivity(intent)
        }
    }
}