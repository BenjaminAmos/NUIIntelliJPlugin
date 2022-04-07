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

package com.github.benjaminamos.nuiplugin.notifications;

import com.github.benjaminamos.nuiplugin.languages.NuiFilePreviewer;
import com.github.benjaminamos.nuiplugin.languages.NuiFileSplitEditor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.LightColors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class BuildMissingClassesNotification extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("com.github.benjaminamos.nuiplugin.notifications.BuildMissingClassesNotification");

    @Override
    public @NotNull Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    public @Nullable EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file,
                                                                     @NotNull FileEditor fileEditor,
                                                                     @NotNull Project project) {
        if (fileEditor instanceof NuiFileSplitEditor) {
            NuiFilePreviewer filePreviewer = (NuiFilePreviewer) ((NuiFileSplitEditor) fileEditor).getPreviewEditor();
            Set<Module> requiredModules = filePreviewer.getRequiredModules();
            if (requiredModules.isEmpty()) {
                return null;
            }

            EditorNotificationPanel panel = new EditorNotificationPanel(LightColors.YELLOW)
                    .text("This file uses modules that have not been built yet.");
            panel.createActionLabel("Build required modules", () -> {
                CompilerManager compilerManager = CompilerManager.getInstance(project);
                compilerManager.make(compilerManager.createModulesCompileScope(requiredModules.toArray(new Module[0]), false),
                        (boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) -> {
                    filePreviewer.reload();
                    EditorNotifications.getInstance(project).updateNotifications(file);
                });
            });
            return panel;
        } else {
            return null;
        }
    }
}
