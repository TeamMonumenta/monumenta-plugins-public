package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class StatMultiplier extends Ability {

	private static final String MESSAGED_PLAYER_METAKEY = "StatMultiplierMessagedPlayerMetakey";
	private static final String HEALTH_MODIFIER_NAME = "DelvesHealthModifier";
	private static final String SPEED_MODIFIER_NAME = "DelvesSpeedModifier";

	private final double mDamageTakenMultiplier;
	private final double mAbilityDamageTakenMultiplier;

	private final double mMobHealthMultiplier;
	private final double mMobSpeedMultiplier;

	private final String[] mMobAbilityPool;
	private final double mMobAbilityChance;

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier, double mobHealthMultiplier) {
		this(plugin, world, player, message,
				damageTakenMultiplier, abilityDamageTakenMultiplier, mobHealthMultiplier,
				1, null, 0);
	}

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobHealthMultiplier, double mobSpeedMultiplier) {
		this(plugin, world, player, message,
				damageTakenMultiplier, abilityDamageTakenMultiplier, mobHealthMultiplier, mobSpeedMultiplier,
				null, 0);
	}

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobHealthMultiplier, String[] mobAbilitiesPool, double mobAbilitiesChance) {
		this(plugin, world, player, message,
				damageTakenMultiplier, abilityDamageTakenMultiplier, mobHealthMultiplier, 1,
				mobAbilitiesPool, mobAbilitiesChance);
	}

	public StatMultiplier(Plugin plugin, World world, Player player, String message,
			double damageTakenMultiplier, double abilityDamageTakenMultiplier,
			double mobHealthMultiplier, double mobSpeedMultiplier, String[] mobAbilityPool, double mobAbilityChance) {
		super(plugin, world, player, null);
		mInfo.mIgnoreTriggerCap = true;

		mDamageTakenMultiplier = damageTakenMultiplier;
		mAbilityDamageTakenMultiplier = abilityDamageTakenMultiplier;

		mMobHealthMultiplier = mobHealthMultiplier;
		mMobSpeedMultiplier = mobSpeedMultiplier;

		mMobAbilityPool = mobAbilityPool;
		mMobAbilityChance = mobAbilityChance;

		// Class may be refreshed in multiple places, only message once per shard entry; metadata is cleared upon exit
		if (player != null && !player.hasMetadata(MESSAGED_PLAYER_METAKEY)) {
			player.setMetadata(MESSAGED_PLAYER_METAKEY, new FixedMetadataValue(mPlugin, null));
			MessagingUtils.sendRawMessage(player, message);
		}
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.CUSTOM) {
			event.setDamage(EntityUtils.getDamageApproximation(event, mAbilityDamageTakenMultiplier));
		} else {
			event.setDamage(EntityUtils.getDamageApproximation(event, mDamageTakenMultiplier));
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, mDamageTakenMultiplier));
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

		if (!hasProperties) {
			// Health
			AttributeModifier healthMod = new AttributeModifier(HEALTH_MODIFIER_NAME,
					mMobHealthMultiplier - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
			health.addModifier(healthMod);
			mob.setHealth(mob.getHealth() * mMobHealthMultiplier);

			// Speed
			AttributeInstance speed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			AttributeModifier speedMod = new AttributeModifier(SPEED_MODIFIER_NAME,
					mMobSpeedMultiplier - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
			speed.addModifier(speedMod);

			// Abilities
			if (FastUtils.RANDOM.nextDouble() < mMobAbilityChance) {
				// This runs prior to BossManager parsing, so we can just add tags directly
				mob.addScoreboardTag("boss_blastresist");
				mob.addScoreboardTag(mMobAbilityPool[FastUtils.RANDOM.nextInt(mMobAbilityPool.length)]);
			}
		}
	}

}
