package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Objective;

public class CustomEffect {
	private static final String COMMAND = "customeffect";
	private static final String PERMISSION = "monumenta.commands.customeffect";

	@SuppressWarnings("unchecked")
	public static void register() {
		Argument<?> entitiesArgument = new EntitySelectorArgument.ManyEntities("entities");
		Argument<?> sourceArgument = new StringArgument("source");

		List<Argument<?>> optionalArguments = new ArrayList<>();
		optionalArguments.add(new DoubleArgument("amount"));
		optionalArguments.add(sourceArgument);

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(entitiesArgument);
		arguments.add(new MultiLiteralArgument("effect", Arrays.stream(EffectType.values()).map(et -> et.getType().toLowerCase(Locale.getDefault())).toArray(String[]::new)));
		arguments.add(new TimeArgument("duration"));

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(optionalArguments)
			.executes((sender, args) -> {
				execute(args);
			}).register();

		arguments.add(new ObjectiveArgument("objective").replaceSuggestions(ArgumentSuggestions.strings("objective", "amount")));

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(optionalArguments)
			.executes((sender, args) -> {
				execute(args);
			}).register();

		arguments.add(new ScoreHolderArgument.Single("scoreholder").replaceSuggestions(ArgumentSuggestions.strings("scoreholder", "amount")));

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(optionalArguments)
			.executes((sender, args) -> {
				execute(args);
			}).register();

		Argument<?> subcommands = new MultiLiteralArgument("subcommand", "haseffect", "clear");
		List<Argument<?>> hasEffectAndClearArguments = Arrays.asList(entitiesArgument, subcommands, sourceArgument);

		new CommandAPICommand(COMMAND).withPermission(PERMISSION).withArguments(hasEffectAndClearArguments).executes((sender, args) -> {
			Collection<Entity> entities = (Collection<Entity>) args.get("entities");
			String subcommand = args.getUnchecked("subcommand");
			String source = args.getUnchecked("source");

			Function<Entity, Boolean> function;
			if (subcommand.equals("clear")) {
				function = entity -> Plugin.getInstance().mEffectManager.clearEffects(entity, source) != null;
			} else {
				function = entity -> Plugin.getInstance().mEffectManager.hasEffect(entity, source);
			}
			return entities.stream().map(function).mapToInt(b -> b ? 1 : 0).sum();
		}).register();
	}

	private static void execute(CommandArguments args) {
		Collection<Entity> entities = (Collection<Entity>) args.get("entities");
		String effect = args.getUnchecked("effect");
		int duration = args.getUnchecked("duration");
		Objective objective = args.getUnchecked("objective");
		String scoreholder = args.getUnchecked("scoreholder");
		double amount = args.getOrDefaultUnchecked("amount", 0);
		String source = args.getUnchecked("source");

		EffectType effectType = EffectType.fromTypeIgnoreCase(effect);
		if (effectType == null) {
			return;
		}

		if (objective != null && scoreholder != null) {
			duration *= ScoreboardUtils.getScoreboardValue(scoreholder, objective).orElse(0);
		}

		for (Entity entity : entities) {
			if (entity instanceof LivingEntity livingEntity) {
				int d = duration;
				if (objective != null && scoreholder == null) {
					d *= ScoreboardUtils.getScoreboardValue(livingEntity, objective).orElse(0);
				}
				EffectType.applyEffect(effectType, livingEntity, d, amount, source, false);
			}
		}
	}
}
