/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jcs3.access.CacheAccess;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxCollection;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxImage;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxLink;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public final class PanoramaxApi {
    private record PanoramaxCache(String api,
                                  CacheAccess<String, PanoramaxCollection> collections,
                                  CacheAccess<String, PanoramaxImage> items,
                                  CacheAccess<String, BufferedImageCacheEntry> images) {
        public PanoramaxCache(String api) {
            this(api, JCSCacheManager.getCache("PanoramaxCollections"), JCSCacheManager.getCache("PanoramaxItems"), JCSCacheManager.getCache("PanoramaxImages"));
        }
    }

    private record LastLiveCheck(String api, Instant time, boolean result, int retryCount) {
    }

    /** The cache map is used to avoid (unlikely) id collisions if multiple servers are supported */
    private static final Map<String, PanoramaxCache> cacheMap = new HashMap<>(1);
    private static final Map<String, LastLiveCheck> liveCheck = new HashMap<>(1);

    private PanoramaxApi() {
    }

    @Nullable
    public static PanoramaxCollection getCollection(@Nonnull String api, @Nonnull String id) {
        if (!isLive(api)) {
            return null;
        }
        final PanoramaxCache cache = cacheMap.computeIfAbsent(api, PanoramaxCache::new);
        return cache.collections().get(id, () -> getRealCollection(api, id));
    }

    @Nonnull
    private static PanoramaxCollection getRealCollection(@Nonnull String api, @Nonnull String id) {
        final List<PanoramaxCollection> collections = new ArrayList<>(1);
        PanoramaxLink next = new PanoramaxLink(buildUri(api, "collections", id, "items"), "", "", "");
        do {
            PanoramaxCollection current = PanoramaxDeserializer.parseCollection(getJson(next.href()));
            collections.add(current);
            next = getNext(current.getLinks());
        } while (next != null);
        if (collections.size() == 1) {
            return collections.getFirst();
        }
        final List<PanoramaxLink> links = new ArrayList<>(4);
        final List<PanoramaxImage> images = new ArrayList<>();
        for (PanoramaxCollection collection : collections) {
            for (PanoramaxLink link : collection.getLinks()) {
                if (!links.contains(link)) {
                    links.add(link);
                }
            }
            images.addAll(collection);
        }
        return new PanoramaxCollection(links.toArray(PanoramaxLink[]::new), images.toArray(PanoramaxImage[]::new));
    }

    @Nullable
    private static PanoramaxLink getNext(@Nonnull PanoramaxLink... links) {
        for (PanoramaxLink link : links) {
            if ("next".equals(link.rel())) {
                return link;
            }
        }
        return null;
    }

    @Nullable
    public static PanoramaxImage getItem(@Nonnull String api, @Nonnull String collectionId, @Nonnull String imageId) {
        if (!isLive(api)) {
            return null;
        }
        final PanoramaxCache cache = cacheMap.computeIfAbsent(api, PanoramaxCache::new);
        // For a more robust implementation, I _should_ try the /api/pictures/{id} endpoint.
        return cache.items().get(imageId, () -> {
            final PanoramaxCollection collection = getCollection(api, collectionId);
            if (collection != null) {
                return collection.stream().filter(p -> imageId.equals(p.id())).findFirst().orElse(null);
            }
            return null;
        });
    }

    @Nullable
    public static BufferedImage getImage(@Nonnull String api, @Nonnull String collectionId, @Nonnull String imageId)
            throws IOException {
        if (!isLive(api)) {
            return null;
        }
        final PanoramaxCache cache = cacheMap.computeIfAbsent(api, PanoramaxCache::new);
        try {
            return cache.images().get(imageId, () -> {
                final PanoramaxImage image = getItem(api, collectionId, imageId);
                if (image == null) {
                    return null;
                }
                final PanoramaxLink link = image.getBestImageLink();
                if (link != null) {
                    HttpClient client = null;
                    try {
                        client = HttpClient.create(link.href().toURL());
                        final HttpClient.Response response = client.connect();
                        return new BufferedImageCacheEntry(response.getContent().readAllBytes());
                    } catch (IOException e) {
                        isLive(api, true);
                        Logging.error(e);
                    } finally {
                        if (client != null)
                            client.disconnect();
                    }
                }
                return null;
            }).getImage();
        } catch (NullPointerException npe) {
            Logging.trace(npe);
        }
        return null;
    }

    /**
     * Check if the API is live
     * @param api The api to check
     * @return {@code true} if the API is live
     */
    public static boolean isLive(String api) {
        return isLive(api, false);
    }

    /**
     * Check if the API is live
     * @param api The api to check
     * @param force Force a check if the API might have changed from live to dead.
     * @return {@code true} if the API is live
     */
    public static boolean isLive(String api, boolean force) {
        final LastLiveCheck check = liveCheck.get(api);
        if (check != null && !check.result()) {
            // Check backoff
            // I'm just doubling until we get to the max backoff time.
            if (Instant.now().isAfter(check.time().plusSeconds(
                    Math.round(Math.min(Math.pow(2, check.retryCount), PanoramaxPreferences.getMaxWaitTime()))))) {
                return false;
            }
        } else if (check != null && Instant.now().isBefore(check.time().plusSeconds(30)) && !force) {
            return true;
        }
        HttpClient client = null;
        try {
            client = HttpClient.create(buildUri(api, "live").toURL(), "HEAD");
            boolean live = client.connect().getResponseCode() == 200;
            liveCheck.put(api,
                    new LastLiveCheck(api, Instant.now(), live, live || check == null ? 0 : check.retryCount() + 1));
            return live;
        } catch (IOException e) {
            Logging.trace(e);
        } finally {
            if (client != null)
                client.disconnect();
        }
        liveCheck.put(api, new LastLiveCheck(api, Instant.now(), false, check == null ? 1 : check.retryCount() + 1));
        return false;
    }

    private static JsonObject getJson(URI uri) {
        HttpClient client = null;
        try {
            client = HttpClient.create(uri.toURL());
            final HttpClient.Response response = client.connect();
            try (JsonReader reader = Json.createReader(response.getContentReader())) {
                return reader.readObject();

            }
        } catch (IOException e) {
            throw new UncheckedIOException(e); // I should handle network issues better.
        } finally {
            if (client != null)
                client.disconnect();
        }
    }

    private static URI buildUri(String api, String... parts) {
        return URI.create(api + (api.endsWith("/") ? "" : "/") + String.join("/", parts));
    }
}
