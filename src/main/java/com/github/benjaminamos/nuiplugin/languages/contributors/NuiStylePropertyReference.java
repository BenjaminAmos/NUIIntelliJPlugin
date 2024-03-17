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
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.skin.UIStyleFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NuiStylePropertyReference extends PsiReferenceBase<JsonStringLiteral> {
    public NuiStylePropertyReference(@NotNull JsonStringLiteral element) {
        super(element, true);
    }

    private Map<String, PsiField> getStyleFields() {
        Map<String, PsiField> fields = new HashMap<>();

        PsiClass styleFragmentClass = JavaPsiFacade.getInstance(myElement.getProject())
                .findClass(UIStyleFragment.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
        if (styleFragmentClass == null) {
            return fields;
        }

        for (PsiField field : styleFragmentClass.getAllFields()) {
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

            fields.put(fieldName, field);
        }

        return fields;
    }

    @Override
    public @Nullable PsiElement resolve() {
        PsiClass styleFragmentClass = JavaPsiFacade.getInstance(myElement.getProject())
                .findClass(UIStyleFragment.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
        if (styleFragmentClass == null) {
            return null;
        }

        for (Map.Entry<String, PsiField> field : getStyleFields().entrySet()) {
            String fieldName = field.getKey();
            if (fieldName.equals(myElement.getValue())) {
                return field.getValue();
            }
        }

        return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        PsiClass styleFragmentClass = JavaPsiFacade.getInstance(myElement.getProject())
                .findClass(UIStyleFragment.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
        if (styleFragmentClass == null) {
            return new Object[0];
        }

        List<LookupElement> variants = new ArrayList<>();

        Set<String> names = new HashSet<>();

        for (Map.Entry<String, PsiField> field : getStyleFields().entrySet()) {
            String fieldName = field.getKey();
            if (!names.contains(fieldName)) {
                variants.add(LookupElementBuilder.create(fieldName)
                        .withPsiElement(field.getValue())
                        .withIcon(field.getValue().getIcon(0)));
                names.add(fieldName);
            }
        }

        return variants.toArray(new LookupElement[0]);
    }
}
