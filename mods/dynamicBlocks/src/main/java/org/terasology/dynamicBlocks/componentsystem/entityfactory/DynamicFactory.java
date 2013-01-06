package org.terasology.dynamicBlocks.componentsystem.entityfactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.components.world.LocationComponent;
import org.terasology.dynamicBlocks.components.DynamicBlockItemComponent;
import org.terasology.dynamicBlocks.components.DynamicGroupComponent;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.dynamicBlocks.components.DynamicBlockComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created with IntelliJ IDEA.
 * User: Pencilcheck
 * Date: 12/22/12
 * Time: 6:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicFactory {

    private static final Logger logger = LoggerFactory.getLogger(DynamicFactory.class);

    private EntityManager entityManager;

    public EntityRef generateDynamicBlock(Vector3f position, Quat4f rotation, EntityRef group, DynamicBlockItemComponent.DynamicType type) {
        EntityRef entity = null;

        switch (type) {
            case Basic: {
                entity = entityManager.create("dynamicBlocks:basic", position);
                break;
            }
            case Locomotive: {
                entity = entityManager.create("dynamicBlocks:locomotive", position);
                break;
            }
            default:
                entity = entityManager.create("dynamicBlocks:basic", position);
        }

        if (entity == null)
            return null;

        logger.info("generate {}", type);

        EntityRef new_group = null;
        if (group == EntityRef.NULL) {
            logger.info("creating a group", type);
            new_group = entityManager.create("dynamicBlocks:group", position);

            if (new_group == null)
                return null;
        } else {
            new_group = group;
        }

        DynamicGroupComponent groupComponent = new_group.getComponent(DynamicGroupComponent.class);
        groupComponent.addMember(entity);
        new_group.saveComponent(groupComponent);



        LocationComponent groupLocation = new_group.getComponent(LocationComponent.class);

        // Calculate relative position from group position
        LocationComponent loc = entity.getComponent(LocationComponent.class);
        Vector3f current_loc = position;
        current_loc.sub(groupLocation.getWorldPosition());
        loc.setWorldPosition(current_loc);
        if (rotation != null)
            loc.setWorldRotation(rotation);
        entity.saveComponent(loc);

        // Before entityManager.create takes a parent field, we have to manually add it after setting the location
        groupLocation.addChild(entity, new_group);
        new_group.saveComponent(groupLocation);

        return new_group;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
