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
import com.bulletphysics.linearmath.Transform;
import com.google.common.collect.Maps;
import org.terasology.asset.AssetType;
import org.terasology.asset.AssetUri;
import org.terasology.components.ItemComponent;
import org.terasology.dynamicBlocks.components.DynamicBlockComponent;
import org.terasology.dynamicBlocks.components.DynamicBlockItemComponent;
import org.terasology.dynamicBlocks.components.DynamicGroupComponent;
import org.terasology.dynamicBlocks.componentsystem.entityfactory.DynamicFactory;
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
import org.terasology.events.*;
import org.terasology.logic.LocalPlayer;
import org.terasology.physics.character.CharacterMovementComponent;
import org.terasology.physics.shapes.BoxShapeComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;

import javax.vecmath.*;
import java.util.Map;

/**
 * @author Pencilcheck <pennsu@gmail.com>
 */
@RegisterComponentSystem
public final class DynamicBlockSystem implements UpdateSubscriberSystem, EventHandlerSystem {
    @In
    private LocalPlayer localPlayer;

    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private BulletPhysics physics;

    private DynamicFactory dynamicFactory;

    private Map<Vector3i, EntityRef> dynamicEntities = Maps.newHashMap();

    private static final Logger logger = LoggerFactory.getLogger(DynamicBlockSystem.class);

