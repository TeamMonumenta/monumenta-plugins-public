package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PhlegmaticResolve extends Ability {

	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "PhlegmaticPercentDamageResistEffect";
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "PhlegmaticPercentKnockbackResistEffect";
	private static final double PERCENT_DAMAGE_RESIST_1 = -0.015;
	private static final double PERCENT_DAMAGE_RESIST_2 = -0.025;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.05;
	private static final int RADIUS = 7;
	private static final double ALLY_MODIFIER = 0.33;

	private final double mPercentDamageResist;
	private double[] mEnhancementDamageSpread = {0, 0, 0};
	private double mLastMaxDamage = 0;
	private int mLastPlayedSoundTick = 0;
	private double mKBR;

	public static final String CHARM_RESIST = "Phlegmatic Resolve Resistance";
	public static final String CHARM_KBR = "Phlegmatic Resolve Knockback Resistance";
	public static final String CHARM_ALLY = "Phlegmatic Resolve Ally Modifier";
	public static final String CHARM_RANGE = "Phlegmatic Resolve Radius";

	public PhlegmaticResolve(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Phlegmatic Resolve");
		mInfo.mScoreboardId = "Phlegmatic";
		mInfo.mShorthandName = "PR";
		mInfo.mDescriptions.add("For each spell on cooldown, gain +1.5% Damage Reduction and +0.5 Knockback Resistance.");
		mInfo.mDescriptions.add("Increase to +2.5% Damage Reduction per spell on cooldown, and players within 7 blocks are given 33% of your bonuses. (Does not stack with multiple Warlocks.)");
		mInfo.mDescriptions.add("All non-ailment damage taken is instead converted into a short Damage-over-Time effect. A third of the damage stored is dealt every second for 3s.");
		mDisplayItem = new ItemStack(Material.SHIELD, 1);
		mPercentDamageResist = (isLevelOne() ? PERCENT_DAMAGE_RESIST_1 : PERCENT_DAMAGE_RESIST_2) - CharmManager.getLevelPercentDecimal(player, CHARM_RESIST);
		mKBR = (CharmManager.getLevelPercentDecimal(player, CHARM_KBR) + PERCENT_KNOCKBACK_RESIST);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second

		if (mPlayer == null) {
			return;
		}

		if (isEnhanced()) {
			if (twoHertz) {
				mLastMaxDamage = 0;
			}

			if (oneSecond) {
				// mPlayer.sendMessage("Phlegmatic: " + mEnhancementDamageSpread[0] + ", " + mEnhancementDamageSpread[1] + ", " + mEnhancementDamageSpread[2]);

				// This follows how reckless swing does it.
				if (mEnhancementDamageSpread[0] > 0.0) {
					if (mPlayer.getHealth() + mPlayer.getAbsorptionAmount() <= mEnhancementDamageSpread[0]) {
						mPlayer.damage(9001); // perish
					} else {
						if (mPlayer.getAbsorptionAmount() > 0) {
							double diff = mPlayer.getAbsorptionAmount() - mEnhancementDamageSpread[0];

							if (diff > 0) {
								mPlayer.setAbsorptionAmount(diff);
								mEnhancementDamageSpread[0] = 0;
							} else {
								mEnhancementDamageSpread[0] -= mPlayer.getAbsorptionAmount();
								mPlayer.setAbsorptionAmount(0);
							}
						}

						mPlayer.setHealth(Math.min(mPlayer.getHealth() - mEnhancementDamageSpread[0], EntityUtils.getMaxHealth(mPlayer)));
						mPlayer.damage(0.01);
					}
					Particle.DustOptions dustColor = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
					mPlayer.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(new Vector(0, 0.5, 0)), 50, 0.5, 0.5, 0.5, dustColor);

					// Shift the array forward
					mEnhancementDamageSpread[0] = mEnhancementDamageSpread[1];
					mEnhancementDamageSpread[1] = mEnhancementDamageSpread[2];
					mEnhancementDamageSpread[2] = 0;
				}

				// If player has died, reset the array
				if (mPlayer.isDead() || !mPlayer.isValid()) {
					mEnhancementDamageSpread[0] = 0;
					mEnhancementDamageSpread[1] = 0;
					mEnhancementDamageSpread[2] = 0;
				}
			}
		}

		int cooldowns = 0;
		for (Integer ability : mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId())) {
			if (ability > 0) {
				cooldowns++;
			}
		}
		if (cooldowns == 0) {
			return;
		}

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(20, mPercentDamageResist * cooldowns));
		mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, mKBR * cooldowns, KNOCKBACK_RESIST_EFFECT_NAME));

		if (isLevelTwo()) {
			for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RANGE, RADIUS), false)) {
				mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(20, mPercentDamageResist * cooldowns * (CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ALLY) + ALLY_MODIFIER)));
				mPlugin.mEffectManager.addEffect(p, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, mKBR * cooldowns * (CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ALLY) + ALLY_MODIFIER), KNOCKBACK_RESIST_EFFECT_NAME));
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (isEnhanced() &&
			event.getType() != DamageEvent.DamageType.AILMENT &&
			event.getType() != DamageEvent.DamageType.POISON &&
			event.getType() != DamageEvent.DamageType.OTHER &&
			event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK &&
			!event.isBlocked() &&
			mPlayer != null) {

			// If player isn't under invincibility ticks. (This means we can have attacks that bypass Iframes hopefully)
			if (event.getDamage() > mLastMaxDamage) {
				double damageSplit = (event.getDamage() - mLastMaxDamage) / 3.0;
				mLastMaxDamage = event.getDamage();

				// Only play sound to player once per second.
				if ((mLastPlayedSoundTick - mPlayer.getTicksLived()) > 20) {
					mLastPlayedSoundTick = mPlayer.getTicksLived();
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_HURT, 1, 1);
				}

				mEnhancementDamageSpread[0] += damageSplit;
				mEnhancementDamageSpread[1] += damageSplit;
				mEnhancementDamageSpread[2] += damageSplit;
			}

			event.setDamage(0.01);
		}
	}
}
