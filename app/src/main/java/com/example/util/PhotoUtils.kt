package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

object PhotoUtils {
    private val bitmapCache = object : LruCache<Int, ImageBitmap>(40) {}

    val PRESET_AVATARS = listOf(
        "avatar:man" to "👨🏽",
        "avatar:woman" to "👩🏽",
        "avatar:elder" to "👳🏽‍♂️",
        "avatar:youth" to "🧑🏽",
        "avatar:shop" to "🏪",
        "avatar:home" to "🏡"
    )

    fun bitmapToBase64Thumbnail(bitmap: Bitmap, maxDimension: Int = 180, quality: Int = 72): String {
        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height).coerceAtMost(1f)
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        val bytes = output.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP).take(58000)
    }

    fun uriToBase64Thumbnail(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                bitmapToBase64Thumbnail(original)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
        if (base64Str.isBlank() || base64Str.startsWith("avatar:")) return null
        val key = base64Str.hashCode()
        bitmapCache.get(key)?.let { return it }
        return try {
            val clean = base64Str.substringAfter(",")
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val imageBitmap = bmp.asImageBitmap()
            bitmapCache.put(key, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getPresetEmoji(photoBase64: String): String? {
        if (!photoBase64.startsWith("avatar:")) return null
        return PRESET_AVATARS.firstOrNull { it.first == photoBase64 }?.second ?: "🧑🏽"
    }
}

fun formatRupees(amount: Long): String {
    val absAmount = kotlin.math.abs(amount)
    val formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).format(absAmount)
    return "₹$formatted"
}

fun formatTimestamp(date: Date?): String {
    if (date == null) return "अभी (Just now)"
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.forLanguageTag("en-IN"))
    return sdf.format(date)
}
