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

package carpettisaddition.mixins.rule.renewableDragonHead;

import carpettisaddition.CarpetTISAdditionSettings;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonEntityMixin extends Mob implements Enemy
{

	@Unique
	private boolean flagDropHead$TISCM;

	public EnderDragonEntityMixin(EntityType<? extends EnderDragon> entityType, Level world)
	{
		super(entityType, world);
	}

	@Inject(
			method = "<init>",
			at = @At(value = "RETURN")
	)
	private void onConstructed(EntityType<? extends EnderDragon> entityType, Level world, CallbackInfo ci)
	{
		this.flagDropHead$TISCM = false;
	}

	@Inject(
			method = "tickDeath",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"
			)
	)
	private void dropHead(CallbackInfo ci)
	{
		if (this.flagDropHead$TISCM)
		{
			this.spawnAtLocation(new ItemStack(Items.DRAGON_HEAD));
		}
	}

	@Inject(
			method = "hurt(Lnet/minecraft/world/entity/boss/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;setHealth(F)V"
			)
	)
	private void onTakingDeathDamage(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) DamageSource damageSource)
	{
		if (CarpetTISAdditionSettings.renewableDragonHead)
		{

			Entity entity = damageSource.getEntity();
			if (entity instanceof Creeper)
			{
				Creeper creeperEntity = (Creeper)entity;
				if (creeperEntity.canDropMobsSkull())
				{
					creeperEntity.increaseDroppedSkulls();
					this.flagDropHead$TISCM = true;
				}
			}
		}
	}
}
