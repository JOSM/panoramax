/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax.data;

import java.io.Serializable;

public record PanoramaxProperties(PanoramaxExif exif, String created, String license, String updated, String datetime, Object[] semantics, Object collection,
                                  String datetimez, Object[] annotations, Integer view_azimuth, String geovisio_image,
                                  String geovisio_status, String geovisio_producer, String geovisio_thumbnail,
                                  String original_file_name, Integer original_file_size, String geovisio_visibility,
                                  PanoramaxPersInteriorOrientation pers_interior_orientation, Integer geovisio_rank_in_collection,
                                  Double quality_horizontal_accuracy) implements Serializable {
}
