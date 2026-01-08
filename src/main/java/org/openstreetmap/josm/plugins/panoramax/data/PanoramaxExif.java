/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax.data;

import java.io.Serializable;

public record PanoramaxExif(String ExifGPSInfoGPSAltitude,
                            String ExifGPSInfoGPSAltitudeRef,
                            String ExifGPSInfoGPSDateStamp,
                            String ExifGPSInfoGPSImgDirection,
                            String ExifGPSInfoGPSImgDirectionRef,
                            String ExifGPSInfoGPSLatitude,
                            String ExifGPSInfoGPSLatitudeRef,
                            String ExifGPSInfoGPSLongitude,
                            String ExifGPSInfoGPSLongitudeRef,
                            String ExifGPSInfoGPSProcessingMethod,
                            String ExifGPSInfoGPSSpeed,
                            String ExifGPSInfoGPSSpeedRef,
                            String ExifGPSInfoGPSTimeStamp,
                            String ExifImageDateTime,
                            String ExifImageExifTag,
                            String ExifImageGPSTag,
                            String ExifImageImageLength,
                            String ExifImageImageWidth,
                            String ExifImageMake,
                            String ExifImageModel,
                            String ExifImageOrientation,
                            String ExifPhotoDateTimeDigitized,
                            String ExifPhotoDateTimeOriginal,
                            String ExifPhotoExifVersion,
                            String ExifPhotoExposureTime,
                            String ExifPhotoFlash,
                            String ExifPhotoFocalLength,
                            String ExifPhotoISOSpeedRatings,
                            String ExifPhotoLightSource,
                            String ExifPhotoOffsetTime,
                            String ExifPhotoOffsetTimeDigitized,
                            String ExifPhotoOffsetTimeOriginal,
                            String ExifPhotoPixelXDimension,
                            String ExifPhotoPixelYDimension,
                            String ExifPhotoSubSecTime,
                            String ExifPhotoSubSecTimeDigitized,
                            String ExifPhotoSubSecTimeOriginal,
                            String ExifPhotoWhiteBalance) implements Serializable {
}
