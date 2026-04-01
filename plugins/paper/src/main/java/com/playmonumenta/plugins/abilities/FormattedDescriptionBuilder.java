package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritIce;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.utils.DescriptionUtils.ALCHEMIST_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.BLACK;
import static com.playmonumenta.plugins.utils.DescriptionUtils.CLERIC_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.DARK_GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.DISABLED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.MAGE_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.RED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.REGION_SCALED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.ROGUE_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.SCOUT_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.SHAMAN_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.TRIGGER_LABEL;
import static com.playmonumenta.plugins.utils.DescriptionUtils.TRIGGER_TEXT;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WARLOCK_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WARRIOR_ARROW;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;
import static com.playmonumenta.plugins.utils.DescriptionUtils.actionLine;

@SuppressWarnings("PMD.TooManyStaticImports")
public class FormattedDescriptionBuilder<T extends Ability> extends DescriptionBuilder<T> {

	// We need to defer accessing MonumentaClasses(), otherwise leads to an infinite static initialization loop
	@SuppressWarnings("UnnecessaryLambda")
	private final Supplier<List<PlayerClass>> CLASSES = () -> new MonumentaClasses().getClasses();
	private final List<Style> ARROW_COLORS = List.of(ALCHEMIST_ARROW, CLERIC_ARROW, MAGE_ARROW, ROGUE_ARROW, SCOUT_ARROW, SHAMAN_ARROW, WARLOCK_ARROW, WARRIOR_ARROW);

	private final int mDescriptionLevel;
	private @Nullable Style mArrowColor = null;

	public FormattedDescriptionBuilder() {
		this(() -> null);
	}

	public FormattedDescriptionBuilder(Supplier<AbilityInfo<T>> info) {
		this(info, 0);
	}

	public FormattedDescriptionBuilder(Supplier<AbilityInfo<T>> info, int level) {
		super(info);
		mDescriptionLevel = level;
	}

	public FormattedDescriptionBuilder<T> arrowColor(Style color) {
		mArrowColor = color;
		return this;
	}

	@Override
	public FormattedDescriptionBuilder<T> add(Description<T> description) {
		mDescriptions.add(description);
		return this;
	}

	/**
	 * Represents the value of a stat on a particular ability.
	 * Supports a base value and a getter to get the charm-affected, real value on the ability.
	 */
	public static class StatValue<S extends Ability> {
		private final BiFunction<S, Player, ? extends Number> mRealValue;
		private final BiFunction<S, Player, ? extends Number> mBaseValue;
		private final boolean mInvertColor;

		/**
		 * Used on abilities that repeat the same stat across multiple levels. i.e. 6 -> 12 damage.
		 * Helps define formatting behavior (i.e. which level to display charm bonus, which level to strike out)
		 */
		public enum StatLevel {
			ENABLED(a -> false, a -> true), // never greyed out; always shows charm bonus
			DISABLED(a -> true, a -> false), // always greyed out, never shows charm bonus

			// value changes between Level 1 and 2
			LEVEL_1(Ability::isLevelTwo, Ability::isLevelOne),
			LEVEL_2(a -> false, Ability::isLevelTwo),

			// value changes between Level 1, 2, and Enhancement
			NOT_ENHANCED_1(a -> a.isEnhanced() || a.isLevelTwo(), a -> !a.isEnhanced() && a.isLevelOne()),
			NOT_ENHANCED_2(a -> a.isEnhanced() && a.isLevelTwo(), a -> !a.isEnhanced() && a.isLevelTwo()),
			ENHANCED(a -> false, Ability::isEnhanced),

			// value changes between Level 1 and Enhancement
			LEVEL_1_E(Ability::isEnhanced, a -> !a.isEnhanced());

			private final Predicate<Ability> mGreyCondition;
			private final Predicate<Ability> mCharmBonusCondition;

			StatLevel(Predicate<Ability> strikeoutCondition, Predicate<Ability> charmBonusCondition) {
				mGreyCondition = strikeoutCondition;
				mCharmBonusCondition = charmBonusCondition;
			}
		}

		public StatValue(BiFunction<S, Player, ? extends Number> baseValue) {
			this(baseValue, baseValue, false);
		}

		public StatValue(BiFunction<S, Player, ? extends Number> realValue, BiFunction<S, Player, ? extends Number> baseValue) {
			this(realValue, baseValue, false);
		}

