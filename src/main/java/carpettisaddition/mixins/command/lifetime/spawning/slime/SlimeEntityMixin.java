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

package carpettisaddition.mixins.command.lifetime.spawning.slime;

import carpettisaddition.commands.lifetime.interfaces.LifetimeTrackerTarget;
import carpettisaddition.commands.lifetime.spawning.LiteralSpawningReason;
import carpettisaddition.utils.ModIds;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * For mc > 1.21.1, see {@link carpettisaddition.mixins.command.lifetime.spawning.conversion.MobEntityMixin}
 */
@Restriction(require = @Condition(value = ModIds.minecraft, versionPredicates = "<=1.21.1"))
@Mixin(Slime.class)
public abstract class SlimeEntityMixin
{
	// S4 / B-class: 1.21.1 Slime#remove no longer calls Level#addFreshEntity directly -- it collects the
	// babies into an ArrayList and then does list.forEach(level::addFreshEntity) via invokedynamic, so the
	// old INVOKE Level.addFreshEntity(Entity) anchor matches 0 targets. javap -c Slime#remove: every baby
	// comes from "invokestatic EntityType.create:(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"
	// (offset 129), then checkcast Slime -> slot 13 -> ArrayList.add.
	// ArrayList.add takes Ljava/lang/Object;, and @ModifyArg refuses to bind that slot to an Entity handler
	// ("Could not find arg matching type Lnet/minecraft/world/entity/Entity;"), so the per-baby hook is taken
	// on the create() expression value instead: same entity, same spawn point, handler body untouched.
	@ModifyExpressionValue(
			method = "remove",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"
			)
	)
	private Entity lifetimeTracker_recordSpawning_slimDivision(Entity slimeEntity)
	{
		((LifetimeTrackerTarget)slimeEntity).recordSpawning$TISCM(LiteralSpawningReason.SLIME);
		return slimeEntity;
	}
}
