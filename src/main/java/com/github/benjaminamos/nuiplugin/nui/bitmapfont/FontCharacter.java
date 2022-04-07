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
// https://github.com/MovingBlocks/Terasology/blob/023571e2360b0c8b7bfdd71e8281cf2c01a54472/engine/src/main/java/org/terasology/engine/rendering/assets/font/FontCharacter.java

package com.github.benjaminamos.nuiplugin.nui.bitmapfont;

import java.awt.Image;

public class FontCharacter {
    private int x;
    private int y;
    private int width;
    private int height;
    private int xOffset;
    private int yOffset;
    private int xAdvance;
    private Image page;

    public FontCharacter(int x, int y, int width, int height, int xOffset, int yOffset, int xAdvance, Image page) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.xAdvance = xAdvance;
        this.page = page;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getTexWidth() {
        return ((float) width) / page.getWidth(null);
    }

    public float getTexHeight() {
        return ((float) height) / page.getHeight(null);
    }

    public int getxOffset() {
        return xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public int getxAdvance() {
        return xAdvance;
    }

    public Image getPage() {
        return page;
    }
}