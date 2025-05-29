package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.TheImpenetrable;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class ImpenetrableTeleport extends Spell {

	// Maximum distance for the teleport
	private static final int TELEPORT_RADIUS_MAX = 14;

	// Minimum distance for the teleport
	private static final int TELEPORT_RADIUS_MIN = 7;

	private static final int MAX_DISTANCE_FROM_SPAWN = TheImpenetrable.OUTER_RADIUS - 5;
	private static final int MAX_SKY_LIGHT_LEVEL = 5;

	private final LivingEntity mBoss;

	private final Entity mVehicle;

	private final Location mBossSpawnLoc;

	public ImpenetrableTeleport(LivingEntity boss, Entity vehicle, Location bossSpawnLoc) {
		mBoss = boss;
		mVehicle = vehicle;
		mBossSpawnLoc = bossSpawnLoc;
	}

	@Override
	public void run() {
		run(true);
	}

	public void run(boolean particles) {
		// Try 40 times to get a solid block to teleport to
		for (int i = 0; i < 40; i++) {
			double r = FastUtils.randomDoubleInRange(TELEPORT_RADIUS_MIN, TELEPORT_RADIUS_MAX);
			double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);

			Location sLoc = mBoss.getLocation().clone();
			sLoc.setY(mBossSpawnLoc.getY());
			Location tLoc = sLoc.add(FastUtils.cos(theta) * r, 4.0, FastUtils.sin(theta) * r);

			Location tpTo = TheImpenetrable.getOnNearestGround(tLoc, 5);
			if (tpTo != null) {
				if (tpTo.distanceSquared(mBossSpawnLoc) >= MAX_DISTANCE_FROM_SPAWN * MAX_DISTANCE_FROM_SPAWN || tpTo.getBlock().getLightFromSky() > MAX_SKY_LIGHT_LEVEL) {
					// Don't let it leave the area out of the players' control
					continue;
				}

				// play at starting location
				World world = mBoss.getWorld();
				Location loc = mBoss.getLocation();
				if (particles) {
					world.playSound(loc, Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.HOSTILE, 5.2f, 0.81f);
					world.playSound(loc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.HOSTILE, 0.6f, 0.0f);
					world.playSound(loc, Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, SoundCategory.HOSTILE, 1.7f, 0.61f);
					world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 0.8f, 0.82f);
				}

				// evil nms because bukkit refuses to teleport entities with passengers
				Vector movement = tpTo.toCenterLocation().add(0, -0.5, 0).toVector().subtract(mVehicle.getLocation().toVector());
				if (particles) {
					new PPLine(Particle.ELECTRIC_SPARK, loc, tpTo).countPerMeter(15).spawnAsBoss();
				}
				NmsUtils.getVersionAdapter().moveEntity(mVehicle, movement);

				if (particles) {
					// play at final location
					loc = mBoss.getLocation();
					world.playSound(loc, Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.HOSTILE, 5.2f, 0.81f);
					world.playSound(loc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.HOSTILE, 0.6f, 0.0f);
					world.playSound(loc, Sound.BLOCK_LARGE_AMETHYST_BUD_BREAK, SoundCategory.HOSTILE, 1.7f, 0.61f);
					world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 0.8f, 0.82f);
				}

				break;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 30;
	}
}
