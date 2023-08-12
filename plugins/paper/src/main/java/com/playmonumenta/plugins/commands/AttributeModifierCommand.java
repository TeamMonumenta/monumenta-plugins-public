package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.Collection;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;

public class AttributeModifierCommand {
	private static final String COMMAND = "attributemodifier";
	private static final String PERMISSION = "monumenta.commands.attributemodifier";

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("remove"),
				new EntitySelectorArgument.ManyEntities("entities"),
				new MultiLiteralArgument(Arrays.stream(Attribute.values()).map(a -> a.getKey().getKey()).toArray(String[]::new)),
				new GreedyStringArgument("modifiername")
			)
			.executes((sender, args) -> {
				Collection<Entity> entities = (Collection<Entity>) args[0];
				String attr = (String) args[1];
				String modifierName = (String) args[2];
				for (Attribute attribute : Attribute.values()) {
					if (attribute.getKey().getKey().equals(attr)) {
						for (Entity entity : entities) {
							if (entity instanceof Attributable attributable) {
								EntityUtils.removeAttribute(attributable, attribute, modifierName);
							}
						}
						break;
					}
				}
			})
			.register();
	}
}
