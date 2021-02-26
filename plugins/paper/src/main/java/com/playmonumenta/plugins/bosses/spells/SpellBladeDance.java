package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellBladeDance extends Spell {
	private Plugin mPlugin;
	private LivingEntity mCaster;

	public SpellBladeDance(Plugin plugin, LivingEntity caster) {
		mPlugin = plugin;
		mCaster = caster;
	}

	@Override
	public void run() {
		World world = mCaster.getWorld();
		mCaster.setInvulnerable(true);
		world.playSound(mCaster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
		world.spawnParticle(Particle.SWEEP_ATTACK, mCaster.getLocation(), 150, 4, 4, 4, 0);
		new BukkitRunnable() {
			int mIndex = 0;
			float mPitch = 0;
			@Override
			public void run() {
				if (mCaster.isDead() || !mCaster.isValid()) {
					this.cancel();
					return;
				}

				mIndex += 2;
				world.spawnParticle(Particle.SWEEP_ATTACK, mCaster.getLocation(), 10, 4, 4, 4, 0);
				world.playSound(mCaster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, mPitch);
				mPitch += 0.2;
				Location loc1 = mCaster.getLocation().add(6, 6, 6);
				Location loc2 = mCaster.getLocation().add(-6, -1, -6);

				double x1 = FastUtils.randomDoubleInRange(loc2.getX(), loc1.getX());
				double y1 = FastUtils.randomDoubleInRange(loc2.getY(), loc1.getY());
				double z1 = FastUtils.randomDoubleInRange(loc2.getZ(), loc1.getZ());

				double x2 = FastUtils.randomDoubleInRange(loc2.getX(), loc1.getX());
				double y2 = FastUtils.randomDoubleInRange(loc2.getY(), loc1.getY());
				double z2 = FastUtils.randomDoubleInRange(loc2.getZ(), loc1.getZ());

				new BukkitRunnable() {
					Location mLoc1 = new Location(world, x1, y1, z1);
					Location mLoc2 = new Location(world, x2, y2, z2);
					Vector mDir = LocationUtils.getDirectionTo(mLoc2, mLoc1);
					int mTicks = 0;

					@Override
					public void run() {
						mTicks++;
						mLoc1.add(mDir.clone().multiply(1.15));
						world.spawnParticle(Particle.CRIT_MAGIC, mLoc1, 4, 0, 0, 0, 0.35);
						world.spawnParticle(Particle.CLOUD, mLoc1, 1, 0, 0, 0, 0);
						world.spawnParticle(Particle.SWEEP_ATTACK, mLoc1, 1, 0, 0, 0, 0);
						if (mTicks >= 10) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);

				if (mIndex >= 40) {
					mCaster.setInvulnerable(false);
					this.cancel();

					//Ultra flash
					new BukkitRunnable() {
						double mRotation = 0;
						Location mLoc = mCaster.getLocation();
						double mRadius = 0;
						double mY = 2.5;
						double mYminus = 0.35;

						@Override
						public void run() {

							mRadius += 1;
							for (int i = 0; i < 15; i += 1) {
								mRotation += 24;
								double radian1 = Math.toRadians(mRotation);
								mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
								world.spawnParticle(Particle.SWEEP_ATTACK, mLoc, 1, 0.1, 0.1, 0.1, 0);
								world.spawnParticle(Particle.EXPLOSION_NORMAL, mLoc, 3, 0.1, 0.1, 0.1, 0.1);
								mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);

							}
							mY -= mY * mYminus;
							mYminus += 0.02;
							if (mYminus >= 1) {
								mYminus = 1;
							}
							if (mRadius >= 7) {
								this.cancel();
							}

						}

					}.runTaskTimer(mPlugin, 0, 1);
					world.playSound(mCaster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
					world.playSound(mCaster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
					world.spawnParticle(Particle.FLAME, mCaster.getLocation(), 150, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.CLOUD, mCaster.getLocation(), 70, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.SWEEP_ATTACK, mCaster.getLocation(), 150, 4, 4, 4, 0);
					for (Player player : PlayerUtils.playersInRange(mCaster.getLocation(), 4)) {
						BossUtils.bossDamage(mCaster, player, 16, null);
						MovementUtils.knockAway(mCaster.getLocation(), player, 0.45f);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}

}
