package com.deploydulupulangnanti.videodownloaderpremium

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object PrivateDns {
    const val HOST = "dns.adguard-dns.com"
    fun connected(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val links = manager.getLinkProperties(network) ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return links.isPrivateDnsActive && links.privateDnsServerName.equals(HOST, ignoreCase = true) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
