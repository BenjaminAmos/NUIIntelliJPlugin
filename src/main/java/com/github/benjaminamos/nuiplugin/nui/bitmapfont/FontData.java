// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
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

// Adapted from Terasology:
// https://github.com/MovingBlocks/Terasology/blob/023571e2360b0c8b7bfdd71e8281cf2c01a54472/engine/src/main/java/org/terasology/engine/rendering/assets/font/FontData.java

package com.github.benjaminamos.nuiplugin.nui.bitmapfont;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class FontData {
    private int lineHeight;
    private int baseHeight;
    private int underlineOffset = 2;
    private int underlineThickness = 1;
    private Map<Integer, FontCharacter> characters;

    public FontData(int lineHeight, int baseHeight, Map<Integer, FontCharacter> characters) {
        this.lineHeight = lineHeight;
        this.baseHeight = baseHeight;
        this.characters = ImmutableMap.copyOf(characters);
    }

    public FontData(FontData other) {
        this.lineHeight = other.lineHeight;
        this.baseHeight = other.baseHeight;
        this.underlineOffset = other.underlineOffset;
        this.underlineThickness = other.underlineThickness;
        this.characters = other.characters;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public int getBaseHeight() {
        return baseHeight;
    }

    public Iterable<Map.Entry<Integer, FontCharacter>> getCharacters() {
        return characters.entrySet();
    }

    public FontCharacter getCharacter(int index) {
        return characters.get(index);
    }

    public int getUnderlineOffset() {
        return underlineOffset;
    }

    public int getUnderlineThickness() {
        return underlineThickness;
    }

    public void setUnderlineOffset(int underlineOffset) {
        this.underlineOffset = underlineOffset;
    }

    public void setUnderlineThickness(int underlineThickness) {
        this.underlineThickness = underlineThickness;
    }
}