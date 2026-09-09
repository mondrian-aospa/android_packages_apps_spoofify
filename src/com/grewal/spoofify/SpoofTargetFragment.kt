/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.spoofify

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import java.net.HttpURLConnection
import java.net.URL

class SpoofTargetFragment : SettingsBasePreferenceFragment() {

    private lateinit var store: SpoofStore
    private lateinit var target: SpoofTarget

    private lateinit var masterSwitch: SwitchPreferenceCompat
    private lateinit var clearPreference: Preference

    private lateinit var detailsCategory: PreferenceCategory

    private val profileRows = mutableListOf<Preference>()

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importFromUri(uri)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        store = SpoofStore(context)
        target = SpoofTarget.valueOf(requireArguments().getString(ARG_TARGET)!!)

        val screen = preferenceManager.createPreferenceScreen(context)

        masterSwitch =
            SwitchPreferenceCompat(context).apply {
                title = getString(target.switchTitleRes)
                isPersistent = false
                isChecked = store.isEnabled(target)
                setOnPreferenceChangeListener { _, newValue ->
                    store.setEnabled(target, newValue as Boolean)
                    refresh(newValue)
                    true
                }
            }
        screen.addPreference(masterSwitch)

        detailsCategory =
            PreferenceCategory(context)
                .apply { title = getString(target.detailsTitleRes) }
                .also { screen.addPreference(it) }

        val profile =
            PreferenceCategory(context).apply { title = getString(R.string.category_profile) }
        screen.addPreference(profile)

        profile.addPreference(
            action(R.string.action_import) { openDocument.launch(arrayOf("*/*")) }
        )
        profile.addPreference(action(R.string.action_url) { showUrlDialog() })
        clearPreference =
            action(R.string.action_clear) {
                writeBlob(null)
                toast(R.string.toast_cleared)
            }
        profile.addPreference(clearPreference)

        preferenceScreen = screen
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::target.isInitialized) activity?.setTitle(target.titleRes)
        if (::store.isInitialized) refresh()
    }

    private fun action(titleRes: Int, onClick: () -> Unit): Preference =
        Preference(requireContext())
            .apply {
                title = getString(titleRes)
                setOnPreferenceClickListener {
                    onClick()
                    true
                }
            }
            .also { profileRows.add(it) }

    private fun refresh(enabled: Boolean = store.isEnabled(target)) {
        val blob = store.blobOf(target)
        masterSwitch.isChecked = enabled
        masterSwitch.summary = getString(if (enabled) target.switchOnRes else target.switchOffRes)
        profileRows.forEach { it.isEnabled = enabled }
        clearPreference.isEnabled = enabled && !blob.isNullOrBlank()
        populateDetails(blob, enabled)
    }

    private fun populateDetails(blob: String?, enabled: Boolean) {
        val rows =
            if (SpoofStatus.isValid(target, blob)) {
                when (target) {
                    SpoofTarget.PROPS -> SpoofStatus.props(blob)
                    SpoofTarget.KEYBOX -> SpoofStatus.keybox(requireContext(), blob)
                }
            } else {
                emptyList()
            }

        detailsCategory.removeAll()
        if (rows.isEmpty()) {
            detailsCategory.addPreference(
                detailRow(SpoofStatus.blobSummary(requireContext(), target, blob), null, enabled)
            )
        } else {
            rows.forEach { (label, value) ->
                detailsCategory.addPreference(detailRow(label, value, enabled))
            }
        }
    }

    private fun detailRow(label: String, value: String?, enabled: Boolean): Preference =
        Preference(requireContext()).apply {
            title = label
            summary = value
            isSelectable = false
            isEnabled = enabled
        }

    private fun writeBlob(value: String?) {
        store.setBlob(target, value)
        refresh()
    }

    private fun applyImport(value: String) {
        when {
            value.isBlank() -> toast(R.string.toast_empty)
            !SpoofStatus.isValid(target, value) -> toast(target.invalidRes)
            else -> {
                writeBlob(value)
                toast(R.string.toast_applied)
            }
        }
    }

    private fun showUrlDialog() {
        val input =
            EditText(requireContext()).apply {
                inputType = InputType.TYPE_TEXT_VARIATION_URI
                setText(getString(target.defaultUrlRes))
                setSelection(text.length)
            }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_url)
            .setView(input)
            .setPositiveButton(R.string.fetch) { _, _ ->
                fetchFromUrl(input.text.toString().trim())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importFromUri(uri: Uri) {
        try {
            val text =
                requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                }
            applyImport(text.orEmpty())
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            toast(R.string.toast_error)
        }
    }

    private fun fetchFromUrl(url: String) {
        if (url.isEmpty()) {
            toast(R.string.toast_empty)
            return
        }
        toast(R.string.toast_fetching)
        Thread {
                val body =
                    try {
                        val conn =
                            (URL(url).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 15000
                                readTimeout = 15000
                                requestMethod = "GET"
                            }
                        try {
                            if (conn.responseCode in 200..299) {
                                conn.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                null
                            }
                        } finally {
                            conn.disconnect()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fetch failed", e)
                        null
                    }
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    if (body.isNullOrBlank()) {
                        toast(R.string.toast_fetch_failed)
                    } else {
                        applyImport(body)
                    }
                }
            }
            .start()
    }

    private fun toast(resId: Int) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "SpoofTargetFragment"
        private const val ARG_TARGET = "target"

        fun newInstance(target: SpoofTarget) =
            SpoofTargetFragment().apply {
                arguments = Bundle().apply { putString(ARG_TARGET, target.name) }
            }
    }
}
