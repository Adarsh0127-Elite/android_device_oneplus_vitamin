/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.power;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.R;

public class PauseChargingTileService extends TileService {

    private PauseChargingController mPauseController;
    private boolean mEnabled;

    @Override
    public void onCreate() {
        super.onCreate();
        mPauseController = PauseChargingController.getInstance(this);
    }

    @Override
    public void onStartListening() {
        int status = mPauseController.getPauseChargingStatus();
        mEnabled = status != Constants.PAUSE_OFF;
        updateTileState(status);
    }

    @Override
    public void onClick() {
        if (PauseChargingController.isLineageChargingControlEnabled(this)) {
            Toast.makeText(this, R.string.pause_charging_disabled_by_lineage_health, Toast.LENGTH_SHORT).show();
            updateTileState(Constants.PAUSE_OFF);
            return;
        }

        boolean enabled = mPauseController.getPauseChargingStatus() != Constants.PAUSE_OFF;
        if (mEnabled == enabled) {
            boolean success;
            if (mEnabled) {
                success = mPauseController.disablePauseCharging();
            } else {
                success = mPauseController.enablePauseCharging();
            }
            if (success) {
                mEnabled = !mEnabled;
                updateTileState(mPauseController.getPauseChargingStatus());
            }
        }
    }

    private void updateTileState(int status) {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean active = status != Constants.PAUSE_OFF;
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.pause_charging_title));
        tile.setContentDescription(getString(R.string.pause_charging_summary));
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_pause_charging));
        tile.updateTile();
    }
}
