/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.power

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.settings.oplusparts.R

class GentleChargingTileService : TileService() {

    private fun updateUI(enabled: Boolean) {
        qsTile?.let { tile ->
            if (!GentleChargingUtils.isSupported()) {
                tile.state = Tile.STATE_UNAVAILABLE
                tile.subtitle = null
            } else if (enabled) {
                val watt = GentleChargingUtils.getWattCap(this)
                tile.state = Tile.STATE_ACTIVE
                tile.subtitle = getString(R.string.gentle_charging_tile_active_sub, watt)
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitle = getString(R.string.gentle_charging_tile_inactive_sub)
            }
            tile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateUI(GentleChargingUtils.isEnabled(this))
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        if (tile.state == Tile.STATE_UNAVAILABLE) return
        val newState = tile.state == Tile.STATE_INACTIVE
        GentleChargingUtils.setGentleCharging(this, newState)
        updateUI(newState)
    }

    companion object {
        @JvmStatic
        fun updateTile(context: Context) {
            requestListeningState(
                context,
                ComponentName(context, GentleChargingTileService::class.java)
            )
        }
    }
}
