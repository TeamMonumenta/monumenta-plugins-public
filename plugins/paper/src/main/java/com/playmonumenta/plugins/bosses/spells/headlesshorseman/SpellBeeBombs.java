package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Bee Bomb - The Horseman summons bees around himself to distract his enemies. After some seconds
the bees explode dealing damage to anyone nearby, killing the bee in the process.
 */
public class SpellBeeBombs extends Spell {

	private static final int DAMAGE = 40;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private int mRange;
	private int mCount;
	private int mCooldownTicks = 0;

	public SpellBeeBombs(Plugin plugin, LivingEntity entity, int cooldown, Location center, int count, int range) {
		mPlugin = plugin;
		mBoss = entity;
		mCenter = center;
		mCooldownTicks = cooldown;
		mCount = count;
		mRange = range;
	}

	public void spawnBat(Location loc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;

				if (mTicks >= 30) {
					this.cancel();
					world.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0.15, .15, .15, 0.125);

					LivingEntity bat = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, "ExplosiveDrone");
					new BukkitRunnable() {
						int mTicks = 0;
						@Override
						public void run() {
							mTicks++;
							if (mTicks % 2 == 0) {
								world.spawnParticle(Particle.FLAME, bat.getLocation(), 1, 0.25, .25, .25, 0.025);
								world.spawnParticle(Particle.SMOKE_NORMAL, bat.getLocation(), 2, 0.25, .25, .25, 0.025);
							}
							if (mTicks >= 20 * 6) {
								bat.remove();
								this.cancel();
								Location loc = bat.getLocation();
								world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.15);
								world.spawnParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.1);
								world.spawnParticle(Particle.SMOKE_NORMAL, loc, 50, 0, 0, 0, 0.15);
								world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
								world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.65f, 1);

								for (Player player : PlayerUtils.playersInRange(loc, 5.5, true)) {
									if (mCenter.distance(player.getLocation()) < mRange) {
										BossUtils.bossDamage(mBoss, player, DAMAGE, loc, "Bee Bombs");
										if (!BossUtils.bossDamageBlocked(player, DAMAGE, loc)) {
											MovementUtils.knockAway(loc, player, 0.2f, 0.4f);
										}
									}
								}
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		for (int i = 0; i < mCount; i++) {
			Location loc = new Location(
			    world,
			    mCenter.getX() + FastUtils.randomDoubleInRange(-15, 15),
			    mCenter.getY() + FastUtils.randomDoubleInRange(1, 3),
			    mCenter.getZ() + FastUtils.randomDoubleInRange(-15, 15)
			);
			spawnBat(loc);
		}

		world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 3, 1.1f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 3, 0.75f);
		world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 20, 0.4, .4, .4, 0.1);
		new BukkitRunnable() {
			int mTicks = 0;
			Location mLoc = mBoss.getLocation().add(0, 1, 0);
			@Override
			public void run() {
				mTicks++;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				world.spawnParticle(Particle.SMOKE_NORMAL, mLoc, 5, 0.4, .4, .4, 0.025);
				if (mTicks >= 50) {
					this.cancel();
					world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 5, 0.4, .4, .4, 0.125);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 20, 0.4, .4, .4, 0.09);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 40, 0.4, .4, .4, 0.1);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 0.65f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 3, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 3, 1f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 3, 1.25f);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

}
