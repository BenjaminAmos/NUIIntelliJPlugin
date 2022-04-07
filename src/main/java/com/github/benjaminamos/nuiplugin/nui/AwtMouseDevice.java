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

import com.google.common.collect.Queues;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.terasology.input.device.MouseAction;
import org.terasology.input.device.MouseDevice;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Queue;

public class AwtMouseDevice implements MouseDevice, MouseMotionListener {
    private Vector2i position = new Vector2i();
    private Vector2d delta = new Vector2d();

    public AwtMouseDevice(Component component) {
        component.addMouseMotionListener(this);
    }

    /**
     * @return A queue of all input actions that have occurred over the last update for this device
     */
    @Override
    public Queue<MouseAction> getInputQueue() {
        return Queues.newArrayDeque();
    }

    /**
     * @return The current position of the first mouse pointer in screen space
     */
    @Override
    public Vector2i getPosition() {
        return position;
    }

    /**
     * @return The change in mouse position over the last update
     */
    @Override
    public Vector2d getDelta() {
        return delta;
    }

    /**
     * @param button
     * @return The current state of the given button
     */
    @Override
    public boolean isButtonDown(int button) {
        return false;
    }

    @Override
    public void update() {
    }

    /**
     * @return Whether the mouse cursor is visible
     */
    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setGrabbed(boolean grabbed) {
        throw new UnsupportedOperationException("It would not be wise to grab the user's mouse cursor in an IDE.");
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        delta.set(mouseEvent.getX() - position.x, mouseEvent.getY() - position.y);
        position.set(mouseEvent.getX(), mouseEvent.getY());
    }
}
