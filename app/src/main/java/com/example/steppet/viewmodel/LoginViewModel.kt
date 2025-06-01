package com.example.steppet.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest

/**
 * ViewModel für Firebase‐basierte Authentifizierung (E-Mail/Passwort).
 * Enthält Login, Registration, Logout und Username‐Änderung.
 * Die Lösch‐Logik (deleteAccount) wurde komplett entfernt.
 */
class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Registriert einen neuen Benutzer per E-Mail/Passwort.
     * onComplete(true, null) bei Erfolg, ansonsten onComplete(false, errorMessage).
     */
    fun registerWithEmail(
        email: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.localizedMessage)
            }
    }

    /**
     * Meldet einen Benutzer per E-Mail/Passwort an.
     * onComplete(true, null) bei Erfolg, ansonsten onComplete(false, errorMessage).
     */
    fun loginWithEmail(
        email: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { exception ->
                onComplete(false, exception.localizedMessage)
            }
    }

    /**
     * Ändert den Nutzernamen (displayName) des aktuell eingeloggten Firebase-Users.
     * onComplete(true, null) bei Erfolg, ansonsten onComplete(false, errorMessage).
     */
    fun changeUsername(
        newUsername: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        if (user != null) {
            val profileUpdates = userProfileChangeRequest {
                displayName = newUsername
            }
            user.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    onComplete(true, null)
                }
                .addOnFailureListener { exception ->
                    onComplete(false, exception.localizedMessage)
                }
        } else {
            onComplete(false, "No user is currently logged in")
        }
    }

    /**
     * Loggt den aktuell eingeloggten Benutzer aus.
     */
    fun logout() {
        auth.signOut()
    }
}
