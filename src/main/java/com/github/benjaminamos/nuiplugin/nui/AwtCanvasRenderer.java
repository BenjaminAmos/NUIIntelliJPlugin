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

import com.github.benjaminamos.nuiplugin.nui.bitmapfont.FontCharacter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.Border;
import org.terasology.nui.Color;
import org.terasology.nui.Colorc;
import org.terasology.nui.HorizontalAlign;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.TextLineBuilder;
import org.terasology.nui.UITextureRegion;
import org.terasology.nui.VerticalAlign;
import org.terasology.nui.asset.font.Font;
import org.terasology.nui.canvas.CanvasRenderer;
import org.terasology.nui.util.NUIMathUtil;
import org.terasology.nui.util.RectUtility;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.RGBImageFilter;
import java.util.List;

@SuppressWarnings("UseJBColor")
public class AwtCanvasRenderer implements CanvasRenderer {
    private static final java.awt.Color TRANSPARENT = new java.awt.Color(0, 0, 0, 0);
    private static final AwtFont FALLBACK_FONT = new AwtFont(UIUtil.getLabelFont());

    // NOTE: These constants were taken from Terasology's FontMeshBuilder class
    private static final int SHADOW_HORIZONTAL_OFFSET = 1;
    private static final int SHADOW_VERTICAL_OFFSET = 1;

    private Graphics graphics;
    private Vector2i size;
    private Table<Image, Colorc, Image> tintedTextCache = HashBasedTable.create();

    public AwtCanvasRenderer(Vector2i size) {
        this.size = size;
    }

    @Override
    public void preRender() {
        graphics.setClip(null);
    }

    @Override
    public void postRender() {
    }

    @Override
    public Vector2i getTargetSize() {
        return size;
    }

    @Override
    public void crop(Rectanglei cropRegion) {
        graphics.setClip(cropRegion.minX, cropRegion.minY, cropRegion.getSizeX(), cropRegion.getSizeY());
    }

    @Override
    public void drawLine(int sx, int sy, int ex, int ey, Colorc color) {
        graphics.setColor(nuiToAwtColour(color));
        graphics.drawLine(sx, sy, ex, ey);
    }

