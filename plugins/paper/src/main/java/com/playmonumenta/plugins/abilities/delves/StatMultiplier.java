package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class StatMultiplier extends Ability {

	private static final String HEALTH_MODIFIER_NAME = "DelvesHealthModifier";
	private static final String SPEED_MODIFIER_NAME = "DelvesSpeedModifier";
	private static final String AVOID_MODIFIERS = "boss_delveimmune";

	private final double mDamageTakenMultiplier;
	private final double mAbilityDamageTakenMultiplier;

	private final double mMobHealthMultiplier;
	private final double mMobSpeedMultiplier;

	private final String[] mMobAbilityPool;
	private final double mMobAbilityChance;

	public StatMultiplier(Plugin plugin, Player player,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier, double mobHealthMultiplier) {
		this(plugin, player,
				damageTakenMultiplier, abilityDamageTakenMultiplier, mobHealthMultiplier,
				1, null, 0);
	}

	public StatMultiplier(Plugin plugin, Player player,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobHealthMultiplier, double mobSpeedMultiplier) {
		this(plugin, player,
				damageTakenMultiplier, abilityDamageTakenMultiplier, mobHealthMultiplier, mobSpeedMultiplier,
				null, 0);
	}

	public StatMultiplier(Plugin plugin, Player player,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobHealthMultiplier, String[] mobAbilitiesPool, double mobAbilitiesChance) {
		this(plugin, player,
				damageTakenMultiplier, abilityDamageTakenMultiplier, mobHealthMultiplier, 1,
				mobAbilitiesPool, mobAbilitiesChance);
	}

	public StatMultiplier(Plugin plugin, Player player,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobHealthMultiplier, double mobSpeedMultiplier, String[] mobAbilityPool, double mobAbilityChance) {
		super(plugin, player, null);
		mInfo.mIgnoreTriggerCap = true;

		mDamageTakenMultiplier = damageTakenMultiplier;
		mAbilityDamageTakenMultiplier = abilityDamageTakenMultiplier;

		mMobHealthMultiplier = mobHealthMultiplier;
		mMobSpeedMultiplier = mobSpeedMultiplier;

		mMobAbilityPool = mobAbilityPool;
		mMobAbilityChance = mobAbilityChance;
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.CUSTOM && (event.getDamager().getScoreboardTags() == null || !event.getDamager().getScoreboardTags().contains(AVOID_MODIFIERS))) {
			event.setDamage(EntityUtils.getDamageApproximation(event, mAbilityDamageTakenMultiplier));
		} else if (event.getDamager().getScoreboardTags() == null || !event.getDamager().getScoreboardTags().contains(AVOID_MODIFIERS)) {
			event.setDamage(EntityUtils.getDamageApproximation(event, mDamageTakenMultiplier));
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		if (event.getDamager().getScoreboardTags() == null || !event.getDamager().getScoreboardTags().contains(AVOID_MODIFIERS)) {
			event.setDamage(EntityUtils.getDamageApproximation(event, mDamageTakenMultiplier));
		}

		return true;
	}

	public void applyOnSpawnModifiers(LivingEntity mob) {
		// Good candidate for property detection since both the speed and ability modifiers have health modifiers
		boolean hasProperties = false;
		AttributeInstance health = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		for (AttributeModifier mod : health.getModifiers()) {
			if (mod != null && mod.getName().equals(HEALTH_MODIFIER_NAME)) {
				hasProperties = true;
				break;
			}
		}

		if (!hasProperties && (mob.getScoreboardTags() == null || !mob.getScoreboardTags().contains(AVOID_MODIFIERS))) {
			// Health
			EntityUtils.addAttribute(mob, Attribute.GENERIC_MAX_HEALTH,
			                         new AttributeModifier(HEALTH_MODIFIER_NAME, mMobHealthMultiplier - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
			// Scale the mobs health up to their maximum health
			mob.setHealth(Math.min(health.getValue(), mob.getHealth() * mMobHealthMultiplier));

			// Speed
			EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED,
			                         new AttributeModifier(SPEED_MODIFIER_NAME, mMobSpeedMultiplier - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));

			// Abilities
			if (FastUtils.RANDOM.nextDouble() < mMobAbilityChance) {
				// This runs prior to BossManager parsing, so we can just add tags directly
				mob.addScoreboardTag("boss_blastresist");
				mob.addScoreboardTag(mMobAbilityPool[FastUtils.RANDOM.nextInt(mMobAbilityPool.length)]);
			}
		}
	}

}
