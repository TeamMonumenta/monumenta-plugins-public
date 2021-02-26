package com.playmonumenta.plugins.bosses.spells.frostgiant;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
Whirlwind - The Frost giant gains slowness 2 for 6 seconds while dealing 18
damage and knocking back players slightly if they are within 8 blocks for
those 6 seconds every half second
 */
public class SpellWhirlwind extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellWhirlwind(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.25f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.65f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0.5f);

		//Boss does not move at beginning
		Creature c = (Creature) mBoss;
		LivingEntity target = c.getTarget();
		mBoss.setAI(false);

		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 2f;
			@Override
			public void run() {
				mTicks += 2;

				//Sweep warning particle effects
				Location loc = mBoss.getLocation();
				double r = 10 - (0.75 * mTicks);
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					loc.add(FastUtils.cos(radian) * r, 0, FastUtils.sin(radian) * r);
					world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
				}

				//Plays warning sound
				if (mTicks % 2 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, mPitch);
					if (mPitch > 0.1f) {
						mPitch -= 0.1f;
					}
				}

				//After 1.5 seconds, starts moving and attacking
				if (mTicks >= 30) {
					mBoss.setAI(true);
					FrostGiant.unfreezeGolems(mBoss);
					if (target != null) {
						c.setTarget(target);
					}
					mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 6, 0));
					this.cancel();
					BukkitRunnable runnable = new BukkitRunnable() {
						int mTicks = 0;
						double mSin = 0;
						double mRotation = 0;
						double mRadius = 8;
						@Override
						public void run() {
							mTicks += 2;
							mSin += 0.4;
							mRotation += 40;
							Location loc = mBoss.getLocation();
							world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 2, 0.75f);
							for (int i = 0; i < 2; i++) {
								double radian = Math.toRadians(mRotation + (i * 180));
								loc.add(FastUtils.cos(radian) * mRadius, 6 + (FastUtils.sin(mSin) * 6), FastUtils.sin(radian) * mRadius);
								world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 8, 1, 1, 1, 0);
								world.spawnParticle(Particle.SWEEP_ATTACK, loc, 4, 1, 1, 1, 0);
								loc.subtract(FastUtils.cos(radian) * mRadius, 6 + (FastUtils.sin(mSin) * 6), FastUtils.sin(radian) * mRadius);
							}
							if (mTicks % 10 == 0) {
								for (Player player : PlayerUtils.playersInRange(loc, 8)) {
									BossUtils.bossDamage(mBoss, player, 28);
								}
							}
							if (mTicks >= 20 * 6) {
								this.cancel();
							}
						}

					};
					runnable.runTaskTimer(mPlugin, 0, 2);
					mActiveRunnables.add(runnable);
				}
			}

		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}
