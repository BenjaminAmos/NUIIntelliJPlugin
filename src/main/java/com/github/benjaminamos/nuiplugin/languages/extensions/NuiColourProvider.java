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

package com.github.benjaminamos.nuiplugin.languages.extensions;

import com.github.benjaminamos.nuiplugin.languages.contributors.NuiPropertyReference;
import com.github.benjaminamos.nuiplugin.languages.contributors.NuiStylePropertyReference;
import com.intellij.json.psi.JsonElementGenerator;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

@SuppressWarnings("UseJBColor")
public class NuiColourProvider implements ElementColorProvider {
    private PsiReference getNuiPropertyReference(JsonProperty property) {
        PsiReference[] references = PsiReferenceService.getService().getContributedReferences(property.getNameElement());
        for (PsiReference reference : references) {
            if (reference instanceof NuiPropertyReference || reference instanceof NuiStylePropertyReference) {
                return reference;
            }
        }
        return null;
    }

    @Override
    public @Nullable Color getColorFrom(@NotNull PsiElement element) {
        PsiElement parentElement = element.getParent();
        if (!(parentElement instanceof JsonStringLiteral)) {
            return null;
        }

        PsiElement elementParent = parentElement.getParent();
        if (elementParent instanceof JsonProperty) {
            // We're returning the colour for the name element, even though we want to change the value element.
            // IntelliJ's colour picker will not work when you replace the element being changed.
            JsonProperty elementProperty = (JsonProperty) elementParent;
            if (elementProperty.getNameElement() != parentElement || elementProperty.getValue() == null) {
                return null;
            }

            PsiReference propertyReference = getNuiPropertyReference(elementProperty);
            if (propertyReference != null) {
                PsiElement propertyField = propertyReference.resolve();
                if (propertyField instanceof PsiField &&
                        ((PsiField) propertyField).getType().equalsToText(org.terasology.nui.Color.class.getCanonicalName())) {
                    int colourIntValue = Integer.parseUnsignedInt(((JsonStringLiteral) elementProperty.getValue()).getValue(), 16);
                    org.terasology.nui.Color nuiColour = new org.terasology.nui.Color(colourIntValue);
                    return new Color(nuiColour.r(), nuiColour.g(), nuiColour.b(), nuiColour.a());
                }
            }
        }
        return null;
    }

    @Override
    public void setColorTo(@NotNull PsiElement element, @NotNull Color color) {
        PsiElement elementParent = element.getParent();
        if (elementParent instanceof JsonStringLiteral) {
            PsiElement elementProperty = elementParent.getParent();
            if (elementProperty instanceof JsonProperty) {
                JsonStringLiteral elementValue = (JsonStringLiteral) ((JsonProperty) elementProperty).getValue();
                if (elementValue == null) {
                    return;
                }

                org.terasology.nui.Color nuiColour = new org.terasology.nui.Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
                Project project = element.getProject();
                Document document = PsiDocumentManager.getInstance(project).getDocument(element.getContainingFile());
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    JsonElementGenerator jsonElementGenerator = new JsonElementGenerator(elementParent.getProject());
                    JsonStringLiteral newStringLiteral =
                            jsonElementGenerator.createStringLiteral(String.format("%08X", nuiColour.rgba()));
                    elementValue.replace(newStringLiteral);
                }, "Change Colour Command", null, document);
            }
        }
    }
}
