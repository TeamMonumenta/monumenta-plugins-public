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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class CustomEffect {
	private static final String COMMAND = "customeffect";
	private static final String PERMISSION = "monumenta.commands.customeffect";

	public static void register() {
		Argument<?> entitiesArgument = new EntitySelectorArgument.ManyEntities("entities");
		Argument<?> effectArgument = new MultiLiteralArgument(Arrays.stream(EffectType.values()).map(et -> et.getType().toLowerCase()).toArray(String[]::new));
		Argument<?> durationArgument = new TimeArgument("duration");
		Argument<?> objectiveArgument = new ObjectiveArgument("objective").replaceSuggestions(ArgumentSuggestions.strings("objective", "amount"));
		Argument<?> scoreholderArgument = new ScoreHolderArgument.Single("scoreholder").replaceSuggestions(ArgumentSuggestions.strings("scoreholder", "amount"));
		Argument<?> amountArgument = new DoubleArgument("amount");
		Argument<?> sourceArgument = new StringArgument("source");

		List<List<Argument<?>>> withoutObjective = new ArrayList<>();
		List<List<Argument<?>>> withObjective = new ArrayList<>();
		List<List<Argument<?>>> withObjectiveAndScoreholder = new ArrayList<>();

		List<Argument<?>> defaultArgs = Arrays.asList(entitiesArgument, effectArgument, durationArgument);
		List<Argument<?>> defaultObjectiveArgs = new ArrayList<>(defaultArgs);
		defaultObjectiveArgs.add(objectiveArgument);
		List<Argument<?>> defaultObjectiveScoreholderArgs = new ArrayList<>(defaultObjectiveArgs);
		defaultObjectiveScoreholderArgs.add(scoreholderArgument);

		withoutObjective.add(defaultArgs);
		withObjective.add(defaultObjectiveArgs);
		withObjectiveAndScoreholder.add(defaultObjectiveScoreholderArgs);

		for (List<List<Argument<?>>> setting : Arrays.asList(withoutObjective, withObjective, withObjectiveAndScoreholder)) {
			List<Argument<?>> withAmount = new ArrayList<>(setting.get(0));
			withAmount.add(amountArgument);
			setting.add(withAmount);
			List<Argument<?>> withSource = new ArrayList<>(withAmount);
			withSource.add(sourceArgument);
			setting.add(withSource);
		}

		for (List<Argument<?>> arguments : withoutObjective) {
			new CommandAPICommand(COMMAND).withPermission(PERMISSION).withArguments(arguments).executes((sender, args) -> {
				Collection<Entity> entities = (Collection<Entity>) args[0];
				String effect = (String) args[1];
				int duration = (int) args[2];
				double amount = args.length > 3 ? (double) args[3] : 0;
				String source = args.length > 4 ? (String) args[4] : null;
				applyEffect(entities, effect, duration, null, null, amount, source);
			}).register();
		}

		for (List<Argument<?>> arguments : withObjective) {
			new CommandAPICommand(COMMAND).withPermission(PERMISSION).withArguments(arguments).executes((sender, args) -> {
				Collection<Entity> entities = (Collection<Entity>) args[0];
				String effect = (String) args[1];
				int duration = (int) args[2];
				String objective = (String) args[3];
				double amount = args.length > 4 ? (double) args[4] : 0;
				String source = args.length > 5 ? (String) args[5] : null;
				applyEffect(entities, effect, duration, objective, null, amount, source);
			}).register();
		}

		for (List<Argument<?>> arguments : withObjectiveAndScoreholder) {
			new CommandAPICommand(COMMAND).withPermission(PERMISSION).withArguments(arguments).executes((sender, args) -> {
				Collection<Entity> entities = (Collection<Entity>) args[0];
				String effect = (String) args[1];
				int duration = (int) args[2];
				String objective = (String) args[3];
				String scoreholder = (String) args[4];
				double amount = args.length > 5 ? (double) args[5] : 0;
				String source = args.length > 6 ? (String) args[6] : null;
				applyEffect(entities, effect, duration, objective, scoreholder, amount, source);
			}).register();
		}

		Argument<?> subcommands = new MultiLiteralArgument("haseffect", "clear");
		List<Argument<?>> hasEffectAndClearArguments = Arrays.asList(entitiesArgument, subcommands, sourceArgument);

		new CommandAPICommand(COMMAND).withPermission(PERMISSION).withArguments(hasEffectAndClearArguments).executes((sender, args) -> {
			Collection<Entity> entities = (Collection<Entity>) args[0];
			String subcommand = (String) args[1];
			String source = (String) args[2];

			Function<Entity, Boolean> function;
			if (subcommand.equals("clear")) {
				function = entity -> Plugin.getInstance().mEffectManager.clearEffects(entity, source) != null;
			} else {
				function = entity -> Plugin.getInstance().mEffectManager.hasEffect(entity, source);
			}
			return entities.stream().map(function).mapToInt(b -> b ? 1 : 0).sum();
		}).register();
	}

	private static void applyEffect(Collection<Entity> entities, String effect, int duration, @Nullable String objective, @Nullable String scoreholder, double amount, @Nullable String source) {
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
