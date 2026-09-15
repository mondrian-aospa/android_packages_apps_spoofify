/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.spoofify

import android.content.Context
import android.util.Log
import android.util.Xml
import java.io.StringReader
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

object SpoofStatus {

    private const val TAG = "SpoofStatus"

    private const val VERSION_PREFIX = "VERSION."

    private val VERSION_FIELDS =
        setOf(
            "SECURITY_PATCH",
            "DEVICE_INITIAL_SDK_INT",
            "RELEASE",
            "RELEASE_OR_CODENAME",
            "INCREMENTAL",
            "SDK_INT",
            "PREVIEW_SDK_INT",
            "BASE_OS",
            "CODENAME",
            "MEDIA_PERFORMANCE_CLASS",
        )

    fun isValid(target: SpoofTarget, blob: String?): Boolean {
        if (blob.isNullOrBlank()) return false
        return when (target) {
            SpoofTarget.PROPS -> props(blob).isNotEmpty()
            SpoofTarget.KEYBOX -> isUsableKeybox(blob)
        }
    }

    fun blobSummary(context: Context, target: SpoofTarget, blob: String?): String {
        if (blob.isNullOrBlank()) return context.getString(R.string.status_unset)
        if (!isValid(target, blob)) return context.getString(R.string.status_invalid)
        return when (target) {
            SpoofTarget.PROPS -> context.getString(R.string.status_props_set, props(blob).size)

            SpoofTarget.KEYBOX -> context.getString(R.string.status_keybox_set)
        }
    }

    fun props(blob: String?): List<Pair<String, String>> {
        if (blob.isNullOrBlank()) return emptyList()
        val root =
            try {
                JSONObject(blob)
            } catch (e: JSONException) {
                Log.d(TAG, "Props blob is not a JSON object", e)
                return emptyList()
            }

        val fields = mutableMapOf<String, String>()
        for (name in root.keys()) {
            val value = root.opt(name)
            if (
                value == null ||
                    value == JSONObject.NULL ||
                    value is JSONObject ||
                    value is JSONArray
            ) {
                continue
            }
            val field = normalizeKey(name.trim())
            val text = value.toString().trim()
            if (field.isEmpty() || text.isEmpty()) continue
            fields[field] = text
        }
        return fields.toList().sortedBy { it.first }
    }

    private fun normalizeKey(name: String): String =
        when {
            name.startsWith(VERSION_PREFIX) -> name
            name == "FIRST_API_LEVEL" -> VERSION_PREFIX + "DEVICE_INITIAL_SDK_INT"
            name == "BUILD_ID" -> "ID"
            name in VERSION_FIELDS -> VERSION_PREFIX + name
            else -> name
        }

    /** Mirrors KeyProviderManager in frameworks/base, so only keyboxes it will load pass. */
    private fun isUsableKeybox(blob: String): Boolean {
        var keyboxCount: Int? = null
        var algorithm: String? = null
        var keyCertificates = 0
        val privateKeys = mutableSetOf<String>()
        val certificates = mutableMapOf<String, Int>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(blob))
            var event = parser.next()
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "NumberOfKeyboxes" -> {
                            parser.next()
                            keyboxCount = parser.text?.trim()?.toIntOrNull() ?: return false
                        }

                        "Key" -> {
                            algorithm =
                                when (parser.getAttributeValue(null, "algorithm")?.lowercase()) {
                                    "ecdsa" -> "EC"
                                    "rsa" -> "RSA"
                                    else -> null
                                }
                            keyCertificates = 0
                        }

                        "PrivateKey" -> {
                            if (!isPem(parser)) return false
                            parser.next()
                            algorithm?.let {
                                if (parser.text == null) return false
                                privateKeys.add(it)
                            }
                        }

                        "Certificate" -> {
                            if (!isPem(parser)) return false
                            val alg = algorithm
                            if (alg != null && keyCertificates < 3) {
                                parser.next()
                                if (parser.text == null) return false
                                keyCertificates++
                                certificates[alg] = maxOf(certificates[alg] ?: 0, keyCertificates)
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Couldn't parse keybox", e)
            return false
        }
        return keyboxCount == 1 &&
            listOf("EC", "RSA").all { it in privateKeys && (certificates[it] ?: 0) >= 3 }
    }

    private fun isPem(parser: XmlPullParser): Boolean =
        "pem".equals(parser.getAttributeValue(null, "format"), ignoreCase = true)

    fun keybox(context: Context, blob: String?): List<Pair<String, String>> {
        if (blob.isNullOrBlank()) return emptyList()

        var deviceId: String? = null
        val algorithms = mutableListOf<String>()
        var certificates = 0
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(blob))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val name = parser.name
                    when {
                        "Keybox".equals(name, true) ->
                            deviceId =
                                parser.getAttributeValue(null, "DeviceID")?.trim()?.ifEmpty { null }

                        "Key".equals(name, true) ->
                            parser
                                .getAttributeValue(null, "algorithm")
                                ?.trim()
                                ?.ifEmpty { null }
                                ?.let { algorithms.add(it.uppercase()) }

                        "Certificate".equals(name, true) -> certificates++
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Couldn't parse keybox", e)
        }

        val rows = mutableListOf<Pair<String, String>>()
        deviceId?.let { rows.add(context.getString(R.string.keybox_device_id) to it) }
        if (algorithms.isNotEmpty()) {
            rows.add(context.getString(R.string.keybox_keys) to algorithms.joinToString(", "))
        }
        if (certificates > 0) {
            rows.add(context.getString(R.string.keybox_certificates) to certificates.toString())
        }
        return rows
    }
}
