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
package org.terasology.dynamicBlocks.componentsystem.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Lists;
import org.terasology.asset.AssetType;
import org.terasology.asset.AssetUri;
import org.terasology.components.ItemComponent;
import org.terasology.dynamicBlocks.components.DynamicBlockComponent;
import org.terasology.dynamicBlocks.componentsystem.entityfactory.DynamicFactory;
import org.terasology.game.CoreRegistry;
import org.terasology.logic.manager.AudioManager;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.physics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.componentSystem.UpdateSubscriberSystem;
import org.terasology.components.world.LocationComponent;
import org.terasology.entitySystem.*;
import org.terasology.entitySystem.event.RemovedComponentEvent;
import org.terasology.events.*;
import org.terasology.logic.LocalPlayer;
import org.terasology.physics.character.CharacterMovementComponent;
import org.terasology.physics.shapes.BoxShapeComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockItemComponent;

import javax.vecmath.*;

/**
 * @author Pencilcheck <pennsu@gmail.com>
 */
@RegisterComponentSystem
public final class DynamicBlocksSystem implements UpdateSubscriberSystem, EventHandlerSystem {
    @In
    private LocalPlayer localPlayer;

    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private BulletPhysics physics;

    DynamicFactory dynamicFactory;

    private static final Logger logger = LoggerFactory.getLogger(DynamicBlocksSystem.class);

