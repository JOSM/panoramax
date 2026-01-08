/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxCollection;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxImage;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxLink;
import org.openstreetmap.josm.tools.Logging;

public class PanoramaxJosmImage implements IImageEntry<PanoramaxJosmImage> {
    private final PanoramaxImage image;

    public PanoramaxJosmImage(PanoramaxImage image) {
        this.image = image;
    }

    @Override
    public BufferedImage read(Dimension target) throws IOException {
        return PanoramaxApi.getImage(getRootApi(), this.image.collection(), this.image.id());
    }

    @Override
    public PanoramaxJosmImage getNextImage() {
        final PanoramaxCollection collection = getCollection();

        if (collection != null) {
            final int index = collection.indexOf(this.image);
            if (collection.size() > index + 1 && index >= 0) {
                final PanoramaxJosmImage next = new PanoramaxJosmImage(collection.get(index + 1));
                MainApplication.worker.execute(() -> {
                    try {
                        next.read(null);
                    } catch (IOException e) {
                        Logging.trace(e); // Not super important.
                    }
                });
                return next;
            }
        }
        return null;
    }

    @Override
    public PanoramaxJosmImage getPreviousImage() {
        final PanoramaxCollection collection = getCollection();
        if (collection != null) {
            final int index = collection.indexOf(this.image);
            if (index > 0) {
                return new PanoramaxJosmImage(collection.get(index - 1));
            }
        }
        return null;
    }

    @Override
    public PanoramaxJosmImage getFirstImage() {
        final PanoramaxCollection collection = getCollection();
        if (collection != null) {
            final int index = collection.indexOf(this.image);
            if (index > 0) {
                return new PanoramaxJosmImage(collection.getFirst());
            }
        }
        return null;
    }

    @Override
    public PanoramaxJosmImage getLastImage() {
        final PanoramaxCollection collection = getCollection();
        if (collection != null) {
            final int index = collection.indexOf(this.image);
            if (collection.size() > index + 1 && index >= 0) {
                return new PanoramaxJosmImage(collection.getLast());
            }
        }
        return null;
    }

    @Override
    public void selectImage(ImageViewerDialog imageViewerDialog, IImageEntry<?> entry) {
        if (entry instanceof PanoramaxJosmImage pji) {
            selectImage(pji);
        }
        IImageEntry.super.selectImage(imageViewerDialog, entry);
    }

    private void selectImage(PanoramaxJosmImage pji) {
        if (pji != null && !this.equals(pji)) {
            for (PanoramaxLayer pl : MainApplication.getLayerManager().getLayersOfType(PanoramaxLayer.class)) {
                pl.imageChanged(pl, Collections.singletonList(this), Collections.singletonList(pji));
            }
        }
    }

    private PanoramaxCollection getCollection() {
        return PanoramaxApi.getCollection(getRootApi(), this.image.collection());
    }

    private String getRootApi() {
        return Arrays.stream(this.image.links()).filter(l -> "root".equals(l.rel())).map(PanoramaxLink::href)
                .map(URI::toString).findFirst().orElse(PanoramaxPreferences.getBaseApiUrl());
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public void setWidth(int width) {

    }

    @Override
    public void setHeight(int height) {

    }

    @Override
    public File getFile() {
        return new File(getImageURI());
    }

    @Override
    public URI getImageURI() {
        return this.image.getBestImageLink().href();
    }

    @Override
    public ILatLon getPos() {
        return this.image;
    }

    public PanoramaxImage getImage() {
        return this.image;
    }

    @Override
    public Double getSpeed() {
        return 0.0;
    }

    @Override
    public Double getElevation() {
        return 0.0;
    }

    @Override
    public Integer getGpsDiffMode() {
        return 0;
    }

    @Override
    public Integer getGps2d3dMode() {
        return 0;
    }

    @Override
    public Double getExifGpsDop() {
        return 0.0;
    }

    @Override
    public String getExifGpsDatum() {
        return "";
    }

    @Override
    public String getExifGpsProcMethod() {
        return "";
    }

    @Override
    public Double getExifImgDir() {
        return 0.0;
    }

    @Override
    public Double getExifGpsTrack() {
        return 0.0;
    }

    @Override
    public Double getExifHPosErr() {
        return 0.0;
    }

    @Override
    public boolean hasExifTime() {
        return false;
    }

    @Override
    public Instant getExifInstant() {
        return null;
    }

    @Override
    public boolean hasGpsTime() {
        return false;
    }

    @Override
    public Instant getGpsInstant() {
        return null;
    }

    @Override
    public Instant getExifGpsInstant() {
        return null;
    }

    @Override
    public String getIptcCaption() {
        return "";
    }

    @Override
    public String getIptcHeadline() {
        return "";
    }

    @Override
    public List<String> getIptcKeywords() {
        return List.of();
    }

    @Override
    public String getIptcObjectName() {
        return "";
    }
}
