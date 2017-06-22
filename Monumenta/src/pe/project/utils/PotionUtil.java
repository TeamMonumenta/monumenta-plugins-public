package pe.project.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class PotionUtil {
	private static final int SECONDS_1 = 20;
	private static final int SECONDS_22_HALF = (int)(22.5 * SECONDS_1);
	private static final int SECONDS_30 = 30 * SECONDS_1;
	private static final int SECONDS_45 = 45 * SECONDS_1;
	private static final int MINUTES_1 = 60 * SECONDS_1;
	private static final int MINUTES_1_HALF = MINUTES_1 + SECONDS_30;
	private static final int MINUTES_3 = MINUTES_1 * 3;
	private static final int MINUTES_8 = MINUTES_1 * 8;
	
	public static class PotionInfo {
		public static final PotionInfo HEALING = new PotionInfo(PotionEffectType.HEAL, 0, 0);
		public static final PotionInfo HEALING_STRONG = new PotionInfo(PotionEffectType.HEAL, 0, 1);
		
		public static final PotionInfo REGENERATION = new PotionInfo(PotionEffectType.REGENERATION, SECONDS_45, 0);
		public static final PotionInfo REGENERATION_LONG = new PotionInfo(PotionEffectType.REGENERATION, MINUTES_1_HALF, 0);
		public static final PotionInfo REGENERATION_STRONG = new PotionInfo(PotionEffectType.REGENERATION, SECONDS_22_HALF, 1);
		
		public static final PotionInfo SWIFTNESS = new PotionInfo(PotionEffectType.SPEED, MINUTES_3, 0);
		public static final PotionInfo SWIFTNESS_LONG = new PotionInfo(PotionEffectType.SPEED, MINUTES_8, 0);
		public static final PotionInfo SWIFTNESS_STRONG = new PotionInfo(PotionEffectType.SPEED, MINUTES_1_HALF, 1);
		
		public static final PotionInfo STRENGTH = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_3, 0);
		public static final PotionInfo STRENGTH_LONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_8, 0);
		public static final PotionInfo STRENGTH_STRONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_1_HALF, 1);
		
		public static final PotionInfo LEAPING = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_3, 0);
		public static final PotionInfo LEAPING_LONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_8, 0);
		public static final PotionInfo LEAPING_STRONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_1_HALF, 1);
		
		public PotionInfo(PotionEffectType _type, int _duration, int _amplifier) {
			type = _type;
			duration = _duration;
			amplifier = _amplifier;
		}
		
		public PotionEffectType type;
		public int duration;
		public int amplifier;
	}
	
	public static PotionInfo getPotionInfo(PotionData data) {
		PotionType type = data.getType();
		boolean isExtended = data.isExtended();
		boolean isUpgraded = data.isUpgraded();
		
		if (type == PotionType.INSTANT_HEAL) {
			if (isUpgraded) {
				return PotionInfo.HEALING_STRONG;
			} else {
				return PotionInfo.HEALING;
			}
		} else if (type == PotionType.REGEN) {
			if (isExtended) {
				return PotionInfo.REGENERATION_LONG;
			} else if (isUpgraded) {
				return PotionInfo.REGENERATION_STRONG;
			} else {
				return PotionInfo.REGENERATION;
			}
		} else if (type == PotionType.SPEED) {
			if (isExtended) {
				return PotionInfo.SWIFTNESS_LONG;
			} else if (isUpgraded) {
				return PotionInfo.SWIFTNESS_STRONG;
			} else {
				return PotionInfo.SWIFTNESS;
			}
		} else if (type == PotionType.STRENGTH) {
			if (isExtended) {
				return PotionInfo.STRENGTH_LONG;
			} else if (isUpgraded) {
				return PotionInfo.STRENGTH_STRONG;
			} else {
				return PotionInfo.STRENGTH;
			}
		} else if (type == PotionType.JUMP) {
			if (isExtended) {
				return PotionInfo.LEAPING_LONG;
			} else if (isUpgraded) {
				return PotionInfo.LEAPING_STRONG;
			} else {
				return PotionInfo.LEAPING;
			}
		}
		
		return null;
	}
	
	public static List<PotionEffect> getEffects(PotionMeta meta) {
		List<PotionEffect> effectsList = new ArrayList<PotionEffect>();
		
		PotionData data = meta.getBasePotionData();
		if (data != null) {
			PotionUtil.PotionInfo info = PotionUtil.getPotionInfo(data);
			if (info != null) {
				PotionEffect effect = new PotionEffect(info.type, info.duration, info.amplifier);
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
	
	public static void applyPotion(LivingEntity entity, PotionEffect effect) {
		if (effect.getType().getName() == PotionEffectType.HEAL.getName()) {
			double health = entity.getHealth();
			double healthToAdd = 4 * (effect.getAmplifier()+1);
			
			health = Math.min(health + healthToAdd, 20);
			
			entity.setHealth(health);
		} else {
			entity.addPotionEffect(effect);
		}
	}
}
