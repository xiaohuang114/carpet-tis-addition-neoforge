/*
 * This file is part of the Carpet TIS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2023  Fallen_Breath and contributors
 *
 * Carpet TIS Addition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet TIS Addition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet TIS Addition.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpettisaddition.helpers.rule.dispensersFireDragonBreath;

import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;

public class FireDragonBreathDispenserBehaviour extends DefaultDispenseItemBehavior
{
	public static final FireDragonBreathDispenserBehaviour INSTANCE = new FireDragonBreathDispenserBehaviour();

	@Override
	protected ItemStack execute(BlockSource source, ItemStack stack)
	{
		Direction sourceFace = source.state().getValue(DispenserBlock.FACING);
		Level world = source.level();
		BlockPos blockpos = source.pos().relative(sourceFace);

		// Vanilla copy of DragonFireballEntity#onHit
		AreaEffectCloud areaEffectCloudEntity = new AreaEffectCloud(world, blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
		areaEffectCloudEntity.setOwner(null);  // it doesn't have an entity owner
		areaEffectCloudEntity.setParticle(
				ParticleTypes.DRAGON_BREATH
		);
		areaEffectCloudEntity.setRadius(3.0F);
		areaEffectCloudEntity.setDuration(600);
		areaEffectCloudEntity.setRadiusPerTick((7.0F - areaEffectCloudEntity.getRadius()) / (float)areaEffectCloudEntity.getDuration());
		MobEffectInstance statusEffectInstance = new MobEffectInstance(MobEffects.HARM, 1, 1);
		areaEffectCloudEntity.addEffect(statusEffectInstance);
		// use event 2007 from ThrownPotionEntity.java#onHit instead of event 2006 from DragonFireballEntity#onHit
		world.levelEvent(
				2007, blockpos,
				PotionContents.getColor(Potions.HARMING)
		);
		world.addFreshEntity(areaEffectCloudEntity);
		// Vanilla copy ends

		stack.shrink(1);
		return stack;
	}
}
