package com.example.onetapsignin

import android.R.attr.data
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.onetapsignin.ui.theme.OneTapSignInTheme
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = ""
        const val REQ_ONE_TAP = 2
    }
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    var signedIn = MutableLiveData<Boolean>(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                // Your server's client ID, not your Android client ID.
                //.setServerClientId(getString(R.string.default_web_client_id))
                .setServerClientId("148563418025-c8pim60ajatcdpfq3olq50ncggh50vn8.apps.googleusercontent.com")
                // Only show accounts previously used to sign in.
                .setFilterByAuthorizedAccounts(true)
                .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build();

        setContent {
            OneTapSignInTheme {

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting("Android")
                        if (!signedIn.value!!) {
                            Button(onClick = {
                                oneTapClient.beginSignIn(signInRequest)
                                    .addOnSuccessListener(
                                        this@MainActivity
                                    ) { result ->
                                        try {
                                            startIntentSenderForResult(
                                                result.pendingIntent.intentSender, REQ_ONE_TAP,
                                                null, 0, 0, 0
                                            )
                                            Log.e(TAG, "One Tap Success ")
                                        } catch (e: SendIntentException) {
                                            Log.e(TAG, "Couldn't start One Tap UI: " + e.localizedMessage)
                                        }
                                    }
                                    .addOnFailureListener(this@MainActivity) { e -> // No saved credentials found. Launch the One Tap sign-up flow, or
                                        // do nothing and continue presenting the signed-out UI.
                                        e.localizedMessage?.let { Log.d(TAG, it) }
                                        signedIn.postValue(false)
                                    }
                            }) {
                                Text("Sign In")
                            }
                        }
                    }

                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_ONE_TAP -> try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    // Got an ID token from Google. Use it to authenticate
                    // with your backend.
                    Log.d(TAG, "Got ID token.")

                    val client = HttpClient {
                        install(HttpCookies)
                    }
                    CoroutineScope(IO).launch {
                        val response: HttpResponse = client.post("https://cslinux0.comp.hkbu.edu.hk/~kennycheng/breadline/api/v2.0/auth/token") {
                            contentType(ContentType.Application.Json)
                            setBody("{\"idToken\":\"${idToken}\"}")
                        }
                        Log.d(TAG, response.body())
                    }
                }
            } catch (e: ApiException) {
                // ...
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OneTapSignInTheme {
        Greeting("Android")
    }
}