		public StatValue(BiFunction<S, Player, ? extends Number> realValue, BiFunction<S, Player, ? extends Number> baseValue, boolean invertColor) {
			mRealValue = realValue;
			mBaseValue = baseValue;
			mInvertColor = invertColor;
		}

		public static <S extends Ability> StatValue<S> stat(Number baseValue) {
			return new StatValue<>((a, p) -> baseValue);
		}

		public static <S extends Ability> StatValue<S> stat(BiFunction<S, Player, ? extends Number> baseValueGetter) {
			return new StatValue<>(baseValueGetter);
		}

		public static <S extends Ability> StatValue<S> stat(Function<S, ? extends Number> realValue, Number baseValue) {
			return new StatValue<>((a, p) -> realValue.apply(a), (a, p) -> baseValue);
		}

		public static <S extends Ability> StatValue<S> stat(BiFunction<S, Player, ? extends Number> realValueGetter, BiFunction<S, Player, ? extends Number> baseValueGetter) {
			return new StatValue<>(realValueGetter, baseValueGetter);
		}

		public static <S extends Ability> StatValue<S> perRegion(Number... baseValues) {
			BiFunction<S, Player, ? extends Number> baseValueGetter = (a, p) -> {
				if (baseValues.length <= 3) {
					return AbilityUtils.getRegionScaled(p, baseValues);
				} else {
					throw new IllegalArgumentException("StatValue.perRegion() only accepts 2 or 3 region values!");
				}
			};
			return new StatValue<>(baseValueGetter);
		}

		public static <S extends Ability> StatValue<S> perRegion(Function<S, ? extends Number> realValue, Number... baseValues) {
			BiFunction<S, Player, ? extends Number> baseValueGetter = (a, p) -> {
				if (baseValues.length <= 3) {
					return AbilityUtils.getRegionScaled(p, baseValues);
				} else {
					throw new IllegalArgumentException("StatValue.perRegion() only accepts 2 or 3 region values!");
				}
			};
			return new StatValue<>((a, p) -> realValue.apply(a), baseValueGetter);
		}

		public static <S extends Ability> StatValue<S> perLevel(Number... baseValues) {
			BiFunction<S, Player, ? extends Number> baseValueGetter = (a, p) -> {
				if (baseValues.length == 2) {
					return (a == null || a.isLevelOne()) ? baseValues[0] : baseValues[1];
				} else {
					throw new IllegalArgumentException("StatValue.perLevel() only accepts 2 values!");
				}
			};
			return new StatValue<>(baseValueGetter);
		}

		public static <S extends Ability> StatValue<S> perLevel(Function<S, ? extends Number> realValue, Number... baseValues) {
			BiFunction<S, Player, ? extends Number> baseValue = (a, p) -> {
				if (baseValues.length == 2) {
					return (a == null || a.isLevelOne()) ? baseValues[0] : baseValues[1];
				} else {
					throw new IllegalArgumentException("StatValue.perLevel() only accepts 2 values!");
				}
			};
			return new StatValue<>((a, p) -> realValue.apply(a), baseValue);
		}

		public static <S extends Ability> StatValue<S> cooldown(int baseValue) {
			return new StatValue<>((a, p) -> a.getCharmCooldown(baseValue), (a, p) -> baseValue, true);
		}

		public static <S extends Ability> StatValue<S> scoreboard(String objective) {
			return new StatValue<>((a, p) -> ScoreboardUtils.getScoreboardValue(p, objective).orElse(0));
		}

