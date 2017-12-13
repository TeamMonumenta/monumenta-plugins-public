package pe.project.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.google.gson.JsonObject;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

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

		public PotionInfo(PotionEffectType _type, int _duration, int _amplifier, boolean ambient, boolean hasParticles) {
			type = _type;
			duration = _duration;
			amplifier = _amplifier;
		}

		public PotionEffectType type;
		public int duration;
		public int amplifier;
		public boolean ambient;
		public boolean showParticles;

		public JsonObject getAsJsonObject(boolean ignoreType) {
			JsonObject potionInfoObject = new JsonObject();

			if (!ignoreType) {
				potionInfoObject.addProperty("type", type.getName());
			}

			potionInfoObject.addProperty("duration", duration);
			potionInfoObject.addProperty("amplifier", amplifier);
			potionInfoObject.addProperty("ambient", ambient);
			potionInfoObject.addProperty("show_particles", showParticles);

			return potionInfoObject;
		}

		public void loadFromJsonObject(JsonObject object, PotionEffectType potionType) {
			if (object.has("type")) {
				type = PotionEffectType.getByName(object.get("type").getAsString());
			} else {
				type = potionType;
			}

			duration = object.get("duration").getAsInt();
			amplifier = object.get("amplifier").getAsInt();
			ambient = object.get("ambient").getAsBoolean();
			showParticles = object.get("show_particles").getAsBoolean();
		}
	}

	public static PotionInfo getPotionInfo(PotionData data) {
		PotionInfo newInfo = null;
		PotionType type = data.getType();
		boolean isExtended = data.isExtended();
		boolean isUpgraded = data.isUpgraded();

		if (type == PotionType.INSTANT_HEAL) {
			if (isUpgraded) {
				newInfo = PotionInfo.HEALING_STRONG;
			} else {
				newInfo = PotionInfo.HEALING;
			}
		} else if (type == PotionType.REGEN) {
			if (isExtended) {
				newInfo = PotionInfo.REGENERATION_LONG;
			} else if (isUpgraded) {
				newInfo = PotionInfo.REGENERATION_STRONG;
			} else {
				newInfo = PotionInfo.REGENERATION;
			}
		} else if (type == PotionType.SPEED) {
			if (isExtended) {
				newInfo = PotionInfo.SWIFTNESS_LONG;
			} else if (isUpgraded) {
				newInfo = PotionInfo.SWIFTNESS_STRONG;
			} else {
				newInfo = PotionInfo.SWIFTNESS;
			}
		} else if (type == PotionType.STRENGTH) {
			if (isExtended) {
				newInfo = PotionInfo.STRENGTH_LONG;
			} else if (isUpgraded) {
				newInfo = PotionInfo.STRENGTH_STRONG;
			} else {
				newInfo = PotionInfo.STRENGTH;
			}
		} else if (type == PotionType.JUMP) {
			if (isExtended) {
				newInfo = PotionInfo.LEAPING_LONG;
			} else if (isUpgraded) {
				newInfo = PotionInfo.LEAPING_STRONG;
			} else {
				newInfo = PotionInfo.LEAPING;
			}
		} else if (type == PotionType.NIGHT_VISION) {
			if (isExtended) {
				newInfo = PotionInfo.NIGHT_VISION_LONG;
			} else {
				newInfo = PotionInfo.NIGHT_VISION;
			}
		} else if (type == PotionType.FIRE_RESISTANCE) {
			if (isExtended) {
				newInfo = PotionInfo.FIRE_RESISTANCE_LONG;
			} else {
				newInfo = PotionInfo.FIRE_RESISTANCE;
			}
		} else if (type == PotionType.LUCK) {
			newInfo = PotionInfo.LUCK;
		}

		if (newInfo != null) {
			return new PotionInfo(newInfo.type, newInfo.duration, newInfo.amplifier, newInfo.ambient, newInfo.showParticles);
		} else {
			return null;
		}
	}

	public static List<PotionEffect> getEffects(PotionMeta meta) {
		List<PotionEffect> effectsList = new ArrayList<PotionEffect>();

		PotionData data = meta.getBasePotionData();
		if (data != null) {
			PotionUtils.PotionInfo info = PotionUtils.getPotionInfo(data);
			if (info != null) {
				PotionEffect effect = new PotionEffect(info.type, info.duration, info.amplifier, info.ambient, info.showParticles);
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
		if (effect.getType().getName() == PotionEffectType.HEAL.getName()) {
			double health = player.getHealth();
			double healthToAdd = 4 * (effect.getAmplifier()+1);

			health = Math.min(health + healthToAdd, 20);

			player.setHealth(health);
		} else {
			plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
		}
	}

	public static boolean hasPositiveEffects(Collection<PotionEffect> effects) {
		for (PotionEffect effect: effects) {
			PotionEffectType type = effect.getType();
			String name = type.getName();
			if (name.equals(PotionEffectType.ABSORPTION.getName())
				|| name.equals(PotionEffectType.DAMAGE_RESISTANCE.getName())
				|| name.equals(PotionEffectType.FAST_DIGGING.getName())
				|| name.equals(PotionEffectType.FIRE_RESISTANCE.getName())
				|| name.equals(PotionEffectType.HEAL.getName())
				|| name.equals(PotionEffectType.HEALTH_BOOST.getName())
				|| name.equals(PotionEffectType.INCREASE_DAMAGE.getName())
				|| name.equals(PotionEffectType.INVISIBILITY.getName())
				|| name.equals(PotionEffectType.JUMP.getName())
				|| name.equals(PotionEffectType.LEVITATION.getName())
				|| name.equals(PotionEffectType.LUCK.getName())
				|| name.equals(PotionEffectType.NIGHT_VISION.getName())
				|| name.equals(PotionEffectType.REGENERATION.getName())
				|| name.equals(PotionEffectType.SATURATION.getName())
				|| name.equals(PotionEffectType.SPEED.getName())
				|| name.equals(PotionEffectType.WATER_BREATHING.getName())) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasNegativeEffects(Collection<PotionEffect> effects) {
		for (PotionEffect effect: effects) {
			PotionEffectType type = effect.getType();
			String name = type.getName();
			if (name.equals(PotionEffectType.BLINDNESS.getName())
				|| name.equals(PotionEffectType.CONFUSION.getName())
				|| name.equals(PotionEffectType.HARM.getName())
				|| name.equals(PotionEffectType.HUNGER.getName())
				|| name.equals(PotionEffectType.POISON.getName())
				|| name.equals(PotionEffectType.SLOW.getName())
				|| name.equals(PotionEffectType.SLOW_DIGGING.getName())
				|| name.equals(PotionEffectType.UNLUCK.getName())
				|| name.equals(PotionEffectType.WEAKNESS.getName())
				|| name.equals(PotionEffectType.WITHER.getName())) {
				return true;
			}
		}

		return false;
	}
}
