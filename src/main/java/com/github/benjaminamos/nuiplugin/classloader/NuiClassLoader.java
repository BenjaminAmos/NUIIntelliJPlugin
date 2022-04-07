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

package com.github.benjaminamos.nuiplugin.classloader;

import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.UIWidget;

import java.nio.file.Path;
import java.util.List;

public class NuiClassLoader extends UrlClassLoader {
    public NuiClassLoader(@NotNull List<Path> classPaths) {
        super(build()
                .allowBootstrapResources(false)
                .allowLock(false)
                .files(classPaths), true);
    }

    @Override
    protected Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
        if (name.startsWith("org.terasology.nui")) {
            return UIWidget.class.getClassLoader().loadClass(name);
        }

        if (name.startsWith("org.joml")) {
            return Vector2i.class.getClassLoader().loadClass(name);
        }

        if (name.startsWith("org.terasology.joml.geom")) {
            return Rectanglei.class.getClassLoader().loadClass(name);
        }

        return super.findClass(name);
    }
}
