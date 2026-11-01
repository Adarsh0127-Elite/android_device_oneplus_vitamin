/*
 * Copyright (C) 2018-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.display

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DcDimmingTileService : TileService() {

    private fun updateUI(enabled: Boolean) {
        qsTile?.let { tile ->
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateUI(DcDimmingUtils.isEnabled(this))
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val newState = tile.state == Tile.STATE_INACTIVE
        DcDimmingUtils.setEnabled(this, newState)
        updateUI(newState)
    }

    companion object {
        @JvmStatic
        fun updateTile(context: Context) {
            requestListeningState(
                context,
                ComponentName(context, DcDimmingTileService::class.java)
            )
        }
    }
}
