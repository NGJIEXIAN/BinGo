
package com.example.madproject

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class loginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher:
            ActivityResultLauncher<Intent>

    private var loadingDialog: Dialog? = null

    // Random BinGo loading messages
    private val loadingMessages = listOf(
        "BinGo is bingoing... 🎲",
        "Bingo! Almost there... ✨",
        "BinGo-ing green... 🌱",
        "Finding your green light... 🍀",
        "Your BinGo moment is loading... 🌿",
        "One little step to BinGo... 👣",
        "Getting you ready to go green... 🌎",
        "Let's make it a BinGo! 🎉"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .requestIdToken(
                getString(R.string.default_web_client_id)
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Handle Google account picker result
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                val task =
                    GoogleSignIn.getSignedInAccountFromIntent(
                        result.data
                    )

                try {
                    val account = task.getResult(
                        ApiException::class.java
                    )

                    val email = account.email

                    // Only allow UM student email accounts
                    if (
                        email.isNullOrBlank() ||
                        !email.endsWith(
                            "@siswa.um.edu.my",
                            ignoreCase = true
                        )
                    ) {
                        Toast.makeText(
                            this,
                            "Please use your UM student Google account.",
                            Toast.LENGTH_LONG
                        ).show()

                        googleSignInClient.signOut()

                    } else {

                        val googleIdToken = account.idToken

                        if (googleIdToken == null) {
                            Toast.makeText(
                                this,
                                "Unable to get Google ID token.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            signInToFirebase(googleIdToken)
                        }
                    }

                } catch (e: ApiException) {
                    Toast.makeText(
                        this,
                        "Google sign-in failed: ${e.statusCode}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } else {
                Toast.makeText(
                    this,
                    "Google sign-in cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Continue with Google button
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

        btnSignIn.setOnClickListener {

            // Show Google account chooser
            googleSignInClient.signOut()
                .addOnCompleteListener {

                    val signInIntent =
                        googleSignInClient.signInIntent

                    googleSignInLauncher.launch(signInIntent)
                }
        }
    }

    // Show custom BinGo loading popup
    private fun showProcessing() {

        // Pick a random message every time loading starts
        val randomMessage = loadingMessages.random()

        val dialog = Dialog(this)

        dialog.setContentView(
            R.layout.dialog_bingo_loading
        )

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        // Set random loading message
        val messageText = dialog.findViewById<TextView>(
            R.id.tvLoadingMessage
        )

        messageText.text = randomMessage

        // Transparent dialog background
        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        loadingDialog = dialog
    }

    // Hide loading popup
    private fun hideProcessing() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    // Sign in to Firebase using Google ID token
    private fun signInToFirebase(googleIdToken: String) {

        showProcessing()

        val credential = GoogleAuthProvider.getCredential(
            googleIdToken,
            null
        )

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val currentUser = auth.currentUser

                    if (currentUser == null) {
                        hideProcessing()

                        Toast.makeText(
                            this,
                            "Firebase user not found.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    // Get Firebase ID token
                    currentUser.getIdToken(true)
                        .addOnCompleteListener { tokenTask ->

                            if (tokenTask.isSuccessful) {

                                val firebaseIdToken =
                                    tokenTask.result?.token

                                if (firebaseIdToken != null) {
                                    sendFirebaseTokenToBackend(
                                        firebaseIdToken
                                    )
                                } else {
                                    hideProcessing()

                                    Toast.makeText(
                                        this,
                                        "Unable to get Firebase ID token.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } else {
                                hideProcessing()

                                Toast.makeText(
                                    this,
                                    "Failed to get Firebase ID token.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                } else {
                    hideProcessing()

                    Toast.makeText(
                        this,
                        "Firebase sign-in failed: ${
                            task.exception?.message
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // Send Firebase ID token to Spring Boot backend
    private fun sendFirebaseTokenToBackend(idToken: String) {

        Thread {

            var connection: HttpURLConnection? = null

            try {
                // Your original URL — unchanged
                val url = URL(
                    "http://127.0.0.1:8080/api/auth/google"
                )

                connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                // Matches backend AuthRequest
                val json = JSONObject()
                json.put("idToken", idToken)

                connection.outputStream.use { output ->
                    output.write(
                        json.toString().toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val response =
                    responseStream?.bufferedReader()?.use {
                        it.readText()
                    } ?: ""

                runOnUiThread {

                    hideProcessing()

                    if (responseCode in 200..299) {

                        Toast.makeText(
                            this,
                            "Backend login successful!",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(
                            this,
                            MainActivity::class.java
                        )

                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(
                            this,
                            "Backend login failed ($responseCode): $response",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "BACKEND_CONNECTION",
                    "Backend request failed",
                    e
                )

                runOnUiThread {

                    hideProcessing()

                    Toast.makeText(
                        this,
                        "Backend error: ${e.javaClass.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {
                connection?.disconnect()
            }

        }.start()
    }

    override fun onDestroy() {
        hideProcessing()
        super.onDestroy()
    }
}
