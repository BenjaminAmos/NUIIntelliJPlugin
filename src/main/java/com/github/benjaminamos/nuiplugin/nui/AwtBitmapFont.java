/*
 * Copyright 2016 MovingBlocks
 * Modifications Copyright 2022 Benjamin Amos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Most of the code here is taken from Terasology:
// https://github.com/MovingBlocks/Terasology/blob/023571e2360b0c8b7bfdd71e8281cf2c01a54472/engine/src/main/java/org/terasology/engine/rendering/assets/font/FontImpl.java

package com.github.benjaminamos.nuiplugin.nui;

import com.github.benjaminamos.nuiplugin.nui.bitmapfont.FontCharacter;
import com.github.benjaminamos.nuiplugin.nui.bitmapfont.FontData;
import org.joml.Vector2i;
import org.terasology.nui.asset.font.Font;

import java.util.List;

public class AwtBitmapFont implements Font {
    private final FontData bitmapFont;

    public AwtBitmapFont(FontData bitmapFont) {
        this.bitmapFont = bitmapFont;
    }

    @Override
    public int getWidth(String text) {
        int largestWidth = 0;
        int currentWidth = 0;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                largestWidth = Math.max(largestWidth, currentWidth);
                currentWidth = 0;
            } else {
                FontCharacter character = bitmapFont.getCharacter(c);
                if (character != null) {
                    currentWidth += character.getxAdvance();
                }
            }
        }
        return Math.max(largestWidth, currentWidth);
    }

    @Override
    public int getWidth(Character c) {
        FontCharacter character = bitmapFont.getCharacter(c);
        if (character != null) {
            return character.getxAdvance();
        }
        return 0;
    }

    @Override
    public int getHeight(String text) {
        int height = bitmapFont.getLineHeight();
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                height += bitmapFont.getLineHeight();
            }
        }
        return height;
    }

    @Override
    public int getLineHeight() {
        return bitmapFont.getLineHeight();
    }

    @Override
    public int getBaseHeight() {
        return bitmapFont.getBaseHeight();
    }

    @Override
    public Vector2i getSize(List<String> lines) {
        int height = getLineHeight() * lines.size();
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, getWidth(line));
        }
        return new Vector2i(width, height);
    }

    @Override
    public boolean hasCharacter(Character c) {
        return c == '\n' || bitmapFont.getCharacter(c) != null;
    }

    public FontCharacter getCharacterData(Character c) {
        return bitmapFont.getCharacter(c);
    }

    @Override
    public int getUnderlineOffset() {
        return bitmapFont.getUnderlineOffset();
    }

    @Override
    public int getUnderlineThickness() {
        return bitmapFont.getUnderlineThickness();
    }
}
