package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev.jorel.commandapi.Tooltip;

public class EffectsList {
	public static class Effect {
		public static final Map<String, EffectRunner> EFFECT_RUNNER;

		static {
			EFFECT_RUNNER = new HashMap<>();
			EFFECT_RUNNER.put("fire", (p, boss, duration) -> p.setFireTicks((int) duration));
			EFFECT_RUNNER.put("silence", (p, boss, duration) -> AbilityUtils.silencePlayer(p, (int) duration));
			EFFECT_RUNNER.put("pullforce", (p, boss, duration) -> MovementUtils.pullTowards(boss, p, duration));
			EFFECT_RUNNER.put("pull", (p, boss, duration) -> MovementUtils.pullTowardsByUnit(boss, p, duration));
			EFFECT_RUNNER.put("pushforce", (p, boss, duration) -> MovementUtils.knockAway(boss, p, duration));
			EFFECT_RUNNER.put("push", (p, boss, duration) -> MovementUtils.knockAwayRealistic(boss.getLocation(), p, duration, 0.5f));
		}

		// Common to both potion effects and custom effects
		private final String mName;

		// Only for potion effects
		private final PotionEffectType mEffect;
		private final int mAmplifier;
		private final int mDurationTicks;

		// Only for custom effects
		private final float mCustomEffectStrength;

		// This constructor for custom effects
		public Effect(String name, float effectStrength) {
			mName = name;
			mCustomEffectStrength = effectStrength;

			mDurationTicks = 0;
			mAmplifier = 0;
			mEffect = null;
		}

		// This constructor for potion effects
		public Effect(PotionEffectType effect, int duration, int amplifier) {
			mName = effect.getName();
			mDurationTicks = duration;
			mAmplifier = amplifier;
			mEffect = effect;

			mCustomEffectStrength = 0;
		}

		// This constructor for potion effects with default amplifier (0)
		public Effect(PotionEffectType effect, int duration) {
			this(effect, duration, 0);
		}

		public void apply(Player p, LivingEntity boss) {
			if (mEffect != null) {
				p.addPotionEffect(new PotionEffect(mEffect, mDurationTicks, mAmplifier, true, false));
			} else {
				EFFECT_RUNNER.get(mName).apply(p, boss, mCustomEffectStrength);
			}
		}

		@Override
		public String toString() {
			if (mEffect != null) {
				return "(" + mName + "," + mDurationTicks + "," + mAmplifier + ")";
			} else {
				return "(" + mName + "," + mCustomEffectStrength + ")";
			}
		}

		@FunctionalInterface
		private interface EffectRunner {
			void apply(Player p, LivingEntity boss, float duration);
		}
	}

	private final List<Effect> mEffectList;
	public static final EffectsList EMPTY = EffectsList.fromString("");

	private EffectsList(List<Effect> effects) {
		mEffectList = effects;

	}

	public void apply(Player player, LivingEntity boss) {
		//if the list is empty the for will be skipped
		for (Effect effect : mEffectList) {
			effect.apply(player, boss);
		}
	}

	@Override
	public String toString() {
		String msg = "[";
		for (Effect effect : mEffectList) {
			msg = msg + effect.toString() + ",";
		}
		//remove last comma
		if (msg.endsWith(",")) {
			msg = msg.substring(0, msg.length() - 1);
		}
		return msg + "]";
	}

	public static EffectsList fromString(String string) {
		ParseResult<EffectsList> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as EffectsList");
			Thread.dumpStack();
			return new EffectsList(new ArrayList<>(0));
		}

		return result.getResult();
	}

	/*
	 * Parses an EffectsList at the next position in the StringReader.
	 * If this item parses successfully:
	 *   The returned ParseResult will contain a non-null getResult() and a null getTooltip()
	 *   The reader will be advanced to the next character past this EffectsList value.
	 * Else:
	 *   The returned ParseResult will contain a null getResult() and a non-null getTooltip()
	 *   The reader will not be advanced
	 */
	public static ParseResult<EffectsList> fromReader(StringReader reader, String hoverDescription) {
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "[", hoverDescription)));
		}

		List<Effect> effectsList = new ArrayList<>(4);

		boolean atLeastOneEffectIter = false;
		while (true) {
			// Start trying to parse the next individual effect entry in the list

			if (reader.advance("]")) {
				// Got closing bracket and parsed rest successfully - complete effect list, break this loop
				break;
			}

			if (atLeastOneEffectIter) {
				if (!reader.advance(",")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)
					));
				}
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "(", hoverDescription)));
				}
			} else {
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + "(", hoverDescription),
						Tooltip.of(reader.readSoFar() + "]", hoverDescription)
					));
				}
			}

			atLeastOneEffectIter = true;

			final boolean foundPotionEffect;
			PotionEffectType effect = reader.readPotionEffectType();
			String customEffect = null;
			if (effect == null) {
				// Failed to read potion effect type - maybe it's a specialty effect instead?
				customEffect = reader.readOneOf(EffectsList.Effect.EFFECT_RUNNER.keySet());
				if (customEffect == null) {
					// Nope, neither matched
					List<Tooltip<String>> suggArgs = new ArrayList<>(PotionEffectType.values().length + EffectsList.Effect.EFFECT_RUNNER.size());
					String soFar = reader.readSoFar();
					for (String valid : EffectsList.Effect.EFFECT_RUNNER.keySet()) {
						suggArgs.add(Tooltip.of(soFar + valid, hoverDescription));
					}
					for (PotionEffectType valid : PotionEffectType.values()) {
						suggArgs.add(Tooltip.of(soFar + valid.getName(), hoverDescription));
					}
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				}
				foundPotionEffect = false;
			} else {
				foundPotionEffect = true;
			}

			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", hoverDescription)));
			}

			Long durationInTicks = 0L;
			Double effectStrength = 0d;
			if (foundPotionEffect) {
				durationInTicks = reader.readLong();
				if (durationInTicks == null || durationInTicks <= 0) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "20", "Duration in ticks > 0")));
				}
			} else {
				effectStrength = reader.readDouble();
				if (effectStrength == null) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1.0", "Custom effect strength")));
				}
			}

			if (!foundPotionEffect || !reader.advance(",")) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.of(reader.readSoFar() + ",", "Specify amplifier"),
						Tooltip.of(reader.readSoFar() + ")", "Use 0 as amplifier")
					));
				}
				// End of this effect, loop to next
				if (foundPotionEffect) {
					effectsList.add(new Effect(effect, durationInTicks.intValue()));
				} else {
					effectsList.add(new Effect(customEffect, effectStrength.floatValue()));
				}
				continue;
			}

			// Only foundPotionEffect = true possible after this point
			// Amplifier only relevant to potion effect, not custom effects

			Long amplifier = reader.readLong();
			if (amplifier == null || amplifier < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "0", "Effect amplifier, starting at 0 for first level")));
			}

			if (!reader.advance(")")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
			}

			// End of this effect, loop to next
			effectsList.add(new Effect(effect, durationInTicks.intValue(), amplifier.intValue()));
		}

		return ParseResult.of(new EffectsList(effectsList));
	}
}
