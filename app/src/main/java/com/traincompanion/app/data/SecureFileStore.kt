package com.traincompanion.app.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Encrypted-at-rest local storage for ticket/Aadhaar document bytes and JSON metadata tables.
 * Backed by Android Keystore (AES-256-GCM); nothing here ever leaves the device.
 */
class SecureFileStore(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val secureDir: File by lazy {
        File(context.filesDir, "secure").apply { mkdirs() }
    }

    private val docsDir: File by lazy {
        File(secureDir, "documents").apply { mkdirs() }
    }

    private fun encryptedFile(file: File): EncryptedFile =
        EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

    fun documentFile(fileName: String): File = File(docsDir, fileName)

    fun writeDocumentBytes(fileName: String, bytes: ByteArray) {
        val file = documentFile(fileName)
        if (file.exists()) file.delete()
        encryptedFile(file).openFileOutput().use { it.write(bytes) }
    }

    fun readDocumentBytes(fileName: String): ByteArray {
        val file = documentFile(fileName)
        return encryptedFile(file).openFileInput().use { it.readBytes() }
    }

    fun deleteDocument(fileName: String) {
        documentFile(fileName).delete()
    }

    fun writeJson(name: String, content: String) {
        val file = File(secureDir, name)
        if (file.exists()) file.delete()
        encryptedFile(file).openFileOutput().use { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    fun readJson(name: String): String? {
        val file = File(secureDir, name)
        if (!file.exists()) return null
        return encryptedFile(file).openFileInput().use { it.readBytes() }.toString(Charsets.UTF_8)
    }
}
