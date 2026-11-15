package com.example.madproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class profileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
            finish() // 关闭当前个人主页，防止按手机返回键又退回来
        }
    }
}