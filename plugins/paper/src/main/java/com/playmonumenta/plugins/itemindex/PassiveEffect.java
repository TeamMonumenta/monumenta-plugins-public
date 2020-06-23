package com.playmonumenta.plugins.itemindex;

import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

public enum PassiveEffect {

	// positive effects
	ABSORPTION(false, "Absorption", PotionEffectType.ABSORPTION),
	CONDUIT_POWER(false, "Conduit Power", PotionEffectType.CONDUIT_POWER),
	RESISTANCE(false, "Resistance", PotionEffectType.DAMAGE_RESISTANCE),
	DOLPHINS_GRACE(false, "Dolphin's Grace", PotionEffectType.DOLPHINS_GRACE),
	REGENERATION(false, "Regeneration", PotionEffectType.REGENERATION),
	STRENGTH(false, "Strength", PotionEffectType.INCREASE_DAMAGE),
	HASTE(false, "Haste", PotionEffectType.FAST_DIGGING),
	FIRE_RESISTANCE(false, "Fire Resistance", PotionEffectType.FIRE_RESISTANCE),
	GLOWING(false, "Glowing", PotionEffectType.GLOWING),
	HEALTH_BOOST(false, "Health boost", PotionEffectType.HEALTH_BOOST),
	INVISIBILITY(false, "Invisibility", PotionEffectType.INVISIBILITY),
	JUMP(false, "Jump boost", PotionEffectType.JUMP),
	NIGHT_VISION(false, "Night Vision", PotionEffectType.NIGHT_VISION),
	SATURATION(false, "Saturation", PotionEffectType.SATURATION),
	SLOW_FALLING(false, "Slow Falling", PotionEffectType.SLOW_FALLING),
	SPEED(false, "Speed", PotionEffectType.SPEED),
	WATER_BREATHING(false, "Water Breathing", PotionEffectType.WATER_BREATHING),

	// negative effects
	BLINDNESS(true, "Blindness", PotionEffectType.BLINDNESS),
	HUNGER(true, "Hunger", PotionEffectType.HUNGER),
	NAUSEA(true, "Nausea", PotionEffectType.CONFUSION),
	POISON(true, "Poison", PotionEffectType.POISON),
	LEVITATION(true, "Levitation", PotionEffectType.LEVITATION),
	SLOWNESS(true, "Slowness", PotionEffectType.SLOW),
	MINING_FATIGUE(true, "Mining Fatigue", PotionEffectType.SLOW_DIGGING),
	WEAKNESS(true, "Weakness", PotionEffectType.WEAKNESS),
	WITHER(true, "Wither", PotionEffectType.WITHER);

	private String mReadableStr;
	private boolean mIsNegative;
	private PotionEffectType mBukkitEffect;

	PassiveEffect(boolean neg, String s, PotionEffectType bukkitEffect) {
		this.mIsNegative = neg;
		this.mReadableStr = s;
		this.mBukkitEffect = bukkitEffect;
	}

	boolean isNegative() {
		return this.mIsNegative;
	}

	PotionEffectType getBukkitEffect() {
		return this.mBukkitEffect;
	}

	boolean isCustom() {
		return this.mBukkitEffect == null;
	}

	String getReadableStr() {
		return this.mReadableStr;
	}

	static String[] valuesAsStringArray() {
		ArrayList<String> out = new ArrayList<>();
		for (PassiveEffect e : PassiveEffect.values()) {
			out.add(e.toString());
		}
		return out.toArray(new String[0]);
	}
}
