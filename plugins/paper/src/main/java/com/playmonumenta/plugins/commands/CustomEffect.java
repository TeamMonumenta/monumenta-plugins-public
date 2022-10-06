package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbilityCooldownDecrease;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.ArrowSaving;
import com.playmonumenta.plugins.effects.BonusSoulThreads;
import com.playmonumenta.plugins.effects.BoonOfKnightlyPrayer;
import com.playmonumenta.plugins.effects.BoonOfThePit;
import com.playmonumenta.plugins.effects.CrystalineBlessing;
import com.playmonumenta.plugins.effects.DeepGodsEndowment;
import com.playmonumenta.plugins.effects.DurabilitySaving;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentExperience;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SilverPrayer;
import com.playmonumenta.plugins.effects.StarCommunion;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.effects.TuathanBlessing;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Collection;
import java.util.HashMap;
import org.bukkit.entity.Entity;

public class CustomEffect {
	private static final String COMMAND = "customeffect";
	private static final String PERMISSION = "monumenta.commands.customeffect";

	@FunctionalInterface
	private interface ZeroArgument {
		void run(Entity entity, int duration);
	}

	@FunctionalInterface
	private interface SingleArgument {
		void run(Entity entity, int duration, double amount, String source);
	}

	public static void register() {
		HashMap<String, ZeroArgument> zeroArgumentEffects = new HashMap<>();
		zeroArgumentEffects.put("stasis", (Entity entity, int duration) -> Plugin.getInstance().mEffectManager.addEffect(entity, Stasis.GENERIC_NAME, new Stasis(duration)));
		zeroArgumentEffects.put("silence", (Entity entity, int duration) -> Plugin.getInstance().mEffectManager.addEffect(entity, AbilitySilence.GENERIC_NAME, new AbilitySilence(duration)));

		HashMap<String, SingleArgument> singleArgumentEffects = new HashMap<>();
		singleArgumentEffects.put("speed", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentSpeed.GENERIC_NAME), new PercentSpeed(duration, amount, getSource(source, PercentSpeed.GENERIC_NAME))));
		singleArgumentEffects.put("damagedealt", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentDamageDealt.GENERIC_NAME), new PercentDamageDealt(duration, amount)));
		singleArgumentEffects.put("damagereceived", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentDamageReceived.GENERIC_NAME), new PercentDamageReceived(duration, amount)));
		singleArgumentEffects.put("experience", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentExperience.GENERIC_NAME), new PercentExperience(duration, amount)));
		singleArgumentEffects.put("attackspeed", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentAttackSpeed.GENERIC_NAME), new PercentAttackSpeed(duration, amount, getSource(source, PercentAttackSpeed.GENERIC_NAME))));
		singleArgumentEffects.put("knockbackresist", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentKnockbackResist.GENERIC_NAME), new PercentKnockbackResist(duration, amount, getSource(source, PercentKnockbackResist.GENERIC_NAME))));
		singleArgumentEffects.put("arrowsaving", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, ArrowSaving.GENERIC_NAME), new ArrowSaving(duration, amount)));
		singleArgumentEffects.put("durabilitysaving", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, DurabilitySaving.GENERIC_NAME), new DurabilitySaving(duration, amount)));
		singleArgumentEffects.put("soul", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, BonusSoulThreads.GENERIC_NAME), new BonusSoulThreads(duration, amount)));
		singleArgumentEffects.put("cdr", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, AbilityCooldownDecrease.GENERIC_NAME), new AbilityCooldownDecrease(duration, amount)));
		singleArgumentEffects.put("heal", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentHeal.GENERIC_NAME), new PercentHeal(duration, amount)));
		singleArgumentEffects.put("antiheal", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentHeal.GENERIC_NAME), new PercentHeal(duration, -amount)));
		singleArgumentEffects.put("healthboost", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, PercentHealthBoost.GENERIC_NAME), new PercentHealthBoost(duration, amount, PercentHealthBoost.GENERIC_NAME)));

		// R3 Shrine Effects
		singleArgumentEffects.put("boonofthepit", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, BoonOfThePit.GENERIC_NAME), new BoonOfThePit(duration)));
		singleArgumentEffects.put("boonofknightlyprayer", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, BoonOfKnightlyPrayer.GENERIC_NAME), new BoonOfKnightlyPrayer(duration)));
		singleArgumentEffects.put("crystallineblessing", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, CrystalineBlessing.GENERIC_NAME), new CrystalineBlessing(duration)));
		singleArgumentEffects.put("deepgodsendowment", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, DeepGodsEndowment.GENERIC_NAME), new DeepGodsEndowment(duration)));
		singleArgumentEffects.put("silverprayer", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, SilverPrayer.GENERIC_NAME), new SilverPrayer(duration)));
		singleArgumentEffects.put("starcommunion", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, StarCommunion.GENERIC_NAME), new StarCommunion(duration)));
		singleArgumentEffects.put("tuathanblessing", (Entity entity, int duration, double amount, String source) -> Plugin.getInstance().mEffectManager.addEffect(entity, getSource(source, TuathanBlessing.GENERIC_NAME), new TuathanBlessing(duration)));

		HashMap<String, String> translations = new HashMap<>();
		translations.put("stasis", Stasis.GENERIC_NAME);
		translations.put("silence", AbilitySilence.GENERIC_NAME);
		translations.put("speed", PercentSpeed.GENERIC_NAME);
		translations.put("damagedealt", PercentDamageDealt.GENERIC_NAME);
		translations.put("damagereceived", PercentDamageReceived.GENERIC_NAME);
		translations.put("experience", PercentExperience.GENERIC_NAME);
		translations.put("attackspeed", PercentAttackSpeed.GENERIC_NAME);
		translations.put("knockbackresist", PercentKnockbackResist.GENERIC_NAME);
		translations.put("arrowsaving", ArrowSaving.GENERIC_NAME);
		translations.put("durabilitysaving", DurabilitySaving.GENERIC_NAME);
		translations.put("soul", BonusSoulThreads.GENERIC_NAME);
		translations.put("cdr", AbilityCooldownDecrease.GENERIC_NAME);
		translations.put("heal", PercentHeal.GENERIC_NAME);
		translations.put("antiheal", PercentHeal.GENERIC_NAME);
		translations.put("healthboost", PercentHealthBoost.GENERIC_NAME);
		translations.put("boonofthepit", BoonOfThePit.GENERIC_NAME);
		translations.put("boonofknightlyprayer", BoonOfKnightlyPrayer.GENERIC_NAME);
		translations.put("crystalineblessing", CrystalineBlessing.GENERIC_NAME);
		translations.put("deepgodsendowment", DeepGodsEndowment.GENERIC_NAME);
		translations.put("silverprayer", SilverPrayer.GENERIC_NAME);
		translations.put("starcommunion", StarCommunion.GENERIC_NAME);
		translations.put("tuathanblessing", TuathanBlessing.GENERIC_NAME);

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(zeroArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("seconds")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					zeroArgumentEffects.get((String) args[1]).run(entity, (int) (((double) args[2]) * 20));
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("seconds"),
				new DoubleArgument("amount")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					singleArgumentEffects.get((String) args[1]).run(entity, (int) (((double) args[2]) * 20), (double) args[3], null);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("seconds"),
				new DoubleArgument("amount"),
				new GreedyStringArgument("source")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					singleArgumentEffects.get((String) args[1]).run(entity, (int) (((double) args[2]) * 20), (double) args[3], (String) args[4]);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(zeroArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("time"),
				new MultiLiteralArgument("minutes", "seconds", "ticks")
			).executes((sender, args) -> {
				int duration = getDuration((String) args[3], (double) args[2]);
				for (Entity entity : (Collection<Entity>) args[0]) {
					zeroArgumentEffects.get((String) args[1]).run(entity, duration);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("time"),
				new MultiLiteralArgument("minutes", "seconds", "ticks"),
				new DoubleArgument("amount")
			).executes((sender, args) -> {
				int duration = getDuration((String) args[3], (double) args[2]);
				for (Entity entity : (Collection<Entity>) args[0]) {
					singleArgumentEffects.get((String) args[1]).run(entity, duration, (double) args[4], null);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new DoubleArgument("time"),
				new MultiLiteralArgument("minutes", "seconds", "ticks"),
				new DoubleArgument("amount"),
				new GreedyStringArgument("source")
			).executes((sender, args) -> {
				int duration = getDuration((String) args[3], (double) args[2]);
				for (Entity entity : (Collection<Entity>) args[0]) {
					singleArgumentEffects.get((String) args[1]).run(entity, duration, (double) args[4], (String) args[5]);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(zeroArgumentEffects.keySet().toArray(String[]::new)),
				new ObjectiveArgument("objective"),
				new MultiLiteralArgument("minutes", "seconds", "ticks")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					int duration = getDuration((String) args[3], ScoreboardUtils.getScoreboardValue(entity, (String) args[2]).orElse(0));
					if (duration > 0) {
						zeroArgumentEffects.get((String) args[1]).run(entity, duration);
					}
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new ObjectiveArgument("objective"),
				new MultiLiteralArgument("minutes", "seconds", "ticks"),
				new DoubleArgument("amount")
			).executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[0]) {
					int duration = getDuration((String) args[3], ScoreboardUtils.getScoreboardValue(entity, (String) args[2]).orElse(0));
					if (duration > 0) {
						singleArgumentEffects.get((String) args[1]).run(entity, duration, (double) args[4], null);
					}
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(zeroArgumentEffects.keySet().toArray(String[]::new)),
				new ObjectiveArgument("objective"),
				new MultiLiteralArgument("minutes", "seconds", "ticks"),
				new ScoreHolderArgument("scoreholder", ScoreHolderArgument.ScoreHolderType.SINGLE)
			).executes((sender, args) -> {
				int duration = getDuration((String) args[3], ScoreboardUtils.getScoreboardValue((String) args[4], (String) args[2]).orElse(0));
				for (Entity entity : (Collection<Entity>) args[0]) {
					if (duration > 0) {
						zeroArgumentEffects.get((String) args[1]).run(entity, duration);
					}
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument(singleArgumentEffects.keySet().toArray(String[]::new)),
				new ObjectiveArgument("objective"),
				new MultiLiteralArgument("minutes", "seconds", "ticks"),
				new ScoreHolderArgument("scoreholder", ScoreHolderArgument.ScoreHolderType.SINGLE),
				new DoubleArgument("amount")
			).executes((sender, args) -> {
				int duration = getDuration((String) args[3], ScoreboardUtils.getScoreboardValue((String) args[4], (String) args[2]).orElse(0));
				for (Entity entity : (Collection<Entity>) args[0]) {
					if (duration > 0) {
						singleArgumentEffects.get((String) args[1]).run(entity, duration, (double) args[5], null);
					}
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument("clear"),
				new StringArgument("effect")
					.replaceSuggestions((info) ->
						translations.keySet().stream().toList().toArray(String[]::new)
					)
			).executes((sender, args) -> {
				String source = (String) args[2];
				String translation = translations.get(source);
				if (translation != null) {
					source = translation;
				}
				for (Entity entity : (Collection<Entity>) args[0]) {
					Plugin.getInstance().mEffectManager.clearEffects(entity, source);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument("clear"),
				new StringArgument("effect")
					.replaceSuggestions((info) ->
						translations.keySet().stream().toList().toArray(String[]::new)
					),
				new StringArgument("source")
			).executes((sender, args) -> {
				String source = (String) args[3];
				String translation = translations.get(source);
				if (translation != null) {
					source = translation;
				}
				for (Entity entity : (Collection<Entity>) args[0]) {
					Plugin.getInstance().mEffectManager.clearEffects(entity, source);
				}
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument("haseffect"),
				new StringArgument("effect")
					.replaceSuggestions((info) ->
						translations.keySet().stream().toList().toArray(String[]::new)
					)
			).executes((sender, args) -> {
				String source = (String) args[2];
				String translation = translations.get(source);
				if (translation != null) {
					source = translation;
				}
				boolean out = true;
				for (Entity entity : (Collection<Entity>) args[0]) {
					out = out && Plugin.getInstance().mEffectManager.hasEffect(entity, source);
				}
				return out ? 1 : 0;
			}).register();

		new CommandAPICommand(COMMAND).withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument("entities", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
				new MultiLiteralArgument("haseffect"),
				new StringArgument("effect")
					.replaceSuggestions((info) ->
						translations.keySet().stream().toList().toArray(String[]::new)
					),
				new StringArgument("source")
			).executes((sender, args) -> {
				String source = (String) args[3];
				String translation = translations.get(source);
				if (translation != null) {
					source = translation;
				}
				boolean out = true;
				for (Entity entity : (Collection<Entity>) args[0]) {
					out = out && Plugin.getInstance().mEffectManager.hasEffect(entity, source);
				}
				return out ? 1 : 0;
			}).register();
	}

	private static int getDuration(String timeValue, double num) {
		return switch (timeValue) {
			case "minutes" -> (int) (num * 60 * 20);
			case "seconds" -> (int) (num * 20);
			default -> (int) num;
		};
	}

	private static String getSource(String provided, String generic) {
		if (provided == null || provided.isEmpty()) {
			return generic;
		} else {
			return provided;
		}
	}
}
