package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class EffectListCommand {
	public static void register() {
		new CommandAPICommand("effectlist")
			.executesPlayer((player, args) -> {
				Map<String, Effect> priorityEffects = Plugin.getInstance().mEffectManager.getPriorityEffects(player);
				Map<DisplayableEffect, Component> componentMap = new HashMap<>();
				priorityEffects.forEach((source, effect) -> {
					Component component = createComponent(source, effect);
					if (component != null) {
						componentMap.put(effect, component);
					}
				});
				for (DisplayableEffect effect : DisplayableEffect.sortEffects(new ArrayList<>(componentMap.keySet()))) {
					player.sendMessage(componentMap.get(effect));
				}
			})
			.register();
	}

	public static @Nullable Component createComponent(String source, Effect effect) {
		Component display = effect.getDisplay();
		if (display == null) {
			return null;
		}
		Component hover = Component.text("Source: " + source).append(Component.newline())
			.append(Component.text("ID: " + effect.mEffectID)).append(Component.newline())
			.append(Component.text("Duration: " + effect.getDuration())).append(Component.newline())
			.append(Component.text("Magnitude: " + StringUtils.to2DP(effect.getMagnitude())));
		return display.hoverEvent(hover);
	}
}
