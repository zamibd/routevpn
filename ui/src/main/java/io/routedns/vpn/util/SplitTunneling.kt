/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package io.routedns.vpn.util

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.routedns.vpn.Application
import io.routedns.vpn.R
import io.routedns.vpn.backend.AwgQuickBackend
import io.routedns.vpn.backend.RootGoBackend
import io.routedns.vpn.backend.Tunnel
import io.routedns.vpn.fragment.AppListDialogFragment
import io.routedns.vpn.model.ObservableTunnel
import io.routedns.vpn.viewmodel.ConfigProxy
import io.routedns.vpn.viewmodel.InterfaceProxy
import kotlinx.coroutines.launch

object SplitTunneling {

    suspend fun isSupported(): Boolean {
        val backend = Application.getBackend()
        return backend !is RootGoBackend && backend !is AwgQuickBackend
    }

    /** Tunnel to configure from Settings: last used, else any UP tunnel, else first tunnel. */
    suspend fun resolveSettingsTunnel(): ObservableTunnel? {
        val manager = Application.getTunnelManager()
        manager.lastUsedTunnel?.let { return it }
        manager.getTunnels().firstOrNull { it.state == Tunnel.State.UP }?.let { return it }
        return manager.getTunnels().firstOrNull()
    }

    suspend fun settingsSummary(context: Context): CharSequence {
        val tunnel = resolveSettingsTunnel()
            ?: return context.getString(R.string.split_tunneling_no_tunnel)
        val iface = tunnel.getConfigAsync().`interface`
        val appsSummary = formatAppListSummary(
            context,
            iface.includedApplications.size,
            iface.excludedApplications.size,
        )
        return context.getString(R.string.split_tunneling_settings_summary, tunnel.name, appsSummary)
    }

    fun formatAppListSummary(context: Context, includedCount: Int, excludedCount: Int): String =
        when {
            includedCount > 0 -> context.resources.getQuantityString(
                R.plurals.set_included_applications,
                includedCount,
                includedCount,
            )
            excludedCount > 0 -> context.resources.getQuantityString(
                R.plurals.set_excluded_applications,
                excludedCount,
                excludedCount,
            )
            else -> context.getString(R.string.split_tunneling_mode_all)
        }

    fun showUnsupportedMessage(fragment: Fragment) {
        Toast.makeText(
            fragment.requireContext(),
            R.string.split_tunneling_unavailable_root,
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Opens the per-app picker. Updates [interfaceProxy] in memory; optionally persists to [tunnel].
     */
    fun configure(
        fragment: Fragment,
        interfaceProxy: InterfaceProxy,
        tunnel: ObservableTunnel? = null,
        onUpdated: (() -> Unit)? = null,
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            if (!isSupported()) {
                showUnsupportedMessage(fragment)
                return@launch
            }
            openAppPicker(fragment, interfaceProxy, tunnel, onUpdated)
        }
    }

    private fun openAppPicker(
        fragment: Fragment,
        interfaceProxy: InterfaceProxy,
        tunnel: ObservableTunnel?,
        onUpdated: (() -> Unit)?,
    ) {
        var isExcluded = true
        var selectedApps = ArrayList(interfaceProxy.excludedApplications)
        if (selectedApps.isEmpty()) {
            selectedApps = ArrayList(interfaceProxy.includedApplications)
            if (selectedApps.isNotEmpty()) {
                isExcluded = false
            }
        }

        fragment.childFragmentManager.setFragmentResultListener(
            AppListDialogFragment.REQUEST_SELECTION,
            fragment.viewLifecycleOwner
        ) { _, bundle ->
            val newSelections = requireNotNull(bundle.getStringArray(AppListDialogFragment.KEY_SELECTED_APPS))
            val excluded = requireNotNull(bundle.getBoolean(AppListDialogFragment.KEY_IS_EXCLUDED))
            if (excluded) {
                interfaceProxy.includedApplications.clear()
                interfaceProxy.excludedApplications.apply {
                    clear()
                    addAll(newSelections)
                }
            } else {
                interfaceProxy.excludedApplications.clear()
                interfaceProxy.includedApplications.apply {
                    clear()
                    addAll(newSelections)
                }
            }
            if (tunnel != null) {
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val proxy = ConfigProxy(tunnel.getConfigAsync())
                        proxy.`interface`.excludedApplications.clear()
                        proxy.`interface`.excludedApplications.addAll(interfaceProxy.excludedApplications)
                        proxy.`interface`.includedApplications.clear()
                        proxy.`interface`.includedApplications.addAll(interfaceProxy.includedApplications)
                        tunnel.setConfigAsync(proxy.resolve())
                        Toast.makeText(fragment.requireContext(), R.string.split_tunneling_saved, Toast.LENGTH_SHORT).show()
                        onUpdated?.invoke()
                    } catch (e: Throwable) {
                        Toast.makeText(
                            fragment.requireContext(),
                            ErrorMessages[e],
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                onUpdated?.invoke()
            }
        }
        AppListDialogFragment.newInstance(selectedApps, isExcluded)
            .show(fragment.childFragmentManager, null)
    }
}