		/**
		 * <p>Creates a Component containing the numerical value of the stat, alongside any bonus from charms.
		 * Only displays charm bonus when the ability is at the corresponding level.
		 * The number format depends on the content of the placeholder string in the description.
		 * The placeholder string has format "%[formatOption][disableChar?][regionScaled?]".</p>
		 *
		 * <p>formatOption: required. Determines how the stat value is represented.
		 * <ul>
		 * <li>%d = decimal, i.e. 2.5 (default)</li>
		 * <li>%p = percentage, i.e. 20%</li>
		 * <li>%r = radius/range, i.e. 2.5 Blocks</li>
		 * <li>%t = time, i.e. 2.5s </li>
		 * </ul></p>
		 *
		 * <p>levelOption: not required. Determines how the stat should behave when other levels are selected.
		 * Used when a stat (for example, damage) gets upgraded from L1 to L2. L1 should grey out if L2 is selected.
		 * <ul>
		 * <li>%d1 = this is the stat at level 1.</li>
		 * <li>%d2 = this is the stat at level 2.</li>
		 * </ul></p>
		 *
		 * @param ability the ability this stat value is for
		 * @see FormattedDescriptionBuilder#statValues(StatValue[])
		 * @return a Component rendering a stat value with strikeout and/or charm bonus
		 */
		public Component get(@Nullable S ability, @Nullable Player player, String placeholder, int descriptionLevel) {
			boolean hasSign = false;
			if (placeholder.charAt(0) == '+') {
				hasSign = true;
				placeholder = placeholder.substring(1);
			}

			String formatOption = (placeholder.length() >= 2) ? placeholder.substring(1, 2) : "d";
			String levelOption = (placeholder.length() >= 3) ? placeholder.substring(2, 3) : "0";
			boolean regionScaled = placeholder.length() >= 4 && placeholder.charAt(3) == 'R';

			Function<Double, String> valueFormat = switch (formatOption) {
				case "r" -> value -> StringUtils.to2DP(value) + " Blocks";
				case "p" -> StringUtils::multiplierToPercentageWithSign;
				case "t" -> value -> StringUtils.ticksToSeconds(value) + "s";
				default -> StringUtils::to2DP; // case 'd'
			};

			StatLevel level = switch (descriptionLevel) {
				case 1 -> switch (levelOption) {
					case "1" -> StatLevel.LEVEL_1; // this stat gets upgraded by L2
					case "1e_only" -> StatLevel.LEVEL_1_E; // this stat gets upgraded by ONLY the enhance
					case "1e" -> StatLevel.NOT_ENHANCED_1; // this stat gets upgraded by both L2 and enhance
					default -> StatLevel.ENABLED;
				};
				case 2 -> switch (levelOption) {
					case "1", "1e" -> StatLevel.DISABLED; // this L1 stat should appear disabled since it's replaced by L2
					case "2e" -> StatLevel.NOT_ENHANCED_2; // this L2 stat gets upgraded by the enhance
					default -> StatLevel.LEVEL_2;
				};
				case 3 -> switch (levelOption) {
					case "1e", "2e" -> StatLevel.DISABLED; // this L1/L2 stat should appear disabled since it's replaced by enhance
					default -> StatLevel.ENHANCED;
				};
				default -> StatLevel.ENABLED;
			};

			Component output = Component.text((hasSign ? "+" : "") + valueFormat.apply(mBaseValue.apply(ability, player).doubleValue()));
			if (regionScaled) {
				output = output.style(REGION_SCALED);
			}
			if (ability == null) {
				if (level == StatLevel.DISABLED) {
					return output.style(DISABLED);
				}
				return output;
			} else if (level.mGreyCondition.test(ability)) {
				return output.style(DISABLED);
			} else if (level.mCharmBonusCondition.test(ability)) {
				double diff = mRealValue.apply(ability, player).doubleValue() - mBaseValue.apply(ability, player).doubleValue();
				if (Math.abs(diff) > 0.001) {
					boolean positive = diff >= 0;
					String sign = positive ? "+" : "";
					TextColor color = CharmManager.getCharmEffectColor(positive, mInvertColor);

					if (formatOption.equals("r")) {
						output = Component.text((hasSign ? "+" : "") + StringUtils.to2DP(mBaseValue.apply(ability, player).doubleValue()))
							.append(Component.text(" (" + sign + StringUtils.to2DP(diff) + ") ", color))
							.append(Component.text("Blocks"));
					} else {
						output = output.append(Component.text(" (" + sign + valueFormat.apply(diff) + ")", color));
					}
				}
				return output;
			} else {
				return output;
			}
		}
	}

	/**
	 * Inserts a placeholder for a width-adaptive dashed line.
	 * Will be replaced with the correct width at the final get() method in the builder.
	 * @see FormattedDescriptionBuilder#get(Ability, Player)
	 * @see FormattedDescriptionBuilder#applyDashedLines(Component)
	 */
	public FormattedDescriptionBuilder<T> addDashedLine() {
		return add((a, p) -> Component.text("---"));
	}

	public FormattedDescriptionBuilder<T> addLine() {
		return add((a, p) -> Component.newline().appendSpace());
	}

