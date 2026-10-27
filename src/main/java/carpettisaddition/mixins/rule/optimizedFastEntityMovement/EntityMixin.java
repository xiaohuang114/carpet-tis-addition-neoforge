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

package carpettisaddition.mixins.rule.optimizedFastEntityMovement;

import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.helpers.rule.optimizedFastEntityMovement.OFEMContext;
import carpettisaddition.helpers.rule.optimizedFastEntityMovement.OFEMUtil;
import carpettisaddition.utils.mixin.testers.LithiumEntityMovementOptimizationTester;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Collections;
import java.util.List;

@Restriction(conflict = @Condition(type = Condition.Type.TESTER, tester = LithiumEntityMovementOptimizationTester.class))
@Mixin(Entity.class)
public abstract class EntityMixin
{
	@Unique
	private static final ThreadLocal<OFEMContext> ofemContext = ThreadLocal.withInitial(() -> null);
	@Unique
	private static final ThreadLocal<Vec3> ofemCurrentMovement = ThreadLocal.withInitial(() -> null);

	@ModifyVariable(
			method = "collideBoundingBox",
			at = @At("HEAD"),
			argsOnly = true
	)
	private static Vec3 optimizedFastEntityMovement_checkMovement(Vec3 movement)
	{
		if (CarpetTISAdditionSettings.optimizedFastEntityMovement)
		{
			ofemCurrentMovement.set(movement);
		}
		return movement;
	}

	@WrapOperation(
			method = "collectColliders",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;getBlockCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/lang/Iterable;"
			)
	)
	private static
	Iterable<VoxelShape>
	optimizedFastEntityMovement_dontUseThatLargeBlockCollisions(
			Level world, Entity entity, AABB box,
			Operation<
					Iterable<VoxelShape>
			> original
	)
	{
		if (CarpetTISAdditionSettings.optimizedFastEntityMovement)
		{
			Vec3 movement = ofemCurrentMovement.get();
			boolean ok = movement != null && OFEMUtil.checkMovement(movement);
			if (ok)
			{
				ofemContext.set(OFEMUtil.createContext(world, entity, box, movement));
				return Collections.emptyList();
			}
		}
		ofemContext.remove();

		// vanilla
		return original.call(world, entity, box);
	}
	@ModifyExpressionValue(
			method = "collideWithShapes",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/List;isEmpty()Z",
					ordinal = 0
			)
	)
	private static boolean optimizedFastEntityMovement_theCollisionsListParameterIsIncompleteSoDontReturnEvenIfItIsEmpty(boolean isEmpty)
	{
		if (ofemContext.get() != null)
		{
			isEmpty = false;
		}
		return isEmpty;
	}

	@ModifyArgs(
			method = "collideWithShapes",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/phys/shapes/Shapes;collide(Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/AABB;Ljava/lang/Iterable;D)D"
			)
			, require = 4
	)
	private static void optimizedFastEntityMovement_useTheAxisOnlyBlockCollisionsForSpeed(Args args)
	{
		OFEMContext ctx = ofemContext.get();
		if (ctx != null)
		{
			// Direction.Axis axis, AABB box, (Iterable<VoxelShape> | Stream<VoxelShape>) shapes, double maxDist
			Direction.Axis axis = args.get(0);
			AABB entityBoundingBox = args.get(1);
			List<VoxelShape>
					entityAndBorderCollisions = args.get(2);
			double maxDist = args.get(3);

			ctx.axis = axis;
			ctx.movementOnAxis = maxDist;
			ctx.entityBoundingBox = entityBoundingBox;
			Iterable<VoxelShape>
					blockCollisions = OFEMUtil.getAxisOnlyBlockCollision(ctx);

			// order: (entity, border), block
			List<VoxelShape> voxelShapeList = Lists.newArrayList();
			voxelShapeList.addAll(entityAndBorderCollisions);
			blockCollisions.forEach(voxelShapeList::add);
			args.set(2, voxelShapeList);
		}
	}
}
