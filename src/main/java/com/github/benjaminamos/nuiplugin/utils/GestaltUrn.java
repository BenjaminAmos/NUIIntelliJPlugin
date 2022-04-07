/*
 * Copyright 2022 Benjamin Amos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.benjaminamos.nuiplugin.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GestaltUrn {
    // Regex taken from https://github.com/MovingBlocks/gestalt/blob/7259b21d2ddca4c0222234b4aa80c751973d4a0f/gestalt-asset-core/src/main/java/org/terasology/gestalt/assets/ResourceUrn.java#L48.
    private static final Pattern URN_PATTERN = Pattern.compile("([^:]+):([^#!]+)(?:#([^!]+))?(!instance)?");
    private final String module;
    private final String asset;
    private final String fragment;

    public GestaltUrn(String module, String asset) {
        this(module, asset, "");
    }

    public GestaltUrn(String module, String asset, String fragment) {
        this.module = module.toLowerCase(Locale.ROOT);
        this.asset = asset.toLowerCase(Locale.ROOT);
        this.fragment = fragment.toLowerCase(Locale.ROOT);
    }

    public static GestaltUrn parse(String urn) {
        Matcher matcher = URN_PATTERN.matcher(urn);
        if (!matcher.matches() || matcher.groupCount() < 2) {
            return null;
        }

        String module = matcher.group(1);
        String asset = matcher.group(2);
        String instance = matcher.group(3);
        if (instance != null) {
            return new GestaltUrn(module, asset, instance);
        } else {
            return new GestaltUrn(module, asset);
        }
    }

    public String getModule() {
        return module;
    }

    public String getAsset() {
        return asset;
    }

    public String getFragment() {
        return fragment;
    }

    @Override
    public String toString() {
        String urn = module + ":" + asset;
        if (!fragment.isEmpty()) {
            urn += "#" + fragment;
        }
        return urn;
    }
}
