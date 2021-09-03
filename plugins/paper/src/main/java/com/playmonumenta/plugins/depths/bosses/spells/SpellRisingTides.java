package com.playmonumenta.plugins.depths.bosses.spells;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;


/*
 *
 * Shatter - All players within a 70 degree cone in front of the giant after
a 1 second charge up take 24 damage and are knocked back X blocks. If they
collide with a wall they take 10 additional damage and are stunned (Slowness 7,
Negative Jump Boost, weakness 10, maybe putting bows on cooldown, you get the
idea) for 2 seconds.
 */
public class SpellRisingTides extends Spell {

	public static final double DAMAGE = .5;
	private static final Particle.DustOptions UP_COLOR = new Particle.DustOptions(Color.fromRGB(66, 140, 237), 1.0f);
	private static final Particle.DustOptions DOWN_COLOR = new Particle.DustOptions(Color.fromRGB(226, 88, 34), 1.0f);

	private Plugin mPlugin;
	private LivingEntity mBoss;
	public int mCooldownTicks;
	private Location mStartLoc;
	public Nucleus mBossInstance;

	public SpellRisingTides(Plugin plugin, LivingEntity boss, Location startLoc, int cooldownTicks, Nucleus bossInstance) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mCooldownTicks = cooldownTicks;
		mBossInstance = bossInstance;
	}

	@Override
	public boolean canRun() {
		return mBossInstance.mIsHidden;
	}

	@Override
	public void run() {
		int lowCount = 0;
		int highCount = 0;
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, 50, true);
		for (Player p : players) {
			if (p.getLocation().getY() < mStartLoc.getY() + .5) {
				lowCount++;
			} else if (p.getLocation().getY() > mStartLoc.getY() + .5) {
				highCount++;
			}
		}

		if (lowCount > highCount) {
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), 50, "tellraw @s [\"\",{\"text\":\"The frigid water rises below...\",\"color\":\"blue\"}]");

			cast(true, players);

		} else {
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), 50, "tellraw @s [\"\",{\"text\":\"The water above boils with heat..\",\"color\":\"red\"}]");

			cast(false, players);
		}
	}

	public void cast(boolean tide, List<Player> players) {

		World world = mStartLoc.getWorld();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT += 5;

				//Play knockup sound
				if (mT % 5 == 0 && mT < 55) {
					int t = mT / 5;
					float dPitch = t * 0.2f;
					if (tide) {
						world.playSound(mStartLoc, Sound.ENTITY_DOLPHIN_SPLASH, 10, 0f + dPitch);
					} else {
						world.playSound(mStartLoc, Sound.ITEM_FIRECHARGE_USE, 10, 2.0f - dPitch);
					}
					for (double deg = 0; deg < 360; deg += 4) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = 1; x <= 30; x += 1) {
							Location loc = mStartLoc.clone().add(cos * x, 0, sin * x);
							for (Player player : players) {
								double dist = player.getLocation().distance(loc);

								if (dist < 10 || x % 4 == 0) {
									if (tide) {
										player.spawnParticle(Particle.REDSTONE, loc.add(0, -.5 + (dPitch / 2), 0), 1, 0.15, 0.15, 0.15, UP_COLOR);
									} else {
										player.spawnParticle(Particle.REDSTONE, loc.add(0, 2.5 - (dPitch / 2), 0), 1, 0.15, 0.15, 0.15, DOWN_COLOR);
									}
								}
							}
						}
					}

				}

				if (mT == 55) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 20.0f, 1);

					for (double deg = 0; deg < 360; deg += 4) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = 1; x <= 30; x += 1) {
							Location loc = mStartLoc.clone().add(cos * x, 0, sin * x);
							for (Player player : players) {
								double dist = player.getLocation().distance(loc);

								if (dist < 10 || x % 8 == 0) {
									if (tide) {
										player.spawnParticle(Particle.SMOKE_NORMAL, loc.add(0, -.5, 0), 1, 0.15, 0.15, 0.15, 0);

										if (deg % 16 == 0) {
											player.spawnParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.15, 0.15, 0.15);
											//Remove if explosions unwanted
											player.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.15, 0.15, 0.15, 0);
										}
									} else {
										player.spawnParticle(Particle.SMOKE_NORMAL, loc.add(0, 2.5, 0), 1, 0.15, 0.15, 0.15, 0);

										if (deg % 16 == 0) {
											player.spawnParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.15, 0.15, 0.15);
											//Remove if explosions unwanted
											player.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.15, 0.15, 0.15, 0);
										}
									}
								}
							}
						}
					}

					for (Player p : PlayerUtils.playersInRange(mStartLoc, 50, true)) {
						if (tide && p.getLocation().getY() < mStartLoc.getY() + .5) {
							BossUtils.bossDamagePercent(mBoss, p, DAMAGE, mStartLoc);
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "Tectonic", new PercentSpeed(2 * 20, -.99, "Tectonic"));

						} else if (!tide && p.getLocation().getY() > mStartLoc.getY() + .5) {
							BossUtils.bossDamagePercent(mBoss, p, DAMAGE, mStartLoc);
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "Tectonic", new PercentSpeed(2 * 20, -.99, "Tectonic"));
						}
					}
					this.cancel();
				}

			}
		};
		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}


	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
