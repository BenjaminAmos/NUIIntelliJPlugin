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
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import org.joml.Vector2i;
import org.terasology.nui.Color;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.LayoutHint;
import org.terasology.nui.UILayout;
import org.terasology.nui.UITextureRegion;
import org.terasology.nui.UIWidget;
import org.terasology.nui.asset.font.Font;
import org.terasology.nui.skin.UISkin;
import org.terasology.nui.widgets.UILabel;
import org.terasology.reflection.ReflectionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class UIElementLoader {
    public static final String CONTENTS_FIELD = "contents";
    public static final String LAYOUT_INFO_FIELD = "layoutInfo";
    public static final String ID_FIELD = "id";
    public static final String TYPE_FIELD = "type";
    private static final Logger logger = Logger.getInstance(UIElementLoader.class);

    private final GestaltModuleService gestaltModuleService;
    private String moduleContext;
    private Set<String> missingClasses = new HashSet<>();

    public UIElementLoader(GestaltModuleService gestaltModuleService) {
        this.gestaltModuleService = gestaltModuleService;
    }

    public UIWidget load(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            return load(new JsonParser().parse(reader));
        }
    }

    public UIWidget load(String text) throws IOException {
        return load(new JsonParser().parse(text));
    }

    public UIWidget load(JsonElement element) throws IOException {
        missingClasses.clear();

        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
                .registerTypeAdapter(UISkin.class, (JsonDeserializer<UISkin>) (json, typeOfT, context) ->
                        gestaltModuleService.getSkinByUrn(moduleContext, json.getAsString()))
                .registerTypeAdapter(UITextureRegion.class, new TextureRegionTypeAdapter(gestaltModuleService))
                .registerTypeAdapter(Optional.class, new OptionalTextureRegionTypeAdapter(gestaltModuleService))
                .registerTypeAdapter(Font.class, new FontTypeAdapter(gestaltModuleService))
                .registerTypeAdapter(Color.class, new ColorTypeHandler())
                .registerTypeAdapter(Vector2i.class, new Vector2iTypeAdaptor())
                .registerTypeHierarchyAdapter(UIWidget.class, new UIWidgetTypeAdapter());
        Gson gson = gsonBuilder.create();
        return gson.fromJson(element, UIWidget.class);
    }

    public Set<String> getMissingClasses() {
        return missingClasses;
    }

    public String getModuleContext() {
        return moduleContext;
    }

    public void setModuleContext(String moduleContext) {
        this.moduleContext = moduleContext;
    }

    private static final class Vector2iTypeAdaptor implements JsonDeserializer<Vector2i> {
        @Override
        public Vector2i deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray array = json.getAsJsonArray();
            return new Vector2i(array.get(0).getAsInt(), array.get(1).getAsInt());
        }
    }

    /**
     * Loads a widget. This requires the following custom handling:
     * <ul>
     * <li>The class of the widget is determined through a URI in the "type" attribute</li>
     * <li>If the "id" attribute is present, it is passed to the constructor</li>
     * <li>If the widget is a layout, then a "contents" attribute provides a list of widgets for content.
     * Each contained widget may have a "layoutInfo" attribute providing the layout hint for its container.</li>
     * </ul>
     */
    private final class UIWidgetTypeAdapter implements JsonDeserializer<UIWidget> {
        @Override
        public UIWidget deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                return new UILabel(json.getAsString());
            }

            JsonObject jsonObject = json.getAsJsonObject();

            String type = jsonObject.get(TYPE_FIELD).getAsString();
            Class<? extends UIWidget> widgetClass = gestaltModuleService.getWidgetLibrary().getWidgetClassByName(type);
            if (widgetClass == null) {
                if (moduleContext != null && !type.contains(":")) {
                    widgetClass = gestaltModuleService.getWidgetLibrary().getWidgetClassByName(moduleContext + ":" + type);
                }
                if (widgetClass == null) {
                    //logger.error("Unknown UIWidget type " + type);
                    missingClasses.add(type);
                    return null;
                }
            }

            String id = null;
            if (jsonObject.has(ID_FIELD)) {
                id = jsonObject.get(ID_FIELD).getAsString();
            }

            UIWidget element;
            try {
                element = widgetClass.newInstance();
                if (id != null) {
                    Class<?> parentClass = widgetClass;
                    Field fieldMetadata = null;
                    while (fieldMetadata == null && widgetClass != UIWidget.class) {
                        try {
                            fieldMetadata = parentClass.getDeclaredField(ID_FIELD);
                        } catch (Throwable ignore) {
                        }
                        parentClass = parentClass.getSuperclass();
                    }

                    if (fieldMetadata == null) {
                        logger.warn("UIWidget type " + element.getId() + " lacks id field " + element.getId());
                    } else {
                        fieldMetadata.setAccessible(true);
                        fieldMetadata.set(element, id);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to instantiate UI widget type " + widgetClass.getName(), e);
                return null;
            }

            // Deserialize normal fields.
            Set<String> unknownFields = new HashSet<>();
            for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String name = entry.getKey();
                if (!ID_FIELD.equals(name)
                        && !CONTENTS_FIELD.equals(name)
                        && !TYPE_FIELD.equals(name)
                        && !LAYOUT_INFO_FIELD.equals(name)) {
                    unknownFields.add(name);
                }
            }

            for (Field field : getFields(widgetClass)) {
                Class<?> fieldType = determineFieldType(field);

                String serialisedFieldName;
                SerializedName name = field.getAnnotation(SerializedName.class);
                if (name != null) {
                    serialisedFieldName = name.value();
                } else {
                    serialisedFieldName = field.getName();
                }

                Method setter;
                try {
                    setter = ReflectionUtil.findSetter(field.getName(), widgetClass, fieldType);
                } catch (NoClassDefFoundError ignore) {
                    continue;
                }

                if (jsonObject.has(serialisedFieldName)) {
                    unknownFields.remove(serialisedFieldName);
                    if (field.getName().equals(CONTENTS_FIELD) && UILayout.class.isAssignableFrom(widgetClass)) {
                        continue;
                    }
                    try {
                        if (List.class.isAssignableFrom(field.getType())) {
                            Type contentType = ReflectionUtil.getTypeParameter(field.getGenericType(), 0);
                            if (contentType != null) {
                                List<Object> result = Lists.newArrayList();
                                JsonArray list = jsonObject.getAsJsonArray(serialisedFieldName);
                                for (JsonElement item : list) {
                                    result.add(context.deserialize(item, contentType));
                                }

                                if (setter != null) {
                                    setter.invoke(element, result);
                                } else {
                                    field.set(element, result);
                                }
                            }
                        } else {
                            if (setter != null) {
                                Object value = context.deserialize(jsonObject.get(serialisedFieldName), fieldType);
                                setter.invoke(element, value);
                            } else {
                                field.set(element, context.deserialize(jsonObject.get(serialisedFieldName), field.getType()));
                            }
                        }
                    } catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
                        logger.error("Failed to deserialize field " + field.getName() + " of " + type + "\n" + e.getMessage());
                    }
                }
            }

            for (String key : unknownFields) {
                logger.warn("Field '" + key + "' not recognized for " + typeOfT + " in " + json);
            }

            // Deserialize contents and layout hints
            if (UILayout.class.isAssignableFrom(widgetClass)) {
                UILayout<LayoutHint> layout = (UILayout<LayoutHint>) element;

                Class<? extends LayoutHint> layoutHintType = (Class<? extends LayoutHint>)
                        ReflectionUtil.getTypeParameter(widgetClass.getGenericSuperclass(), 0);
                if (jsonObject.has(CONTENTS_FIELD)) {
                    for (JsonElement child : jsonObject.getAsJsonArray(CONTENTS_FIELD)) {
                        UIWidget childElement = context.deserialize(child, UIWidget.class);
                        if (childElement != null) {
                            LayoutHint hint = null;
                            if (child.isJsonObject()) {
                                JsonObject childObject = child.getAsJsonObject();
                                if (layoutHintType != null && !layoutHintType.isInterface() && !Modifier.isAbstract(layoutHintType.getModifiers())
                                        && childObject.has(LAYOUT_INFO_FIELD)) {
                                    hint = context.deserialize(childObject.get(LAYOUT_INFO_FIELD), layoutHintType);
                                }
                            }
                            layout.addWidget(childElement, hint);
                        }
                    }
                }
            }
            return element;
        }

        private Class<?> determineFieldType(Field field) {
            try {
                Method getter = ReflectionUtil.findGetter(field.getName(), field.getDeclaringClass());
                if (getter != null) {
                    return getter.getReturnType();
                } else {
                    return field.getType();
                }
            } catch (NoClassDefFoundError ignore) {
                return field.getType();
            }
        }

        private List<Field> getFields(Class<?> widgetType) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> widgetClass = widgetType; widgetClass != null; widgetClass = widgetClass.getSuperclass()) {
                for (Field field : widgetClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(LayoutConfig.class)) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
            }
            return fields;
        }
    }
}