    private Image createTintedImage(Image sourceImage, Colorc colour, float alpha) {
        return ImageUtil.filter(sourceImage, new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int rgb) {
                int newAlpha = (int) (((rgb >> 24) & 0xFF) * colour.af() * alpha);
                int newRed = (int) (((rgb >> 16) & 0xFF) * colour.rf());
                int newGreen = (int) (((rgb >> 8) & 0xFF) * colour.gf());
                int newBlue = (int) ((rgb & 0xFF) * colour.bf());
                return (newAlpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
            }
        });
    }

    @Override
    public void drawTexture(UITextureRegion texture, Colorc color, ScaleMode mode, Rectanglei absoluteRegion,
                            float ux, float uy, float uw, float uh, float alpha) {
        if (!(texture instanceof AwtTextureRegion)) {
            throw new IllegalArgumentException("Textures must be of type AwtTextureRegion!");
        }

        AwtTextureRegion awtTexture = (AwtTextureRegion) texture;

        Image actualImage = awtTexture.getAwtImage();
        if (!color.equals(org.terasology.nui.Color.white) || alpha != 1.0f) {
            actualImage = createTintedImage(awtTexture.getAwtImage(), color, alpha);
        }

        Vector2f scale = mode.scaleForRegion(absoluteRegion, texture.getWidth(), texture.getHeight());

        if (mode != ScaleMode.TILED) {
            graphics.drawImage(actualImage,
                    absoluteRegion.minX,
                    absoluteRegion.minY,
                    absoluteRegion.minX + (int) scale.x,
                    absoluteRegion.minY + (int) scale.y,
                    (int) Math.ceil(ux * texture.getWidth()),
                    (int) Math.ceil(uy * texture.getWidth()),
                    (int) Math.ceil((ux + uw) * texture.getWidth()),
                    (int) Math.ceil((uy + uh) * texture.getHeight()),
                    TRANSPARENT, null);
        } else {
            int textureWidth = texture.getWidth();
            int textureHeight = texture.getHeight();
            for (int x = 0; x < absoluteRegion.getSizeX() / textureWidth; x++) {
                for (int y = 0; y < absoluteRegion.getSizeY() / textureHeight; y++) {
                    int startX = absoluteRegion.minX + (textureWidth * x);
                    int startY = absoluteRegion.minY + (textureHeight * y);
                    graphics.drawImage(actualImage,
                            startX,
                            startY,
                            startX + textureWidth,
                            startY + textureHeight,
                            (int) Math.ceil(ux * texture.getWidth()),
                            (int) Math.ceil(uy * texture.getWidth()),
                            (int) Math.ceil((ux + uw) * texture.getWidth()),
                            (int) Math.ceil((uy + uh) * texture.getHeight()),
                            TRANSPARENT, null);
                }
            }
        }
    }

    private void drawBitmapFontString(String line, AwtBitmapFont bitmapFont, int minX, int minY, Colorc colour) {
        if (colour.equals(Color.transparent)) {
            return;
        }

        int x = minX;
        for (int charNo = 0; charNo < line.length(); charNo++) {
            FontCharacter fontCharacter = bitmapFont.getCharacterData(line.charAt(charNo));
            Image page = fontCharacter.getPage();
            if (!colour.equals(Color.white)) {
                if (tintedTextCache.contains(page, colour)) {
                    page = tintedTextCache.get(page, colour);
                } else {
                    Image tintedPage = createTintedImage(page, colour, 1.0f);
                    tintedTextCache.put(page, colour, tintedPage);
                    page = tintedPage;
                }
            }
            int startX = x + fontCharacter.getxOffset();
            int startY = minY + fontCharacter.getyOffset();
            graphics.drawImage(page,
                    startX,
                    startY,
                    startX + fontCharacter.getWidth(),
                    startY + fontCharacter.getHeight(),
                    fontCharacter.getX(),
                    fontCharacter.getY(),
                    fontCharacter.getX() + fontCharacter.getWidth(),
                    fontCharacter.getY() + fontCharacter.getHeight(),
                    TRANSPARENT, null);
            x += fontCharacter.getxAdvance();
        }
    }

    @Override
    public void drawText(String text, Font font, HorizontalAlign hAlign, VerticalAlign vAlign, Rectanglei absoluteRegion,
                         Colorc color, Colorc shadowColor, float alpha, boolean underlined) {
        if (font == null) {
            font = FALLBACK_FONT;
        }

        if (font instanceof AwtFont) {
            graphics.setFont(((AwtFont)font).getAwtFont());
        }

        List<String> lines = TextLineBuilder.getLines(font, text, absoluteRegion.getSizeX());

        int minY = absoluteRegion.minY + vAlign.getOffset(lines.size() * font.getLineHeight(), absoluteRegion.getSizeY());
        for (int lineNo = 0; lineNo < lines.size(); lineNo++) {
            String line = lines.get(lineNo);
            int minX = absoluteRegion.minX + hAlign.getOffset(font.getWidth(line), absoluteRegion.getSizeX());
            if (font instanceof AwtFont) {
                graphics.drawString(line, minX, minY + (graphics.getFontMetrics().getHeight() * (lineNo + 1)));
            } else if (font instanceof AwtBitmapFont) {
                AwtBitmapFont bitmapFont = (AwtBitmapFont) font;

                // Draw shadow
                drawBitmapFontString(line, bitmapFont, minX + SHADOW_HORIZONTAL_OFFSET,
                        minY + (bitmapFont.getLineHeight() * lineNo) + SHADOW_VERTICAL_OFFSET, shadowColor);
                // Draw text
                drawBitmapFontString(line, bitmapFont, minX, minY + (bitmapFont.getLineHeight() * lineNo), color);
            }
        }
    }

    @Override
    public void drawTextureBordered(UITextureRegion texture, Rectanglei absoluteRegion, Border border, boolean tile,
                                    float ux, float uy, float uw, float uh, float alpha) {
        // See https://github.com/Terasology/TutorialNui/wiki/Skinning#background-options for border rendering information

        Vector2i textureSize = new Vector2i(NUIMathUtil.ceilToInt(texture.getWidth() * uw), NUIMathUtil.ceilToInt(texture.getHeight() * uh));

        float borderTextureLeft = (float) border.getLeft() / texture.getWidth();
        float borderTextureRight = (float) border.getRight() / texture.getWidth();
        float borderTextureTop = (float) border.getTop() / texture.getHeight();
        float borderTextureBottom = (float) border.getBottom() / texture.getHeight();

        int borderlessAbsoluteWidth = absoluteRegion.getSizeX() - (border.getLeft() + border.getRight());
        float borderlessTextureWidth = uw - (float) (border.getLeft() + border.getRight()) / texture.getWidth();
        int borderlessAbsoluteHeight = absoluteRegion.getSizeY() - (border.getTop() + border.getBottom());
        float borderlessTextureHeight = uh - (float) (border.getTop() + border.getBottom()) / texture.getHeight();

        // Draw texture without borders
        drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH, absoluteRegion,
                ux + borderTextureLeft, uy + borderTextureTop,
                borderlessTextureWidth,
                borderlessTextureHeight, alpha);

        // Draw borders around texture

        // Left border
        drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                RectUtility.createFromMinAndSize(absoluteRegion.minX, absoluteRegion.minY + border.getTop(), border.getLeft(),
                        borderlessAbsoluteHeight),
                ux, uy + borderTextureBottom, borderTextureLeft, borderlessTextureHeight, alpha);

        // Right border
        drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                RectUtility.createFromMinAndSize(absoluteRegion.maxX - border.getRight(), absoluteRegion.minY + border.getTop(),
                        border.getRight(), borderlessAbsoluteHeight),
                ux + uw - borderTextureRight, uy + borderTextureTop,
                borderTextureRight,
                borderlessTextureHeight, alpha);

        // Top border
        drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                RectUtility.createFromMinAndSize(absoluteRegion.minX + border.getLeft(), absoluteRegion.minY,
                        borderlessAbsoluteWidth, border.getTop()),
                ux + borderTextureLeft, uy,
                borderlessTextureWidth,
                borderTextureTop, alpha);

        // Bottom border
        drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                RectUtility.createFromMinAndSize(absoluteRegion.minX + border.getLeft(), absoluteRegion.maxY - border.getBottom(),
                        borderlessAbsoluteWidth, border.getBottom()),
                ux + borderTextureLeft, uy + uh - borderTextureBottom,
                borderlessTextureWidth,
                borderTextureBottom, alpha);

        // Draw corners over texture (if needed)

        // Top Left corner
        if (border.getLeft() != 0 && border.getTop() != 0) {
            drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                    RectUtility.createFromMinAndSize(absoluteRegion.minX, absoluteRegion.minY,
                            border.getLeft(), border.getTop()),
                    ux, uy,
                    borderTextureLeft,
                    borderTextureTop, alpha);
        }

        // Top Right corner
        if (border.getRight() != 0 && border.getTop() != 0) {
            drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                    RectUtility.createFromMinAndSize(absoluteRegion.maxX - border.getRight(), absoluteRegion.minY,
                            border.getRight(), border.getTop()),
                    ux + uw - borderTextureRight, uy,
                    borderTextureRight,
                    borderTextureTop, alpha);
        }

        // Bottom Left corner
        if (border.getLeft() != 0 && border.getBottom() != 0) {
            drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                    RectUtility.createFromMinAndSize(absoluteRegion.minX, absoluteRegion.maxY - border.getBottom(),
                            border.getLeft(), border.getBottom()),
                    ux, uy + uh - borderTextureBottom,
                    borderTextureLeft,
                    borderTextureBottom, alpha);
        }

        // Bottom Right corner
        if (border.getRight() != 0 && border.getBottom() != 0) {
            drawTexture(texture, Color.white, tile ? ScaleMode.TILED : ScaleMode.STRETCH,
                    RectUtility.createFromMinAndSize(absoluteRegion.maxX - border.getRight(), absoluteRegion.minY,
                            border.getRight(), border.getBottom()),
                    ux + uw - borderTextureRight, uy + uh - borderTextureBottom,
                    borderTextureRight,
                    borderTextureBottom, alpha);
        }
    }

    @Override
    public void setUiScale(float uiScale) {
        // TODO
        throw new UnsupportedOperationException("UI Scaling is not implemented yet.");
    }

    public void setSize(Vector2i size) {
        this.size = size;
    }

    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    private static java.awt.Color nuiToAwtColour(Colorc nuiColour) {
        return new java.awt.Color(nuiColour.r(), nuiColour.g(), nuiColour.b(), nuiColour.a());
    }

    private static java.awt.Color nuiToAwtColour(Colorc nuiColour, float alpha) {
        return new java.awt.Color(nuiColour.rf(), nuiColour.gf(), nuiColour.bf(), nuiColour.af() * alpha);
    }
}
