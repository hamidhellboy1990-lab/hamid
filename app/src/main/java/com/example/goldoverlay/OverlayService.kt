package com.example.goldoverlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastPrice: Double? = null

    private val updateIntervalMs = 15_000L // refresh every 15 seconds

    private val updateRunnable = object : Runnable {
        override fun run() {
            refreshPrice()
            handler.postDelayed(this, updateIntervalMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        showOverlay()
        isRunning = true
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // view already removed
            }
        }
    }

    private fun showOverlay() {
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_layout, null)

        val layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        windowManager.addView(overlayView, params)

        // Make the bubble draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        overlayView?.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            stopSelf()
        }

        refreshPrice()
    }

    private fun refreshPrice() {
        val apiKey = PrefsManager.getApiKey(this)
        val priceView = overlayView?.findViewById<TextView>(R.id.tvOverlayPrice) ?: return
        val arrowView = overlayView?.findViewById<TextView>(R.id.tvOverlayArrow)
        val timeView = overlayView?.findViewById<TextView>(R.id.tvOverlayTime)

        GoldPriceApi.fetchPrice(apiKey, object : GoldPriceApi.PriceCallback {
            override fun onSuccess(pricePerOunceUsd: Double) {
                handler.post {
                    priceView.text = "$%.2f".format(pricePerOunceUsd)

                    val previous = lastPrice
                    if (previous != null && arrowView != null) {
                        if (pricePerOunceUsd > previous) {
                            arrowView.text = "▲"
                            arrowView.setTextColor(ContextCompat.getColor(this@OverlayService, R.color.price_up))
                            arrowView.visibility = View.VISIBLE
                        } else if (pricePerOunceUsd < previous) {
                            arrowView.text = "▼"
                            arrowView.setTextColor(ContextCompat.getColor(this@OverlayService, R.color.price_down))
                            arrowView.visibility = View.VISIBLE
                        }
                    }
                    lastPrice = pricePerOunceUsd

                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    timeView?.text = sdf.format(Date())
                }
            }

            override fun onError(message: String) {
                handler.post {
                    priceView.text = "خطا"
                }
            }
        })
    }

    private fun startForegroundServiceNotification() {
        val channelId = "gold_overlay_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "قیمت طلا",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java)
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("قیمت انس طلا فعال است")
            .setContentText("در حال نمایش قیمت لحظه‌ای روی صفحه")
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }
}
