package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class PotionUtils {
	private static final int SECONDS_1 = 20;
	private static final int SECONDS_22_HALF = (int)(22.5 * SECONDS_1);
	private static final int SECONDS_30 = 30 * SECONDS_1;
	private static final int SECONDS_45 = 45 * SECONDS_1;
	private static final int MINUTES_1 = 60 * SECONDS_1;
	private static final int MINUTES_1_HALF = MINUTES_1 + SECONDS_30;
	private static final int MINUTES_3 = MINUTES_1 * 3;
	private static final int MINUTES_5 = MINUTES_1 * 5;
	private static final int MINUTES_8 = MINUTES_1 * 8;

	private static final PotionEffectType[] POSITIVE_EFFECTS = new PotionEffectType[] {
	    PotionEffectType.ABSORPTION,
	    PotionEffectType.DAMAGE_RESISTANCE,
	    PotionEffectType.FAST_DIGGING,
	    PotionEffectType.FIRE_RESISTANCE,
	    PotionEffectType.HEAL,
	    PotionEffectType.HEALTH_BOOST,
	    PotionEffectType.INCREASE_DAMAGE,
	    PotionEffectType.INVISIBILITY,
	    PotionEffectType.JUMP,
	    PotionEffectType.NIGHT_VISION,
	    PotionEffectType.REGENERATION,
	    PotionEffectType.SATURATION,
	    PotionEffectType.SPEED,
	    PotionEffectType.LUCK,
	    PotionEffectType.WATER_BREATHING
	};

	private static final PotionEffectType[] NEGATIVE_EFFECTS = new PotionEffectType[] {
	    PotionEffectType.BLINDNESS,
	    PotionEffectType.POISON,
	    PotionEffectType.CONFUSION,
	    PotionEffectType.SLOW,
	    PotionEffectType.SLOW_DIGGING,
	    PotionEffectType.WITHER,
	    PotionEffectType.WEAKNESS,
	    PotionEffectType.HARM,
	    PotionEffectType.HUNGER,
	    PotionEffectType.LEVITATION,
	    PotionEffectType.UNLUCK
	};

	public static class PotionInfo {
		public static final PotionInfo HEALING = new PotionInfo(PotionEffectType.HEAL, 0, 0, false, true);
		public static final PotionInfo HEALING_STRONG = new PotionInfo(PotionEffectType.HEAL, 0, 1, false, true);

		public static final PotionInfo REGENERATION = new PotionInfo(PotionEffectType.REGENERATION, SECONDS_45, 0, false, true);
		public static final PotionInfo REGENERATION_LONG = new PotionInfo(PotionEffectType.REGENERATION, MINUTES_1_HALF, 0, false, true);
		public static final PotionInfo REGENERATION_STRONG = new PotionInfo(PotionEffectType.REGENERATION, SECONDS_22_HALF, 1, false, true);

		public static final PotionInfo SWIFTNESS = new PotionInfo(PotionEffectType.SPEED, MINUTES_3, 0, false, true);
		public static final PotionInfo SWIFTNESS_LONG = new PotionInfo(PotionEffectType.SPEED, MINUTES_8, 0, false, true);
		public static final PotionInfo SWIFTNESS_STRONG = new PotionInfo(PotionEffectType.SPEED, MINUTES_1_HALF, 1, false, true);

		public static final PotionInfo STRENGTH = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_3, 0, false, true);
		public static final PotionInfo STRENGTH_LONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_8, 0, false, true);
		public static final PotionInfo STRENGTH_STRONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_1_HALF, 1, false, true);

		public static final PotionInfo LEAPING = new PotionInfo(PotionEffectType.JUMP, MINUTES_3, 0, false, true);
		public static final PotionInfo LEAPING_LONG = new PotionInfo(PotionEffectType.JUMP, MINUTES_8, 0, false, true);
		public static final PotionInfo LEAPING_STRONG = new PotionInfo(PotionEffectType.JUMP, MINUTES_1_HALF, 1, false, true);

		public static final PotionInfo NIGHT_VISION = new PotionInfo(PotionEffectType.NIGHT_VISION, MINUTES_3, 0, false, true);
		public static final PotionInfo NIGHT_VISION_LONG = new PotionInfo(PotionEffectType.NIGHT_VISION, MINUTES_8, 0, false, true);

		public static final PotionInfo FIRE_RESISTANCE = new PotionInfo(PotionEffectType.FIRE_RESISTANCE, MINUTES_3, 0, false, true);
		public static final PotionInfo FIRE_RESISTANCE_LONG = new PotionInfo(PotionEffectType.FIRE_RESISTANCE, MINUTES_8, 0, false, true);

		public static final PotionInfo LUCK = new PotionInfo(PotionEffectType.LUCK, MINUTES_5, 0, false, true);

		public PotionInfo() {
		}

		public PotionInfo(PotionEffect effect) {
			type = effect.getType();
			duration = effect.getDuration();
			amplifier = effect.getAmplifier();
			ambient = effect.isAmbient();
			showParticles = effect.hasParticles();
		}

		public PotionInfo(PotionEffectType _type, int _duration, int _amplifier, boolean _ambient,
		                  boolean _showParticles) {
			type = _type;
			duration = _duration;
			amplifier = _amplifier;
			ambient = _ambient;
			showParticles = _showParticles;
		}

		public PotionEffectType type;
		public int duration;
		public int amplifier;
		public boolean ambient;
		public boolean showParticles;

		public JsonObject getAsJsonObject() {
			JsonObject potionInfoObject = new JsonObject();

			potionInfoObject.addProperty("type", type.getName());
			potionInfoObject.addProperty("duration", duration);
			potionInfoObject.addProperty("amplifier", amplifier);
			potionInfoObject.addProperty("ambient", ambient);
			potionInfoObject.addProperty("show_particles", showParticles);

			return potionInfoObject;
		}

		public void loadFromJsonObject(JsonObject object) throws Exception {
			type = PotionEffectType.getByName(object.get("type").getAsString());
			duration = object.get("duration").getAsInt();
			amplifier = object.get("amplifier").getAsInt();
			ambient = object.get("ambient").getAsBoolean();
			showParticles = object.get("show_particles").getAsBoolean();
		}
	}

	/*
	 * Dividend should be 1 for drink/splash, 4 for lingering potions, 8 for tipped arrows
	 * NOTE: This may return NULL for some broken potions!
	 */
	public static PotionInfo getPotionInfo(PotionData data, int dividend) {
		PotionInfo newInfo = null;
		PotionType type = data.getType();
		boolean isExtended = data.isExtended();
		boolean isUpgraded = data.isUpgraded();

		/* Some bugged potion types don't actually have types... */
		if (type == null || type.getEffectType() == null) {
			return null;
		}

		if (type.isInstant()) {
			if (isUpgraded) {
				newInfo = new PotionInfo(type.getEffectType(), 0, 1, false, true);
			} else {
				newInfo = new PotionInfo(type.getEffectType(), 0, 0, false, true);
			}
		} else {
			if (type == PotionType.REGEN || type == PotionType.POISON) {
				if (isExtended) {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_1_HALF / dividend, 0, false, true);
				} else if (isUpgraded) {
					newInfo = new PotionInfo(type.getEffectType(), SECONDS_22_HALF / dividend, 0, false, true);
				} else {
					newInfo = new PotionInfo(type.getEffectType(), SECONDS_45 / dividend, 0, false, true);
				}
			} else if (type == PotionType.LUCK) {
				newInfo = new PotionInfo(type.getEffectType(), MINUTES_5 / dividend, 0, false, true);
			} else {
				if (isExtended) {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_8 / dividend, 0, false, true);
				} else if (isUpgraded) {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_1_HALF / dividend, 1, false, true);
				} else {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_3 / dividend, 0, false, true);
				}
			}
		}

		return newInfo;
	}

	public static List<PotionEffect> getEffects(ItemStack item) {
		if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta) {
			return getEffects((PotionMeta)item.getItemMeta());
		}
		// Not a potion - return an empty list to simplify callers iterating the result
		return new ArrayList<PotionEffect>(0);
	}

	public static List<PotionEffect> getEffects(PotionMeta meta) {
		List<PotionEffect> effectsList = new ArrayList<PotionEffect>();

		PotionData data = meta.getBasePotionData();
		if (data != null) {
			PotionUtils.PotionInfo info = PotionUtils.getPotionInfo(data, 1);
			if (info != null) {
				PotionEffect effect = new PotionEffect(info.type, info.duration, info.amplifier, info.ambient,
				                                       info.showParticles);
				effectsList.add(effect);
			}
		}

		if (meta.hasCustomEffects()) {
			List<PotionEffect> effects = meta.getCustomEffects();
			for (PotionEffect effect : effects) {
				effectsList.add(effect);
			}
		}

		return effectsList;
	}

	public static void applyPotion(Plugin plugin, Player player, PotionEffect effect) {
		if (effect.getType().equals(PotionEffectType.HEAL)) {
			double health = player.getHealth();
			double healthToAdd = 4 * (effect.getAmplifier() + 1);

			health = Math.min(health + healthToAdd, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

			player.setHealth(health);
		} else {
			plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
		}
	}

	public static boolean hasPositiveEffects(Collection<PotionEffect> effects) {
		for (PotionEffect effect : effects) {
			if (hasPositiveEffects(effect.getType())) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasPositiveEffects(PotionEffectType type) {
		String name = type.getName();
		for (PotionEffectType testType : POSITIVE_EFFECTS) {
			if (name.equals(testType.getName())) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasNegativeEffects(ItemStack potionItem) {
		for (PotionEffect effect : getEffects(potionItem)) {
			if (hasNegativeEffects(effect.getType())) {
				if (effect.getDuration() > 30 || effect.getAmplifier() == 0) {
					// The negative effect lasts longer than 1s or is only level 1
					// Probably not an antidote / other "good" negative potion
					return true;
				}
			}
		}

		return false;
	}

	public static void clearNegatives(Plugin plugin, Player player) {
		for (PotionEffectType type : NEGATIVE_EFFECTS) {
			plugin.mPotionManager.removePotion(player, PotionID.ALL, type);
		}
	}

	public static boolean hasNegativeEffects(PotionEffectType type) {
		String name = type.getName();
		for (PotionEffectType testType : NEGATIVE_EFFECTS) {
			if (name.equals(testType.getName())) {
				return true;
			}
		}

		return false;
	}
}
