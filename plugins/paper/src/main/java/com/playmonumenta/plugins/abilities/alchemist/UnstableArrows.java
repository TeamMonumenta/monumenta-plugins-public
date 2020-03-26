package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;


public class UnstableArrows extends Ability {
	private static final int UNSTABLE_ARROWS_COOLDOWN = 16 * 20;
	private static final int UNSTABLE_ARROWS_DURATION = 3 * 20;
	private static final int UNSTABLE_ARROWS_PARTICLE_PERIOD = 3;
	private static final float UNSTABLE_ARROWS_KNOCKBACK_SPEED = 2.5f;
	private static final int UNSTABLE_ARROWS_1_DAMAGE = 15;
	private static final int UNSTABLE_ARROWS_2_DAMAGE = 24;
	private static final int UNSTABLE_ARROWS_RADIUS = 4;

	private Arrow mUnstableArrow = null;

	public UnstableArrows(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Unstable Arrows");
		mInfo.linkedSpell = Spells.UNSTABLE_ARROWS;
		mInfo.scoreboardId = "BombArrow";
		mInfo.mShorthandName = "UA";
		mInfo.mDescriptions.add("When you crouch and fire an arrow it will begin to hiss on landing. 3s later it explodes, dealing 15 damage to mobs within a four block radius. If you have skill points in Basilisk Poison enemies caught in the explosion will be affected by it. Cooldown: 16 seconds. You can toggle whether the explosion will apply knockback to you or not in the P.E.B.");
		mInfo.mDescriptions.add("The damage is increased to 24");
		if (player != null && ScoreboardUtils.getScoreboardValue(player, "RocketJumper") == 9001) {
			mInfo.cooldown = 0;
		} else {
			mInfo.cooldown = UNSTABLE_ARROWS_COOLDOWN;
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Arrow arrow) {
		if (mUnstableArrow != null && arrow == mUnstableArrow) {
			arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
			putOnCooldown();
			mUnstableArrow = null;

			Location loc = arrow.getLocation();

			// Run cleansing rain here until it finishes
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					mWorld.spawnParticle(Particle.FLAME, loc, 3, 0.3, 0.3, 0.3, 0.05);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 7, 0.5, 0.5, 0.5, 0.075);
					mWorld.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 0.3f,
					                 ((UNSTABLE_ARROWS_DURATION / 3.0f) + mTicks) / (1.5f * UNSTABLE_ARROWS_DURATION));
					if (mTicks % 18 == 0) {
						mWorld.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 1.6f, 1f + mTicks / 36f);
						mWorld.spawnParticle(Particle.LAVA, loc, 80, UNSTABLE_ARROWS_RADIUS, 0, UNSTABLE_ARROWS_RADIUS, 0);
					}
					if (mTicks >= UNSTABLE_ARROWS_DURATION) {
						arrow.remove();
						Location explodeLoc = loc;
						mWorld.playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0f);
						mWorld.playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.25f);

						mWorld.spawnParticle(Particle.FLAME, explodeLoc, 115, 0.02, 0.02, 0.02, 0.2);
						mWorld.spawnParticle(Particle.SMOKE_LARGE, explodeLoc, 40, 0.02, 0.02, 0.02, 0.35);
						mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, explodeLoc, 40, 0.02, 0.02, 0.02, 0.35);
						BasiliskPoison bp = (BasiliskPoison) AbilityManager.getManager().getPlayerAbility(mPlayer, BasiliskPoison.class);

						int baseDamage = (getAbilityScore() == 1) ? UNSTABLE_ARROWS_1_DAMAGE : UNSTABLE_ARROWS_2_DAMAGE;

						for (LivingEntity mob : EntityUtils.getNearbyMobs(explodeLoc, UNSTABLE_ARROWS_RADIUS, mPlayer)) {
							EntityUtils.damageEntity(mPlugin, mob, baseDamage, mPlayer, MagicType.ALCHEMY, true, mInfo.linkedSpell);
							MovementUtils.knockAwayRealistic(explodeLoc, mob, UNSTABLE_ARROWS_KNOCKBACK_SPEED, 0.5f);
							if (bp != null) {
								bp.apply(mob);
							}
						}

						// Custom knockback function because this is unreliable as is with weird arrow location calculations
						if (ScoreboardUtils.getScoreboardValue(mPlayer, "RocketJumper") == 1
							&& mPlayer.getLocation().distance(explodeLoc) < UNSTABLE_ARROWS_RADIUS / 2.0) {
							if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
								Vector dir = mPlayer.getLocation().subtract(explodeLoc.toVector()).toVector();
								dir.setY(dir.getY() / 1.5).normalize().multiply(2);
								dir.setY(dir.getY() + 0.5);
								mPlayer.setVelocity(dir);
							}
						} else if (ScoreboardUtils.getScoreboardValue(mPlayer, "RocketJumper") == 9001
							&& mPlayer.getLocation().distance(explodeLoc) < UNSTABLE_ARROWS_RADIUS) {
							MovementUtils.knockAwayRealistic(explodeLoc, mPlayer, UNSTABLE_ARROWS_KNOCKBACK_SPEED * 4, 6);
						}
						this.cancel();
					}
					mTicks += UNSTABLE_ARROWS_PARTICLE_PERIOD;
				}
			}.runTaskTimer(mPlugin, 0, UNSTABLE_ARROWS_PARTICLE_PERIOD);
		}
	}

	@Override
	public boolean playerShotArrowEvent(Arrow arrow) {
		// Can't use runCheck for this because player doesn't need to be sneaking when arrow lands
		if (mPlayer.isSneaking()) {
			mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 5.0f, 0.25f);
			mUnstableArrow = arrow;
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
		}

		return true;
	}
}
