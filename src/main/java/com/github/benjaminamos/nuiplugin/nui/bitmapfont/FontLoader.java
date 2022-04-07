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
// https://github.com/MovingBlocks/Terasology/blob/023571e2360b0c8b7bfdd71e8281cf2c01a54472/engine/src/main/java/org/terasology/engine/rendering/assets/font/FontFormat.java

package com.github.benjaminamos.nuiplugin.nui.bitmapfont;

import com.google.common.base.Charsets;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ImageLoader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FontLoader {
    private static final String INTEGER_PATTERN = "((?:[\\+-]?\\d+)(?:[eE][\\+-]?\\d+)?)";

    // Common patterns
    private Pattern lineHeightPattern = Pattern.compile("lineHeight=" + INTEGER_PATTERN);
    private Pattern baseHeightPattern = Pattern.compile("base=" + INTEGER_PATTERN);
    private Pattern pagesPattern = Pattern.compile("pages=" + INTEGER_PATTERN);

    private Pattern pagePattern = Pattern.compile("page id=" + INTEGER_PATTERN + " file=\"(.*)\"");
    private Pattern charsPattern = Pattern.compile("chars count=" + INTEGER_PATTERN);
    private Pattern charPattern = Pattern.compile("char\\s+" +
            "id=" + INTEGER_PATTERN + "\\s+" +
            "x=" + INTEGER_PATTERN + "\\s+" +
            "y=" + INTEGER_PATTERN + "\\s+" +
            "width=" + INTEGER_PATTERN + "\\s+" +
            "height=" + INTEGER_PATTERN + "\\s+" +
            "xoffset=" + INTEGER_PATTERN + "\\s+" +
            "yoffset=" + INTEGER_PATTERN + "\\s+" +
            "xadvance=" + INTEGER_PATTERN + "\\s+" +
            "page=" + INTEGER_PATTERN + "\\s+" +
            "chnl=" + INTEGER_PATTERN + "\\s*");
    private VirtualFile basePath;

    public FontData load(VirtualFile basePath, InputStream inputStream) throws IOException {
        this.basePath = basePath;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8))) {
            FontDataBuilder builder = new FontDataBuilder();
            parseHeader(reader.readLine());
            int numPages = parseCommon(builder, reader.readLine());

            for (int i = 0; i < numPages; ++i) {
                parsePage(builder, reader.readLine());
            }

            int charCount = getCharacterCount(reader.readLine());
            for (int i = 0; i < charCount; ++i) {
                parseCharacter(builder, reader.readLine());
            }

            return builder.build();
        }
    }

    private void parseCharacter(FontDataBuilder builder, String charInfo) throws IOException {
        Matcher matcher = charPattern.matcher(charInfo);
        if (matcher.matches()) {
            try {
                builder.startCharacter(Integer.parseInt(matcher.group(1)))
                        .setCharacterX(Integer.parseInt(matcher.group(2)))
                        .setCharacterY(Integer.parseInt(matcher.group(3)))
                        .setCharacterWidth(Integer.parseInt(matcher.group(4)))
                        .setCharacterHeight(Integer.parseInt(matcher.group(5)))
                        .setCharacterXOffset(Integer.parseInt(matcher.group(6)))
                        .setCharacterYOffset(Integer.parseInt(matcher.group(7)))
                        .setCharacterXAdvance(Integer.parseInt(matcher.group(8)))
                        .setCharacterPage(Integer.parseInt(matcher.group(9)))
                        .endCharacter();
            } catch (IllegalArgumentException e) {
                throw new IOException("Failed to load font", e);
            }
        } else {
            throw new IOException("Failed to parse font - invalid char line '" + charInfo + "'");
        }
    }

    private int getCharacterCount(String charsInfo) throws IOException {
        Matcher charsMatcher = charsPattern.matcher(charsInfo);
        if (charsMatcher.matches()) {
            return Integer.parseInt(charsMatcher.group(1));
        } else {
            throw new IOException("Failed to load font - invalid chars line '" + charsInfo + "'");
        }
    }

    private void parsePage(FontDataBuilder builder, String pageInfo) throws IOException {
        Matcher pageMatcher = pagePattern.matcher(pageInfo);
        if (pageMatcher.matches()) {
            int pageId = Integer.parseInt(pageMatcher.group(1));
            String textureName = pageMatcher.group(2);
            builder.addPage(pageId, ImageLoader.loadFromStream(basePath.findChild(textureName).getInputStream()));
        } else {
            throw new IOException("Failed to load font - invalid page line '" + pageInfo + "'");
        }
    }


    private void parseHeader(String info) throws IOException {
        if (!info.startsWith("info ")) {
            throw new IOException("Invalid font - missing info line");
        }
        // We don't actually use anything from the info line
    }

    private int parseCommon(FontDataBuilder builder, String commonLine) throws IOException {
        if (!commonLine.startsWith("common ")) {
            throw new IOException("Failed to load font - missing common line");
        }
        builder.setLineHeight(findInteger(lineHeightPattern, commonLine));
        builder.setBaseHeight(findInteger(baseHeightPattern, commonLine));
        return findInteger(pagesPattern, commonLine);
    }

    private int findInteger(Pattern pattern, String in) {
        Matcher matcher = pattern.matcher(in);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