	public FormattedDescriptionBuilder<T> addLine(String string) {
		return add((a, p) -> {
			Component line = Component.text(" " + string, GREY);

			if (!(string.startsWith("*") && string.endsWith("*"))) {
				line = line.replaceText(builder -> {
					// Style any StatValues in normal text lines as WHITE
					builder.match("(?<!\\()[+-]?%[dprt]\\w*(?![^()]*\\))");
					builder.replacement((matchResult, b) -> b.style(WHITE));
				}).replaceText(builder -> {
					// Parentheticals should be DARK_GREY
					builder.match("\\(.*?\\)");
					builder.replacement((matchResult, b) -> b.style(DARK_GREY));
				});
			}

			return line.appendNewline().appendSpace();
		});
	}

	public FormattedDescriptionBuilder<T> tab() {
		return add((a, p) -> Component.text(" ".repeat(5)));
	}

	public FormattedDescriptionBuilder<T> addListItem(String str) {
		return add((a, p) -> {
			Component value = Component.text(str, WHITE);
			value = applyStatStyling(value);

			return Component.text(" ".repeat(5)).append(Component.text("- ", DARK_GREY)).append(value).appendNewline().appendSpace();
		});
	}

	public FormattedDescriptionBuilder<T> addStat(String str) {
		return add((a, p) -> {
			String[] nameAndValue = str.split(":", 2);

			Component label = Component.text(nameAndValue[0] + ":", GREY);
			Component value = Component.text(nameAndValue.length > 1 ? nameAndValue[1] : "", WHITE);

			value = applyStatStyling(value);

			return Component.text(" ▶ ", getArrowColor()).append(label).append(value).appendNewline().appendSpace();
		});
	}

	public FormattedDescriptionBuilder<T> addStatComparison(String string) {
		return add((a, p) -> {
			// All strings should have structure: "label: oldValue -> newValue"
			String[] parts = string.split(":|->");
			Component label = Component.text(parts[0] + ": ", GREY);
			Component oldValue = Component.text(parts[1].trim(), DISABLED);
			Component arrow = Component.text(" →", GREY);
			Component newValue = Component.text(parts[2], WHITE);

			// We want 2 -> 4 Blocks, not 2 Blocks -> 4 Blocks!
			oldValue = oldValue.replaceText(builder -> {
				builder.match("%r(\\w*)");
				builder.replacement((matchResult, b) -> b.content("%d" + matchResult.group(1)));
			});

			newValue = applyStatStyling(newValue);

			return Component.text(" ▶ ", getArrowColor()).append(label).append(oldValue).append(arrow).append(newValue).appendNewline().appendSpace();
		});
	}

	/**
	 * Applies consistent styling for stats, such as automatically coloring certain prepositions or parentheticals.
	 * @param component the component to style
	 * @return the styled component
	 */
	private static Component applyStatStyling(Component component) {
		return component.replaceText(builder -> {
			// Prepositions like "for" in "20% Slowness for 5s" should be GREY
			builder.match("\\s(for|every|over|of|per|if|to|\\+)\\s|,");
			builder.replacement((matchResult, b) -> b.style(GREY));
		}).replaceText(builder -> {
			// Parentheticals in stats "(max 4 mobs)" should be DARK_GREY
			builder.match("\\(.*?\\)");
			builder.replacement((matchResult, b) -> b.style(DARK_GREY));
		});
	}

	public FormattedDescriptionBuilder<T> addIf(BiPredicate<T, Player> condition, UnaryOperator<FormattedDescriptionBuilder<T>> descApplier) {
		return add((a, p) -> condition.test(a, p) ? descApplier.apply(new FormattedDescriptionBuilder<>(mInfo, mDescriptionLevel)).get(a, p) : Component.empty());
	}

	public FormattedDescriptionBuilder<T> addIfElse(BiPredicate<T, Player> condition, UnaryOperator<FormattedDescriptionBuilder<T>> descIfTrue, UnaryOperator<FormattedDescriptionBuilder<T>> descIfFalse) {
		return add((a, p) -> condition.test(a, p) ? descIfTrue.apply(new FormattedDescriptionBuilder<>(mInfo, mDescriptionLevel)).get(a, p) : descIfFalse.apply(new FormattedDescriptionBuilder<>(mInfo, mDescriptionLevel)).get(a, p));
	}

	public <S extends Ability> FormattedDescriptionBuilder<T> addOtherAbility(Supplier<AbilityInfo<S>> otherInfo, Class<S> otherClass, UnaryOperator<FormattedDescriptionBuilder<S>> descApplier) {
		FormattedDescriptionBuilder<S> otherDescription = descApplier.apply(new FormattedDescriptionBuilder<>(otherInfo, mDescriptionLevel));
		return add((a, p) -> otherDescription.get(Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(p, otherClass), p));
	}

