/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grewal.spoofify

enum class SpoofTarget(
    val titleRes: Int,
    val switchTitleRes: Int,
    val switchOnRes: Int,
    val switchOffRes: Int,
    val defaultUrlRes: Int,
    val detailsTitleRes: Int,
    val invalidRes: Int,
) {
    KEYBOX(
        titleRes = R.string.keybox_title,
        switchTitleRes = R.string.keybox_switch_title,
        switchOnRes = R.string.keybox_switch_on,
        switchOffRes = R.string.keybox_switch_off,
        defaultUrlRes = R.string.url_default_keybox,
        detailsTitleRes = R.string.category_keybox_details,
        invalidRes = R.string.toast_invalid_keybox,
    ),
    PROPS(
        titleRes = R.string.props_title,
        switchTitleRes = R.string.props_switch_title,
        switchOnRes = R.string.props_switch_on,
        switchOffRes = R.string.props_switch_off,
        defaultUrlRes = R.string.url_default_props,
        detailsTitleRes = R.string.category_properties,
        invalidRes = R.string.toast_invalid_props,
    ),
}
