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

package com.github.benjaminamos.nuiplugin.languages.contributors;

import com.github.benjaminamos.nuiplugin.languages.NuiSkinFileType;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NuiSkinStructureCompletionContributor extends CompletionContributor {
    public NuiSkinStructureCompletionContributor() {
        PsiElementPattern.Capture<PsiElement> uiElementPattern = PlatformPatterns.psiElement()
                .andOr(
                        PlatformPatterns.psiElement()
                                .withSuperParent(3, JsonFile.class),
                        PlatformPatterns.psiElement()
                                .withSuperParent(5,
                                        PlatformPatterns.psiElement(JsonProperty.class)
                                                .withName("families")
                                                .withSuperParent(2, JsonFile.class)
                                )
                );

        extend(CompletionType.BASIC, PlatformPatterns.psiElement()
                .withParent(
                        PlatformPatterns.psiElement(JsonStringLiteral.class)
                                .and(uiElementPattern)
                )
                .and(new FilterPattern(new ElementFilter() {
                    @Override
                    public boolean isAcceptable(Object element, @Nullable PsiElement context) {
                        JsonStringLiteral key = (JsonStringLiteral) ((PsiElement) element).getParent();
                        PsiElement keyParent = key.getParent();
                        if (!(keyParent instanceof JsonProperty)) {
                            return false;
                        }

                        return key.getContainingFile().getFileType() == NuiSkinFileType.INSTANCE &&
                                ((JsonProperty) keyParent).getNameElement() == key;
                    }

                    @Override
                    public boolean isClassAcceptable(Class hintClass) {
                        return true;
                    }
                })), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                result.addElement(LookupElementBuilder.create("elements"));
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement()
                .withParent(PlatformPatterns
                        .psiElement(JsonStringLiteral.class)
                        .withSuperParent(3, JsonFile.class)
                )
                .and(new FilterPattern(new ElementFilter() {
                    @Override
                    public boolean isAcceptable(Object element, @Nullable PsiElement context) {
                        JsonStringLiteral key = (JsonStringLiteral) ((PsiElement) element).getParent();
                        PsiElement keyParent = key.getParent();
                        if (!(keyParent instanceof JsonProperty)) {
                            return false;
                        }

                        return key.getContainingFile().getFileType() == NuiSkinFileType.INSTANCE &&
                                ((JsonProperty) keyParent).getNameElement() == key;
                    }

                    @Override
                    public boolean isClassAcceptable(Class hintClass) {
                        return true;
                    }
                })), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                result.addElement(LookupElementBuilder.create("families"));
            }
        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement()
                .withParent(PlatformPatterns.psiElement(JsonStringLiteral.class)
                        .withSuperParent(4, PlatformPatterns.psiElement()
                                .withParent(PlatformPatterns.psiElement(JsonProperty.class).withName("elements"))
                                .and(uiElementPattern)
                        )
                ), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                result.addElement(LookupElementBuilder.create("modes"));
                result.addElement(LookupElementBuilder.create("parts"));
            }
        });
    }
}
