/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax.data;

import java.io.Serializable;
import java.net.URI;

import jakarta.annotation.Nullable;

public record PanoramaxLink(URI href, String rel, String type, @Nullable String title) implements Serializable {
}
