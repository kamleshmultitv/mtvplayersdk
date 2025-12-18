package com.app.sample.utils

import android.util.Base64
import java.nio.charset.StandardCharsets

class ApiEncryptionHelper {
    fun decryptionResponse(encryptionString: String): String {
        val mcipher = MultitvCipher()
        var str: String? = null
        try {
            str = String(mcipher.decryptmyapi(encryptionString))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return str.orEmpty()
    }

    companion object {
        fun convertStringToBase64(text: String): String {
            val data = text.toByteArray(StandardCharsets.UTF_8)
            return Base64.encodeToString(data, Base64.DEFAULT)
        }
    }
}