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

package com.github.benjaminamos.nuiplugin.nui;

import org.terasology.nui.UIWidget;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WidgetLibrary {
    private Map<String, Class<? extends UIWidget>> widgetsByName = new HashMap<>();

    public Class<? extends UIWidget> getWidgetClassByName(String context, String name) {
        Class<? extends UIWidget> widgetClass = widgetsByName.get(name.toLowerCase());
        if (widgetClass != null) {
            return widgetClass;
        }
        return widgetsByName.get(context + ":" + name.toLowerCase());
    }

    public Class<? extends UIWidget> getWidgetClassByName(String name) {
        return widgetsByName.get(name.toLowerCase());
    }

    public void addWidgetClass(Class<? extends UIWidget> widgetClass) {
        addWidgetClass(widgetClass.getSimpleName(), widgetClass);
    }

    public void addWidgetClass(String alias, Class<? extends UIWidget> widgetClass) {
        widgetsByName.put(alias.toLowerCase(), widgetClass);
    }

    public void addWidgetClasses(Collection<Class<? extends UIWidget>> classes) {
        for (Class<? extends UIWidget> widgetClass : classes) {
            addWidgetClass(widgetClass);
        }
    }
}
