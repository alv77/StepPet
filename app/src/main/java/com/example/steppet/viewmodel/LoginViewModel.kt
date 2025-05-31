package com.example.steppet.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel für Firebase-basierte Authentifizierung (E-Mail/Passwort).
 * Bietet genau diese Signatur:
 *   • loginWithEmail(email, password, callback)
 *   • registerWithEmail(email, password, callback)
 *   • deleteAccount(callback)
 *   • logout()
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
     * Löscht das aktuell eingeloggte Benutzerkonto.
     * onComplete(true) bei Erfolg, sonst onComplete(false).
     */
    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.delete()
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        } else {
            onComplete(false)
        }
    }

    /**
     * Loggt den aktuell eingeloggten Benutzer aus.
     */
    fun logout() {
        auth.signOut()
    }
}



