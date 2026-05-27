/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.routedns.vpn.util

import android.content.RestrictionsManager
import androidx.core.content.getSystemService
import io.routedns.vpn.Application

object AdminKnobs {
    private val restrictions: RestrictionsManager? = Application.get().getSystemService()
    val disableConfigExport: Boolean
        get() = restrictions?.applicationRestrictions?.getBoolean("disable_config_export", false)
            ?: false
}
