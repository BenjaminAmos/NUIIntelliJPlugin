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

import com.github.benjaminamos.nuiplugin.languages.NuiFileType;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NuiLayoutReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement().and(new FilterPattern(new ElementFilter() {
            @Override
            public boolean isAcceptable(Object element, @Nullable PsiElement context) {
                JsonValue value = (JsonValue) element;
                PsiElement valueParent = value.getParent();
                if (!(valueParent instanceof JsonProperty)) {
                    return false;
                }

                JsonProperty valueProperty = (JsonProperty) valueParent;
                return value.getContainingFile().getFileType() == NuiFileType.INSTANCE &&
                        valueProperty.getValue() == value && "type".equals(valueProperty.getName());
            }

            @Override
            public boolean isClassAcceptable(Class hintClass) {
                return JsonValue.class.isAssignableFrom(hintClass);
            }
        })), new NuiElementTypeReferenceProvider());

        PatternCondition<JsonProperty> withTypePropertySiblingCondition = new PatternCondition<>("withSibling") {
            @Override
            public boolean accepts(@NotNull JsonProperty jsonProperty, ProcessingContext context) {
                PsiElement parent = jsonProperty.getParent();
                if (parent == null) {
                    return false;
                }

                for (PsiElement child : parent.getChildren()) {
                    if (child instanceof JsonProperty) {
                        JsonProperty typeProperty = (JsonProperty) child;
                        if ("type".equals(typeProperty.getName())) {
                            return true;
                        }
                    }
                }

                return false;
            }
        };

        registrar.registerReferenceProvider(PlatformPatterns.psiElement()
                .andOr(
                    PlatformPatterns.psiElement().withParent(
                            PlatformPatterns.psiElement(JsonProperty.class)
                                    .with(withTypePropertySiblingCondition)
                    ),
                    PlatformPatterns.psiElement().withSuperParent(3,
                            PlatformPatterns.psiElement(JsonProperty.class)
                                    .withName("layoutInfo")
                                    .with(withTypePropertySiblingCondition)
                    )
                )
                .and(new FilterPattern(new ElementFilter() {
            @Override
            public boolean isAcceptable(Object element, @Nullable PsiElement context) {
                JsonStringLiteral value = (JsonStringLiteral) element;
                PsiElement valueParent = value.getParent();
                if (!(valueParent instanceof JsonProperty)) {
                    return false;
                }

                JsonProperty valueProperty = (JsonProperty) valueParent;
                return value.getContainingFile().getFileType() == NuiFileType.INSTANCE &&
                        valueProperty.getNameElement() == value &&
                        valueProperty.getParent() != null &&
                        !"type".equals(valueProperty.getName());
            }

            @Override
            public boolean isClassAcceptable(Class hintClass) {
                return JsonStringLiteral.class.isAssignableFrom(hintClass);
            }
        })), new NuiElementPropertyReferenceProvider());
    }
}
