/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-FileCopyrightText: 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts.refreshrate;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import org.settings.oplusparts.Constants;
import org.settings.oplusparts.R;

public class RefreshRateTileService extends TileService {
    private static final String TAG = "RefreshRateTileService";

    private static final int[] RATES = {
            RefreshRateController.REFRESH_RATE_AUTO,
            RefreshRateController.REFRESH_RATE_60,
            RefreshRateController.REFRESH_RATE_90,
            RefreshRateController.REFRESH_RATE_120
    };

    private RefreshRateController mController;

    @Override
    public void onCreate() {
        super.onCreate();
        mController = RefreshRateController.getInstance(getApplicationContext());
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        int current = mController.getGlobalRefreshRate();
        int next = getNextRate(current);

        updateTileImmediate(next);
        mController.setGlobalRefreshRate(next);

        if (Constants.DEBUG) Log.i(TAG, "Refresh rate: " + current + " -> " + next);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    private int getNextRate(int current) {
        for (int i = 0; i < RATES.length; i++) {
            if (RATES[i] == current) {
                return RATES[(i + 1) % RATES.length];
            }
        }
        return RefreshRateController.REFRESH_RATE_AUTO;
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        int current = mController.getGlobalRefreshRate();
        updateTileWithRate(tile, current);
    }

    private void updateTileImmediate(int fps) {
        Tile tile = getQsTile();
        if (tile == null) return;

        updateTileWithRate(tile, fps);
    }

    private void updateTileWithRate(Tile tile, int fps) {
        tile.setState(fps == RefreshRateController.REFRESH_RATE_AUTO
                ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_refresh_rate));
        tile.setLabel(getString(R.string.refresh_rate_title));

        String summary;
        switch (fps) {
            case 60:
                summary = getString(R.string.refresh_rate_60hz);
                break;
            case 90:
                summary = getString(R.string.refresh_rate_90hz);
                break;
            case 120:
                summary = getString(R.string.refresh_rate_120hz);
                break;
            default:
                summary = getString(R.string.refresh_rate_auto);
                break;
        }
        tile.setSubtitle(summary);
        tile.setContentDescription(summary);
        tile.updateTile();
    }
}
