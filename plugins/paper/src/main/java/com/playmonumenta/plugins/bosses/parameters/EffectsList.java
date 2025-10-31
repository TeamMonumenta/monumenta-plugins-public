package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public record EffectsList(List<Effect> mEffectList) {
	public abstract static class Effect {
		public static final Map<String, EffectRunner> EFFECT_RUNNER;
		public static final Map<String, CustomEffectRunner> CUSTOM_EFFECT_RUNNER;

		public static final Set<String> EFFECT_IDENTIFIERS;

		protected Effect(String name) {
			mName = name;
		}

		static {
			EFFECT_RUNNER = new HashMap<>();
			EFFECT_RUNNER.put("fire", (p, boss, duration) -> EntityUtils.applyFire(Plugin.getInstance(), (int) duration, p, boss));
			EFFECT_RUNNER.put("silence",
				(target, boss, duration) ->
					EffectType.applyEffect(EffectType.SILENCE, target, (int) duration, 1, null, false)
			);
			EFFECT_RUNNER.put("pullforce", (p, boss, duration) -> MovementUtils.pullTowards(boss, p, duration));
			EFFECT_RUNNER.put("pull", (p, boss, duration) -> MovementUtils.pullTowardsByUnit(boss, p, duration));
			EFFECT_RUNNER.put("pushforce", (p, boss, duration) -> MovementUtils.knockAway(boss, p, duration, false));
			EFFECT_RUNNER.put("push", (p, boss, duration) -> MovementUtils.knockAwayRealistic(boss.getLocation(), p, duration, 0.5f, true));
			EFFECT_RUNNER.put("InstantHealthPercent", (target, boss, strength) -> {
				EffectType.applyEffect(EffectType.INSTANT_HEALTH, target, 1, strength, "EffectListInstantHealthPercent", false);
			});
			EFFECT_RUNNER.put("InstantDamagePercent", (target, boss, strength) -> {
				EffectType.applyEffect(EffectType.INSTANT_DAMAGE, target, 1, strength, "EffectListInstantDamagePercent", false);
			});


			CUSTOM_EFFECT_RUNNER = new HashMap<>();
			// not a vanilla potion effect or custom implemented effect
			for (EffectType effectType : EffectType.values()) {
				if (effectType.getPotionEffectType() == null && !EnumSet.of(
					EffectType.INSTANT_HEALTH,
					EffectType.INSTANT_DAMAGE
				).contains(effectType)) {

					String type = effectType.getType();
					if (effectType.isConstant()) {
						EFFECT_RUNNER.put("Custom" + type,
							(target, boss, duration) ->
								EffectType.applyEffect(effectType, target, (int) duration, 1, null, false)
						);
					} else {// for some reason DamageIncrease is called 'damage' here?
						CUSTOM_EFFECT_RUNNER.put("Custom" + (type.equals("damage") ? "DamageIncrease" : type),
							(target, boss, duration, strength, effectName) ->
								EffectType.applyEffect(effectType, target, duration, strength, effectName, false)
						);
					}
				}
			}
			// Aliases
			CUSTOM_EFFECT_RUNNER.put("CustomDamageDecrease", Objects.requireNonNull(CUSTOM_EFFECT_RUNNER.get("CustomWeakness")));

			HashSet<String> hashSet = new HashSet<>(EFFECT_RUNNER.keySet());
			hashSet.addAll(CUSTOM_EFFECT_RUNNER.keySet());
			for (PotionEffectType potionEffectType : Registry.POTION_EFFECT_TYPE) {
				hashSet.add(potionEffectType.getKey().getKey());
			}
			EFFECT_IDENTIFIERS = hashSet;
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
			void apply(LivingEntity p, LivingEntity boss, int duration, float strength, @Nullable String customEffectName);
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
			return String.format("(%s,%d,%f,%s)", mName, mDuration, mAmplifier, mSource);
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


	public static final EffectsList EMPTY = fromString("[]");

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

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Builder() {

		}

		List<Effect> mEffects = new ArrayList<>();

		public Builder add(Effect effect) {
			mEffects.add(effect);
			return this;
		}

		public EffectsList build() {
			return new EffectsList(mEffects);
		}
	}
}
