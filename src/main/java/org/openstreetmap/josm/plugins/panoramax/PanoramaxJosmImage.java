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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
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
import org.openstreetmap.josm.tools.date.DateUtils;

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
        final StringBuilder sb = new StringBuilder();
        boolean added = false;
        for (var provider : this.image.providers()) {
            if (added)
                sb.append(", ");
            sb.append(provider.name());
            added = true;
        }
        if (this.hasExifTime()) {
            sb.append(" - ");
            sb.append(DateUtils.getDateTimeFormatter(FormatStyle.SHORT, FormatStyle.MEDIUM)
                    .format(this.getExifGpsInstant()));
        }

        return sb.toString();
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
        for (var link : this.image.links()) {
            if ("via".equals(link.rel())) {
                return link.href().resolve("?focus=pic&pic=" + this.image.id());
            }
        }
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
        // TODO: Use this.image.properties().exif().ExifGPSInfoGPSSpeedRef()?
        return parseRational64u(this.image.properties().exif().ExifGPSInfoGPSSpeed());
    }

    @Override
    public Double getElevation() {
        return parseRational64u(this.image.properties().exif().ExifGPSInfoGPSAltitude());
    }

    @Override
    public Integer getGpsDiffMode() {
        return null;
    }

    @Override
    public Integer getGps2d3dMode() {
        return null;
    }

    @Override
    public Double getExifGpsDop() {
        return null;
    }

    @Override
    public String getExifGpsDatum() {
        return null;
    }

    @Override
    public String getExifGpsProcMethod() {
        return this.image.properties().exif().ExifGPSInfoGPSProcessingMethod();
    }

    @Override
    public Double getExifImgDir() {
        // TODO use this.image.properties().exif().ExifGPSInfoGPSImgDirectionRef();
        return parseRational64u(this.image.properties().exif().ExifGPSInfoGPSImgDirection());
    }

    @Override
    public Double getExifGpsTrack() {
        return null;
    }

    @Override
    public Double getExifHPosErr() {
        return null;
    }

    @Override
    public boolean hasExifTime() {
        return this.image.properties().exif().ExifImageDateTime() != null;
    }

    @Override
    public Instant getExifInstant() {
        return parseDateTime(this.image.properties().exif().ExifImageDateTime());
    }

    @Override
    public boolean hasGpsTime() {
        return this.image.properties().exif().ExifGPSInfoGPSTimeStamp() != null;
    }

    @Override
    public Instant getGpsInstant() {
        return this.getExifGpsInstant();
    }

    @Override
    public Instant getExifGpsInstant() {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy:MM:dd").withZone(ZoneOffset.UTC);
        final TemporalAccessor ta = dtf.parse(this.image.properties().exif().ExifGPSInfoGPSDateStamp());
        final String[] parts = this.image.properties().exif().ExifGPSInfoGPSTimeStamp().split(" ", 3);
        final double[] hhmmss = new double[3];
        for (int i = 0; i < parts.length; i++) {
            hhmmss[i] = parseRational64u(parts[i]);
        }
        final ZonedDateTime zdt = ZonedDateTime.of(ta.get(ChronoField.YEAR), ta.get(ChronoField.MONTH_OF_YEAR),
                ta.get(ChronoField.DAY_OF_MONTH), (int) Math.round(hhmmss[0]), (int) Math.round(hhmmss[1]),
                (int) Math.round(hhmmss[2]), 0, ZoneOffset.UTC);
        return zdt.toInstant();
    }

    @Override
    public String getIptcCaption() {
        return null;
    }

    @Override
    public String getIptcHeadline() {
        return null;
    }

    @Override
    public List<String> getIptcKeywords() {
        return null; // Yes, we want to return null.
    }

    @Override
    public String getIptcObjectName() {
        return null;
    }

    private static Double parseRational64u(String value) {
        if (value != null) {
            try {
                if (value.contains("/")) {
                    String[] split = value.split("/", 2);
                    return Double.parseDouble(split[0]) / Double.parseDouble(split[1]);
                } else {
                    return Double.parseDouble(value);
                }
            } catch (NumberFormatException nfe) {
                Logging.trace(nfe);
            }
        }
        return null;
    }

    private static Instant parseDateTime(CharSequence value) {
        // TODO: is this normalized to UTC in all cases?
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").withZone(ZoneOffset.UTC);
        final ZonedDateTime zdt = ZonedDateTime.parse(value, dtf);
        return zdt.toInstant();
    }
}
