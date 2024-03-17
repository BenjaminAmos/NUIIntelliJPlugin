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

package com.github.benjaminamos.nuiplugin.nui;

import com.intellij.util.JBHiDPIScaledImage;
import org.joml.Vector2i;
import org.terasology.joml.geom.Rectanglef;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.UITextureRegion;

import java.awt.Image;

public class AwtTextureRegion implements UITextureRegion {
    // For debugging
    private final String name;
    private final Image image;

    public AwtTextureRegion(String name, Image image) {
        this.name = name;
        this.image = image;
    }

    /**
     * @return The region of the texture represented by this asset
     */
    @Override
    public Rectanglef getRegion() {
        return new Rectanglef(0, 0, getWidth(), getHeight());
    }

    /**
     * @return The pixel region of the texture represented by this asset
     */
    @Override
    public Rectanglei getPixelRegion() {
        return new Rectanglei(0, 0, getWidth(), getHeight());
    }

    @Override
    public int getWidth() {
        int width = image.getWidth(null);
        if (image instanceof JBHiDPIScaledImage) {
            return (int) (width * ((JBHiDPIScaledImage) image).getScale());
        } else {
            return width;
        }
    }

    @Override
    public int getHeight() {
        int height = image.getHeight(null);
        if (image instanceof JBHiDPIScaledImage) {
            return (int) (height * ((JBHiDPIScaledImage) image).getScale());
        } else {
            return height;
        }
    }

    @Override
    public Vector2i size() {
        return new Vector2i(getWidth(), getHeight());
    }

    public Image getAwtImage() {
        return image;
    }

    public String getName() {
        return name;
    }
}
