package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.CustomLogger;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.EnumSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

public enum EffectType {
	//when ever a new EffectType get added here remember to add it also in the switch inside applyEffect(...)

	// type: is the unique key save inside the nbt of the item
	// name: is the name that the player will see on the item -> format:  +/-dd% name (x:yy)
	SPEED("speed", "Speed"),
	SLOW("slow", "Speed"),

	//Resistance type of effects
	RESISTANCE("Resistance", "Resistance"),
	MELEE_RESISTANCE("MeleeResistance", "Melee Resistance"),
	PROJECTILE_RESISTANCE("ProjectileResistance", "Projectile Resistance"),
	MAGIC_RESISTANCE("MagicResistance", "Magic Resistance"),
	BLAST_RESISTANCE("BlastResistance", "Blast Resistance"),
	FIRE_RESISTANCE("FireResistance", "Fire Resistance"),
	FALL_RESISTANCE("FallResistance", "Fall Resistance"),


	//VULNERABILITY type of effects
	VULNERABILITY("Vulnerability", "Resistance"),
	MELEE_VULNERABILITY("MeleeVulnerability", "Melee Resistance"),
	PROJECTILE_VULNERABILITY("ProjectileVulnerability", "Projectile Resistance"),
	MAGIC_VULNERABILITY("MagicVulnerability", "Magic Resistance"),
	BLAST_VULNERABILITY("BlastVulnerability", "Blast Resistance"),
	FIRE_VULNERABILITY("FireVulnerability", "Fire Resistance"),
	FALL_VULNERABILITY("FallVulnerability", "Fall Resistance"),


	//Damage type of effects
	DAMAGE("damage", "Strength"),
	MAGIC_DAMAGE("MagicDamage", "Magic Damage"),
	MELEE_DAMAGE("MeleeDamage", "Melee Damage"),
	PROJECTILE_DAMAGE("ProjectileDamage", "Projectile Damage"),


	//Weakness type of effects
	WEAKNESS("Weakness", "Weakness"),
	MAGIC_WEAKNESS("MagicWeakness", "Magic Damage"),
	MELEE_WEAKNESS("MeleeWeakness", "Melee Damage"),
	PROJECTILE_WEAKNESS("ProjectileWeakness", "Projectile Damage"),

	HEAL("Heal", "Healing"),
	ANTI_HEAL("AntiHeal", "Healing");

	public static final String KEY = "Effects";

	public static final Set<EffectType> POSITIVE_EFFECTS = EnumSet.of(SPEED, HEAL);
	public static final Set<EffectType> NEGATIVE_EFFECTS = EnumSet.of(SLOW, ANTI_HEAL);

	static {
		for (EffectType effectType : values()) {
			if (effectType.name().contains("RESISTANCE") || effectType.name().contains("DAMAGE")) {
				POSITIVE_EFFECTS.add(effectType);
			} else if (effectType.name().contains("VULNERABILITY") || effectType.name().contains("WEAKNESS")) {
				NEGATIVE_EFFECTS.add(effectType);
			}
		}
	}

	private final String mType;
	private final String mName;

	EffectType(String type, String name) {
		mType = type;
		mName = name;
	}

	public String getType() {
		return mType;
	}

	public static EffectType fromType(String type) {
		for (EffectType effectType : values()) {
			if (effectType.mType.equals(type)) {
				return effectType;
			}
		}
		return null;
	}

	public static Component getComponent(@Nullable EffectType effectType, double strength, int duration) {
		if (effectType == null) {
			return Component.empty();
		}

		int minutes = duration / 1200;
		int seconds = (duration / 20) % 60;
		String timeString = "(" + minutes + ":" + (seconds > 9 ? seconds : "0" + seconds) + ")";

		if (POSITIVE_EFFECTS.contains(effectType)) {
			return Component.text("+" + (int) (strength * 100) + "% " + effectType.mName + " ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).append(
				Component.text(timeString)
			);
		}

		if (NEGATIVE_EFFECTS.contains(effectType)) {
			return Component.text("-" + (int) (strength * 100) + "% " + effectType.mName + " ", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false).append(
				Component.text(timeString)
			);
		}

		return Component.text((int) (strength * 100) + "% " + effectType.mName + " " + timeString, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
	}

	public static void applyEffect(@Nullable EffectType effectType, Entity entity, int duration, double strength, @Nullable String source) {
		if (effectType == null) {
			return;
		}

		String sourceString = (source != null ? effectType.mName + source : effectType.mName);

		switch (effectType) {
			case SPEED -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentSpeed(duration, strength, sourceString));
			case SLOW -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentSpeed(duration, -strength, sourceString));

			case RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength));
			case MELEE_RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.MELEE)));
			case PROJECTILE_RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case BLAST_RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.BLAST)));
			case FIRE_RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FIRE)));
			case FALL_RESISTANCE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FALL)));

			case VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength));
			case MELEE_VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.MELEE)));
			case PROJECTILE_VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case BLAST_VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.BLAST)));
			case FIRE_VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FIRE)));
			case FALL_VULNERABILITY -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FALL)));

			case DAMAGE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength));
			case PROJECTILE_DAMAGE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_DAMAGE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case MELEE_DAMAGE -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.MELEE)));

			case WEAKNESS -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength));
			case PROJECTILE_WEAKNESS -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_WEAKNESS -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case MELEE_WEAKNESS -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.MELEE)));

			case HEAL -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentHeal(duration, strength));
			case ANTI_HEAL -> Plugin.getInstance().mEffectManager.addEffect(entity, sourceString, new PercentHeal(duration, -strength));

			default -> CustomLogger.getInstance().warning("No EffectType implemented in applyEffect(..) for: " + effectType.mType);

		}
	}


	//this function is used from PotionConsumeListener.appliedPotionEvent(..) -> convert some vanilla effects to MM custom
	//----------------------------Currently not used--------------PotionConsumeListener.appliedPotionEvent(...) ROW 354
	public static boolean convertPotionEffect(EntityPotionEffectEvent event) {
		PotionEffect effect = event.getNewEffect();
		if (effect == null) {
			return false;
		}

		switch (effect.getType().getName()) {
			case "SPEED" -> applyEffect(SPEED, event.getEntity(), effect.getDuration(), (effect.getAmplifier() + 1) * 0.2, null);
			case "SLOW" -> applyEffect(SLOW, event.getEntity(), effect.getDuration(), (effect.getAmplifier() + 1) * 0.15, null);
			case "INCREASE_DAMAGE" -> applyEffect(DAMAGE, event.getEntity(), effect.getDuration(), (effect.getAmplifier() + 1) * 0.15, null); //TODO check numbers
			//case "DAMAGE_RESISTANCE" -> applyEffect(RESISTANCE, event.getEntity(), effect.getDuration(), (effect.getAmplifier() + 1) * 0.2, null); //TODO - in a lot of place we check for Resistance 5

			default -> {
				return false;
			}
		}

		return true;
	}


}