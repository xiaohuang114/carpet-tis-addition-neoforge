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

package carpettisaddition.mixins.carpet.tweaks.rule.creativeNoClip;

import carpettisaddition.helpers.carpet.tweaks.rule.creativeNoClip.CreativeNoClipHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import carpet.CarpetSettings;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbEntityMixin
{
	@Shadow
	private Player followingPlayer;

	// S4 / B-class: 1.21.1 NeoForge no longer calls Level#getNearestPlayer here -- scanForEntities posts
	// XpOrbTargetingEvent (javap -c ExperienceOrb#scanForEntities: new XpOrbTargetingEvent at 26,
	// invokespecial at 34, EventBus.post at 37). The player search now happens inside that event
	// constructor, so the no-clip predicate flag brackets the constructor call instead.
	@Inject(
			method = "scanForEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/neoforged/neoforge/entity/XpOrbTargetingEvent;<init>(Lnet/minecraft/world/entity/ExperienceOrb;D)V"
			)
	)
	private void creativeNoClipEnhancement_doNotFindNoCreateClipPlayer_xpOrb_enter(CallbackInfo ci)
	{
		if (CarpetSettings.creativeNoClip)
		{
			CreativeNoClipHelper.exceptSpectatorPredicateIgnoreNoClipPlayers.set(true);
		}
	}

	// S4 / B-class: 1.21.1 NeoForge no longer calls Level#getNearestPlayer here -- scanForEntities posts
	// XpOrbTargetingEvent (javap -c ExperienceOrb#scanForEntities: new XpOrbTargetingEvent at 26,
	// invokespecial at 34, EventBus.post at 37). The player search now happens inside that event
	// constructor, so the no-clip predicate flag brackets the constructor call instead.
	@Inject(
			method = "scanForEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/neoforged/neoforge/entity/XpOrbTargetingEvent;<init>(Lnet/minecraft/world/entity/ExperienceOrb;D)V",
					shift = At.Shift.AFTER
			)
	)
	private void creativeNoClipEnhancement_doNotFindCreateNoClipPlayer_xpOrb_exit(CallbackInfo ci)
	{
		if (CarpetSettings.creativeNoClip)
		{
			CreativeNoClipHelper.exceptSpectatorPredicateIgnoreNoClipPlayers.set(false);
		}
	}

	@ModifyExpressionValue(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z",
					ordinal = 0
			)
	)
	private boolean creativeNoClipEnhancement_xpOrbForgetCreateNoClipPlayer(boolean isSpectator)
	{
		if (this.followingPlayer != null && CreativeNoClipHelper.isNoClipPlayer(this.followingPlayer))
		{
			isSpectator = true;
		}
		return isSpectator;
	}
}
