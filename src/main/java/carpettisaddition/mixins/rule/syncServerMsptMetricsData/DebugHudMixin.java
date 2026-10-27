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

package carpettisaddition.mixins.rule.syncServerMsptMetricsData;

import carpettisaddition.helpers.rule.syncServerMsptMetricsData.ServerMsptMetricsDataSyncer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.debugchart.LocalSampleLogger;

/**
 * mc1.14 ~ mc1.20.1: subproject 1.15.2 (main project)
 * mc1.20.2+        : subproject 1.20.2        <--------
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin
{
	@Shadow @Final private Font font;
	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private TpsDebugChart tpsChart;
	@Shadow @Final private LocalSampleLogger tickTimeLogger;

	@Inject(
			method = "lambda$render$2(Lnet/minecraft/client/gui/GuiGraphics;)V",  // lambda method in render()
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/debugchart/LocalSampleLogger;size()I"
			)
	)
	private void syncServerMsptMetricsData_drawIfPossible(
			GuiGraphics drawContext,
			CallbackInfo ci,
			@Local(ordinal = 0) int windowWidth,
			@Local(ordinal = 1) int centerX
	)
	{
		boolean shouldVanillaChartDraw =
				this.tickTimeLogger.size() > 0;
		if (!shouldVanillaChartDraw)
		{
			if (ServerMsptMetricsDataSyncer.getInstance().isServerSupportOk())
			{
				var data = ServerMsptMetricsDataSyncer.getInstance().getMetricsData();
				if (data.size() <= 0)
				{
					return;
				}

				var chart = new TpsDebugChart(
						this.font, data
						, ((TickChartAccessor)this.tpsChart).getMillisPerTickSupplier()
				);

				// vanilla copy
				int width = chart.getWidth(centerX);
				chart.drawChart(drawContext, windowWidth - width, width);
			}
		}
	}
}
