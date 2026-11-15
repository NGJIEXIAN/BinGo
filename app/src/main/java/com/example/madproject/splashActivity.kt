package com.example.madproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 停留 2000 毫秒（2 秒）后自动跳转到登录页
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
            finish() // 销毁启动页，防止用户在登录页按返回键又退回启动页
        }, 2000)
    }
}