package com.dubproductions.bracket.data.repository

import android.util.Log
import com.dubproductions.bracket.data.Tournament
import com.dubproductions.bracket.data.User
import com.dubproductions.bracket.domain.repository.TournamentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private const val TAG = "Firebase Manager"

class TournamentRepositoryImpl: TournamentRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    override suspend fun registerUser(
        email: String,
        password: String,
        username: String,
        firstName: String,
        lastName: String,
    ): Boolean {
         return try {
             val taskResult = auth
                .createUserWithEmailAndPassword(email, password)
                .await()

             val creationResult = taskResult
                 .user?.uid?.let { userId ->
                     createUserData(
                         userData = User(
                             username = username,
                             userId = userId,
                             email = email,
                             firstName = firstName,
                             lastName = lastName
                         )
                     )
                 }

             if (creationResult == true) {
                 true
             } else {
                 deleteUserSignup()
                 false
             }

        } catch (e: Exception) {
             Log.e(TAG, "registerUser: $e")
             false
        }
    }

    override suspend fun createUserData(
        userData: User
    ): Boolean {
        return try {
            firestore
                .collection("Users")
                .document(userData.userId!!)
                .set(userData)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "createUserData: $e")
            false
        }

    }

    override fun deleteUserSignup() {
        auth.currentUser?.delete()
    }

    override suspend fun signInUser(
        email: String,
        password: String
    ): Boolean {
        return try {
            auth
                .signInWithEmailAndPassword(email, password)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "signInUser: $e")
            false
        }

    }

    override suspend fun resetPassword(email: String): Boolean {
        return try {
            auth
                .sendPasswordResetEmail(email)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "resetPassword: $e")
            false
        }
    }

    override fun checkLoginStatus(): Boolean {
        return auth.currentUser != null
    }

    override fun fetchUserData(
        onComplete: (User?) -> Unit
    ) {
        auth.currentUser?.uid?.let { userId ->
            firestore
                .collection("Users")
                .document(userId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(TAG, "fetchUserData: ${error.message}")
                        onComplete(null)
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        val user = value.toObject<User>()
                        onComplete(user)
                    } else {
                        onComplete(null)
                    }
                }
        }
    }

    override suspend fun fetchTournamentData(tournamentId: String): Tournament? {
        return try {
            firestore
                .collection("Tournaments")
                .document(tournamentId)
                .get()
                .await()
                .toObject<Tournament>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchTournamentData: $e")
            null
        }
    }

}