    @Override
    public void initialise() {
        dynamicFactory = new DynamicFactory();
        dynamicFactory.setEntityManager(entityManager);
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = {DynamicBlockItemComponent.class, ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onPlaceDynamic(ActivateEvent event, EntityRef item) {
        LocationComponent targetLocation = event.getTarget().getComponent(LocationComponent.class);
        DynamicBlockItemComponent placeDynamicItem = item.getComponent(DynamicBlockItemComponent.class);

        logger.info("placing Dynamic blocks {}", placeDynamicItem.getDynamicType());

        Side surfaceDir = Side.inDirection(event.getHitNormal());
        Side secondaryDirection = TeraMath.getSecondaryPlacementDirection(event.getDirection(), event.getHitNormal());

        if (event.getTarget().hasComponent(BlockComponent.class)) {
            logger.info("placing on normal block");
            if (!placeBlock(placeDynamicItem.getDynamicType(), targetLocation, surfaceDir, secondaryDirection, EntityRef.NULL)) {
                event.cancel();
            }
        } else {
            logger.info("placing on dynamic block");
            EntityRef group = event.getTarget().getComponent(LocationComponent.class).getParent() == EntityRef.NULL ? event.getTarget() : event.getTarget().getComponent(LocationComponent.class).getParent();
            if (!placeBlock(placeDynamicItem.getDynamicType(), targetLocation, surfaceDir, secondaryDirection, group)) {
                event.cancel();
            }
        }
    }

    /**
     * Place a block of a given type at given location offset by surfaceDirection.
     *
     * @param type The type of the block
     * @return True if a block was placed
     */
    private boolean placeBlock(DynamicBlockItemComponent.DynamicType type, LocationComponent locationComponent, Side surfaceDirection, Side secondaryDirection, EntityRef parent) {
        if (type == null)
            return true;

        Vector3f offset = surfaceDirection.getVector3i().toVector3f();
        offset = QuaternionUtil.quatRotate(locationComponent.getWorldRotation(), offset, offset);

        Vector3f placementPos = new Vector3f(locationComponent.getWorldPosition());
        placementPos.add(offset);

        // TODO: position is skewed when the blocks are true rigidbodies
        // it seems like the positions are close to the ground block

        logger.info("target location {}", placementPos);

        if (canPlaceBlock(type, placementPos, parent)) {
            logger.info("creating entity");
            EntityRef placedEntity = dynamicFactory.generateDynamicBlock(placementPos, parent, type);
            if (parent != EntityRef.NULL) {
                LocationComponent parentLocation = parent.getComponent(LocationComponent.class);
                dynamicEntities.put(new Vector3i(parentLocation.getWorldPosition()), placedEntity);
            }
            else
                dynamicEntities.put(new Vector3i(placementPos), placedEntity);
            AudioManager.play(new AssetUri(AssetType.SOUND, "engine:PlaceBlock"), 0.5f);

            return true;
        }
        return false;
    }

    private boolean canPlaceBlock(DynamicBlockItemComponent.DynamicType type, Vector3f placePos, EntityRef parent) {
        Block centerBlock = worldProvider.getBlock((int)placePos.x, (int)placePos.y, (int)placePos.z);

        if (!centerBlock.isPenetrable()) {
            return false;
        }

        return true;
    }

    @ReceiveEvent(components = {DynamicBlockComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onDamaged(DamageEvent event, EntityRef entity) {
        /*
        Vector3f newPosition = new Vector3f(localPlayer.getViewDirection().x,
                0,
                localPlayer.getViewDirection().z
        );

        LocationComponent location = entity.getComponent(LocationComponent.class);
        EntityRef group = location.getParent();
        DynamicGroupComponent groupComponent = group.getComponent(DynamicGroupComponent.class);
        LocationComponent groupLocation = group.getComponent(LocationComponent.class);

        Vector3f delta = groupLocation.getWorldPosition();
        delta.add(newPosition);
        groupLocation.setWorldPosition(delta);
        */

        DynamicBlockComponent blockComponent = entity.getComponent(DynamicBlockComponent.class);
        if (blockComponent.isActivated())
            blockComponent.deactivate();
        else
            blockComponent.activate();

        //entity.destroy();
        // TODO: Don't play this if destroyed?
        // TODO: Configurable via block definition
        AudioManager.play(new AssetUri(AssetType.SOUND, "engine:Dig"), 1.0f);
    }

    public void update(float delta) {
        //for (EntityRef entity : entityManager.iteratorEntities(DynamicBlockComponent.class, LocationComponent.class)) {
        for (EntityRef group : dynamicEntities.values()) {

            if (group == null || group == EntityRef.NULL)
                continue;

            if (!group.hasComponent(LocationComponent.class))
                continue;

            DynamicGroupComponent groupComponent = group.getComponent(DynamicGroupComponent.class);
            LocationComponent groupLocation = group.getComponent(LocationComponent.class);
            Vector3f worldPos = groupLocation.getWorldPosition();

            // Skip this System if not in a loaded chunk
            if (!worldProvider.isBlockActive(worldPos))
                continue;

            if (!localPlayer.isValid())
                return;

            for (EntityRef member : groupComponent.getMembers()) {
                DynamicBlockComponent loco = member.getComponent(DynamicBlockComponent.class);

                if (loco.getDynamicType() == DynamicBlockComponent.DynamicType.Locomotive) {
                    if (loco.isActivated()) {
                        move(group, loco);
                    }
                    /*if (standingOn(member)) {
                        move(member, loco);
                    } else {
                        CharacterMovementComponent movement = member.getComponent(CharacterMovementComponent.class);
                        movement.setDrive(new Vector3f(0, 0, 0));
                        member.saveComponent(movement);
                    }*/
                }
            }
        }
    }

    public void move(EntityRef entity, DynamicBlockComponent loco) {
        /* old code Using character movement
        Vector3f movementDirection = localPlayer.getViewDirection();
        //Vector3f movementDirection = loco.direction.getVector3i().toVector3f();
        float speed = movementDirection.length();
        movementDirection = new Vector3f(movementDirection.x, 0, movementDirection.z);
        movementDirection.normalize();
        movementDirection.scale(speed);

        Vector3f desiredVelocity = new Vector3f(movementDirection);
        desiredVelocity.scale(loco.getMaximumSpeed());

        DynamicGroupMovementComponent movement = entity.getComponent(DynamicGroupMovementComponent.class);
        movement.setDrive(desiredVelocity);
        entity.saveComponent(movement);
        */

        // Manual movement
        LocationComponent location = entity.getComponent(LocationComponent.class);
        Vector3f position = location.getWorldPosition();
        Vector3f diff = loco.direction.getVector3i().toVector3f();
        diff.scale(0.1f);
        position.add(diff);
        location.setWorldPosition(position);

        if (standingOn(entity)) {
            // update player position
            LocationComponent player_location = localPlayer.getEntity().getComponent(LocationComponent.class);
            Vector3f playerPosition = player_location.getWorldPosition();
            playerPosition.add(diff);
            player_location.setWorldPosition(playerPosition);
            localPlayer.getEntity().saveComponent(player_location);
        }

        // Impulses
        //entity.send(new ImpulseEvent(new Vector3f(localPlayer.getViewDirection().x, localPlayer.getViewDirection().y, localPlayer.getViewDirection().z)));
        /*
        DynamicGroupComponent groupComponent = entity.getComponent(DynamicGroupComponent.class);
        for (EntityRef member : groupComponent.getMembers()) {
            LocationComponent location = member.getComponent(LocationComponent.class);
            member.send(new ImpulseEvent(new Vector3f(localPlayer.getViewDirection().x, 0, localPlayer.getViewDirection().z)));
            if (inWater(location)) {
                member.send(new ImpulseEvent(new Vector3f(0, 10f, 0)));
            }
        }
        */
    }

    public boolean inWater(LocationComponent location) {
        Vector3f worldPos = location.getWorldPosition();
        boolean topUnderwater = false;
        boolean bottomUnderwater = false;
        Vector3f top = new Vector3f(worldPos);
        Vector3f bottom = new Vector3f(worldPos);
        top.y += 0.25f * 1f;
        bottom.y -= 0.25f * 1f;

        topUnderwater = worldProvider.getBlock(top).isLiquid();
        bottomUnderwater = worldProvider.getBlock(bottom).isLiquid();
        return topUnderwater && bottomUnderwater;
    }

    @ReceiveEvent(components = {LocationComponent.class, CharacterMovementComponent.class})
    public void onMove(MovedEvent event, EntityRef entity) {
        logger.info("group {} moving {} to {}", entity, event.getDelta(), event.getPosition());
        /*
        if (standingOn(entity)) {
            // update player position
            LocationComponent player_location = localPlayer.getEntity().getComponent(LocationComponent.class);
            Vector3f location = player_location.getWorldPosition();
            location.add(event.getDelta());
            player_location.setWorldPosition(location);
            localPlayer.getEntity().saveComponent(player_location);
        }
        */
    }

    public boolean standingOn(EntityRef entity) {
        DynamicGroupComponent groupComponent = entity.getComponent(DynamicGroupComponent.class);
        for (EntityRef member : groupComponent.getMembers()) {
            BoxShapeComponent boxshape = localPlayer.getEntity().getComponent(BoxShapeComponent.class);
            // Only move when someone is standing on it
            HitResult hit = physics.rayTrace(localPlayer.getPosition(), new Vector3f(0, -1, 0), boxshape.extents.y);
            if (hit.isHit() && hit.getEntity() == member) return true;
        }
        return false;
    }
}
