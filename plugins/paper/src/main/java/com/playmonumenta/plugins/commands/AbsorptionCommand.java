package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.Collection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class AbsorptionCommand {
	public static void register() {
		new CommandAPICommand("absorption")
			.withPermission("monumenta.command.absorption")
			.withArguments(
				new LiteralArgument("flat"),
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new DoubleArgument("absorption"),
				new DoubleArgument("max"),
				new IntegerArgument("ticks"))
			.executes((sender, args) -> {
				for (Entity e : (Collection<Entity>) args[0]) {
					if (e instanceof LivingEntity le) {
						AbsorptionUtils.addAbsorption(le, (double) args[1], (double) args[2], (int) args[3]);
					}
				}
			}).register();

		new CommandAPICommand("absorption")
			.withPermission("monumenta.command.absorption")
			.withArguments(
				new LiteralArgument("percent"),
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new DoubleArgument("absorptionpercent"),
				new DoubleArgument("maxpercent"),
				new IntegerArgument("ticks"))
			.executes((sender, args) -> {
				for (Entity e : (Collection<Entity>) args[0]) {
					if (e instanceof LivingEntity le) {
						double maxHealth = EntityUtils.getMaxHealth(le);
						AbsorptionUtils.addAbsorption(le, (double) args[1] * maxHealth, (double) args[2] * maxHealth, (int) args[3]);
					}
				}
			}).register();
	}
}
