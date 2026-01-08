/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.io.StringReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.HighlightUpdateListener;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.event.IDataSelectionListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.vector.VectorDataSet;
import org.openstreetmap.josm.data.vector.VectorNode;
import org.openstreetmap.josm.data.vector.VectorPrimitive;
import org.openstreetmap.josm.data.vector.VectorRelation;
import org.openstreetmap.josm.data.vector.VectorWay;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.geoimage.IGeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.gui.layer.imagery.MVTLayer;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxImage;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.HiDPISupport;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;

import jakarta.json.Json;
import jakarta.json.JsonString;

public class PanoramaxLayer extends MVTLayer implements HighlightUpdateListener, IGeoImageLayer,
        IDataSelectionListener<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet> {
    private static ImageryInfo getImageryInfo() {
        final ImageryInfo imageryInfo = new ImageryInfo(tr("Panoramax"), PanoramaxPreferences.getMvtUrl());
        imageryInfo.setDefaultMaxZoom(PanoramaxPreferences.getMaxZoom());
        return imageryInfo;
    }

    private final Collection<IPrimitive> highlighted = new HashSet<>();
    private final Collection<VectorPrimitive> selected = new HashSet<>();
    private final ListenerList<ImageChangeListener> imageChangeListenerListenerList = ListenerList.create();
    private final MouseListener mouseListener = new DataMouseListener();
    private final MapView mv = MainApplication.getMap().mapView;
    private final String api;

    /**
     * Creates an instance of a Panoramax layer
     *
     */
    public PanoramaxLayer() {
        super(getImageryInfo());
        this.api = PanoramaxPreferences.getBaseApiUrl();
        this.getData().addSelectionListener(this);
        this.getData().addHighlightUpdateListener(this);
        mv.addMouseListener(mouseListener);
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        final Color sequenceColor = PanoramaxPreferences.getSequenceColor();
        final Color imageColor = PanoramaxPreferences.getImageColor();
        final int imageSize = PanoramaxPreferences.getImageSize();
        this.getData().setZoom(Math.min(this.getZoomLevel(), this.getInfo().getMaxZoom()));
        if (this.getZoomLevel() >= 6) {
            g.setColor(sequenceColor);
            g.setStroke(new BasicStroke(2));
            for (IWay<?> seq : getData().searchWays(box.toBBox())) {
                INode previous = null;
                Point previousPoint = null;
                for (INode current : seq.getNodes()) {
                    if (previous != null && (box.contains(current) || box.contains(previous))) {
                        if (previousPoint == null)
                            previousPoint = mv.getPoint(previous);
                        final Point currentPoint = mv.getPoint(current);
                        g.drawLine(previousPoint.x, previousPoint.y, currentPoint.x, currentPoint.y);
                        previous = current;
                        previousPoint = currentPoint;
                    } else {
                        previous = current;
                        previousPoint = null;
                    }
                }
            }
        }

        final AffineTransform original = g.getTransform();
        // Paint images or overview
        if (this.getZoomLevel() >= 13 || this.getZoomLevel() < 6) {
            // Paint images (tagged)
            for (INode node : getData().searchNodes(box.toBBox())) {
                if (node.isTagged()) {
                    final Point current = mv.getPoint(node);
                    g.setColor(imageColor);
                    g.fillOval(current.x - imageSize / 2, current.y - imageSize / 2, imageSize, imageSize);
                    g.setColor(sequenceColor);
                    if (node.hasKey("heading")) {
                        final AffineTransform transform = new AffineTransform(original);
                        try {
                            final double heading = Double.parseDouble(node.get("heading"));
                            transform.rotate(Math.toRadians(heading), current.x, current.y);
                            g.setTransform(transform);
                            g.drawLine(current.x, current.y, current.x, current.y - imageSize / 2);
                        } catch (NumberFormatException nfe) {
                            Logging.error(nfe);
                        } finally {
                            g.setTransform(original);
                        }
                    }
                    if (this.selected.contains(node)) {
                        g.setColor(Color.GREEN);
                        final int radius = (imageSize + 2) / 2;
                        g.drawOval(current.x - radius, current.y - radius, 2 * radius, 2 * radius);
                    }
                }
            }
        }
        g.setTransform(original);
    }

    @Override
    public void highlightUpdated(HighlightUpdateEvent e) {
        // FIXME
    }

    @Override
    public void selectionChanged(
            SelectionChangeEvent<VectorPrimitive, VectorNode, VectorWay, VectorRelation, VectorDataSet> event) {
        this.selected.removeAll(event.getRemoved());
        this.selected.addAll(event.getAdded());
        if (this.selected.size() == 1) {
            ImageViewerDialog.getInstance().displayImages(getSelection());
        }
        this.invalidate();
    }

    @Override
    public void clearSelection() {
        this.selected.clear();
        this.invalidate();
    }

    @Override
    public List<? extends IImageEntry<?>> getSelection() {
        return this.selected.stream().map(this::getImageFromPrimitive).map(PanoramaxJosmImage::new).toList();
    }

    private PanoramaxImage getImageFromPrimitive(Tagged object) {
        final String seq = object.get("sequences");
        final String[] sequences = Json.createReader(new StringReader(seq)).readArray().stream()
                .filter(JsonString.class::isInstance).map(JsonString.class::cast).map(JsonString::getString)
                .toArray(String[]::new);
        return PanoramaxApi.getItem(this.api, sequences[0], object.get("id"));
    }

    @Override
    public boolean containsImage(IImageEntry<?> imageEntry) {
        if (imageEntry instanceof PanoramaxJosmImage pji) {
            final BBox searchBBox = new BBox(pji.getPos());
            searchBBox.addLatLon(new LatLon(pji.getPos().lat(), pji.getPos().lon()), .0001);
            final String id = pji.getImage().id();
            return this.getData().searchNodes(searchBBox).stream().filter(p -> p.hasKey("id")).map(p -> p.get("id"))
                    .anyMatch(id::equals);
        }
        return false;
    }

    @Override
    public void addImageChangeListener(ImageChangeListener listener) {
        this.imageChangeListenerListenerList.addListener(listener);
    }

    @Override
    public void removeImageChangeListener(ImageChangeListener listener) {
        this.imageChangeListenerListenerList.removeListener(listener);
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        mv.removeMouseListener(this.mouseListener);
    }

    private void fireClickEvent(MouseEvent e) {
        if (e.getClickCount() >= 3) {
            this.clearSelection();
        } else {
            final ILatLon latLon = mv.getLatLon(e.getX(), e.getY());
            final BBox searchBBox = makeSearchBBox(mv, latLon);
            this.getData().searchNodes(searchBBox).stream().min(Comparator.comparingDouble(v -> v.distanceSq(latLon)))
                    .map(v -> new SelectionReplaceEvent<>(this.getData(), new HashSet<>(this.selected), Stream.of(v)))
                    .ifPresent(this::selectionChanged);
        }
    }

    private static BBox makeSearchBBox(MapView mv, ILatLon latLon) {
        final double scaleInEastNorthUnitsPerPixel = mv.getScale();
        final double metersPerPixel = ProjectionRegistry.getProjection().getMetersPerUnit()
                * scaleInEastNorthUnitsPerPixel;
        final double r = metersPerPixel * HiDPISupport.getHiDPIScale() * 5;
        final BBox searchBBox = new BBox(latLon.lon(), latLon.lat());
        searchBBox.add(Geometry.getLatLonFrom(latLon, Math.PI / 4, r));
        searchBBox.add(Geometry.getLatLonFrom(latLon, 5 * Math.PI / 4, r));
        return searchBBox;
    }

    public void imageChanged(IGeoImageLayer source, List<? extends IImageEntry<?>> oldImages,
            List<? extends IImageEntry<?>> newImages) {
        if (source != this) {
            return;
        }
        final List<VectorNode> newImagesFiltered = newImages.stream().filter(PanoramaxJosmImage.class::isInstance)
                .map(PanoramaxJosmImage.class::cast).map(this::findNode).toList();
        this.getData().setSelected(newImagesFiltered);
        this.invalidate();
    }

    private VectorNode findNode(PanoramaxJosmImage image) {
        final BBox searchBBox = new BBox(image.getImage());
        searchBBox.addLatLon(new LatLon(image.getImage()), 0.0001);
        return this.getData().searchNodes(searchBBox).stream().filter(n -> image.getImage().id().equals(n.get("id")))
                .findFirst().orElse(null);
    }

    private class DataMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            fireClickEvent(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Skip
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Skip
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Skip
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Skip
        }
    }
}
