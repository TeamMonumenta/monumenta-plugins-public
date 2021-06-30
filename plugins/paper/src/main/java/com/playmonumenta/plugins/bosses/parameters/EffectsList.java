package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EffectsList {
	public static class Effect {
		private static final Map<String, EffectRunner> EFFECT_RUNNER;

		static {
			EFFECT_RUNNER = new HashMap<>();
			EFFECT_RUNNER.put("fire", (p, boss, duration, lvl) -> p.setFireTicks((int) duration));
			EFFECT_RUNNER.put("silence", (p, boss, duration, lvl) -> AbilityUtils.silencePlayer(p, (int) duration));
			EFFECT_RUNNER.put("pullforce", (p, boss, duration, lvl) -> MovementUtils.pullTowards(boss, p, duration));
			EFFECT_RUNNER.put("pull", (p, boss, duration, lvl) -> MovementUtils.pullTowardsByUnit(boss, p, duration));
			EFFECT_RUNNER.put("pushforce", (p, boss, duration, lvl) -> MovementUtils.knockAway(boss, p, duration));
			EFFECT_RUNNER.put("push", (p, boss, duration, lvl) -> MovementUtils.knockAwayRealistic(boss.getLocation(), p, duration, 0.5f));
		}

		PotionEffectType mEffect;
		String mName;
		int mLevel;
		float mDuration;

		public Effect(String name, float duration, int level) {
			mName = name;
			mDuration = duration;
			mLevel = level;
			mEffect = null;
		}

		public Effect(PotionEffectType effect, float duration, int level) {
			mName = effect.getName();
			mDuration = duration;
			mLevel = level;
			mEffect = effect;
		}

		public Effect(String name, float duration) {
			this(name, duration, 0);
		}

		public Effect(PotionEffectType effect, float duration) {
			this(effect, duration, 0);
		}

		public void apply(Player p, LivingEntity boss) {
			if (mEffect != null) {
				p.addPotionEffect(new PotionEffect(mEffect, (int) mDuration, mLevel, true, false));
			} else {
				EFFECT_RUNNER.get(mName).apply(p, boss, mDuration, mLevel);
			}
		}

		@Override
		public String toString() {
			return "(" + mName + "," + mDuration + "," + mLevel + ")";
		}

		public static Effect fromString(String value) throws Exception {
			if (value.startsWith("(")) {
				value = value.substring(1);
			}
			if (value.endsWith(")")) {
				value = value.substring(0, value.length() - 1);
			}
			String[] split = value.split(",");
			PotionEffectType effect = PotionEffectType.getByName(split[0].toUpperCase());

			if (effect != null) {
				if (split.length == 3) {
					return new Effect(effect, Float.valueOf(split[1]), Integer.parseInt(split[2]));
				} else if (split.length == 2) {
					return new Effect(effect, Float.valueOf(split[1]));
				} else {
					throw new IllegalFormatException("Fail loading custom sound. Object of size " + split.length);
				}
			} else {
				if (EFFECT_RUNNER.keySet().contains(split[0])) {
					if (split.length == 3) {
						return new Effect(split[0], Float.valueOf(split[1]), Integer.parseInt(split[2]));
					} else if (split.length == 2) {
						return new Effect(split[0], Float.valueOf(split[1]));
					} else {
						throw new IllegalFormatException("Fail loading custom sound. Object of size " + split.length);
					}
				} else {
					throw new EffectNotFoundException(split[0]);
				}
			}
		}

		@FunctionalInterface
		private interface EffectRunner {
			void apply(Player p, LivingEntity boss, float duration, int lvl);
		}
	}

	private List<Effect> mEffectList;
	public static final EffectsList EMPTY = EffectsList.fromString("");

	public EffectsList(String values) {
		mEffectList = new ArrayList<>();
		List<String> split = BossUtils.splitByCommasUsingBrackets(values);

		for (String stringSplitted : split) {
			try {
				mEffectList.add(Effect.fromString(stringSplitted));
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to parse '" + stringSplitted + "': " + e.getMessage());
			}
		}

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
		return new EffectsList(string.replace(" ", ""));
	}

	private static class EffectNotFoundException extends RuntimeException {
		EffectNotFoundException(String value) {
			super("Effect don't found for argument: " + value);
		}
	}

	private static class IllegalFormatException extends RuntimeException {
		public IllegalFormatException(String value) {
			super(value);
		}
	}
}
