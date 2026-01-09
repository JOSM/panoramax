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
    private final PanoramaxLink[] links;

    public PanoramaxCollection(PanoramaxLink[] links, PanoramaxImage... features) {
        this.images = Arrays.copyOf(features, features.length);
        this.links = links;
    }

    /**
     * Get the links for this collection
     * @return The links
     */
    public PanoramaxLink[] getLinks() {
        return this.links;
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
