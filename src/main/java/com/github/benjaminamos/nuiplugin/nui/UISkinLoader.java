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

package com.github.benjaminamos.nuiplugin.nui;

import com.github.benjaminamos.nuiplugin.nui.json.ColorTypeHandler;
import com.github.benjaminamos.nuiplugin.nui.json.FontTypeAdapter;
import com.github.benjaminamos.nuiplugin.nui.json.OptionalTextureRegionTypeAdapter;
import com.github.benjaminamos.nuiplugin.nui.json.TextureRegionTypeAdapter;
import com.github.benjaminamos.nuiplugin.services.GestaltModuleService;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.diagnostic.Logger;
import org.terasology.nui.Color;
import org.terasology.nui.UITextureRegion;
import org.terasology.nui.UIWidget;
import org.terasology.nui.asset.font.Font;
import org.terasology.nui.skin.UISkin;
import org.terasology.nui.skin.UISkinBuilder;
import org.terasology.nui.skin.UIStyleFragment;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

public class UISkinLoader {
    private static final Logger logger = Logger.getInstance(UISkinLoader.class);
    private Gson gson;
    private GestaltModuleService gestaltModuleService;
    private String moduleContext;

    public UISkinLoader(GestaltModuleService gestaltModuleService, String moduleContext) {
        gson = new GsonBuilder()
                .registerTypeAdapter(UISkin.class, new UISkinTypeAdapter())
                .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
                .registerTypeAdapter(UITextureRegion.class, new TextureRegionTypeAdapter(gestaltModuleService))
                .registerTypeAdapter(Optional.class, new OptionalTextureRegionTypeAdapter(gestaltModuleService))
                .registerTypeAdapter(Font.class, new FontTypeAdapter(gestaltModuleService))
                .registerTypeAdapter(Color.class, new ColorTypeHandler())
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();
        this.gestaltModuleService = gestaltModuleService;
        this.moduleContext = moduleContext;
    }

    public UISkin load(String path) throws IOException {
        try (JsonReader reader = new JsonReader(new FileReader(path, Charsets.UTF_8))) {
            reader.setLenient(true);
            return gson.fromJson(reader, UISkin.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            throw new IOException("Failed to load skin '" + path + "'", e);
        }
    }

    public UISkin load(JsonElement element) throws IOException {
        return gson.fromJson(element, UISkin.class);
    }

    public UISkin load(InputStream stream) throws IOException {
        return gson.fromJson(new InputStreamReader(stream), UISkin.class);
    }

    private class UISkinTypeAdapter implements JsonDeserializer<UISkin> {
        @Override
        public UISkin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                UISkinBuilder builder = new UISkinBuilder();
                DefaultInfo defaultInfo = null;
                defaultInfo = context.deserialize(json, DefaultInfo.class);
                defaultInfo.setGestaltModuleData(moduleContext, gestaltModuleService);
                defaultInfo.apply(builder);
                return builder.build();
            }
            return null;
        }
    }

    private static class DefaultInfo extends FamilyInfo {
        public String inherit;
        public Map<String, FamilyInfo> families;

        @Override
        public void apply(UISkinBuilder builder) {
            super.apply(builder);
            if (inherit != null) {
                UISkin skin = gestaltModuleService.getSkinByUrn(moduleContext, inherit);
                if (skin != null) {
                    builder.setBaseSkin(skin);
                }
            }
            if (families != null) {
                for (Map.Entry<String, FamilyInfo> entry : families.entrySet()) {
                    builder.setFamily(entry.getKey());
                    FamilyInfo familyInfo = entry.getValue();
                    familyInfo.setGestaltModuleData(moduleContext, gestaltModuleService);
                    familyInfo.apply(builder);
                }
            }
        }
    }

    private static class FamilyInfo extends StyleInfo {
        public Map<String, ElementInfo> elements;
        protected String moduleContext;
        protected GestaltModuleService gestaltModuleService;

        public void setGestaltModuleData(String moduleContext, GestaltModuleService gestaltModuleService) {
            this.moduleContext = moduleContext;
            this.gestaltModuleService = gestaltModuleService;
        }

        public void apply(UISkinBuilder builder) {
            super.apply(builder);
            if (elements != null) {
                for (Map.Entry<String, ElementInfo> entry : elements.entrySet()) {
                    WidgetLibrary library = gestaltModuleService.getWidgetLibrary();
                    Class<? extends UIWidget> widgetClass = library.getWidgetClassByName(moduleContext, entry.getKey());
                    if (widgetClass != null) {
                        builder.setElementClass(widgetClass);
                        entry.getValue().apply(builder);
                    } else {
                        logger.warn("Failed to resolve UIWidget class " + entry.getKey() + ", skipping style information");
                    }

                }
            }
        }
    }

    private static class PartsInfo extends StyleInfo {
        public Map<String, StyleInfo> modes;

        public void apply(UISkinBuilder builder) {
            super.apply(builder);
            if (modes != null) {
                for (Map.Entry<String, StyleInfo> entry : modes.entrySet()) {
                    builder.setElementMode(entry.getKey());
                    entry.getValue().apply(builder);
                }
            }
        }
    }

    private static class ElementInfo extends StyleInfo {
        public Map<String, PartsInfo> parts;
        public Map<String, StyleInfo> modes;

        public void apply(UISkinBuilder builder) {
            super.apply(builder);
            if (modes != null) {
                for (Map.Entry<String, StyleInfo> entry : modes.entrySet()) {
                    builder.setElementMode(entry.getKey());
                    entry.getValue().apply(builder);
                }
            }
            if (parts != null) {
                for (Map.Entry<String, PartsInfo> entry : parts.entrySet()) {
                    builder.setElementPart(entry.getKey());
                    entry.getValue().apply(builder);
                }
            }
        }
    }

    private static class StyleInfo extends UIStyleFragment {
        private void apply(UISkinBuilder builder) {
            builder.setStyleFragment(this);
        }
    }
}
