/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;

import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.spi.preferences.Config;

public final class PanoramaxPreferences {
    private static final NamedColorProperty COLOR_SEQUENCE = new NamedColorProperty(
            NamedColorProperty.COLOR_CATEGORY_GENERAL, "PanoramaxSequence", marktr("Panoramax Sequence Color"),
            Color.ORANGE);
    private static final NamedColorProperty COLOR_IMAGE = new NamedColorProperty(
            NamedColorProperty.COLOR_CATEGORY_GENERAL, "PanoramaxImage", marktr("Panoramax Image Color"), Color.RED);

    private PanoramaxPreferences() {
        // Hide constructor
    }

    public static String getMvtUrl() {
        return getBaseApiUrl() + Config.getPref().get("panoramax.api.mvt", "/map/{z}/{x}/{y}.mvt");
    }

    public static String getBaseApiUrl() {
        return Config.getPref().get("panoramax.api", "https://api.panoramax.xyz/api");
    }

    public static Color getSequenceColor() {
        return COLOR_SEQUENCE.get();
    }

    public static Color getImageColor() {
        return COLOR_IMAGE.get();
    }

    public static int getImageSize() {
        return Config.getPref().getInt("panoramax.image.size", 10);
    }

    public static int getMaxZoom() {
        // ought to read it from https://api.panoramax.xyz/api/map/style.json
        return Config.getPref().getInt("panoramax.map.max.zoom", 15); // Higher z levels did not return data.
    }

    public static double getMaxWaitTime() {
        return Config.getPref().getInt("panoramax.download.backoff", 600 /* 10 minutes */);
    }
}
