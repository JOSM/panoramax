/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax.data;

import java.io.Serializable;

public record PanoramaxPersInteriorOrientation(String camera_model, double focal_length, String camera_manufacturer,
                                               Integer[] sensor_array_dimensions) implements Serializable {
    public PanoramaxPersInteriorOrientation {
        if (sensor_array_dimensions.length != 2) {
            throw new IllegalArgumentException("The sensor array must only have two dimensions");
        }
    }
}
