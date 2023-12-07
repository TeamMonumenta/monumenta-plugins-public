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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DescriptionBuilder<T extends Ability> implements Description<T> {

	private final @Nullable TextColor mBaseColor;
	private final List<Description<T>> mDescriptions;

	public DescriptionBuilder() {
		this(null);
	}

	public DescriptionBuilder(@Nullable TextColor baseColor) {
		mBaseColor = baseColor;
		mDescriptions = new ArrayList<>();
	}

	public DescriptionBuilder<T> add(Description<T> description) {
		mDescriptions.add(description);
		return this;
	}

	public DescriptionBuilder<T> add(Component component) {
		return add(a -> component);
	}

	public DescriptionBuilder<T> add(String string) {
		return add(Component.text(string));
	}

	public DescriptionBuilder<T> add(Function<T, ? extends Number> getter, double baseValue, boolean invertColor, @Nullable Function<Double, String> formatter, boolean useBaseColor) {
		return add(a -> {
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
			if (Math.abs(diff) > 0.01) {
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
		return add(getter, baseValue, invertColor, null);
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

	public DescriptionBuilder<T> addDuration(Function<T, Integer> getter, int baseDuration) {
		return addDuration(getter, baseDuration, false);
	}

	public DescriptionBuilder<T> addDuration(int baseDuration) {
		return addDuration(a -> baseDuration, baseDuration);
	}

	// Adds the ENTIRE cooldown string, including a space in front.
	// If this is the last thing (which it should be), finish the previous sentence as if it is the last.
	public DescriptionBuilder<T> addCooldown(int baseCooldown, boolean useBaseColor) {
		return add(" Cooldown: ")
			.addDuration(a -> a.getCharmCooldown(baseCooldown), baseCooldown, true, useBaseColor)
			.add("s.");
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

	public DescriptionBuilder<T> addPercent(Function<T, Double> getter, double basePercent) {
		return addPercent(getter, basePercent, false);
	}

	public DescriptionBuilder<T> addPercent(double basePercent) {
		return addPercent(a -> basePercent, basePercent);
	}

	public DescriptionBuilder<T> addDepthsDamage(Function<T, Double> getter, double baseDamage, boolean useBaseColor) {
		return add(getter.andThen(d -> d * DepthsUtils.getDamageMultiplier()), baseDamage * DepthsUtils.getDamageMultiplier(), false, null, useBaseColor);
	}

	public DescriptionBuilder<T> addConditional(Predicate<T> condition, Description<T> description) {
		return add(a -> a != null && condition.test(a) ? description.get(a) : Component.empty());
	}

	public DescriptionBuilder<T> addConditional(Description<T> description) {
		return addConditional(a -> true, description);
	}

	public DescriptionBuilder<T> addConditionalPlayer(Predicate<Player> condition, Description<T> description) {
		return addConditional(a -> condition.test(a.getPlayer()), description);
	}

	public DescriptionBuilder<T> addConditionalTree(DepthsTree tree, Description<T> description) {
		return addConditionalPlayer(p -> DepthsManager.getInstance().hasTreeUnlocked(p, tree), description);
	}

	public DescriptionBuilder<T> addConditionalDepthsContent(DepthsContent content, String s) {
		return addConditional(a -> DepthsUtils.getDepthsContent() == content, a -> Component.text(s));
	}

	@Override
	public Component get(@Nullable T ability) {
		Component output = Component.empty();
		for (Description<T> desc : mDescriptions) {
			output = output.append(desc.get(ability));
		}
		return output;
	}
}
