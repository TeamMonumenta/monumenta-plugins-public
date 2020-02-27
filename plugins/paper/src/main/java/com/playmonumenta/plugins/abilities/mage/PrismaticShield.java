package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class PrismaticShield extends Ability {

	private static final float PRISMATIC_SHIELD_RADIUS = 4.0f;
	private static final int PRISMATIC_SHIELD_TRIGGER_HEALTH = 6;
	private static final int PRISMATIC_SHIELD_1_AMPLIFIER = 1;
	private static final int PRISMATIC_SHIELD_2_AMPLIFIER = 2;
	private static final int PRISMATIC_SHIELD_1_DURATION = 12 * 20;
	private static final int PRISMATIC_SHIELD_2_DURATION = 12 * 20;
	private static final int PRISMATIC_SHIELD_1_COOLDOWN = 90 * 20;
	private static final int PRISMATIC_SHIELD_2_COOLDOWN = 80 * 20;
	private static final float PRISMATIC_SHIELD_KNOCKBACK_SPEED = 0.7f;
	private static final int PRISMATIC_SHIELD_1_DAMAGE = 3;
	private static final int PRISMATIC_SHIELD_2_DAMAGE = 6;

	private final int mAmplifier;
	private final int mDuration;
	private final int mDamage;

	public PrismaticShield(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Prismatic Shield");
		mInfo.linkedSpell = Spells.PRISMATIC_SHIELD;
		mInfo.scoreboardId = "Prismatic";
		mInfo.mShorthandName = "PS";
		mInfo.mDescriptions.add("When your health drops below 3 hearts (including if the attack would've killed you), you receive an Absorption II shield (4 hearts) which lasts up to 12 s. In addition enemies within four blocks are knocked back and take 3 damage. (cooldown: 80 seconds).");
		mInfo.mDescriptions.add("The shield is improved to Absorption III (6 hearts) for 12 s. Enemies within four blocks now take 6 damage.");
		mInfo.cooldown = getAbilityScore() == 1 ? PRISMATIC_SHIELD_1_COOLDOWN : PRISMATIC_SHIELD_2_COOLDOWN;
		mAmplifier = getAbilityScore() == 1 ? PRISMATIC_SHIELD_1_AMPLIFIER : PRISMATIC_SHIELD_2_AMPLIFIER;
		mDuration = getAbilityScore() == 1 ? PRISMATIC_SHIELD_1_DURATION : PRISMATIC_SHIELD_2_DURATION;
		mDamage = getAbilityScore() == 1 ? PRISMATIC_SHIELD_1_DAMAGE : PRISMATIC_SHIELD_2_DAMAGE;
	}

	/*
	 * Evasion works off the PlayerDamagedByLivingEntity event, and the order of event
	 * triggers only applies to events of the same type, so we need to break Prismatic
	 * into two pieces for the trigger order to be correct.
	 */
	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		execute(event);
		return true;
	}

	/*
	 * Works against all types of damage
	 */
	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		// Do not process cancelled damage events
		if (event.isCancelled() || event instanceof EntityDamageByEntityEvent) {
			return true;
		}

		execute(event);
		return true;
	}

	private void execute(EntityDamageEvent event) {
		// Calculate whether this effect should not be run based on player health.
		// It is intentional that Prismatic Shield saves you from death if you take a buttload of damage somehow.
		double healthRemaining = mPlayer.getHealth() - EntityUtils.getRealFinalDamage(event);

		// Health is less than 0 but does not penetrate the absorption shield
		boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -4 * (mAmplifier + 1);


		if (healthRemaining > PRISMATIC_SHIELD_TRIGGER_HEALTH) {
			return;
		} else if (dealDamageLater) {
			// The player has taken fatal damage BUT will be saved by the absorption, so set damage to 0 and compensate later
			event.setCancelled(true);
		}

		// Put on cooldown before processing results to prevent infinite recursion
		putOnCooldown();

		// Conditions match - prismatic shield
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), PRISMATIC_SHIELD_RADIUS, mPlayer)) {
			EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ARCANE);
			MovementUtils.knockAway(mPlayer, mob, PRISMATIC_SHIELD_KNOCKBACK_SPEED);
		}

		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.ABSORPTION, mDuration, mAmplifier, true, true));
		mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1);
		mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Prismatic Shield has been activated");

		if (dealDamageLater) {
			mPlayer.setHealth(1);
			AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
		}
	}
}
