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
// https://github.com/MovingBlocks/Terasology/blob/023571e2360b0c8b7bfdd71e8281cf2c01a54472/engine/src/main/java/org/terasology/engine/rendering/assets/font/FontDataBuilder.java

package com.github.benjaminamos.nuiplugin.nui.bitmapfont;

import com.google.common.collect.Maps;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

public class FontDataBuilder {

    private int lineHeight;
    private int baseHeight;
    private Map<Integer, Image> pages = new HashMap<>();
    private Map<Integer, FontCharacter> characters = Maps.newHashMap();

    private int currentCharacterId;
    private int characterX;
    private int characterY;
    private int characterWidth;
    private int characterHeight;
    private int characterXOffset;
    private int characterYOffset;
    private int characterXAdvance;
    private int characterPage;

    public FontDataBuilder() {
    }

    public FontData build() {
        return new FontData(lineHeight, baseHeight, characters);
    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public void setBaseHeight(int baseHeight) {
        this.baseHeight = baseHeight;
    }

    public void addPage(int pageId, Image texture) {
        pages.put(pageId, texture);
    }

    public FontDataBuilder startCharacter(int characterId) {
        this.currentCharacterId = characterId;
        return this;
    }

    public FontDataBuilder setCharacterX(int value) {
        this.characterX = value;
        return this;
    }

    public FontDataBuilder setCharacterY(int value) {
        this.characterY = value;
        return this;
    }

    public FontDataBuilder setCharacterWidth(int value) {
        this.characterWidth = value;
        return this;
    }

    public FontDataBuilder setCharacterHeight(int value) {
        this.characterHeight = value;
        return this;
    }

    public FontDataBuilder setCharacterXOffset(int value) {
        this.characterXOffset = value;
        return this;
    }

    public FontDataBuilder setCharacterYOffset(int value) {
        this.characterYOffset = value;
        return this;
    }

    public FontDataBuilder setCharacterXAdvance(int value) {
        this.characterXAdvance = value;
        return this;
    }

    public FontDataBuilder setCharacterPage(int value) {
        this.characterPage = value;
        if (pages.get(value) == null) {
            throw new IllegalArgumentException("Invalid font - character on missing page '" + value + "'");
        }
        return this;
    }

    public FontDataBuilder endCharacter() {
        Image page = pages.get(characterPage);
        FontCharacter character = new FontCharacter(characterX, characterY,
                characterWidth, characterHeight, characterXOffset, characterYOffset, characterXAdvance, page);
        characters.put(currentCharacterId, character);
        return this;
    }

}