    @Override
    public void initialise() {
        dynamicFactory = new DynamicFactory();
        dynamicFactory.setEntityManager(entityManager);
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = {DynamicBlockComponent.class, ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {
        DynamicBlockComponent placeDynamicItem = item.getComponent(DynamicBlockComponent.class);
        LocationComponent selectedLocation = event.getTarget().getComponent(LocationComponent.class);


        // TODO: Check whether it is possible to place it (e.g. boat cannot be placed on land)

        Side surfaceDir = Side.inDirection(event.getHitNormal());
        Side secondaryDirection = TeraMath.getSecondaryPlacementDirection(event.getDirection(), event.getHitNormal());

        if (event.getTarget().hasComponent(BlockComponent.class)) {
            BlockComponent selectedItem = event.getTarget().getComponent(BlockComponent.class);

            if (!placeBlock(placeDynamicItem.getDynamicType(), selectedItem, surfaceDir, secondaryDirection)) {
                event.cancel();
            }
        } else if (event.getTarget().hasComponent(DynamicBlockComponent.class)) {
            DynamicBlockComponent selectedDynamicItem = event.getTarget().getComponent(DynamicBlockComponent.class);

            if (!stackBlock(placeDynamicItem.getDynamicType(), selectedDynamicItem.getDynamicType(), new Vector3i(selectedLocation.getWorldPosition()), selectedLocation.getLocalRotation(), surfaceDir, secondaryDirection)) {
                event.cancel();
            }
        }


        //if (worldProvider.getBlock(newPosition).isPenetrable()) {
            //functionalEntity.send(new ImpulseEvent(new Vector3f(localPlayer.getViewDirection().x, localPlayer.getViewDirection().y, localPlayer.getViewDirection().z)));

        //}
    }

    /**
     * Places a block of a given type in front of the player, not stacking to any blocks.
     *
     * @param type The type of the block
     * @return True if a block was placed
     */
    private boolean placeBlock(DynamicBlockComponent.DynamicType type, BlockComponent selectedBlock, Side surfaceDirection, Side secondaryDirection) {
        if (type == null)
            return true;

        /*
        Vector3f newPosition = new Vector3f(localPlayer.getPosition().x + localPlayer.getViewDirection().x*1.5f,
                localPlayer.getPosition().y + localPlayer.getViewDirection().y*1.5f,
                localPlayer.getPosition().z + localPlayer.getViewDirection().z*1.5f
        );
        */

        Vector3i placementPos = new Vector3i(selectedBlock.getPosition());
        placementPos.add(surfaceDirection.getVector3i());

        if (canPlaceBlock(placementPos, selectedBlock)) {
            EntityRef entity = dynamicFactory.generateDynamicBlock(placementPos.toVector3f(), null, type);
            AudioManager.play(new AssetUri(AssetType.SOUND, "engine:PlaceBlock"), 0.5f);

            return true;
        }
        return false;
    }

    private boolean canPlaceBlock(Vector3i placePos, BlockComponent selectedBlock) {
        Block centerBlock = worldProvider.getBlock(placePos.x, placePos.y, placePos.z);

        if (!centerBlock.isPenetrable()) {
            return false;
        }

        return true;
    }

    /**
     * Places a block of a given type stacking with other dynamic blocks.
     *
     * @param type The type of the block
     * @return True if a block was placed
     */
    private boolean stackBlock(DynamicBlockComponent.DynamicType type, DynamicBlockComponent.DynamicType selectedDynamicType, Vector3i selectedPosition, Quat4f rot, Side surfaceDirection, Side secondaryDirection) {
        if (type == null)
            return true;

        // Rotate local coordinates
        //Vector3i offset = new Vector3i(QuaternionUtil.quatRotate(rot, surfaceDirection.getVector3i().toVector3f(), surfaceDirection.getVector3i().toVector3f()));
        Vector3i offset = surfaceDirection.getVector3i();
        Vector3i placementPos = new Vector3i(selectedPosition);
        placementPos.add(offset);

        EntityRef entity = dynamicFactory.generateDynamicBlock(placementPos.toVector3f(), rot, selectedDynamicType);
        AudioManager.play(new AssetUri(AssetType.SOUND, "engine:PlaceBlock"), 0.5f);

        return true;
    }

    public void update(float delta) {
        for (EntityRef entity : entityManager.iteratorEntities(DynamicBlockComponent.class, LocationComponent.class)) {
            DynamicBlockComponent loco = entity.getComponent(DynamicBlockComponent.class);
            LocationComponent location = entity.getComponent(LocationComponent.class);
            Vector3f worldPos = location.getWorldPosition();

            // Skip this System if not in a loaded chunk
            if (!worldProvider.isBlockActive(worldPos))
                continue;

            if (!localPlayer.isValid())
                return;

            if (entity.hasComponent(CharacterMovementComponent.class)) {
                if (standingOn(entity)) {
                    Vector3f movementDirection = localPlayer.getViewDirection();
                    float speed = movementDirection.length();
                    movementDirection = new Vector3f(movementDirection.x, 0, movementDirection.z);
                    movementDirection.normalize();
                    movementDirection.scale(speed);

                    Vector3f desiredVelocity = new Vector3f(movementDirection);
                    desiredVelocity.scale(loco.getMaximumSpeed());

                    CharacterMovementComponent movement = entity.getComponent(CharacterMovementComponent.class);
                    movement.setDrive(desiredVelocity);
                    entity.saveComponent(movement);
                } else {
                    CharacterMovementComponent movement = entity.getComponent(CharacterMovementComponent.class);
                    movement.setDrive(new Vector3f(0, 0, 0));
                    entity.saveComponent(movement);

                }
            }
        }
    }

    @ReceiveEvent(components = {LocationComponent.class, DynamicBlockComponent.class})
    public void onMove(MovedEvent event, EntityRef entity) {
        if (standingOn(entity)) {
            // update player position
            LocationComponent player_location = localPlayer.getEntity().getComponent(LocationComponent.class);
            Vector3f location = player_location.getWorldPosition();
            location.add(event.getDelta());
            player_location.setWorldPosition(location);
            localPlayer.getEntity().saveComponent(player_location);
        }
    }

    public boolean standingOn(EntityRef entity) {
        BoxShapeComponent boxshape = localPlayer.getEntity().getComponent(BoxShapeComponent.class);
        // Only move when someone is standing on it
        HitResult hit = physics.rayTrace(localPlayer.getPosition(), new Vector3f(0, -1, 0), boxshape.extents.y);
        return hit.isHit() && hit.getEntity() == entity;
    }
}
