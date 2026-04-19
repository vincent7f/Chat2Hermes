package com.herdroid.app.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** 无活动网络时（如飞行模式）提前提示；纯局域网服务在已连 Wi‑Fi 时通常仍有 INTERNET 能力位。 */
fun Context.hasActiveNetwork(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
