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

	private static final EntitySelectorArgument.ManyEntities entitiesArgument = new EntitySelectorArgument.ManyEntities("entities");
	private static final StringArgument sourceArgument = new StringArgument("source");
	private static final StringArgument sourceArgumentOptional = new StringArgument("source");
	private static final DoubleArgument amountArg = new DoubleArgument("amount");
	private static final MultiLiteralArgument effectArg = new MultiLiteralArgument("effect", Arrays.stream(EffectType.values()).map(et -> et.getType().toLowerCase(Locale.getDefault())).toArray(String[]::new));
	private static final TimeArgument durationArg = new TimeArgument("duration");
	private static final Argument<Objective> objectiveArg = new ObjectiveArgument("objective").replaceSuggestions(ArgumentSuggestions.strings("objective", "amount"));
	private static final Argument<String> scoreholderArg = new ScoreHolderArgument.Single("scoreholder").replaceSuggestions(ArgumentSuggestions.strings("scoreholder", "amount"));
	private static final MultiLiteralArgument subcommandArg = new MultiLiteralArgument("subcommand", "haseffect", "clear");

	@SuppressWarnings("unchecked")
	public static void register() {
		List<Argument<?>> optionalArguments = new ArrayList<>();
		optionalArguments.add(amountArg);
		optionalArguments.add(sourceArgumentOptional);

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(entitiesArgument);
		arguments.add(effectArg);
		arguments.add(durationArg);

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(optionalArguments)
			.executes((sender, args) -> {
				execute(args);
			}).register();

		arguments.add(objectiveArg);

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(optionalArguments)
			.executes((sender, args) -> {
				execute(args);
			}).register();

		arguments.add(scoreholderArg);

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(arguments)
			.withOptionalArguments(optionalArguments)
			.executes((sender, args) -> {
				execute(args);
			}).register();

		List<Argument<?>> hasEffectAndClearArguments = Arrays.asList(entitiesArgument, subcommandArg, sourceArgument);

		new CommandAPICommand(COMMAND).withPermission(PERMISSION).withArguments(hasEffectAndClearArguments).executes((sender, args) -> {
			Collection<Entity> entities = args.getByArgument(entitiesArgument);
			String subcommand = args.getByArgument(subcommandArg);
			String source = args.getByArgument(sourceArgument);

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
		Collection<Entity> entities = args.getByArgument(entitiesArgument);
		String effect = args.getByArgument(effectArg);
		int duration = args.getByArgument(durationArg);
		Objective objective = args.getByArgument(objectiveArg);
		String scoreholder = args.getByArgument(scoreholderArg);
		double amount = args.getByArgumentOrDefault(amountArg, 0d);
		String source = args.getByArgument(sourceArgumentOptional);

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
