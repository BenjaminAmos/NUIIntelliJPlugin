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
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.FilterPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NuiSkinReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        PsiElementPattern.Capture<PsiElement> isElementPropertyPattern = PlatformPatterns.psiElement()
                .withName("elements")
                .andOr(
                        PlatformPatterns.psiElement()
                                .withSuperParent(2, JsonFile.class),
                        PlatformPatterns.psiElement()
                                .withSuperParent(4,
                                        PlatformPatterns.psiElement(JsonProperty.class)
                                                .withName("families")
                                                .withSuperParent(2, JsonFile.class)
                                )
                );

        PsiElementPattern.Capture<PsiElement> elementPattern = PlatformPatterns.psiElement()
                .withSuperParent(3,
                        StandardPatterns.instanceOf(JsonProperty.class)
                                .and(isElementPropertyPattern)
                );

        registrar.registerReferenceProvider(elementPattern
                .and(new FilterPattern(new ElementFilter() {
                    @Override
                    public boolean isAcceptable(Object element, @Nullable PsiElement context) {
                        JsonStringLiteral key = (JsonStringLiteral) element;
                        PsiElement keyParent = key.getParent();
                        if (!(keyParent instanceof JsonProperty)) {
                            return false;
                        }

                        return key.getContainingFile().getFileType() == NuiSkinFileType.INSTANCE &&
                                ((JsonProperty) keyParent).getNameElement() == key;
                    }

                    @Override
                    public boolean isClassAcceptable(Class hintClass) {
                        return JsonStringLiteral.class.isAssignableFrom(hintClass);
                    }
                })), new NuiElementTypeReferenceProvider());
        registrar.registerReferenceProvider(PlatformPatterns.psiElement()
                .andOr(
                    PlatformPatterns.psiElement().withSuperParent(3, JsonFile.class),
                    PlatformPatterns.psiElement().withSuperParent(2, elementPattern),
                        PlatformPatterns.psiElement()
                                .withSuperParent(5,
                                        PlatformPatterns.psiElement(JsonProperty.class)
                                                .withName("families")
                                                .withSuperParent(2, JsonFile.class)
                                ),
                    PlatformPatterns.psiElement()
                            .withSuperParent(3, PlatformPatterns.psiElement(JsonProperty.class)
                                    .withSuperParent(2, PlatformPatterns.psiElement(JsonProperty.class)
                                         .andOr(
                                             PlatformPatterns.psiElement().withName("modes"),
                                             PlatformPatterns.psiElement().withName("parts")
                                         )
                                        .withSuperParent(1, elementPattern)
                                    )
                            )

                )
                .and(new FilterPattern(new ElementFilter() {
                    @Override
                    public boolean isAcceptable(Object element, @Nullable PsiElement context) {
                        JsonStringLiteral key = (JsonStringLiteral) element;
                        PsiElement keyParent = key.getParent();
                        if (!(keyParent instanceof JsonProperty)) {
                            return false;
                        }

                        return key.getContainingFile().getFileType() == NuiSkinFileType.INSTANCE &&
                                ((JsonProperty) keyParent).getNameElement() == key &&
                                !"families".equals(key.getValue()) &&
                                !"elements".equals(key.getValue()) &&
                                !"modes".equals(key.getValue()) &&
                                !"parts".equals(key.getValue());
                    }

                    @Override
                    public boolean isClassAcceptable(Class hintClass) {
                        return JsonStringLiteral.class.isAssignableFrom(hintClass);
                    }
                })), new NuiStyleReferenceProvider());
    }
}
