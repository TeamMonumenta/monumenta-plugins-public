package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectsList {
	public static class Effect {
		public static final Map<String, EffectRunner> EFFECT_RUNNER;
		public static final Map<String, CustomEffectRunner> CUSTOM_EFFECT_RUNNER;

		static {
			EFFECT_RUNNER = new HashMap<>();
			EFFECT_RUNNER.put("fire", (p, boss, duration) -> EntityUtils.applyFire(Plugin.getInstance(), (int) duration, p, boss));
			EFFECT_RUNNER.put("silence", (p, boss, duration) -> {
				if (p instanceof Player player) {
					AbilityUtils.silencePlayer(player, (int) duration);
				} else {
					EntityUtils.applySilence(Plugin.getInstance(), (int) duration, p);
				}
			});
			EFFECT_RUNNER.put("pullforce", (p, boss, duration) -> MovementUtils.pullTowards(boss, p, duration));
			EFFECT_RUNNER.put("pull", (p, boss, duration) -> MovementUtils.pullTowardsByUnit(boss, p, duration));
			EFFECT_RUNNER.put("pushforce", (p, boss, duration) -> MovementUtils.knockAway(boss, p, duration, false));
			EFFECT_RUNNER.put("push", (p, boss, duration) -> MovementUtils.knockAwayRealistic(boss.getLocation(), p, duration, 0.5f, true));

			CUSTOM_EFFECT_RUNNER = new HashMap<>();
			CUSTOM_EFFECT_RUNNER.put("CustomSpeed", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentSpeed((int) duration, strength, effectName));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomSpeed", new PercentSpeed((int) duration, strength, "EffectListCustomSpeed"));
					}
				}
			});
			CUSTOM_EFFECT_RUNNER.put("CustomSlow", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentSpeed((int) duration, -strength, effectName));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomSlow", new PercentSpeed((int) duration, -strength, "EffectListCustomSlow"));
					}
				}
			});

			CUSTOM_EFFECT_RUNNER.put("CustomHeal", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentHeal((int) duration, strength));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomHeal", new PercentHeal((int) duration, strength));
					}
				}
			});

			CUSTOM_EFFECT_RUNNER.put("CustomAntiHeal", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						target.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, (int) duration, (int) (strength/0.1), true, false, true));
						EffectManager.getInstance().addEffect(target, effectName, new PercentHeal((int) duration, -strength));
					} else {
						target.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, (int) duration, (int) (strength/0.1), true, false, true));
						EffectManager.getInstance().addEffect(target, "EffectListCustomAntiHeal", new PercentHeal((int) duration, -strength));
					}
				}
			});

			CUSTOM_EFFECT_RUNNER.put("CustomResistance", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentDamageReceived((int) duration, -strength));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomResistance", new PercentDamageReceived((int) duration, -strength));
					}
				}
			});

			CUSTOM_EFFECT_RUNNER.put("CustomVulnerability", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentDamageReceived((int) duration, strength));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomVulnerability", new PercentDamageReceived((int) duration, strength));
					}
				}
			});

			CUSTOM_EFFECT_RUNNER.put("CustomDamageIncrease", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentDamageDealt((int) duration, strength));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomDamageIncrease", new PercentDamageDealt((int) duration, strength));
					}
				}
			});

			CUSTOM_EFFECT_RUNNER.put("CustomDamageDecrease", (target, boss, duration, strength, effectName) -> {
				if (EffectManager.getInstance() != null && strength > 0) {
					if (effectName != null) {
						EffectManager.getInstance().addEffect(target, effectName, new PercentDamageDealt((int) duration, -strength));
					} else {
						EffectManager.getInstance().addEffect(target, "EffectListCustomDamageIncrease", new PercentDamageDealt((int) duration, -strength));
					}
				}
			});







		}

		// Common to both potion effects and custom effects
		private final String mName;

		// Only for potion effects
		private final @Nullable PotionEffectType mEffect;
		private final int mAmplifier;
		private final int mDurationTicks;

		// Only for custom effects
		private final float mCustomEffectStrength;
		private final float mCustomEffectDuration;
		private final String mCustomEffectSource;

		// This constructor for custom effects used in EFFECT_RUNNER
		public Effect(String name, float effectStrength) {
			mName = name;
			mCustomEffectStrength = effectStrength;

			mCustomEffectDuration = 0;
			mCustomEffectSource = null;

			mDurationTicks = 0;
			mAmplifier = 0;
			mEffect = null;
		}

		// This constructor for custom effects used in CUSTOM_EFFECT_RUNNER
		public Effect(String name, float effectDuration, float effectStrength, String effectSource) {
			mName = name;
			mCustomEffectDuration = effectDuration;
			mCustomEffectStrength = effectStrength;
			mCustomEffectSource = effectSource;

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

			mCustomEffectDuration = 0;
			mCustomEffectStrength = 0;
			mCustomEffectSource = null;

		}

		// This constructor for potion effects with default amplifier (0)
		public Effect(PotionEffectType effect, int duration) {
			this(effect, duration, 0);
		}

		public void apply(LivingEntity p, LivingEntity boss) {
			if (mEffect != null) {
				if (p instanceof Player player) {
					Plugin.getInstance().mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(mEffect, mDurationTicks, mAmplifier, true, false, true));
				} else {
					p.addPotionEffect(new PotionEffect(mEffect, mDurationTicks, mAmplifier, true, false, true));
				}
			} else {
				EffectRunner runner = EFFECT_RUNNER.get(mName);
				if (runner != null) {
					runner.apply(p, boss, mCustomEffectStrength);
				}

				CustomEffectRunner cRunner = CUSTOM_EFFECT_RUNNER.get(mName);
				if (cRunner != null) {
					cRunner.apply(p, boss, mCustomEffectDuration, mCustomEffectStrength, mCustomEffectSource);
				}
			}
		}

		@Override
		public String toString() {
			if (mEffect != null) {
				return "(" + mName + "," + mDurationTicks + "," + mAmplifier + ")";
			} else {
				if (EFFECT_RUNNER.get(mName) != null) {
					return "(" + mName + "," + mCustomEffectStrength + ")";
				}

				if (CUSTOM_EFFECT_RUNNER.get(mName) != null) {
					return "(" + mName + "," + mCustomEffectDuration + "," + mCustomEffectStrength + (mCustomEffectSource != null ? ",\"" + mCustomEffectSource + "\")" : ")");
				}

				return "INVALID";
			}
		}

		@FunctionalInterface
		private interface EffectRunner {
			void apply(LivingEntity p, LivingEntity boss, float duration);
		}

		@FunctionalInterface
		private interface CustomEffectRunner {
			void apply(LivingEntity p, LivingEntity boss, float duration, float strength, String customEffectName);
		}
	}

	private final List<Effect> mEffectList;
	public static final EffectsList EMPTY = EffectsList.fromString("[]");

	private EffectsList(List<Effect> effects) {
		mEffectList = effects;

	}

	public void apply(LivingEntity player, LivingEntity boss) {
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
			final boolean foundCustomPotionEffect;
			PotionEffectType effect = reader.readPotionEffectType();
			String customEffect = null;
			if (effect == null) {
				// Failed to read potion effect type - maybe it's a specialty effect instead?
				customEffect = reader.readOneOf(Effect.EFFECT_RUNNER.keySet());
				if (customEffect == null) {
					customEffect = reader.readOneOf(Effect.CUSTOM_EFFECT_RUNNER.keySet());
				}
				if (customEffect == null) {
					// Nope, neither matched
					List<Tooltip<String>> suggArgs = new ArrayList<>(PotionEffectType.values().length + Effect.EFFECT_RUNNER.size() + Effect.CUSTOM_EFFECT_RUNNER.size());
					String soFar = reader.readSoFar();
					for (String valid : Effect.EFFECT_RUNNER.keySet()) {
						suggArgs.add(Tooltip.of(soFar + valid, hoverDescription));
					}
					for (String valid : Effect.CUSTOM_EFFECT_RUNNER.keySet()) {
						suggArgs.add(Tooltip.of(soFar + valid, hoverDescription));
					}
					for (PotionEffectType valid : PotionEffectType.values()) {
						suggArgs.add(Tooltip.of(soFar + valid.getName(), hoverDescription));
					}
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				}
				foundCustomPotionEffect = Effect.CUSTOM_EFFECT_RUNNER.get(customEffect) != null;
				foundPotionEffect = false;
			} else {
				foundPotionEffect = true;
				foundCustomPotionEffect = false;
			}

			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ",", hoverDescription)));
			}

			Long durationInTicks = 0L;
			Double effectStrength = 0d;
			if (foundPotionEffect || foundCustomPotionEffect) {
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

			if ((!foundPotionEffect && !foundCustomPotionEffect) || !reader.advance(",")) {
				if (!reader.advance(")")) {
					if (foundCustomPotionEffect) {
						return ParseResult.of(Tooltip.arrayOf(
							Tooltip.of(reader.readSoFar() + ",", "Specify strength")));
					} else {
						return ParseResult.of(Tooltip.arrayOf(
							Tooltip.of(reader.readSoFar() + ",", "Specify amplifier"),
							Tooltip.of(reader.readSoFar() + ")", "Use 0 as amplifier")
						));
					}
				}
				// End of this effect, loop to next
				if (foundPotionEffect) {
					effectsList.add(new Effect(effect, durationInTicks.intValue()));
				} else {
					//effect from effectRunner
					effectsList.add(new Effect(customEffect, effectStrength.floatValue()));
				}
				continue;
			}

			// Only foundPotionEffect = true possible after this point
			// Amplifier only relevant to potion effect, not custom effects

			Double amplifier = reader.readDouble();
			if (amplifier == null) {
				if (foundCustomPotionEffect) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "1.0", "Effect strength percentage")));
				} else {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "0", "Effect amplifier, starting at 0 for first level")));
				}
			}

			if (!reader.advance(",")) {
				if (!reader.advance(")")) {
					if (foundCustomPotionEffect) {
						return ParseResult.of(Tooltip.arrayOf(
							Tooltip.of(reader.readSoFar() + ",", "Specify source"),
							Tooltip.of(reader.readSoFar() + ")", "Use default source")
						));
					} else {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
					}
				}

				if (foundCustomPotionEffect) {
					effectsList.add(new Effect(customEffect, durationInTicks.floatValue(), amplifier.floatValue(), null));
				} else {
					effectsList.add(new Effect(effect, durationInTicks.intValue(), amplifier.intValue()));
				}
				continue;
			}

			String source = reader.readString();
			if (source == null) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + "\"EffectsList" + customEffect + "\"", "Effect source")));
			}

			if (!reader.advance(")")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.of(reader.readSoFar() + ")", hoverDescription)));
			}


			// End of this effect, loop to next
			effectsList.add(new Effect(customEffect, durationInTicks.intValue(), amplifier.floatValue(), source));
		}

		return ParseResult.of(new EffectsList(effectsList));
	}
}
