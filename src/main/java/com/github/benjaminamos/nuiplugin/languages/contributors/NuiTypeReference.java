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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.terasology.nui.UIWidget;

import java.util.ArrayList;
import java.util.List;

public class NuiTypeReference extends PsiPolyVariantReferenceBase<JsonStringLiteral> {
    public NuiTypeReference(JsonStringLiteral typeJsonElement) {
        super(typeJsonElement);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        PsiClass uiWidgetClass = JavaPsiFacade.getInstance(myElement.getProject())
                .findClass(UIWidget.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));
        String type = myElement.getValue();
        PsiClass[] referencedClasses = PsiShortNamesCache.getInstance(myElement.getProject())
                .getClassesByName(type, GlobalSearchScope.allScope(myElement.getProject()));

        List<ResolveResult> results = new ArrayList<>();
        for (PsiClass referencedClass : referencedClasses) {
            if (referencedClass.isInheritor(uiWidgetClass, true)) {
                results.add(new PsiElementResolveResult(referencedClass));
            }
        }

        return results.toArray(new ResolveResult[0]);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return resolve();
    }

    @Override
    public boolean isSoft() {
        return true;
    }

    @Override
    public Object @NotNull [] getVariants() {
        PsiClass uiWidgetClass = JavaPsiFacade.getInstance(myElement.getProject())
                .findClass(UIWidget.class.getName(), GlobalSearchScope.allScope(myElement.getProject()));

        @Nullable Module fileModule = ModuleUtil.findModuleForFile(myElement.getContainingFile().getOriginalFile().getVirtualFile(), myElement.getProject());
        if (fileModule == null) {
            return new Object[0];
        }

        @Nullable Module codeModule = ModuleManager.getInstance(myElement.getProject()).findModuleByName(fileModule.getName() + ".main");
        if (codeModule != null) {
            fileModule = codeModule;
        }

        PsiClass[] inherited = ClassInheritorsSearch
                .search(uiWidgetClass, fileModule.getModuleRuntimeScope(false), true)
                .toArray(new PsiClass[0]);
        LookupElement[] variants = new LookupElement[inherited.length];
        for (int classNo = 0; classNo < inherited.length; classNo++) {
            PsiClass variant = inherited[classNo];
            variants[classNo] = LookupElementBuilder.create(variant)
                    .withLookupString("engine:" + variant.getName())
                    .withIcon(variant.getIcon(0));
        }
        return variants;
    }
}
