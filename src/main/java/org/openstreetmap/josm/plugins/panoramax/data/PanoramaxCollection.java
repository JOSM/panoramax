/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax.data;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;

public final class PanoramaxCollection extends AbstractList<PanoramaxImage> implements Serializable {
    private final PanoramaxImage[] images;

    public PanoramaxCollection(PanoramaxLink[] links, PanoramaxImage... features) {
        this.images = Arrays.copyOf(features, features.length);
    }

    @Override
    public PanoramaxImage get(int index) {
        return this.images[index];
    }

    @Override
    public int size() {
        return this.images.length;
    }
}
