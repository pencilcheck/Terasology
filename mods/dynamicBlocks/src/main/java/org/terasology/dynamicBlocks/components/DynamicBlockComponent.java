/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
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
package org.terasology.dynamicBlocks.components;

import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.math.Side;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.world.block.family.BlockFamily;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * @author Pencilcheck <pennsu@gmail.com>
 */
public final class DynamicBlockComponent implements Component {
    // TODO: Extract out into one class like BlockFamily
    public enum DynamicType {
        Locomotive,
        Basic
    }

    public boolean activated = false;

    public DynamicType dynamicType = DynamicType.Basic;

    public Side direction = Side.FRONT;

    public float maximumSpeed = 1f;

    public BlockFamily[] allowedIn = {};
    public BlockFamily[] allowedOnTopOf = {};

    public DynamicType getDynamicType() {
        return dynamicType;
    }

    public void setDynamicType(DynamicType new_type) {
        dynamicType = new_type;
    }

    public float getMaximumSpeed() {
        return maximumSpeed;
    }

    public void setMaximumSpeed(float new_maximumSpeed) {
        maximumSpeed = new_maximumSpeed;
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void activate() {
        this.activated = true;
    }

    public void deactivate() {
        this.activated = false;
    }
}
