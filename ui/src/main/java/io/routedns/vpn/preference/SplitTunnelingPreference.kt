/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package io.routedns.vpn.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.Preference
import io.routedns.vpn.Application
import io.routedns.vpn.R
import io.routedns.vpn.activity.SettingsActivity
import io.routedns.vpn.util.SplitTunneling
import io.routedns.vpn.util.activity
import io.routedns.vpn.util.lifecycleScope
import io.routedns.vpn.viewmodel.ConfigProxy
import kotlinx.coroutines.launch

/**
 * Opens per-app split tunneling for the tunnel used most recently (or the active tunnel).
 */
class SplitTunnelingPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    init {
        isPersistent = false
        isSingleLineTitle = false
    }

    override fun getTitle() = context.getString(R.string.split_tunneling_title)

    override fun getSummary(): CharSequence = summaryText ?: context.getString(R.string.split_tunneling_summary)

    private var summaryText: CharSequence? = null

    fun refreshSummary() {
        lifecycleScope.launch {
            summaryText = SplitTunneling.settingsSummary(context)
            notifyChanged()
        }
    }

    override fun onClick() {
        val fragment = activity.supportFragmentManager
            .findFragmentById(R.id.settings_container) as? SettingsActivity.SettingsFragment
            ?: return
        lifecycleScope.launch {
            if (!SplitTunneling.isSupported()) {
                SplitTunneling.showUnsupportedMessage(fragment)
                return@launch
            }
            val tunnel = SplitTunneling.resolveSettingsTunnel()
            if (tunnel == null) {
                Toast.makeText(context, R.string.split_tunneling_no_tunnel, Toast.LENGTH_LONG).show()
                return@launch
            }
            val interfaceProxy = ConfigProxy(tunnel.getConfigAsync()).`interface`
            SplitTunneling.configure(fragment, interfaceProxy, tunnel) {
                refreshSummary()
            }
        }
    }
}
