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

import com.google.common.collect.Lists;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.EntityRef;
import org.terasology.world.block.family.BlockFamily;

import java.util.List;

/**
 * @author Pencilcheck <pennsu@gmail.com>
 */
public final class DynamicGroupComponent implements Component {
    private List<EntityRef> members = Lists.newArrayList();

    public void addMember(EntityRef member) {
        this.members.add(member);
    }

    public List<EntityRef> getMembers() {
        return members;
    }

    public void setMembers(List<EntityRef> members) {
        this.members = members;
    }
}
