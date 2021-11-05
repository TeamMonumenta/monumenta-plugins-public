package com.playmonumenta.plugins.depths.bosses.spells;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class VolcanicDeepmise extends Spell {

	public static final double ARENA_SIZE = 30.0;
	public static final int DAMAGE = 35;

	private Location mCenter;
	private int mTicks = 0;
	private final LivingEntity mBoss;


	public VolcanicDeepmise(LivingEntity boss, Location spawnLoc) {
		mBoss = boss;
		mCenter = spawnLoc;
	}

	@Override
	public void run() {
		//Runs every 5 ticks
		mTicks += 5;

		if (mTicks % 20 == 0) {

			//Rain two meteors every second

			Location l = null;
			double checkCircle = -1;
			while (l == null || checkCircle <= 0) {
				double x = FastUtils.randomDoubleInRange(-ARENA_SIZE, ARENA_SIZE);
				double z = FastUtils.randomDoubleInRange(-ARENA_SIZE, ARENA_SIZE);
				l = new Location(mCenter.getWorld(), mCenter.getX() + x, mCenter.getY(), mCenter.getZ() + z);
				checkCircle = Math.pow(ARENA_SIZE, 2) - (Math.pow(x, 2) + Math.pow(z, 2));
			}
			rainMeteor(l, PlayerUtils.playersInRange(mCenter, 30, true), 30);
			checkCircle = -1;
			while (l == null || checkCircle <= 0) {
				double x = FastUtils.randomDoubleInRange(-ARENA_SIZE, ARENA_SIZE);
				double z = FastUtils.randomDoubleInRange(-ARENA_SIZE, ARENA_SIZE);
				l = new Location(mCenter.getWorld(), mCenter.getX() + x, mCenter.getY(), mCenter.getZ() + z);
				checkCircle = Math.pow(ARENA_SIZE, 2) - (Math.pow(x, 2) + Math.pow(z, 2));
			}
			rainMeteor(l, PlayerUtils.playersInRange(mCenter, 30, true), 30);
		}
	}

	private void rainMeteor(Location locInput, List<Player> players, double spawnY) {

		//Bukkit.broadcastMessage("Player size " + players.size());

		if (players == null || players.size() == 0) {
			return;
		}

		if (locInput.distance(mCenter) > 50) {
			// Somehow tried to spawn a meteor too far away from the center point
			return;
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			double mY = spawnY;
			Location mLoc = locInput.clone();
			World mWorld = locInput.getWorld();

			@Override
			public void run() {
				players.removeIf(p -> p.getLocation().distance(mCenter) > 50);

				mY -= 1;
				if (mY % 2 == 0) {
					for (Player player : players) {
						// Player gets more particles the closer they are to the landing area
						double dist = player.getLocation().distance(mLoc);
						double step = dist < 10 ? 0.5 : (dist < 15 ? 1 : 3);
						for (double deg = 0; deg < 360; deg += (step * 30)) {
							player.spawnParticle(Particle.FLAME, mLoc.clone().add(FastUtils.cos(deg) * 3, 0, FastUtils.sin(deg) * 3), 1, 0.15, 0.15, 0.15, 0);
						}
					}
				}
				Location particle = mLoc.clone().add(0, mY, 0);
				mWorld.spawnParticle(Particle.FLAME, particle, 3, 0.2f, 0.2f, 0.2f, 0.05, null, true);
				if (FastUtils.RANDOM.nextBoolean()) {
					mWorld.spawnParticle(Particle.SMOKE_LARGE, particle, 1, 0, 0, 0, 0, null, true);
				}
				mWorld.playSound(particle, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				if (mY <= 0) {
					this.cancel();
					mWorld.spawnParticle(Particle.FLAME, mLoc, 50, 0, 0, 0, 0.175, null, true);
					mWorld.spawnParticle(Particle.SMOKE_LARGE, mLoc, 10, 0, 0, 0, 0.25, null, true);
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
					BoundingBox box = BoundingBox.of(mLoc, 4, 4, 4);
					for (Player player : PlayerUtils.playersInRange(mLoc, 4, true)) {
						BoundingBox pBox = player.getBoundingBox();
						if (pBox.overlaps(box)) {
							BossUtils.bossDamage(mBoss, player, DAMAGE, mLoc, "Volcanic Deepmise");
							if (!BossUtils.bossDamageBlocked(player, DAMAGE, mLoc)) {
								MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
							}
						}
					}
				}
			}
		};
		runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
