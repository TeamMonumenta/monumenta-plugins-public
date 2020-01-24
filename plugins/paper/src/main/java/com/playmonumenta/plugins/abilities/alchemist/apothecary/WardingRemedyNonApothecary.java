package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class WardingRemedyNonApothecary extends Ability {

	private static final int REMEDY_RANGE = 12;
	private static final int REMEDY_SHIELD = 1;
	private static final int REMEDY_SHIELD_THRESHOLD = 4;
	private static final int REMEDY_SHIELD_MAX = 6;
	private static final int REMEDY_1_FREQUENCY = 5;				// Seconds
	private static final int REMEDY_2_FREQUENCY = 4;				// Seconds
	private static final double REMEDY_1_DAMAGE_MULTIPLIER = 1.1;
	private static final double REMEDY_2_DAMAGE_MULTIPLIER = 1.2;
	private static final Particle.DustOptions APOTHECARY_LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.0f);
	private static final Particle.DustOptions APOTHECARY_DARK_COLOR = new Particle.DustOptions(Color.fromRGB(83, 0, 135), 1.0f);

	// Stored level so we don't have to run checks too often
	private int mLevel = 0;
	private int mSeconds = 0;

	public WardingRemedyNonApothecary(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			applyBonusDamage(event);
		}

		return true;
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		applyBonusDamage(event);

		return true;
	}

	private void applyBonusDamage(EntityDamageByEntityEvent event) {
		mLevel = getWardingRemedyLevel();
		if (mLevel > 0 && AbsorptionUtils.getAbsorption(mPlayer) >= REMEDY_SHIELD_THRESHOLD) {
			double multiplier = mLevel == 1 ? REMEDY_1_DAMAGE_MULTIPLIER : REMEDY_2_DAMAGE_MULTIPLIER;
			Location loc = event.getEntity().getLocation().add(0, 1, 0);
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 8, 0.3, 0.5, 0.3);
			mWorld.spawnParticle(Particle.REDSTONE, loc, 10, 0.4, 0.5, 0.4, APOTHECARY_DARK_COLOR);
			event.setDamage(event.getDamage() * multiplier);
		}
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mSeconds++;

			if (mSeconds == REMEDY_2_FREQUENCY) {
				// We can reuse this value for the next check if needed
				mLevel = getWardingRemedyLevel();
				if (mLevel == 2) {
					AbsorptionUtils.addAbsorption(mPlayer, REMEDY_SHIELD, REMEDY_SHIELD_MAX);
					mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.25, 0), 10, 0.35, 0.4, 0.35, APOTHECARY_LIGHT_COLOR);
					if (AbsorptionUtils.getAbsorption(mPlayer) >= REMEDY_SHIELD_THRESHOLD) {
						mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.25, 0), 7, 0.35, 0.4, 0.35, APOTHECARY_DARK_COLOR);
					}
					mSeconds = 0;
				}
			} else if (mSeconds >= REMEDY_1_FREQUENCY) {
				mSeconds = 0;
				if (mLevel > 0) {
					AbsorptionUtils.addAbsorption(mPlayer, REMEDY_SHIELD, REMEDY_SHIELD_MAX);
					mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.25, 0), 10, 0.35, 0.4, 0.35, APOTHECARY_DARK_COLOR);
					if (AbsorptionUtils.getAbsorption(mPlayer) >= REMEDY_SHIELD_THRESHOLD) {
						mWorld.spawnParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 0.25, 0), 7, 0.35, 0.4, 0.35, APOTHECARY_DARK_COLOR);
					}
				}
			}
		}
	}

	private int getWardingRemedyLevel() {
		int level = 0;

		for (Player player : PlayerUtils.playersInRange(mPlayer, REMEDY_RANGE, true)) {
			Ability wr = AbilityManager.getManager().getPlayerAbility(player, WardingRemedy.class);
			if (wr != null) {
				int score = wr.getAbilityScore();
				if (score == 2) {
					return score;
				} else if (score == 1) {
					level = score;
				}
			}
		}

		return level;
	}

}
