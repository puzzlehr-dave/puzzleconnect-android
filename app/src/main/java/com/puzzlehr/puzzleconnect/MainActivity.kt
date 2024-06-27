package com.puzzlehr.puzzleconnect

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.RelativeLayout
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.gabethecoder.notpwa2.Result
import com.gabethecoder.notpwa2.Response
import com.gabethecoder.notpwa2.WebAppConfiguration
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    class CallRequest {
        var phone: String? = null
    }

    class OpenLinkRequest {
        var url: String? = null
    }

    class TokenRequest {
        var token: String? = null
    }

    var authToken: String? = null
    var updateFetch: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configureApp()

        val url = "https://phr2.geekydevelopment.com"
        val packageName = "versioning.json"

        val content = findViewById<RelativeLayout>(R.id.content)
        val config = WebAppConfiguration(this, content, url, packageName, true, true)

        config.onRecieve { request, response ->
            if (request.function == "subscribeToFetch") {
                updateFetch = {
                    response(Result(null))
                }
            }

            if (request.function == "updateToken") {
                authToken = request.model<TokenRequest>()?.token
                updateToken()
            }

            if (request.function == "fetchedUpdate") {

            }

            if (request.function == "openLink") {
                val openLinkRequest = request.model<OpenLinkRequest>()
                val open = Intent(Intent.ACTION_VIEW, Uri.parse(openLinkRequest?.url))
                startActivity(open)
            }

            if (request.function == "call") {
                val callRequest = request.model<CallRequest>()
                val call = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + callRequest!!.phone!!))
                startActivity(call)
            }

            if (request.function == "showBetaInfo") {

            }
        }

        updateToken()
    }

    override fun onResume() {
        super.onResume()

        updateFetch?.let {
            it()
        }

        updateToken()
    }

    private fun configureApp() {
        val prefs = getSharedPreferences("com.puzzlehr.puzzleconnect", MODE_PRIVATE)
        val launched = prefs.getBoolean("launched", false)

        if (!launched) {
            prefs.edit().putBoolean("launched", true).apply()
            clearCache()

            Toast.makeText(this, "Welcome to PuzzleHR!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCache() {
        WebStorage.getInstance().deleteAllData()

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun updateToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener {
            if (!it.isSuccessful) {
                return@OnCompleteListener
            }

            val url = "https://microservices.geekydevelopment.com/notifications/tokens/add"
            val json = JSONObject()
            json.put("token", "gpnToken:${it.result}")

            val queue = Volley.newRequestQueue(this)
            val request = object: JsonObjectRequest(Request.Method.POST, url, json, com.android.volley.Response.Listener<JSONObject> {

            }, com.android.volley.Response.ErrorListener {

            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    headers["Authorization"] = authToken ?: ""
                    return headers
                }
            }

            queue.add(request)
        })
    }

}