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

package com.github.benjaminamos.nuiplugin.services;

import com.github.benjaminamos.nuiplugin.extensionpoints.beans.GestaltConfiguration;
import com.github.benjaminamos.nuiplugin.nui.AwtBitmapFont;
import com.github.benjaminamos.nuiplugin.nui.AwtTextureRegion;
import com.github.benjaminamos.nuiplugin.nui.UISkinLoader;
import com.github.benjaminamos.nuiplugin.nui.WidgetLibrary;
import com.github.benjaminamos.nuiplugin.nui.bitmapfont.FontLoader;
import com.github.benjaminamos.nuiplugin.utils.GestaltUrn;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.nui.UITextureRegion;
import org.terasology.nui.UIWidget;
import org.terasology.nui.asset.font.Font;
import org.terasology.nui.skin.UISkin;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public final class GestaltModuleService implements DumbService.DumbModeListener, Disposable {
    private static class FileCacheEntry<T> {
        public FileCacheEntry(VirtualFile file) {
            this.file = file;
            this.cache = null;
        }

        public final VirtualFile file;
        public T cache;
        public long lastModified;
    }

    private static final ExtensionPointName<GestaltConfiguration> GESTALT_CONFIGURATION_EXTENSION_POINT_NAME =
            ExtensionPointName.create("com.github.benjaminamos.nuiplugin.gestaltConfiguration");
    private final Project project;
    private final WidgetLibrary widgetLibrary;
    private final Map<String, VirtualFile> moduleRoots = new HashMap<>();
    private final Map<String, FileCacheEntry<UITextureRegion>> moduleImageCache = new HashMap<>();
    private final Map<String, FileCacheEntry<Font>> moduleFontCache = new HashMap<>();
    private final Map<String, FileCacheEntry<UISkin>> moduleSkinCache = new HashMap<>();
    private final Map<VirtualFile, UISkinLoader> skinLoaders = new HashMap<>();

    public GestaltModuleService(Project project) {
        this.project = project;
        this.widgetLibrary = new WidgetLibrary();
        // TODO: Reflections is expensive to use! It's really convenient though...
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addClassLoaders(UIWidget.class.getClassLoader())
                .forPackage("org.terasology.nui", UIWidget.class.getClassLoader())
                .setExpandSuperTypes(true)
                .addScanners(Scanners.SubTypes));
        widgetLibrary.addWidgetClasses(reflections.getSubTypesOf(UIWidget.class));

        updateModuleRoots();

        project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, this);
    }

    public WidgetLibrary getWidgetLibrary() {
        return widgetLibrary;
    }

    public Set<VirtualFile> getModuleRoots() {
        return Set.copyOf(moduleRoots.values());
    }

    public String getModuleIdForFile(VirtualFile file) {
        if (!file.exists()) {
            return null;
        }

        for (Map.Entry<String, VirtualFile> module : moduleRoots.entrySet()) {
            if (file.getPath().startsWith(module.getValue().getPath())) {
                return module.getKey();
            }
        }
        return null;
    }

    public UITextureRegion getImageByUrn(String urn) {
        return getImageByUrn("engine", urn);
    }

    public UITextureRegion getImageByUrn(String moduleContext, String urn) {
        UITextureRegion result = tryGetCache(moduleImageCache, file -> {
            try {
                try (InputStream fileStream = file.getInputStream()) {
                    return new AwtTextureRegion(urn, ImageIO.read(fileStream));
                }
            } catch (Exception ignore) {
                return null;
            }
        }, ".png", moduleContext, urn);
        if (result != null) {
            return result;
        }
        // Alternative Terasology image format. Used for the menu backgrounds, mostly.
        return tryGetCache(moduleImageCache, file -> {
            try {
                try (InputStream fileStream = file.getInputStream()) {
                    return new AwtTextureRegion(urn, ImageIO.read(fileStream));
                }
            } catch (Exception ignore) {
                return null;
            }
        }, ".jpeg", moduleContext, urn);
    }

    public UISkin getSkinByUrn(String urn) {
        return getSkinByUrn("engine", urn);
    }

    public UISkin getSkinByUrn(String moduleContext, String urn) {
        String module;
        if (urn.contains(":")) {
            module = urn.split(":")[0];
        } else {
            module = moduleContext;
        }

        return tryGetCache(moduleSkinCache, file -> {
            try {
                UISkinLoader skinLoader = skinLoaders.get(file);
                if (skinLoader == null) {
                    skinLoader = new UISkinLoader(this, module);
                    skinLoaders.put(file, skinLoader);
                }
                return skinLoader.load(file.getInputStream());
            } catch (Exception ignore) {
                return null;
            }
        }, ".skin", moduleContext, urn);
    }

    public UISkin getDefaultSkin() {
        for (GestaltConfiguration configuration : GESTALT_CONFIGURATION_EXTENSION_POINT_NAME.getExtensionList()) {
            UISkin skin = getSkinByUrn(configuration.defaultSkin);
            if (skin != null) {
                return skin;
            }
        }

        return null;
    }

    public Font getFontByUrn(String urn) {
        return getFontByUrn("engine", urn);
    }

    public Font getFontByUrn(String moduleContext, String urn) {
        Function<VirtualFile, Font> fontLoader = file -> {
            try {
                return new AwtBitmapFont(new FontLoader().load(file.getParent(), file.getInputStream()));
            } catch (Exception ignore) {
                return null;
            }
        };

        Font font = tryGetCache(moduleFontCache, fontLoader, ".fnt", moduleContext, urn);
        if (font != null) {
            return font;
        }

        // Alternative extension
        return tryGetCache(moduleFontCache, fontLoader, ".font", moduleContext, urn);
    }

    public void updateModuleRoots() {
        ReadAction.nonBlocking(Executors.callable(() -> {
            GlobalSearchScope projectSearchScope = GlobalSearchScope.projectScope(project);
            Set<VirtualFile> outputDirectories = new HashSet<>();
            for (VirtualFile moduleRoot : ProjectRootManager.getInstance(project).getContentRootsFromAllModules()) {
                for (GestaltConfiguration configuration : GESTALT_CONFIGURATION_EXTENSION_POINT_NAME.getExtensionList()) {
                    for (String directory : configuration.excludeDirs) {
                        VirtualFile outputRoot = moduleRoot.findChild(directory);
                        if (outputRoot != null && outputRoot.exists()) {
                            outputDirectories.add(outputRoot);
                        }
                    }
                }
            }

            Predicate<VirtualFile> excludeOutputDirectoriesPredicate =
                    file -> file.exists() && outputDirectories.stream().noneMatch(dir -> file.getPath().startsWith(dir.getPath()));

            FilenameIndex.getVirtualFilesByName(project, "module.txt", false, projectSearchScope).stream()
                    .filter(excludeOutputDirectoriesPredicate)
                    .forEach(file -> moduleRoots.put(getModuleNameFromManifest(file), file.getParent()));
            FilenameIndex.getVirtualFilesByName(project, "module.json", false, projectSearchScope).stream()
                    .filter(excludeOutputDirectoriesPredicate)
                    .forEach(file -> moduleRoots.put(getModuleNameFromManifest(file), file.getParent()));
            FilenameIndex.getVirtualFilesByName(project, "module.info", false, projectSearchScope).stream()
                    .filter(excludeOutputDirectoriesPredicate)
                    .forEach(file -> moduleRoots.put(getModuleNameFromManifest(file), file.getParent()));
        })).inSmartMode(project).submit(NonUrgentExecutor.getInstance());
    }

    public void invalidateSkins() {
        moduleSkinCache.clear();
    }

    private String getModuleNameFromManifest(VirtualFile manifestFile) {
        try {
            JsonElement json = new JsonParser().parse(new InputStreamReader(manifestFile.getInputStream()));
            return json.getAsJsonObject().get("id").getAsString();
        } catch (Throwable ignore) {
            // TODO: Error?
            return manifestFile.getParent().getName();
        }
    }

    private <T> T tryGetCache(Map<String, FileCacheEntry<T>> cache, Function<VirtualFile, T> loader, String extension,
                              String moduleContext, String urn) {
        FileCacheEntry<T> cacheEntry = cache.get(urn);
        if (cacheEntry == null || cacheEntry.lastModified != cacheEntry.file.getModificationStamp()) {
            if (!urn.contains(":")) {
                urn = moduleContext + ":" + urn;
            }

            GestaltUrn gestaltUrn = GestaltUrn.parse(urn);
            if (gestaltUrn == null) {
                // TODO: Return error
                return null;
            }

            VirtualFile module = moduleRoots.get(gestaltUrn.getModule().toLowerCase(Locale.ROOT));
            if (module == null) {
                // TODO: Return error
                return null;
            }
            String asset = gestaltUrn.getAsset().toLowerCase(Locale.ROOT) + extension;
            Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, asset, false,
                    GlobalSearchScopesCore.directoryScope(project, module, true));
            if (files.size() != 1) {
                // TODO: Return error
                return null;
            } else {
                try {
                    cacheEntry = new FileCacheEntry<>(files.stream().findFirst().get());
                    T cachedValue = loader.apply(cacheEntry.file);
                    cacheEntry.cache = cachedValue;
                    cacheEntry.lastModified = cacheEntry.file.getModificationStamp();
                    cache.put(urn, cacheEntry);
                    return cachedValue;
                } catch (Exception ignore) {
                    return null;
                }
            }
        } else {
            return cacheEntry.cache;
        }
    }

    @Override
    public void exitDumbMode() {
        updateModuleRoots();
    }

    @Override
    public void dispose() {
    }
}
