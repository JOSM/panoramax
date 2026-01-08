/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Destroyable;

public class PanoramaxPlugin extends Plugin implements Destroyable {
    private final List<Destroyable> destroyableList = new ArrayList<>();

    public PanoramaxPlugin(PluginInformation info) {
        super(info);
        final MainMenu menu = MainApplication.getMenu();
        final PanoramaxDownloadAction panoramaxDownloadAction = new PanoramaxDownloadAction();
        destroyableList.add(panoramaxDownloadAction);
        MainMenu.add(menu.imagerySubMenu, panoramaxDownloadAction, false);
    }

    @Override
    public void destroy() {
        destroyableList.forEach(Destroyable::destroy);
        destroyableList.clear();
    }

}
