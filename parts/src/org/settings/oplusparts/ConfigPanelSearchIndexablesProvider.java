/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2026 Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.settings.oplusparts;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;

import org.settings.oplusparts.display.DcDimmingActivity;
import org.settings.oplusparts.gamebar.GameBarSettingsActivity;
import org.settings.oplusparts.power.GentleChargingActivity;
import org.settings.oplusparts.power.PauseChargingActivity;
import org.settings.oplusparts.refreshrate.RefreshRateActivity;

public class ConfigPanelSearchIndexablesProvider extends SearchIndexablesProvider {

    private static final SearchIndexableResource[] INDEXABLE_RES = new SearchIndexableResource[]{
            new SearchIndexableResource(1, R.xml.oneplus_parts_settings,
                    OnePlusPartsActivity.class.getName(), R.drawable.ic_oneplus_parts),
            new SearchIndexableResource(2, R.xml.dc_dimming_settings,
                    DcDimmingActivity.class.getName(), R.drawable.ic_dc_dimming),
            new SearchIndexableResource(3, R.xml.refresh_rate_preferences,
                    RefreshRateActivity.class.getName(), R.drawable.ic_refresh_rate),
            new SearchIndexableResource(4, R.xml.gentle_charging_settings,
                    GentleChargingActivity.class.getName(), R.drawable.ic_gentle_charging),
            new SearchIndexableResource(5, R.xml.pause_charging_settings,
                    PauseChargingActivity.class.getName(), R.drawable.ic_pause_charging),
            new SearchIndexableResource(6, R.xml.game_bar_preferences,
                    GameBarSettingsActivity.class.getName(), R.drawable.ic_game_bar),
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        for (SearchIndexableResource sir : INDEXABLE_RES) {
            cursor.addRow(generateResourceRef(sir));
        }
        return cursor;
    }

    private static Object[] generateResourceRef(SearchIndexableResource sir) {
        Object[] ref = new Object[7];
        ref[COLUMN_INDEX_XML_RES_RANK] = sir.rank;
        ref[COLUMN_INDEX_XML_RES_RESID] = sir.xmlResId;
        ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = null;
        ref[COLUMN_INDEX_XML_RES_ICON_RESID] = sir.iconResId;
        ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = Intent.ACTION_MAIN;
        ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = "org.settings.oplusparts";
        ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = sir.className;
        return ref;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        return new MatrixCursor(INDEXABLES_RAW_COLUMNS);
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        return new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);
    }
}
