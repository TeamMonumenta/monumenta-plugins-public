package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class EffectsList {
	public abstract static class Effect {
		public static final Map<String, EffectRunner> EFFECT_RUNNER;
		public static final Map<String, CustomEffectRunner> CUSTOM_EFFECT_RUNNER;

		protected Effect(String name) {
			mName = name;
		}

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
			EFFECT_RUNNER.put("InstantHealthPercent", (target, boss, strength) -> {
				EffectType.applyEffect(EffectType.INSTANT_HEALTH, target, 1, strength, "EffectListInstantHealthPercent", false);
			});

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

		public final String mName;

		abstract void apply(LivingEntity p, LivingEntity boss);

		@Override
		public String toString() {
			return String.format("(%s)", mName);
		}

		@FunctionalInterface
		public interface EffectRunner {
			void apply(LivingEntity p, LivingEntity boss, float duration);
		}

		@FunctionalInterface
		public interface CustomEffectRunner {
			void apply(LivingEntity p, LivingEntity boss, float duration, float strength, @Nullable String customEffectName);
		}
	}

	public static final class CustomSingleArgumentEffect extends Effect {
		private final int mDuration;
		private final float mAmplifier;
		@Nullable
		private final String mSource;

		public CustomSingleArgumentEffect(String effectIdentifier, int duration, float amplifier, @Nullable String source) {
			super(effectIdentifier);

			mDuration = duration;
			mAmplifier = amplifier;
			mSource = source;
		}

		@Override
		void apply(LivingEntity p, LivingEntity boss) {
			CustomEffectRunner cRunner = CUSTOM_EFFECT_RUNNER.get(mName);
			if (cRunner != null) {
				cRunner.apply(p, boss, mDuration, mAmplifier, mSource);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s,%d,%f,%s)", mName, mDuration, mAmplifier,mSource);
		}
	}

	public static final class CustomEffect extends Effect {
		private final float mAmplifier;

		public CustomEffect(String effectIdentifier, float amplifier) {
			super(effectIdentifier);

			mAmplifier = amplifier;
		}

		@Override
		void apply(LivingEntity p, LivingEntity boss) {
			EffectRunner runner = EFFECT_RUNNER.get(mName);
			if (runner != null) {
				runner.apply(p, boss, mAmplifier);
			}
		}
	}

	public static final class VanillaEffect extends Effect {
		private final PotionEffectType mEffect;
		private final int mDuration;
		private final int mAmplifier;

		public VanillaEffect(PotionEffectType effect, int duration, int amplifier) {
			super(effect.key().value());

			mEffect = effect;
			mDuration = duration;
			mAmplifier = amplifier;
		}

		@Override
		void apply(LivingEntity p, LivingEntity boss) {
			if (p instanceof Player player) {
				Plugin.getInstance().mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(mEffect, mDuration, mAmplifier, true, false, true));
			} else {
				p.addPotionEffect(new PotionEffect(mEffect, mDuration, mAmplifier, true, false, true));
			}
		}

		@Override
		public String toString() {
			return String.format("(%s,%d)", mName, mAmplifier);
		}
	}


	public final List<Effect> mEffectList;
	public static final EffectsList EMPTY = fromString("[]");

	public EffectsList(List<Effect> effects) {
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
		return Parser.parseOrDefault(Parser.getParserMethod(EffectsList.class), string, EMPTY);
	}
}
