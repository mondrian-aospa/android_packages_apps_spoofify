/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.spoofify

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class SpoofifyFragment : SettingsBasePreferenceFragment() {

    private lateinit var store: SpoofStore

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        store = SpoofStore(requireContext())
        rebuild()
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.app_name)
        if (::store.isInitialized) rebuild()
    }

    private fun rebuild() {
        val context = requireContext()
        val screen = preferenceManager.createPreferenceScreen(context)

        val integrity =
            PreferenceCategory(context).apply { title = getString(R.string.category_integrity) }
        screen.addPreference(integrity)
        SpoofTarget.values().forEach { integrity.addPreference(targetPreference(it)) }

        val google =
            PreferenceCategory(context).apply { title = getString(R.string.category_google) }
        screen.addPreference(google)
        google.addPreference(
            SwitchPreferenceCompat(context).apply {
                title = getString(R.string.photos_title)
                summary = getString(R.string.photos_summary)
                isPersistent = false
                isChecked = store.photosSpoof
                setOnPreferenceChangeListener { _, newValue ->
                    store.photosSpoof = newValue as Boolean
                    true
                }
            }
        )

        preferenceScreen = screen
    }

    private fun targetPreference(target: SpoofTarget): Preference =
        Preference(requireContext()).apply {
            title = getString(target.titleRes)
            summary =
                if (store.isEnabled(target)) {
                    SpoofStatus.blobSummary(context, target, store.blobOf(target))
                } else {
                    getString(R.string.status_off)
                }
            setOnPreferenceClickListener {
                startActivity(
                    Intent(requireContext(), SpoofTargetActivity::class.java)
                        .putExtra(SpoofTargetActivity.EXTRA_TARGET, target.name)
                )
                true
            }
        }
}
