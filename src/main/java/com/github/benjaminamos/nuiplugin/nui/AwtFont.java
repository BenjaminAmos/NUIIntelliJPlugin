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

import org.joml.Vector2i;
import org.terasology.nui.asset.font.Font;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.List;

public class AwtFont implements Font {
    private final java.awt.Font awtFont;
    private final FontRenderContext fontRenderContext;

    public AwtFont(java.awt.Font awtFont) {
        this.awtFont = awtFont;
        this.fontRenderContext = new FontRenderContext(new AffineTransform(), false, false);
    }

    @Override
    public int getWidth(String text) {
        return (int) Math.ceil(awtFont.getStringBounds(text, fontRenderContext).getWidth());
    }

    @Override
    public int getWidth(Character c) {
        return getWidth("" + c);
    }

    @Override
    public int getHeight(String text) {
        return (int) Math.ceil(awtFont.getStringBounds(text, fontRenderContext).getHeight());
    }

    @Override
    public int getLineHeight() {
        return (int) Math.ceil(awtFont.getMaxCharBounds(fontRenderContext).getHeight());
    }

    @Override
    public int getBaseHeight() {
        return awtFont.getBaselineFor(' ');
    }

    @Override
    public Vector2i getSize(List<String> lines) {
        return new Vector2i(getWidth(String.join("\n", lines)), lines.size() * getLineHeight());
    }

    @Override
    public boolean hasCharacter(Character c) {
        return awtFont.canDisplay(c);
    }

    @Override
    public int getUnderlineOffset() {
        // TODO: Magic constants - everyone uses this value but why?
        return 2;
    }

    @Override
    public int getUnderlineThickness() {
        // TODO: Magic constants - everyone uses this value but why?
        return 1;
    }

    public java.awt.Font getAwtFont() {
        return awtFont;
    }
}
