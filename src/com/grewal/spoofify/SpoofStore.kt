/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.spoofify

import android.content.Context
import android.provider.Settings

class SpoofStore(context: Context) {

    private val cr = context.contentResolver

    var keybox: String?
        get() = Settings.Secure.getString(cr, KEY_KEYBOX)
        set(value) {
            Settings.Secure.putString(cr, KEY_KEYBOX, value?.ifBlank { null })
        }

    var props: String?
        get() = Settings.Secure.getString(cr, KEY_PROPS)
        set(value) {
            Settings.Secure.putString(cr, KEY_PROPS, value?.ifBlank { null })
        }

    var keyboxEnabled: Boolean
        get() = Settings.Secure.getInt(cr, KEY_SPOOF_KEYBOX, 1) != 0
        set(value) {
            Settings.Secure.putInt(cr, KEY_SPOOF_KEYBOX, if (value) 1 else 0)
        }

    var propsEnabled: Boolean
        get() = Settings.Secure.getInt(cr, KEY_SPOOF_PROPS, 1) != 0
        set(value) {
            Settings.Secure.putInt(cr, KEY_SPOOF_PROPS, if (value) 1 else 0)
        }

    var photosSpoof: Boolean
        get() = Settings.Secure.getInt(cr, KEY_PHOTOS, 1) != 0
        set(value) {
            Settings.Secure.putInt(cr, KEY_PHOTOS, if (value) 1 else 0)
        }

    fun blobOf(target: SpoofTarget): String? =
        when (target) {
            SpoofTarget.KEYBOX -> keybox
            SpoofTarget.PROPS -> props
        }

    fun setBlob(target: SpoofTarget, value: String?) {
        when (target) {
            SpoofTarget.KEYBOX -> keybox = value
            SpoofTarget.PROPS -> props = value
        }
    }

    fun isEnabled(target: SpoofTarget): Boolean =
        when (target) {
            SpoofTarget.KEYBOX -> keyboxEnabled
            SpoofTarget.PROPS -> propsEnabled
        }

    fun setEnabled(target: SpoofTarget, value: Boolean) {
        when (target) {
            SpoofTarget.KEYBOX -> keyboxEnabled = value
            SpoofTarget.PROPS -> propsEnabled = value
        }
    }

    companion object {
        private const val KEY_KEYBOX = "keybox_data"
        private const val KEY_PROPS = "certified_props_data"
        private const val KEY_SPOOF_KEYBOX = "spoof_keybox"
        private const val KEY_SPOOF_PROPS = "spoof_props"
        private const val KEY_PHOTOS = "spoof_google_photos"
    }
}
