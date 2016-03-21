/*
 * This file is part of EchoPet.
 *
 * EchoPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EchoPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EchoPet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dsh105.echopet.compat.nms.v1_9_R1.entity.type;

import com.dsh105.echopet.compat.api.entity.EntityPetType;
import com.dsh105.echopet.compat.api.entity.EntitySize;
import com.dsh105.echopet.compat.api.entity.IPet;
import com.dsh105.echopet.compat.api.entity.PetType;
import com.dsh105.echopet.compat.api.entity.type.nms.IEntityVillagerPet;
import com.dsh105.echopet.compat.nms.v1_9_R1.NMS;
import com.dsh105.echopet.compat.nms.v1_9_R1.entity.EntityAgeablePet;
import com.dsh105.echopet.compat.nms.v1_9_R1.metadata.MetadataKey;
import com.dsh105.echopet.compat.nms.v1_9_R1.metadata.MetadataType;

import net.minecraft.server.v1_9_R1.DataWatcherObject;
import net.minecraft.server.v1_9_R1.World;

import org.bukkit.Sound;
import org.bukkit.entity.Villager;

@EntitySize(width = 0.6F, height = 1.8F)
@EntityPetType(petType = PetType.VILLAGER)
public class EntityVillagerPet extends EntityAgeablePet implements IEntityVillagerPet {

    public static final MetadataKey<Integer> VILLAGER_PROFESSION_METADATA = new MetadataKey<>(12, MetadataType.VAR_INT);

    public EntityVillagerPet(World world) {
        super(world);
    }

    public EntityVillagerPet(World world, IPet pet) {
        super(world, pet);
    }

    @Override
    public void setProfession(int i) {
        setProfession(Villager.Profession.getProfession(i));
    }

    public void setProfession(Villager.Profession profession) {
        getDatawatcher().set(VILLAGER_PROFESSION_METADATA, profession.getId());
    }

    @Override
    protected Sound getIdleSound() {
        return Sound.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected Sound getDeathSound() {
        return Sound.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public void initDatawatcher() {
        super.initDatawatcher();
        getDatawatcher().register(VILLAGER_PROFESSION_METADATA, Villager.Profession.FARMER.getId());
    }
}
