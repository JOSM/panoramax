/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Taylor Smock
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: AGPL-3.0-or-later WITH agpl-ai-training
 */
package org.openstreetmap.josm.plugins.panoramax;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxCollection;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxImage;
import org.openstreetmap.josm.plugins.panoramax.data.PanoramaxLink;
import org.openstreetmap.josm.tools.JosmRuntimeException;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Take a JSON object and deserialize it
 */
final class PanoramaxDeserializer {
    static PanoramaxCollection parseCollection(JsonObject json) {
        try {
            final PanoramaxImage[] features = parse(PanoramaxImage[].class, json.getJsonArray("features"));
            final PanoramaxLink[] links = parse(PanoramaxLink[].class, json.getJsonArray("links"));
            return new PanoramaxCollection(links, features);
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
    }

    static PanoramaxImage parseImage(JsonObject image) {
        try {
            final RecordComponent[] components = PanoramaxImage.class.getRecordComponents();
            final Object[] args = getJsonArgs(components, image);
            final JsonArray coordinates = image.getJsonObject("geometry").getJsonArray("coordinates");
            final double lon = coordinates.getJsonNumber(0).doubleValue();
            final double lat = coordinates.getJsonNumber(1).doubleValue();
            final JsonObject assetsObject = image.getJsonObject("assets");
            final Map<String, PanoramaxLink> assets = assetsObject.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                        try {
                            return parse(PanoramaxLink.class, entry.getValue());
                        } catch (ReflectiveOperationException e) {
                            throw new JosmRuntimeException(e);
                        }
                    }));
            for (int i = 0; i < components.length; i++) {
                switch (components[i].getName()) {
                case "lat":
                    args[i] = lat;
                    break;
                case "lon":
                    args[i] = lon;
                    break;
                case "assets":
                    args[i] = assets;
                default: // Do nothing
                }
            }
            return PanoramaxImage.class
                    .getConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new))
                    .newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
    }

    private static <T> T parseObject(Class<T> clazz, JsonObject object)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!clazz.isRecord()) {
            throw new IllegalArgumentException("Specified class must be a record: " + clazz.getCanonicalName());
        } else if (PanoramaxImage.class.equals(clazz)) {
            return clazz.cast(parseImage(object));
        }
        final RecordComponent[] components = clazz.getRecordComponents();
        final Object[] args = getJsonArgs(components, object);
        try {
            return clazz
                    .getConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new))
                    .newInstance(args);
        } catch (Exception e) {
            throw new JosmRuntimeException(clazz.getCanonicalName() + " " + object.toString(), e);
        }
    }

    private static Object[] getJsonArgs(RecordComponent[] components, JsonObject object)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object[] args = new Object[components.length];
        final Map<String, String> normalizedKeys = object.keySet().stream()
                .collect(Collectors.toMap(k -> k.replace(".", ""), k -> k));
        for (int i = 0; i < components.length; i++) {
            final RecordComponent component = components[i];
            if (normalizedKeys.containsKey(component.getName())) {
                args[i] = parse(component.getType(), object.get(normalizedKeys.get(component.getName())));
            }
        }
        return args;
    }

    private static <T> T[] parseArray(Class<T> clazz, JsonArray array)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(array);
        T[] rType = (T[]) Array.newInstance(clazz, array.size());
        for (int i = 0; i < array.size(); i++) {
            rType[i] = parse(clazz, array.get(i));
        }
        return rType;
    }

    private static <T> T parse(Class<T> clazz, JsonValue value)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(value);
        return switch (value.getValueType()) {
            case ARRAY -> clazz.cast(parseArray(clazz.getComponentType(), value.asJsonArray()));
            case OBJECT -> {
                if (clazz.isRecord()) {
                    yield parseObject(clazz, value.asJsonObject());
                } else {
                    yield null; // This must be manually handled
                }
            }
            case STRING -> {
                final String str = ((JsonString) value).getString();
                if (URI.class.equals(clazz)) {
                    yield clazz.cast(URI.create(str));
                }
                yield clazz.cast(str);
            }
            case NUMBER -> {
                JsonNumber number = (JsonNumber) value;
                if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
                    yield (T) Integer.valueOf(number.intValueExact());
                } else if (long.class.equals(clazz) || Long.class.equals(clazz)) {
                    yield (T) Long.valueOf(number.longValueExact());
                } else if (short.class.equals(clazz) || Short.class.equals(clazz)) {
                    yield (T) Short.valueOf((short) number.intValueExact());
                } else if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
                    yield (T) Byte.valueOf((byte) number.intValueExact());
                } else if (double.class.equals(clazz) || Double.class.equals(clazz)) {
                    yield (T) Double.valueOf(number.doubleValue());
                } else if (float.class.equals(clazz) || Float.class.equals(clazz)) {
                    yield (T) Float.valueOf((float) number.doubleValue());
                } else {
                    throw new IllegalArgumentException("Unknown class type: " + clazz.getCanonicalName());
                }
            }
            case TRUE -> clazz.cast(Boolean.TRUE);
            case FALSE -> clazz.cast(Boolean.FALSE);
            case NULL -> null;
        };
    }
}
