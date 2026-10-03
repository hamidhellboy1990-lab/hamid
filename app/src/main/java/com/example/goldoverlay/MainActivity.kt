package com.example.goldoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.goldoverlay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etApiKey.setText(PrefsManager.getApiKey(this))

        binding.btnSaveKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            PrefsManager.saveApiKey(this, key)
            Toast.makeText(this, "کلید ذخیره شد", Toast.LENGTH_SHORT).show()
        }

        binding.btnPermission.setOnClickListener {
            requestOverlayPermission()
        }

        binding.btnStart.setOnClickListener {
            if (!hasOverlayPermission()) {
                Toast.makeText(this, "ابتدا مجوز نمایش روی اپ‌ها را فعال کنید", Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return@setOnClickListener
            }
            if (PrefsManager.getApiKey(this).isBlank()) {
                Toast.makeText(this, "ابتدا API Key را وارد و ذخیره کنید", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startService(Intent(this, OverlayService::class.java))
        }

        binding.btnStop.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "مجوز از قبل فعال است", Toast.LENGTH_SHORT).show()
        }
    }
}
