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

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NuiFilePreviewerProvider implements FileEditorProvider, DumbAware {
    /**
     * Method is expected to run fast.
     *
     * @param project
     * @param file    file to be tested for acceptance.
     * @return {@code true} if provider can create valid editor for the specified {@code file}.
     */
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getExtension().equals("ui");
    }

    /**
     * Creates editor for the specified file.
     * <p>
     * This method is called only if the provider has accepted this file (i.e. method {@link #accept(Project, VirtualFile)} returned
     * {@code true}).
     * The provider should return only valid editor.
     *
     * @param project
     * @param file
     * @return created editor for specified file.
     */
    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        // TODO: Don't depend on the PsiAwareTextEditorImpl implementation detail
        return new NuiFileSplitEditor(new PsiAwareTextEditorImpl(project, file, new PsiAwareTextEditorProvider()), new NuiFilePreviewer(project, file));
    }

    /**
     * @return id of type of the editors created with this FileEditorProvider. Each FileEditorProvider should have
     * unique nonnull id. The id is used for saving/loading of EditorStates.
     */
    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return "com.github.benjaminamos.nuiplugin.languages.NuiFilePreviewerProvider";
    }

    /**
     * @return policy that specifies how editor created via this provider should be opened.
     * @see FileEditorPolicy#NONE
     * @see FileEditorPolicy#HIDE_DEFAULT_EDITOR
     * @see FileEditorPolicy#PLACE_BEFORE_DEFAULT_EDITOR
     */
    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
