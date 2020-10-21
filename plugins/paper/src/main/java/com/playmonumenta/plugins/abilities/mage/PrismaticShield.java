package com.playmonumenta.plugins.abilities.mage;

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

	private static final float RADIUS = 4.0f;
	private static final int TRIGGER_HEALTH = 6;
	private static final int AMPLIFIER_1 = 1;
	private static final int AMPLIFIER_2 = 2;
	private static final int DURATION_1 = 12 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int COOLDOWN_1 = 90 * 20;
	private static final int COOLDOWN_2 = 70 * 20;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 6;

	private final int mAmplifier;
	private final int mDuration;
	private final int mDamage;

	public PrismaticShield(Plugin plugin, Player player) {
		super(plugin, player, "Prismatic Shield");
		mInfo.mLinkedSpell = Spells.PRISMATIC_SHIELD;
		mInfo.mScoreboardId = "Prismatic";
		mInfo.mShorthandName = "PS";
		mInfo.mDescriptions.add("When your health drops below 3 hearts (including if the attack would've killed you), you receive an Absorption II shield (4 hearts) which lasts up to 12 s. In addition enemies within four blocks are knocked back and take 3 damage. Cooldown: 90s.");
		mInfo.mDescriptions.add("The shield is improved to Absorption III (6 hearts) for 12 s. Enemies within four blocks now take 6 damage. Cooldown: 70s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mAmplifier = getAbilityScore() == 1 ? AMPLIFIER_1 : AMPLIFIER_2;
		mDuration = getAbilityScore() == 1 ? DURATION_1 : DURATION_2;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
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

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
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
		double healthRemaining = mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) - EntityUtils.getRealFinalDamage(event);

		// Health is less than 0 but does not penetrate the absorption shield
		boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -4 * (mAmplifier + 1);


		if (healthRemaining > TRIGGER_HEALTH) {
			return;
		} else if (dealDamageLater) {
			// The player has taken fatal damage BUT will be saved by the absorption, so set damage to 0 and compensate later
			event.setCancelled(true);
		}

		// Put on cooldown before processing results to prevent infinite recursion
		putOnCooldown();

		// Conditions match - prismatic shield
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
			EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ARCANE, true, mInfo.mLinkedSpell);
			MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED);
		}

		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
		                                 new PotionEffect(PotionEffectType.ABSORPTION, mDuration, mAmplifier, true, true));
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5);
		world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Prismatic Shield has been activated");

		if (dealDamageLater) {
			mPlayer.setHealth(1);
			AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
		}
	}
}