	public FormattedDescriptionBuilder<T> addCharmEffects() {
		AbilityInfo<T> ability = mInfo.get();
		if (ability == null) {
			return this;
		}
		String abilityName = ability.getDisplayName();
		if (abilityName == null) {
			return this;
		}

		return add((a, p) -> {
			if (p == null || !PlayerUtils.hasUnlockedRing(p)) {
				return Component.empty();
			}

			boolean charmsEnabled = ServerProperties.getAbilityEnhancementsEnabled(p);

			Component component = Component.text(" ▶ ", getArrowColor()).append(Component.text("Charm Effects: ", GREY));

			HashSet<Map.Entry<String, Double>> charmEffects = new HashSet<>(CharmManager.getInstance().getSummaryOfAllAttributes(p, CharmManager.CharmType.NORMAL).entrySet());
			charmEffects.removeIf(entry -> !entry.getKey().contains(abilityName));

			// let's sort them in terms of their order of definition
			List<String> effectOrder = Plugin.getInstance().mCharmManager.mCharmEffectList;
			List<Map.Entry<String, Double>> sortedEffects = charmEffects.stream()
				.sorted(Comparator.comparingInt(entry -> effectOrder.indexOf(CharmManager.getPlainEffectName(entry.getKey()))))
				.toList();

			if (sortedEffects.isEmpty()) {
				return component.append(Component.text("None", DARK_GREY)).appendNewline().appendSpace();
			} else {
				if (!charmsEnabled) {
					component = component.append(Component.text("Disabled", RED));
				}
				component = component.appendNewline().appendSpace();
				for (Map.Entry<String, Double> entry : sortedEffects) {
					String effectName = CharmManager.getPlainEffectName(entry.getKey());
					double value = entry.getValue();

					Style style = charmsEnabled ? Style.style(CharmManager.getCharmEffectColor(value > 0, effectName)) : DISABLED;
					Component effect = Component.text((value > 0 ? "+" : "") + StringUtils.to2DP(value) + (entry.getKey().contains("%") ? "% " : " ") + effectName, style);
					Component line = Component.text("      - ", DARK_GREY).append(effect);

					component = component.append(line).appendNewline().appendSpace();
				}
			}

			return component;
		});
	}

	/**
	 * Replaces instances of {%d, %p, %t, ...} in the mostly recently added line with the real stat values.
	 * @param statValues the list of stat values to insert into the component
	 * @see StatValue#get(Ability, Player, String, int)
	 * @return this FormattedDescriptionBuilder, with stat values inserted into the last added line
	 */
	@SafeVarargs
	public final FormattedDescriptionBuilder<T> statValues(StatValue<T>... statValues) {
		if (mDescriptions.isEmpty()) {
			throw new IllegalStateException("No descriptions to apply stat values to!");
		}

		Description<T> lastDescription = mDescriptions.remove(mDescriptions.size() - 1);
		return add((a, p) -> {
			Component component = lastDescription.get();

			for (StatValue<T> statValue : statValues) {
				component = component.replaceText(builder -> {
					builder.match("\\+?%[dprt]\\w*");
					builder.once();
					builder.replacement((matchResult, b) -> statValue.get(a, p, matchResult.group(), mDescriptionLevel));
				});
			}

			return component;
		});
	}

	public FormattedDescriptionBuilder<T> styles(Style... styles) {
		return styles(Arrays.stream(styles).toList());
	}

	/**
	 * Applies styles to sections of the component bounded by asterisks.
	 * The number of styles should correspond to the number of asterisk-bounded sections in the component.
	 * i.e. "Increase *Mana Lance*'s damage."
	 * @param styles the list of styles to apply to sections bounded by asterisks.
	 * @return a Component with styles applied
	 */
	public FormattedDescriptionBuilder<T> styles(List<Style> styles) {
		if (mDescriptions.isEmpty()) {
			throw new IllegalStateException("No descriptions to apply styles to!");
		}

		Description<T> lastDescription = mDescriptions.remove(mDescriptions.size() - 1);
		return add((a, p) -> {
			Component component = lastDescription.get();
			for (Style style : styles) {
				component = component.replaceText(builder -> {
					builder.match("\\*(.*?)\\*");
					builder.once();
					builder.replacement((matchResult, b) -> Component.text(matchResult.group(1), style));
				});
			}
			return component;
		});
	}

