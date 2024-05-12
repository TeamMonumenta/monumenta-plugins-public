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
	private static final String COMMAND = "absorption";
	private static final String PERMISSION = "monumenta.command.absorption";

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("flat"),
				new EntitySelectorArgument.ManyEntities("entities"),
				new DoubleArgument("absorption"),
				new DoubleArgument("max"),
				new IntegerArgument("ticks"))
			.executes((sender, args) -> {
				for (Entity e : (Collection<Entity>) args.get("entities")) {
					if (e instanceof LivingEntity le) {
						AbsorptionUtils.addAbsorption(le, args.getUnchecked("absorption"), args.getUnchecked("max"), args.getUnchecked("ticks"));
					}
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("percent"),
				new EntitySelectorArgument.ManyEntities("entities"),
				new DoubleArgument("absorptionpercent"),
				new DoubleArgument("maxpercent"),
				new IntegerArgument("ticks"))
			.executes((sender, args) -> {
				for (Entity e : (Collection<Entity>) args.get("entities")) {
					if (e instanceof LivingEntity le) {
						double maxHealth = EntityUtils.getMaxHealth(le);
						AbsorptionUtils.addAbsorption(le, (double) args.get("absorptionpercent") * maxHealth, (double) args.get("maxpercent") * maxHealth, args.getUnchecked("ticks"));
					}
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("clear"),
				new EntitySelectorArgument.ManyEntities("entities")
			).executes((sender, args) -> {
				for (Entity e : (Collection<Entity>) args.get("entities")) {
					if (e instanceof LivingEntity le) {
						AbsorptionUtils.clearAbsorption(le);
					}
				}
			}).register();
	}
}
