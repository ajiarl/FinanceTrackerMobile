package com.sena.financetracker.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Native Vibrator & Haptic Engine for Neobrutalism UI interactions.
 * Directly drives hardware vibration motor with proper API level fallbacks,
 * strictly guarded by the application's isHapticEnabled setting.
 */
object NeobrutalHapticEngine {

    /**
     * Micro sharp feedback (~15ms / TICK) for navigation tabs, toggles, switches.
     */
    fun tick(context: Context?, isEnabled: Boolean) {
        if (!isEnabled || context == null) return
        vibrate(context, durationMs = 15L, effectId = VibrationEffect.EFFECT_TICK)
    }

    /**
     * Heavy impactful feedback (~40ms / HEAVY_CLICK) for FAB, submit actions, delete confirmations, CSV export.
     */
    fun heavyClick(context: Context?, isEnabled: Boolean) {
        if (!isEnabled || context == null) return
        vibrate(context, durationMs = 40L, effectId = VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    private fun vibrate(context: Context, durationMs: Long, effectId: Int) {
        runCatching {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Try predefined effect first
                try {
                    vibrator.vibrate(VibrationEffect.createPredefined(effectId))
                } catch (e: Throwable) {
                    android.util.Log.w("FinanceTracker", "Predefined vibration effect $effectId gagal, fallback ke one-shot", e)
                    // Fallback to one-shot effect if predefined not supported
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Throwable) {
            android.util.Log.e("FinanceTracker", "Gagal mendapatkan service Vibrator dari context", e)
            null
        }
    }
}
