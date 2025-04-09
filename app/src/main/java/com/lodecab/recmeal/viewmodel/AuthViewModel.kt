package com.lodecab.recmeal.viewmodel

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.lodecab.recmeal.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context

) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Check the initial authentication state
        firebaseAuth.currentUser?.let {
            _authState.value = AuthState.SignedIn(it.uid, it.email ?: "")
        } ?: run {
            _authState.value = AuthState.SignedOut
        }

        // Listen for authentication state changes
        firebaseAuth.addAuthStateListener { auth ->
            auth.currentUser?.let {
                _authState.value = AuthState.SignedIn(it.uid, it.email ?: "")
            } ?: run {
                _authState.value = AuthState.SignedOut
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    _authState.value = AuthState.SignedIn(user?.uid ?: "", user?.email ?: "")
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Sign-up failed")
                }
            }
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    _authState.value = AuthState.SignedIn(user?.uid ?: "", user?.email ?: "")
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Sign-in failed")
                }
            }
    }

    fun signInWithGoogle(launcher: ActivityResultLauncher<Intent>, context: android.content.Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    fun handleGoogleSignInResult(data: Intent?) {
        _authState.value = AuthState.Loading
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        _authState.value = AuthState.SignedIn(user?.uid ?: "", user?.email ?: "")
                    } else {
                        _authState.value =
                            AuthState.Error(authTask.exception?.message ?: "Google Sign-In failed")
                    }
                }
        } catch (e: ApiException) {
            _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        // Google Sign-In client should also be signed out to allow account selection on next login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        val googleSignInClient = GoogleSignIn.getClient(firebaseAuth.app.applicationContext, gso)
        googleSignInClient.signOut()
        _authState.value = AuthState.SignedOut
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class SignedIn(val uid: String, val email: String) : AuthState()
    object SignedOut : AuthState()
    data class Error(val message: String) : AuthState()
}