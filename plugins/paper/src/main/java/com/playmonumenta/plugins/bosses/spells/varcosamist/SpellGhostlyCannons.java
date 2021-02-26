package com.playmonumenta.plugins.bosses.spells.varcosamist;

import java.util.Collections;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellGhostlyCannons extends Spell {
	private LivingEntity mBoss;
	private Plugin mPlugin;
	private double mRange;
	private Location mCenter;
	private boolean mPhaseThree;
	private String mDio;
	private static final Particle.DustOptions CANNONS_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	public SpellGhostlyCannons(Plugin plugin, LivingEntity boss, double range, Location center, boolean phaseThree, String dio) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mCenter = center;
		mPhaseThree = phaseThree;
		mDio = dio;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		PlayerUtils.executeCommandOnNearbyPlayers(mCenter, 50, "tellraw @s [\"\",{\"text\":\"" + mDio + "\",\"color\":\"red\"}]");

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks += 2;
				float fTick = mTicks;
				float ft = fTick / 25;
				world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005);
				world.spawnParticle(Particle.CRIT, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 10, 0.5f + ft);
				if (mTicks >= 20 * 2) {
					this.cancel();
					mActiveRunnables.remove(this);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 3, 0.5f);
					BukkitRunnable runnable = new BukkitRunnable() {

						int mI = 0;
						@Override
						public void run() {
							mI++;
							List<Player> players = PlayerUtils.playersInRange(mCenter, 24);
							Collections.shuffle(players);
							for (Player player : players) {
								Vector loc = player.getLocation().toVector();
								if (player.getLocation().getBlock().isLiquid() || !loc.isInSphere(mCenter.toVector(), 50)) {
									rainCannons(player.getLocation(), players, mCenter.getY() + 10);
								}
							}
							for (int j = 0; j < 4; j++) {
								rainCannons(mCenter.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), 0, FastUtils.randomDoubleInRange(-mRange, mRange)), players, mCenter.getY() + 10);
							}

							// Target one random player. Have a meteor rain nearby them.
							if (players.size() >= 1) {
								Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
								Location loc = rPlayer.getLocation();
								rainCannons(loc.add(FastUtils.randomDoubleInRange(-2, 2), 0, FastUtils.randomDoubleInRange(-2, 2)), players, mCenter.getY() + 10);
							}

							if (mI >= (mPhaseThree ? 30 : 25)) {
								this.cancel();
								mActiveRunnables.remove(this);
							}
						}

					};
					runnable.runTaskTimer(mPlugin, 0, 10);
					mActiveRunnables.add(runnable);
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void rainCannons(Location locInput, List<Player> players, double spawnY) {
		if (locInput.distance(mCenter) > 24) {
			// Somehow tried to spawn a meteor too far away from the center point
			return;
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			double mY = spawnY;
			Location mLoc = locInput.clone();
			World mWorld = locInput.getWorld();
			@Override
			public void run() {
				players.removeIf(p -> p.getLocation().distance(mCenter) > 30);
				mY -= 1;
				if (mY % 2 == 0) {
					for (Player player : players) {
						// Player gets more particles the closer they are to the landing area
						double dist = player.getLocation().distance(mLoc);
						double step = dist < 10 ? 0.5 : (dist < 15 ? 1 : 3);
						for (double deg = 0; deg < 360; deg += (step * 45)) {
							player.spawnParticle(Particle.REDSTONE, mLoc.clone().add(FastUtils.cos(deg), 0, FastUtils.sin(deg)), 1, 0.15, 0.15, 0.15, 0, CANNONS_COLOR);
						}
					}
				}
				Location particle = mLoc.clone().add(0, mY, 0);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, particle, 3, 0.2f, 0.2f, 0.2f, 0.05, null, true);
				if (FastUtils.RANDOM.nextBoolean()) {
					mWorld.spawnParticle(Particle.CRIT, particle, 1, 0, 0, 0, 0, null, true);
				}
				mWorld.playSound(particle, Sound.ENTITY_ARROW_SHOOT, 1, 1);
				if (mY <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mLoc, 15, 0, 0, 0, 0.175, null, false);
					mWorld.spawnParticle(Particle.CRIT, mLoc, 10, 0, 0, 0, 0.25, null, false);
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
					BoundingBox box = BoundingBox.of(mLoc, 3, 3, 3);
					for (Player player : PlayerUtils.playersInRange(mLoc, 3)) {
						BoundingBox pBox = player.getBoundingBox();
						if (pBox.overlaps(box)) {
							BossUtils.bossDamage(mBoss, player, 45, mLoc);
							MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
							AbilityUtils.silencePlayer(player, 15 * 20);
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int castTicks() {
		return 20 * 18;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 40;
	}
}
