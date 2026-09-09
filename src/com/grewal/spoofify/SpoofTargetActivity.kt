/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.spoofify

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R

class SpoofTargetActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target =
            intent.getStringExtra(EXTRA_TARGET)?.let { name ->
                SpoofTarget.values().firstOrNull { it.name == name }
            }
        if (target == null) {
            finish()
            return
        }

        title = getString(target.titleRes)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, SpoofTargetFragment.newInstance(target), TAG)
            .commit()
    }

    companion object {
        private const val TAG = "SpoofTargetFragment"

        const val EXTRA_TARGET = "target"
    }
}
