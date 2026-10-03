package com.example.goldoverlay

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Fetches the live global gold ounce price (XAU/USD) using the free,
 * no-API-key, no-rate-limit Gold-API.com service.
 * Docs: https://gold-api.com/docs
 */
object GoldPriceApi {

    private val client = OkHttpClient()

    interface PriceCallback {
        fun onSuccess(pricePerOunceUsd: Double)
        fun onError(message: String)
    }

    fun fetchPrice(apiKey: String, callback: PriceCallback) {
        val request = Request.Builder()
            .url("https://api.gold-api.com/price/XAU")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError("خطای شبکه: ${e.message}")
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback.onError("خطای سرور: ${it.code}")
                        return
                    }
                    val body = it.body?.string()
                    if (body.isNullOrBlank()) {
                        callback.onError("پاسخ خالی از سرور")
                        return
                    }
                    try {
                        val json = JSONObject(body)
                        val price = json.optDouble("price", Double.NaN)
                        if (price.isNaN()) {
                            callback.onError("فرمت پاسخ نامعتبر")
                        } else {
                            callback.onSuccess(price)
                        }
                    } catch (e: Exception) {
                        callback.onError("خطا در پردازش پاسخ: ${e.message}")
                    }
                }
            }
        })
    }
}
