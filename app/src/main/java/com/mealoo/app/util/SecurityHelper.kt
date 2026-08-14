package com.mealoo.app.util

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityHelper {

    private const val PREFS_NAME = "secure_db_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    
    // GCM parameters
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    /**
     * Retrieves or generates a secure passphrase for the Room Database (SQLCipher).
     * The passphrase is saved in EncryptedSharedPreferences so it persists across sessions.
     */
    fun getDatabasePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        var passphraseStr = sharedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphraseStr == null) {
            passphraseStr = UUID.randomUUID().toString() + UUID.randomUUID().toString()
            sharedPrefs.edit().putString(KEY_DB_PASSPHRASE, passphraseStr).apply()
        }
        return passphraseStr.toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Derives a stable 256-bit AES key from the user's Firebase UID.
     * Use SHA-256 to ensure exactly 32 bytes.
     */
    private fun getCloudSecretKey(): SecretKeySpec? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val sha256 = MessageDigest.getInstance("SHA-256")
        // Optionally add a fixed salt to the UID
        val seed = "MEALOO_CLOUD_SALT_$uid"
        val keyBytes = sha256.digest(seed.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext JSON data for Cloud Backup using AES-GCM.
     * Returns Base64 encoded string: "IV:Ciphertext"
     */
    fun encryptCloudData(plaintext: String): String {
        val secretKey = getCloudSecretKey()
            ?: throw IllegalStateException("Cannot encrypt cloud data - user not logged in")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        
        val ivStr = Base64.encodeToString(iv, Base64.NO_WRAP)
        val textStr = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$ivStr:$textStr"
    }

    /**
     * Decrypts ciphertext (Base64 "IV:Ciphertext") downloaded from Cloud Backup using AES-GCM.
     * If the data is not in "IV:Ciphertext" format, assuming it is plain JSON.
     */
    fun decryptCloudData(encodedString: String): String {
        val parts = encodedString.split(":")
        if (parts.size != 2) {
            // Assume it's an old unencrypted backup
            return encodedString
        }

        val secretKey = getCloudSecretKey()
            ?: throw IllegalStateException("Cannot decrypt cloud data - user not logged in")

        try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            return String(plaintextBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails (e.g., wrong key/account), throw exception
            e.printStackTrace()
            throw SecurityException("Failed to decrypt cloud backup (wrong account or corrupted)", e)
        }
    }
}
