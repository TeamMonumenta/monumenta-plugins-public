package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class AttributeModifierCommand {
	private static final String COMMAND = "attributemodifier";
	private static final String PERMISSION = "monumenta.commands.attributemodifier";

	@SuppressWarnings("unchecked")
	public static void register() {
		EntitySelectorArgument.ManyEntities entitiesArg = new EntitySelectorArgument.ManyEntities("entities");
		MultiLiteralArgument attributeArg = new MultiLiteralArgument("attribute", Arrays.stream(Attribute.values()).map(a -> a.getKey().getKey()).toArray(String[]::new));
		Argument<String> modifierNameArg = new GreedyStringArgument("modifiername").replaceSuggestions(ArgumentSuggestions.strings(info -> {
			Collection<Entity> entities = info.previousArgs().getByArgument(entitiesArg);
			String attr = info.previousArgs().getByArgument(attributeArg);
			Attribute attribute = getAttribute(attr);
			if (attribute == null) {
				return new String[0];
			}
			Set<String> modifiers = new HashSet<>();
			for (Entity entity : entities) {
				if (entity instanceof Attributable attributable) {
					AttributeInstance instance = attributable.getAttribute(attribute);
					if (instance == null) {
						continue;
					}
					for (AttributeModifier mod : instance.getModifiers()) {
						modifiers.add(mod.getName());
					}
				}
			}
			return modifiers.toArray(new String[0]);
		}));

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("remove"),
				entitiesArg,
				attributeArg,
				modifierNameArg
			)
			.executes((sender, args) -> {
				Collection<Entity> entities = args.getByArgument(entitiesArg);
				String attr = args.getByArgument(attributeArg);
				Attribute attribute = getAttribute(attr);
				if (attribute == null) {
					return;
				}

				String modifierName = args.getByArgument(modifierNameArg);
				for (Entity entity : entities) {
					if (entity instanceof Attributable attributable) {
						EntityUtils.removeAttribute(attributable, attribute, modifierName);
					}
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("list"),
				entitiesArg
			)
			.executes((sender, args) -> {
				Collection<Entity> entities = args.getByArgument(entitiesArg);
				for (Entity entity : entities) {
					if (entity instanceof Attributable attributable) {
						for (Attribute attribute : Attribute.values()) {
							AttributeInstance instance = attributable.getAttribute(attribute);
							if (instance != null) {
								Collection<AttributeModifier> modifiers = instance.getModifiers();
								if (!modifiers.isEmpty()) {
									sender.sendMessage(Component.text(attribute.getKey().getKey() + ":"));
									for (AttributeModifier modifier : modifiers) {
										sender.sendMessage(Component.text(" - " + modifier.getName() + " (" + modifier.getAmount() + ")"));
									}
								}
							}
						}
					}
				}
			}).register();
	}

	private static @Nullable Attribute getAttribute(String attr) {
		for (Attribute attribute : Attribute.values()) {
			if (attribute.getKey().getKey().equals(attr)) {
				return attribute;
			}
		}
		return null;
	}
}
