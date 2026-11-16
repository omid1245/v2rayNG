package com.v2ray.ang.ui.panel

import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager

/**
 * Keeps exactly one subscription = sub_url from /api/info and removes every other one.
 * Stage 1: called after login. Later stages: start, before connect, WorkManager, retry.
 */
object PanelRepository {
    private const val SUB_REMARKS = "پنل کاربری"

    /** Replaces all subscriptions with the panel one and imports its configs. Returns false on failure. */
    fun importSubscription(subUrl: String): Boolean {
        if (subUrl.isBlank()) return false

        val keepGuid = MmkvManager.decodeSubsList().firstOrNull()
            ?: MmkvManager.decodeSubscriptions().firstOrNull()?.guid
            ?: ""
        // Remove every other subscription and its servers.
        MmkvManager.decodeSubscriptions().forEach { sub ->
            if (sub.guid != keepGuid) MmkvManager.removeSubscription(sub.guid)
        }
        MmkvManager.removeAllServer()

        val item = SubscriptionItem(
            remarks = SUB_REMARKS,
            url = subUrl,
            enabled = true,
            autoUpdate = true,
            updateInterval = 180,
        )
        MmkvManager.encodeSubscription(keepGuid, item)
        val guid = MmkvManager.decodeSubsList().firstOrNull() ?: return false
        val result = AngConfigManager.updateConfigViaSub(SubscriptionCache(guid, item))
        return result.successCount > 0
    }
}
