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

import com.github.benjaminamos.nuiplugin.services.GestaltModuleService;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.terasology.nui.asset.font.Font;

import java.lang.reflect.Type;

public final class FontTypeAdapter implements JsonDeserializer<Font> {
    private GestaltModuleService gestaltModuleService;

    public FontTypeAdapter(GestaltModuleService gestaltModuleService) {
        this.gestaltModuleService = gestaltModuleService;
    }

    @Override
    public Font deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String name = json.getAsString();
        if (!name.contains(":")) {
            name = "engine:" + name;
        }

        return gestaltModuleService.getFontByUrn(name);
    }
}
