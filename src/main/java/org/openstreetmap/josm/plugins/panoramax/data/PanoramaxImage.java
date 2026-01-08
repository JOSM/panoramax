/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax.data;

import java.io.Serializable;
import java.util.Map;

import org.openstreetmap.josm.data.coor.ILatLon;

import jakarta.annotation.Nullable;

public record PanoramaxImage(String id,Double[]bbox,String type,PanoramaxLink[]links,Map<String,PanoramaxLink>assets,PanoramaxProvider[]providers,String collection,PanoramaxProperties properties,String stac_version,double lat,double lon,String[]stac_extensions)implements ILatLon,Serializable{

/**
 * Known asset types
 */
public enum Asset {
    hd(Short.MAX_VALUE), sd((short) 2048), thumb((short) 500);

    /** The expected max width of the asset type. The max value of its type indicates that the asset should be the largest size. */
    public final short maxwidth;

    Asset(short maxwidth) {
        this.maxwidth = maxwidth;
    }

    }

    /**
     * Get the best known image link
     */
    @Nullable
    public PanoramaxLink getBestImageLink() {
        if (assets.containsKey("hd")) {
            return assets.get("hd");
        } else if (assets.containsKey("sd")) {
            return assets.get("sd");
        }
        return assets.entrySet().stream().filter(entry -> !"thumb".equals(entry.getKey())).map(Map.Entry::getValue)
                .findAny().orElseGet(() -> assets.getOrDefault("thumb", null));
    }

}
