package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DescriptionBuilder<T extends Ability> implements Description<T> {
	private static final TextColor TRIGGER_COLOR = NamedTextColor.YELLOW;

	private final Supplier<AbilityInfo<T>> mInfo;
	private final @Nullable TextColor mBaseColor;
	private final List<Description<T>> mDescriptions;

	public DescriptionBuilder(Supplier<AbilityInfo<T>> info) {
		this(info, null);
	}

	public DescriptionBuilder(Supplier<AbilityInfo<T>> info, @Nullable TextColor baseColor) {
		mInfo = info;
		mBaseColor = baseColor;
		mDescriptions = new ArrayList<>();
	}

	public DescriptionBuilder<T> add(Description<T> description) {
		mDescriptions.add(description);
		return this;
	}

	public DescriptionBuilder<T> add(Component component) {
		return add((a, p) -> component);
	}

	public DescriptionBuilder<T> add(String string) {
		return add(Component.text(string));
	}

	public DescriptionBuilder<T> add(Function<T, ? extends Number> getter, double baseValue, boolean invertColor, @Nullable Function<Double, String> formatter, boolean useBaseColor) {
		return add((a, p) -> {
			Function<Double, String> f = formatter == null ? StringUtils::to2DP : formatter;
			Component base = Component.text(f.apply(baseValue));
			if (useBaseColor && mBaseColor != null) {
				base = base.color(mBaseColor);
			}
			if (a == null) {
				return base;
			}
			Component output = Component.empty().append(base);
			double diff = getter.apply(a).doubleValue() - baseValue;
			if (Math.abs(diff) >= 0.01 || getHighestDigit(diff) >= getHighestDigit(baseValue) - 1) {
				boolean positive = diff >= 0;
				String sign = positive ? "+" : "";
				TextColor color = CharmManager.getCharmEffectColor(positive, invertColor);
				output = output.append(Component.text(" (" + sign + f.apply(diff) + ")", color));
			}
			return output;
		});
	}

	public DescriptionBuilder<T> add(Function<T, ? extends Number> getter, double baseValue, boolean invertColor, @Nullable Function<Double, String> formatter) {
		return add(getter, baseValue, invertColor, formatter, false);
	}

	public DescriptionBuilder<T> add(Function<T, ? extends Number> getter, double baseValue, boolean invertColor) {
		return add(getter, baseValue, invertColor, (Function<Double, String>) null);
	}

	public DescriptionBuilder<T> add(Function<T, ? extends Number> getter, double baseValue, boolean invertColor, Predicate<Ability> levelCondition) {
		return add(a -> levelCondition.test(a) ? getter.apply(a) : baseValue, baseValue, invertColor);
	}

	public DescriptionBuilder<T> add(Function<T, ? extends Number> getter, double baseValue) {
		return add(getter, baseValue, false);
	}

	public DescriptionBuilder<T> add(double baseValue) {
		return add(a -> baseValue, baseValue);
	}

	public DescriptionBuilder<T> addDuration(Function<T, Integer> getter, int baseDuration, boolean invertColor, boolean useBaseColor) {
		return add(getter, baseDuration, invertColor, StringUtils::ticksToSeconds, useBaseColor);
	}

	public DescriptionBuilder<T> addDuration(Function<T, Integer> getter, int baseDuration, boolean invertColor) {
		return addDuration(getter, baseDuration, invertColor, false);
	}

	public DescriptionBuilder<T> addDuration(Function<T, Integer> getter, int baseDuration, boolean invertColor, Predicate<Ability> levelCondition) {
		return addDuration(a -> levelCondition.test(a) ? getter.apply(a) : baseDuration, baseDuration, invertColor);
	}

	public DescriptionBuilder<T> addDuration(Function<T, Integer> getter, int baseDuration) {
		return addDuration(getter, baseDuration, false);
	}

	public DescriptionBuilder<T> addDuration(int baseDuration) {
		return addDuration(a -> baseDuration, baseDuration);
	}

	// Adds the ENTIRE cooldown string, including a space in front.
	// If this is the last thing (which it should be), finish the previous sentence as if it is the last.
	public DescriptionBuilder<T> addCooldown(int baseCooldown, boolean useBaseColor, Predicate<Ability> levelCondition) {
		return add(" Cooldown: ")
			.addDuration(a -> levelCondition.test(a) ? a.getCharmCooldown(baseCooldown) : baseCooldown, baseCooldown, true, useBaseColor)
			.add("s.");
	}

	public DescriptionBuilder<T> addCooldown(int baseCooldown, boolean useBaseColor) {
		return addCooldown(baseCooldown, useBaseColor, a -> true);
	}

	public DescriptionBuilder<T> addCooldown(int baseCooldown, Predicate<Ability> levelCondition) {
		return addCooldown(baseCooldown, false, levelCondition);
	}

	public DescriptionBuilder<T> addCooldown(int baseCooldown) {
		return addCooldown(baseCooldown, false);
	}

	public DescriptionBuilder<T> addPercent(Function<T, Double> getter, double basePercent, boolean invertColor, boolean useBaseColor) {
		return add(getter, basePercent, invertColor, StringUtils::multiplierToPercentageWithSign, useBaseColor);
	}

	public DescriptionBuilder<T> addPercent(Function<T, Double> getter, double basePercent, boolean invertColor) {
		return addPercent(getter, basePercent, invertColor, false);
	}

	public DescriptionBuilder<T> addPercent(Function<T, Double> getter, double basePercent, boolean invertColor, Predicate<Ability> levelCondition) {
		return addPercent(a -> levelCondition.test(a) ? getter.apply(a) : basePercent, basePercent, invertColor);
	}

	public DescriptionBuilder<T> addPercent(Function<T, Double> getter, double basePercent) {
		return addPercent(getter, basePercent, false);
	}

	public DescriptionBuilder<T> addPercent(double basePercent) {
		return addPercent(a -> basePercent, basePercent);
	}

	public DescriptionBuilder<T> addPotionAmplifier(int baseAmplifier) {
		return add(a -> baseAmplifier, baseAmplifier);
	}

	// Adds 1 because potion amplifiers are lies
	public DescriptionBuilder<T> addPotionAmplifier(Function<T, Integer> getter, int baseAmplifier) {
		return add(a -> getter.apply(a) + 1, baseAmplifier + 1, false, d -> StringUtils.toRoman(d.intValue()), false);
	}

	public DescriptionBuilder<T> addPotionAmplifier(Function<T, Integer> getter, int baseAmplifier, Predicate<Ability> levelCondition) {
		return addPotionAmplifier(a -> levelCondition.test(a) ? getter.apply(a) : baseAmplifier, baseAmplifier);
	}

	public DescriptionBuilder<T> addDepthsDamage(Function<T, Double> getter, double baseDamage, boolean useBaseColor) {
		return add(getter.andThen(d -> d * DepthsUtils.getDamageMultiplier()), baseDamage * DepthsUtils.getDamageMultiplier(), false, null, useBaseColor);
	}

	public DescriptionBuilder<T> addConditionalTree(DepthsTree tree, Description<T> description) {
		return add((a, p) -> {
			if (p != null && DepthsManager.getInstance().hasTreeUnlocked(p, tree)) {
				return description.get(a, p);
			} else {
				return Component.empty();
			}
		});
	}

	public DescriptionBuilder<T> addConditionalTreeOrAbility(DepthsTree tree, Description<T> description) {
		return add((a, p) -> {
			if (p != null && (DepthsManager.getInstance().hasTreeUnlocked(p, tree)
				|| DepthsManager.getInstance().getPlayerAbilities(p).stream().filter(da -> da.getDepthsTree() == tree).anyMatch(AbilityInfo::hasCooldown))) {
				return description.get(a, p);
			} else {
				return Component.empty();
			}
		});
	}

	public DescriptionBuilder<T> addConditionalDepthsContent(DepthsContent content, String s) {
		return add((a, p) -> DepthsUtils.getDepthsContent() == content ? Component.text(s) : Component.empty());
	}

	public DescriptionBuilder<T> addTrigger() {
		return addTrigger(0);
	}

	public DescriptionBuilder<T> addTrigger(@Nullable String extraCondition) {
		return addTrigger(0, extraCondition);
	}

	public DescriptionBuilder<T> addTrigger(int index) {
		return addTrigger(index, null);
	}

	public DescriptionBuilder<T> addTrigger(int index, @Nullable String extraCondition) {
		return add((a, p) -> {
			AbilityInfo<T> info = mInfo.get();
			if (info == null) {
				//TODO eventually remove null check
				return Component.empty();
			}
			List<AbilityTriggerInfo<T>> triggers = info.getTriggers();
			if (index > triggers.size()) {
				return Component.empty();
			}
			AbilityTriggerInfo<T> triggerInfo = triggers.get(index);
			AbilityTriggerInfo<T> customTrigger;
			if (p == null) {
				customTrigger = triggerInfo;
			} else {
				customTrigger = triggerInfo.withCustomTrigger(info, p);
			}
			return customTrigger.getAsNaturalLanguage(extraCondition).color(TRIGGER_COLOR);
		});
	}

	@Override
	public Component get(@Nullable T ability, @Nullable Player player) {
		Component output = Component.empty();
		for (Description<T> desc : mDescriptions) {
			output = output.append(desc.get(ability, player));
		}
		return output;
	}

	private static int getHighestDigit(double d) {
		return (int) Math.floor(Math.log10(d));
	}
}
