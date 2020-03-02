package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

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

	// This map only notes any "useful" effect pairs, i.e. effects that would be non-annoying and balanced to invert
	private static final Map<PotionEffectType, PotionEffectType> OPPOSITE_EFFECTS = new HashMap<PotionEffectType, PotionEffectType>();

	static {
		OPPOSITE_EFFECTS.put(PotionEffectType.SPEED, PotionEffectType.SLOW);
		OPPOSITE_EFFECTS.put(PotionEffectType.SLOW, PotionEffectType.SPEED);
		OPPOSITE_EFFECTS.put(PotionEffectType.FAST_DIGGING, PotionEffectType.SLOW_DIGGING);
		OPPOSITE_EFFECTS.put(PotionEffectType.SLOW_DIGGING, PotionEffectType.FAST_DIGGING);
		OPPOSITE_EFFECTS.put(PotionEffectType.REGENERATION, PotionEffectType.WITHER);
		OPPOSITE_EFFECTS.put(PotionEffectType.WITHER, PotionEffectType.REGENERATION);
		OPPOSITE_EFFECTS.put(PotionEffectType.POISON, PotionEffectType.REGENERATION);
		OPPOSITE_EFFECTS.put(PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.UNLUCK);
		OPPOSITE_EFFECTS.put(PotionEffectType.UNLUCK, PotionEffectType.DAMAGE_RESISTANCE);
		OPPOSITE_EFFECTS.put(PotionEffectType.INCREASE_DAMAGE, PotionEffectType.WEAKNESS);
		OPPOSITE_EFFECTS.put(PotionEffectType.WEAKNESS, PotionEffectType.INCREASE_DAMAGE);
	}

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

		public PotionEffectType mType;
		public int mDuration;
		public int mAmplifier;
		public boolean mAmbient;
		public boolean mShowParticles;

		public PotionInfo() {
		}

		public PotionInfo(PotionEffect effect) {
			mType = effect.getType();
			mDuration = effect.getDuration();
			mAmplifier = effect.getAmplifier();
			mAmbient = effect.isAmbient();
			mShowParticles = effect.hasParticles();
		}

		public PotionInfo(PotionEffectType type, int duration, int amplifier, boolean ambient,
		                  boolean showParticles) {
			mType = type;
			mDuration = duration;
			mAmplifier = amplifier;
			mAmbient = ambient;
			mShowParticles = showParticles;

		}

		public JsonObject getAsJsonObject() {
			JsonObject potionInfoObject = new JsonObject();

			potionInfoObject.addProperty("type", mType.getName());
			potionInfoObject.addProperty("duration", mDuration);
			potionInfoObject.addProperty("amplifier", mAmplifier);
			potionInfoObject.addProperty("ambient", mAmbient);
			potionInfoObject.addProperty("show_particles", mShowParticles);

			return potionInfoObject;
		}

		public void loadFromJsonObject(JsonObject object) throws Exception {
			mType = PotionEffectType.getByName(object.get("type").getAsString());
			mDuration = object.get("duration").getAsInt();
			mAmplifier = object.get("amplifier").getAsInt();
			mAmbient = object.get("ambient").getAsBoolean();
			mShowParticles = object.get("show_particles").getAsBoolean();
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
					newInfo = new PotionInfo(type.getEffectType(), SECONDS_22_HALF / dividend, 1, false, true);
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

	public static void apply(LivingEntity entity, PotionInfo info) {
		entity.addPotionEffect(new PotionEffect(info.mType, info.mDuration, info.mAmplifier, info.mAmbient, info.mShowParticles));
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
				PotionEffect effect = new PotionEffect(info.mType, info.mDuration, info.mAmplifier, info.mAmbient,
				                                       info.mShowParticles);
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

	public static boolean hasNegativeEffects(PotionEffectType type) {
		String name = type.getName();
		for (PotionEffectType testType : NEGATIVE_EFFECTS) {
			if (name.equals(testType.getName())) {
				return true;
			}
		}

		return false;
	}

	public static void clearNegatives(Plugin plugin, Player player) {
		for (PotionEffectType type : NEGATIVE_EFFECTS) {
			if (player.hasPotionEffect(type)) {
				PotionEffect effect = player.getPotionEffect(type);
				if (effect.getDuration() < Constants.THIRTY_MINUTES) {
					plugin.mPotionManager.removePotion(player, PotionID.ALL, type);
				}
			}
		}
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

	public static void applyPotion(Entity applier, LivingEntity applied, PotionEffect effect) {
		if (applied.hasPotionEffect(effect.getType())) {
			if (applied.getPotionEffect(effect.getType()).getAmplifier() < effect.getAmplifier()
			    || applied.getPotionEffect(effect.getType()).getAmplifier() == effect.getAmplifier()
			    && applied.getPotionEffect(effect.getType()).getDuration() < effect.getDuration()) {
				PotionEffectApplyEvent event = new PotionEffectApplyEvent(applier, applied, effect);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					applied.addPotionEffect(event.getEffect(), true);
				}
			}
		} else {
			PotionEffectApplyEvent event = new PotionEffectApplyEvent(applier, applied, effect);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				applied.addPotionEffect(event.getEffect());
			}
		}
	}

	public static void applyPotion(Plugin plugin, Player player, PotionMeta meta) {
		//Do not run if null to avoid NullPointerException
		if (meta == null) {
			return;
		} else if (meta.hasCustomEffects()) {
			for (PotionEffect effect : meta.getCustomEffects()) {
				if (effect.getType() != null) {
					if (effect.getType().equals(PotionEffectType.HARM)) {
						//If 10+, kill, if below, deal normal instant damage
						//If instant healing, manually add health
						if (effect.getAmplifier() >= 9) {
							player.setHealth(0);
						} else {
							EntityUtils.damageEntity(plugin, player, 3 * Math.pow(2, effect.getAmplifier() + 1), null);
						}
					} else if (effect.getType().equals(PotionEffectType.HEAL)) {
						PlayerUtils.healPlayer(player, 2 * Math.pow(2, effect.getAmplifier() + 1));
					}
				}

				plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
			}
		} else {
			PotionInfo info = PotionUtils.getPotionInfo(meta.getBasePotionData(), 1);

			//If instant healing, manually add health, otherwise if instant damage, manually remove health, else add effect
			//Check then add health
			if (info != null && info.mType.equals(PotionEffectType.HEAL)) {
				PlayerUtils.healPlayer(player, 2 * Math.pow(2, info.mAmplifier + 1));
			} else if (info != null && info.mType.equals(PotionEffectType.HARM)) {
				EntityUtils.damageEntity(plugin, player, 3 * Math.pow(2, info.mAmplifier + 1), null);
			} else {
				plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
			}
		}
	}

	public static List<PotionEffectType> getNegativeEffects(LivingEntity le) {
		List<PotionEffectType> types = new ArrayList<PotionEffectType>();
		List<PotionEffectType> negatives = Arrays.asList(NEGATIVE_EFFECTS);
		for (PotionEffect effect : le.getActivePotionEffects()) {
			if (negatives.contains(effect.getType())) {
				types.add(effect.getType());
			}
		}
		return types;
	}

	public static PotionEffectType getOppositeEffect(PotionEffectType type) {
		return OPPOSITE_EFFECTS.get(type);
	}

	public static boolean isLuckPotion(PotionMeta meta) {
		boolean isLuckPotion = false;
		if (meta.getBasePotionData().getType().equals(PotionType.LUCK)) {
			isLuckPotion = true;
		}

		for (PotionEffect effect : meta.getCustomEffects()) {
			if (effect.getType().equals(PotionEffectType.LUCK)) {
				isLuckPotion = true;
				break;
			}
		}

		return isLuckPotion;
	}
}
