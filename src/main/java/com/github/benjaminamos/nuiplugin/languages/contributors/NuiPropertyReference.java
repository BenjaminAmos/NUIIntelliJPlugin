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

import com.google.gson.annotations.SerializedName;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.UILayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NuiPropertyReference extends PsiReferenceBase<JsonStringLiteral> {
    public NuiPropertyReference(@NotNull JsonStringLiteral element) {
        super(element, true);
    }

    private PsiClass getReferencedClass() {
        for (PsiElement child : myElement.getParent().getParent().getChildren()) {
            if (child instanceof JsonProperty) {
                JsonProperty typeProperty = (JsonProperty) child;
                if ("type".equals(typeProperty.getName()) && typeProperty.getValue() != null) {
                    for (PsiReference reference : PsiReferenceService.getService().getContributedReferences(typeProperty.getValue())) {
                        if (reference instanceof NuiTypeReference) {
                            return (PsiClass) reference.resolve();
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable PsiElement resolve() {
        if (myElement.getValue().length() == 0) {
            return null;
        }

        PsiClass referencedClass = getReferencedClass();

        if (referencedClass == null) {
            return null;
        }

        PsiClass layoutInfoClass = JavaPsiFacade.getInstance(myElement.getProject())
                .findClass(UILayout.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
        if (layoutInfoClass != null &&
                referencedClass.isInheritor(layoutInfoClass, true) &&
                "layoutInfo".equals(myElement.getValue())) {
            // Special case: Layout info doesn't exist but is conventional, since it's actually controlled via the API.
            PsiClass hintType = referencedClass.getSuperClass().getTypeParameters()[0];
            return null;
        }

        for (PsiField field : referencedClass.getAllFields()) {
            String fieldName = field.getName();
            PsiAnnotation serialisedNameAnnotation = field.getAnnotation(SerializedName.class.getTypeName());
            if (serialisedNameAnnotation != null) {
                PsiAnnotationMemberValue value = serialisedNameAnnotation.findAttributeValue("value");
                if (value instanceof PsiLiteralValue) {
                    String serialisedName = (String) ((PsiLiteralValue) value).getValue();
                    if (serialisedName != null) {
                        fieldName = serialisedName;
                    }
                }
            }

            if (fieldName.equals(myElement.getValue()) && field.getAnnotation(LayoutConfig.class.getTypeName()) != null) {
                return field;
            }
        }

        for (PsiMethod method : referencedClass.getAllMethods()) {
            String elementName = myElement.getValue();
            String getterName = "set";
            getterName += Character.toUpperCase(elementName.charAt(0));
            if (elementName.length() > 1) {
                getterName += elementName.substring(1);
            }

            if (method.getName().equals(getterName)) {
                return method;
            }
        }

        return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        PsiClass referencedClass = getReferencedClass();
        if (referencedClass == null) {
            return new Object[0];
        }

        List<LookupElement> variants = new ArrayList<>();

        Set<String> names = new HashSet<>();

        for (PsiField field : referencedClass.getAllFields()) {
            String fieldName = field.getName();
            PsiAnnotation serialisedNameAnnotation = field.getAnnotation(SerializedName.class.getTypeName());
            if (serialisedNameAnnotation != null) {
                PsiAnnotationMemberValue value = serialisedNameAnnotation.findAttributeValue("value");
                if (value instanceof PsiLiteralValue) {
                    String serialisedName = (String) ((PsiLiteralValue) value).getValue();
                    if (serialisedName != null) {
                        fieldName = serialisedName;
                    }
                }
            }

            if ( field.getAnnotation(LayoutConfig.class.getTypeName()) != null && !names.contains(fieldName)) {
                variants.add(LookupElementBuilder.create(field)
                        .withLookupString(fieldName)
                        .withIcon(field.getIcon(0)));
                names.add(fieldName);
            }
        }

        for (PsiMethod method : referencedClass.getAllMethods()) {
            if (method.getName().startsWith("set")) {
                String setVariableName = method.getName().substring(3);
                if (setVariableName.length() == 1) {
                    setVariableName = setVariableName.toLowerCase();
                } else {
                    setVariableName = Character.toLowerCase(setVariableName.charAt(0)) + setVariableName.substring(1);
                }

                if (!names.contains(setVariableName)) {
                    variants.add(LookupElementBuilder.create(method, setVariableName)
                            .withIcon(method.getIcon(0)));
                }
            }
        }

        return variants.toArray(new LookupElement[0]);
    }
}
