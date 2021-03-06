/*
 * Copyright 2016 MovingBlocks
 * Modifications Copyright 2022 Benjamin Amos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.benjaminamos.nuiplugin.nui.json;

import com.google.common.primitives.UnsignedInts;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.terasology.nui.Color;

import java.lang.reflect.Type;

/**
 * Serializes {@link Color} instances to an int array <code>[r, g, b, a]</code>.
 * De-serializing also supports hexadecimal strings such as <code>"AAAAAAFF"</code>.
 */
public final class ColorTypeHandler implements JsonDeserializer<Color> {
    @Override
    public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            return new Color(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat());
        }
        if (json.isJsonPrimitive()) {
            // NOTE: Integer.parseUnsignedInt is not available on Android API 24 (7.0).
            //       Since we still have Guava, we use its equivalent.
            return new Color(UnsignedInts.parseUnsignedInt(json.getAsString(), 16));
        }

        return null;
    }
}