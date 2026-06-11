package com.vrt.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.common.BitMatrix

object BarcodeUtils {

    fun generateBarcodeBitmap(text: String, width: Int, height: Int): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, width, height)
            val bitmap = toBitmap(bitMatrix);
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQrCodeBitmap(text: String, size: Int): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = toBitmap(bitMatrix);
            bitmap;
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toBitmap(bitMatrix: BitMatrix?): Bitmap? {
        if (bitMatrix == null)
            return null

            val bitmap = createBitmap(bitMatrix.width, bitMatrix.height)
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitMatrix.height) {
                    bitmap[x, y] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
        return bitmap;
    }
}
