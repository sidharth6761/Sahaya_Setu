/*
package com.sid.civilq_1.Authentication.GoogleSignIn

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

*/
/**
 * ActivityResultContract to launch Google One Tap Sign-In and return FirebaseAuth user
 *//*

class GoogleSignInContract(
    private val context: Context
) : ActivityResultContract<Unit, FirebaseAuth?>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    override fun createIntent(context: Context, input: Unit): Intent {
        // This is not used; launching handled in `launch()`
        return Intent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): FirebaseAuth? {
        // Result is handled in launcher via onResult lambda
        return auth
    }

     fun launch(input: Unit, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("YOUR_WEB_CLIENT_ID") // replace with your Web Client ID
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    // Convert PendingIntent to IntentSenderRequest
                    val intentSenderRequest: IntentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                    launcher.launch()
                } catch (e: Exception) {
                    Log.e("GoogleSignInContract", "Failed to launch Google Sign-In UI", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignInContract", "Google Sign-In failed", e)
            }
    }

    */
/**
     * Use this function to sign in with Google ID token to Firebase
     *//*

    suspend fun firebaseSignInWithToken(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
        } catch (e: Exception) {
            Log.e("GoogleSignInContract", "Firebase sign-in failed", e)
        }
    }
}
*/