	/**
	 * Replaces instances of (m), (p), or (s) in the line component with damage type icons.
	 * (m) - Melee
	 * (p) - Projectile
	 * (s) - Magic
	 * @param component the Component to find and replace in
	 * @return a Component with damage type icons added in
	 */
	private static Component applyDamageTypeIcons(Component component) {
		return component.replaceText(builder -> {
			builder.match("\\((m|p|s|h|m\\/p)\\)");
			builder.replacement((matchResult, b) -> {
				Component icon = switch (matchResult.group(1)) {
					case "m" -> Component.text("🗡", TextColor.color(0xFF5555));
					case "p" -> Component.text("🏹", TextColor.color(0x00AAAA));
					case "s" -> Component.text("⭐", TextColor.color(0xAA00AA));
					case "m/p" -> Component.text("🗡", TextColor.color(0xFF5555)) // for Hunting Companion
						.append(Component.text("/", DARK_GREY))
						.append(Component.text("🏹", TextColor.color(0x00AAAA)));
					default -> Component.empty();
				};
				return Component.text("(", DARK_GREY).append(icon).append(Component.text(")", DARK_GREY));
			});
		});
	}

	private Style getArrowColor() {
		if (mArrowColor != null) {
			return mArrowColor;
		}

		AbilityInfo<T> ability = mInfo.get();

		if (ability == null) {
			return DARK_GREY;
		}

		// Elemental Spirits edge case... because ElementalSpiritsIce isn't technically in Mage's ability list
		if (ability == ElementalSpiritIce.INFO) {
			return MAGE_ARROW;
		}

		for (int i = 0; i < CLASSES.get().size(); i++) {
			PlayerClass playerClass = CLASSES.get().get(i);
			if (playerClass.mAbilities.contains(ability) || playerClass.mPassive == ability
				|| playerClass.mSpecOne.mAbilities.contains(ability) || playerClass.mSpecOne.mPassive == ability
				|| playerClass.mSpecTwo.mAbilities.contains(ability) || playerClass.mSpecTwo.mPassive == ability
				|| playerClass.mUltimate == ability) {
				return ARROW_COLORS.get(i);
			}
		}
		return DARK_GREY;
	}

	/**
	 * Replaces all "---" placeholders with lines of em dashes adapting to the width of the longest line in the description.
	 * @param component the component to replace dashed lines in
	 * @return a component with "---" replaced with horizontal dashed lines
	 * @see FormattedDescriptionBuilder#addDashedLine()
	 */
	public static Component applyDashedLines(Component component) {
		// divide the width of the box by the width of a dash, plus some padding
		int dashesNeeded = (int) Math.ceil((DescriptionUtils.getLongestWidth(component) + 4) / 8d);
		return component.replaceText(builder -> {
			builder.match("---");
			builder.replacement(Component.text("–".repeat(dashesNeeded), BLACK.decorate(TextDecoration.BOLD)));
		});
	}

	@Override
	public FormattedDescriptionBuilder<T> addTrigger() {
		return addTrigger(0);
	}

	@Override
	public FormattedDescriptionBuilder<T> addTrigger(int index) {
		return addTrigger(index, null);
	}

	@Override
	public FormattedDescriptionBuilder<T> addTrigger(int index, @Nullable String extraCondition) {
		return add((a, p) -> {
			AbilityInfo<T> info = mInfo.get();
			if (info == null) {
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

			return customTrigger.getAsFormattedLines(extraCondition).appendNewline().appendSpace();
		});
	}

	public FormattedDescriptionBuilder<T> addCustomTrigger(String trigger) {
		return add((a, p) -> Component.text(" Trigger: ", TRIGGER_LABEL).append(Component.text(trigger, TRIGGER_TEXT)).appendNewline().appendSpace());
	}

	public FormattedDescriptionBuilder<T> addAction(String string, Style style) {
		return add((a, p) -> actionLine(string, style));
	}

	@Override
	public Component get(@Nullable T ability, @Nullable Player player) {
		Component output = Component.empty();
		for (int i = 0; i < mDescriptions.size(); i++) {
			Component component = mDescriptions.get(i).get(ability, player);

			// Apply any final replacements here!
			component = applyDamageTypeIcons(component);

			// add newlines for any dashed lines that aren't the last line
			if (i != mDescriptions.size() - 1) {
				component = component.replaceText(builder -> {
					builder.match("---");
					builder.replacement("---\n ");
				});
			}

			output = output.append(component);
		}

		output = applyDashedLines(output);

		return output;
	}
}
