package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class EffectListCommand {
	private static final String PERMISSION_OTHER = "monumenta.command.effectlistother";

	public static void register() {
		new CommandAPICommand("effectlist")
			.executesPlayer((player, args) -> {
				sendEffectList(player, player, false);
			})
			.register();

		EntitySelectorArgument.OneEntity entityArg = new EntitySelectorArgument.OneEntity("entity");
		new CommandAPICommand("effectlist")
			.withPermission(PERMISSION_OTHER)
			.withArguments(entityArg)
			.executes((sender, args) -> {
				sendEffectList(args.getByArgument(entityArg), sender, true);
			})
			.register();
	}

	public static void sendEffectList(Entity entity, CommandSender sender, boolean debugMode) {
		List<Component> list = getEffectList(entity, debugMode);
		if (list.isEmpty()) {
			sender.sendMessage(Component.text("No effects to display!", NamedTextColor.RED));
		}
		for (Component component : list) {
			sender.sendMessage(component);
		}
	}

	public static List<Component> getEffectList(Entity entity, boolean debugMode) {
		Map<String, Effect> priorityEffects = Plugin.getInstance().mEffectManager.getPriorityEffects(entity);
		Map<DisplayableEffect, Component> componentMap = new HashMap<>();
		priorityEffects.forEach((source, effect) -> {
			Component component = createComponent(source, effect, debugMode);
			if (component != null) {
				componentMap.put(effect, component);
			}
		});
		List<Component> sortedComponents = new ArrayList<>();
		for (DisplayableEffect effect : DisplayableEffect.sortEffects(new ArrayList<>(componentMap.keySet()))) {
			sortedComponents.add(componentMap.get(effect));
		}
		return sortedComponents;
	}

	public static @Nullable Component createComponent(String source, Effect effect, boolean debugMode) {
		Component display = effect.getDisplay();
		if (display == null) {
			if (debugMode) {
				display = Component.text(effect.toString());
			} else {
				return null;
			}
		}
		Component hover = Component.text("Source: " + source).append(Component.newline())
			.append(Component.text("ID: " + effect.mEffectID)).append(Component.newline())
			.append(Component.text("Duration: " + effect.getDuration())).append(Component.newline())
			.append(Component.text("Magnitude: " + StringUtils.to2DP(effect.getMagnitude())));
		return display.hoverEvent(hover);
	}
}
