package com.example.prexam.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val isGuest: Boolean = false
)

object FirebaseAuthHelper {

    private val auth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            // Firebase may not be initialized if google-services.json is absent
            FirebaseAuth.getInstance()
        }
    }

    fun getGoogleSignInClient(context: Context, webClientId: String = ""): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (webClientId.isNotBlank()) {
            gsoBuilder.requestIdToken(webClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getCurrentUser(context: Context): AppUser? {
        try {
            val fbUser = auth.currentUser
            if (fbUser != null) {
                return AppUser(
                    uid = fbUser.uid,
                    displayName = fbUser.displayName ?: "User",
                    email = fbUser.email ?: "",
                    photoUrl = fbUser.photoUrl?.toString(),
                    isGuest = fbUser.isAnonymous
                )
            }
        } catch (_: Exception) { }

        // Fallback to GoogleSignInAccount if Firebase Auth is not active
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {
            return AppUser(
                uid = googleAccount.id ?: "google_user",
                displayName = googleAccount.displayName ?: "User",
                email = googleAccount.email ?: "",
                photoUrl = googleAccount.photoUrl?.toString(),
                isGuest = false
            )
        }

        return null
    }

    fun handleGoogleSignInResult(
        completedTask: Task<GoogleSignInAccount>,
        onSuccess: (AppUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (!idToken.isNullOrBlank()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        onSuccess(
                            AppUser(
                                uid = user?.uid ?: account.id ?: "",
                                displayName = user?.displayName ?: account.displayName ?: "User",
                                email = user?.email ?: account.email ?: "",
                                photoUrl = user?.photoUrl?.toString() ?: account.photoUrl?.toString()
                            )
                        )
                    }
                    .addOnFailureListener { e ->
                        // Fallback to Google Account details if Firebase server auth is unconfigured
                        onSuccess(
                            AppUser(
                                uid = account.id ?: "google_user",
                                displayName = account.displayName ?: "User",
                                email = account.email ?: "",
                                photoUrl = account.photoUrl?.toString()
                            )
                        )
                    }
            } else {
                onSuccess(
                    AppUser(
                        uid = account?.id ?: "google_user",
                        displayName = account?.displayName ?: "User",
                        email = account?.email ?: "",
                        photoUrl = account?.photoUrl?.toString()
                    )
                )
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun signInAsGuest(
        onSuccess: (AppUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val user = result.user
                    onSuccess(
                        AppUser(
                            uid = user?.uid ?: "guest_123",
                            displayName = "Guest Scientist",
                            email = "guest@biokt.app",
                            photoUrl = null,
                            isGuest = true
                        )
                    )
                }
                .addOnFailureListener {
                    // Local guest fallback
                    onSuccess(
                        AppUser(
                            uid = "local_guest",
                            displayName = "Guest Scientist",
                            email = "guest@biokt.app",
                            photoUrl = null,
                            isGuest = true
                        )
                    )
                }
        } catch (e: Exception) {
            onSuccess(
                AppUser(
                    uid = "local_guest",
                    displayName = "Guest Scientist",
                    email = "guest@biokt.app",
                    photoUrl = null,
                    isGuest = true
                )
            )
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        try {
            auth.signOut()
        } catch (_: Exception) { }

        try {
            getGoogleSignInClient(context).signOut().addOnCompleteListener {
                onComplete()
            }
        } catch (_: Exception) {
            onComplete()
        }
    }
}
