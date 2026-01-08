/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class PanoramaxDownloadAction extends JosmAction {
    public static final Shortcut SHORTCUT = Shortcut.registerShortcut("Panoramax", tr("Open Panoramax layer"),
            KeyEvent.VK_COMMA, Shortcut.SHIFT);

    public PanoramaxDownloadAction() {
        super(tr("Panoramax"), (ImageProvider) null /* FIXME: Icon here */, tr("Open Panoramax layer"), SHORTCUT, false,
                "mapillaryDownload", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (MainApplication.getLayerManager().getLayersOfType(PanoramaxLayer.class).isEmpty()) {
            MainApplication.getLayerManager().addLayer(new PanoramaxLayer());
        }
    }

    @Override
    public void updateEnabledState() {
        super.setEnabled(MainApplication.isDisplayingMapView());
    }
}
