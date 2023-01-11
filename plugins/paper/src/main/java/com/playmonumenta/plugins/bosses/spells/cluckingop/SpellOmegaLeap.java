package com.playmonumenta.plugins.bosses.spells.cluckingop;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellOmegaLeap extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellOmegaLeap(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.setVelocity(new Vector(0, 1.5, 0));
		new PartialParticle(Particle.FLAME, mBoss.getLocation(), 175, 0, 0, 0, 0.175, null, true).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 75, 0, 0, 0, 0.25, null, true).spawnAsEntityActive(mBoss);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.9f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.5f, 0f);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks >= 5 && mBoss.isOnGround() && !mBoss.isDead() && mBoss.isValid()) {
					this.cancel();
					Location loc = mBoss.getLocation();
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.175).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.5f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.5f, 0f);
					new BukkitRunnable() {
						double mRotation = 0;
						double mRadius = 1;
						Location mAttackLocation = mBoss.getLocation();

						@Override
						public void run() {
							if (mRadius >= 15) {
								this.cancel();
							}
							for (int i = 0; i < 36; i++) {
								Location loc = mBoss.getLocation();
								mRotation += 10;
								double radian1 = Math.toRadians(mRotation);
								loc.add(FastUtils.cos(radian1) * mRadius, 0.25, FastUtils.sin(radian1) * mRadius);
								new PartialParticle(Particle.FLAME, loc, 2, 0.2, 0.2, 0.2, 0.1, null, true).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.2, 0.2, 0.2, 0.65, null, true).spawnAsEntityActive(mBoss);

								loc.subtract(FastUtils.cos(radian1) * mRadius, 0.25, FastUtils.sin(radian1) * mRadius);
							}
							for (Player player : PlayerUtils.playersInRange(mAttackLocation, mRadius, true)) {
								if (player.getLocation().toVector().isInSphere(mAttackLocation.toVector(), mRadius)) {
									BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, 1);
								}
							}
							mRadius += 1;

						}

					}.runTaskTimer(mPlugin, 1, 1);
				} else if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 6;
	}

}
