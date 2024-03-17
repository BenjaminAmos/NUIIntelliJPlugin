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
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.LayoutHint;
import org.terasology.nui.UILayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NuiPropertyReference extends PsiReferenceBase<JsonStringLiteral> {
    public NuiPropertyReference(@NotNull JsonStringLiteral element) {
        super(element, true);
    }

    private JsonObject getParentObject(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null && !(parent instanceof JsonObject)) {
            parent = parent.getParent();
        }
        return (JsonObject) parent;
    }

    private PsiClass getReferencedClass(PsiElement element) {
        JsonObject parentObject = getParentObject(element);

        PsiReferenceService psiReferenceService = PsiReferenceService.getService();

        PsiElement potentialParentProperty = parentObject.getParent();
        if (potentialParentProperty instanceof JsonProperty) {
            JsonProperty parentProperty = (JsonProperty) parentObject.getParent();
            if ("layoutInfo".equals(parentProperty.getName())) {
                for (PsiReference reference : psiReferenceService.getContributedReferences(parentProperty.getNameElement())) {
                    if (reference instanceof NuiPropertyReference) {
                        return (PsiClass) reference.resolve();
                    }
                }
            }
        }

        for (PsiElement child : parentObject.getChildren()) {
            if (child instanceof JsonProperty) {
                JsonProperty typeProperty = (JsonProperty) child;
                if ("type".equals(typeProperty.getName()) && typeProperty.getValue() != null) {
                    for (PsiReference reference : psiReferenceService.getContributedReferences(typeProperty.getValue())) {
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

        PsiClass referencedClass = getReferencedClass(myElement);
        if (referencedClass == null) {
            return null;
        }

        if ("layoutInfo".equals(myElement.getValue())) {
            // Special case: Layout info doesn't exist but is conventional, since it's actually controlled via the API.
            JsonObject parentWidget = getParentObject(myElement);
            if (parentWidget == null) {
                return null;
            }

            PsiClass layoutInfoClass = JavaPsiFacade.getInstance(myElement.getProject())
                    .findClass(UILayout.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
            PsiClass layoutClass = getReferencedClass(parentWidget);
            if (layoutInfoClass != null && layoutClass != null &&
                    layoutClass.isInheritor(layoutInfoClass, true)) {
                if (layoutClass.getExtendsList() == null) {
                    return null;
                }

                PsiClass layoutHintClass = JavaPsiFacade.getInstance(myElement.getProject())
                        .findClass(LayoutHint.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
                if (layoutHintClass == null) {
                    return null;
                }

                PsiClassType[] referencedTypes = layoutClass.getExtendsList().getReferencedTypes();
                if (referencedTypes.length == 1) {
                    PsiType[] typeParameters = referencedTypes[0].getParameters();
                    if (typeParameters.length == 1 && typeParameters[0] instanceof PsiClassType) {
                        PsiClass hintClass = ((PsiClassType) typeParameters[0]).resolve();
                        if (hintClass != null && hintClass.isInheritor(layoutHintClass, true)) {
                            return hintClass;
                        }
                    }
                }
                return null;
            }
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

        return null;
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (element instanceof PsiField) {
            PsiField fieldElement = (PsiField) element;
            return handleElementRename(fieldElement.getName());
        } else {
            return resolve();
        }
    }

    @Override
    public Object @NotNull [] getVariants() {
        PsiClass referencedClass = getReferencedClass(myElement);
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

            if (field.getAnnotation(LayoutConfig.class.getTypeName()) != null && !names.contains(fieldName)) {
                variants.add(LookupElementBuilder.create(fieldName)
                        .withPsiElement(field)
                        .withIcon(field.getIcon(0)));
                names.add(fieldName);
            }
        }

        return variants.toArray(new LookupElement[0]);
    }
}
