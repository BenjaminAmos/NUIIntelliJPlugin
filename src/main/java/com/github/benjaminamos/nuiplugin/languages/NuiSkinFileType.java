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

package com.github.benjaminamos.nuiplugin.languages;

import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class NuiSkinFileType extends LanguageFileType {
    public static NuiSkinFileType INSTANCE = new NuiSkinFileType();

    private NuiSkinFileType() {
        super(JsonLanguage.INSTANCE, true);
    }

    /**
     * Returns the name of the file type. The name must be unique among all file types registered in the system.
     */
    @Override
    public @NonNls @NotNull String getName() {
        return "com.github.benjaminamos.nui.files.skin";
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "NUI Skin";
    }

    /**
     * Returns the user-readable description of the file type.
     */
    @Override
    public @NotNull String getDescription() {
        return "NUI skin file";
    }

    /**
     * Returns the default extension for files of the type, <em>not</em> including the leading '.'.
     */
    @Override
    public @NotNull String getDefaultExtension() {
        return "skin";
    }

    /**
     * Returns the icon used for showing files of the type, or {@code null} if no icon should be shown.
     * @return the icon shown.
     */
    @Override
    public @Nullable Icon getIcon() {
        // TODO
        return JsonFileType.INSTANCE.getIcon();
    }
}
