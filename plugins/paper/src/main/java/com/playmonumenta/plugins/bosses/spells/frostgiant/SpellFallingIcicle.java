package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class SpellFallingIcicle extends Spell {
	private static final int RESPAWN_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final int HITBOX_RADIUS = 4;
	private static final int HITBOX_HEIGHT = 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Hitbox mProjectileDetectionBox;
	private boolean mCurrentlyRespawning = false;

	public SpellFallingIcicle(final Plugin plugin, final LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = mBoss.getWorld();
		mProjectileDetectionBox = new Hitbox.UprightCylinderHitbox(mBoss.getLocation(), HITBOX_HEIGHT, HITBOX_RADIUS);
	}

	@Override
	public void run() {
		mProjectileDetectionBox.getHitEntitiesByClass(Projectile.class).forEach(projectile -> {
			projectile.setShooter(mBoss);

			/* Check if it is an arrow or a trident since tridents inherit from ThrowableProjectile and AbstractArrow */
			if (projectile instanceof AbstractArrow && ((AbstractArrow) projectile).getAttachedBlock() != null) {
				runIcicleFall(projectile);
			}
		});
	}

	@Override
	public void bossProjectileHit(final ProjectileHitEvent event) {
		if (mProjectileDetectionBox.getBoundingBox().overlaps(event.getEntity().getBoundingBox())) {
			runIcicleFall(event.getEntity());
		}
	}

	private void runIcicleFall(final Projectile proj) {
		if (mCurrentlyRespawning) {
			/* No arrow checks while still respawning */
			return;
		}
		if (proj.isValid()) {
			proj.remove();
		}
		final Location loc = mBoss.getLocation();

		//Create the bounding box for the whole icicle off of the smallest and largest x, y, z values
		double minX = loc.getX();
		double minY = loc.getY();
		double minZ = loc.getZ();

		double maxX = minX;
		double maxY = minY;
		double maxZ = minZ;

		final List<Location> icicleBlockLocs = new ArrayList<>();
		Location tempLoc;
		for (int x = -HITBOX_RADIUS; x <= HITBOX_RADIUS; x++) {
			for (int z = -HITBOX_RADIUS; z <= HITBOX_RADIUS; z++) {
				for (int y = 0; y <= HITBOX_HEIGHT; y++) {
					//Can't make location values more optimized - each one needs to be a separate Location object
					tempLoc = loc.clone().add(x, y, z);
					if (tempLoc.getBlock().getType() == Material.ICE) {
						if (tempLoc.getX() < minX) {
							minX = tempLoc.getX();
						} else if (tempLoc.getX() > maxX) {
							maxX = tempLoc.getX();
						}

						if (tempLoc.getY() < minY) {
							minY = tempLoc.getY();
						} else if (tempLoc.getY() > maxY) {
							maxY = tempLoc.getY();
						}

						if (tempLoc.getZ() < minZ) {
							minZ = tempLoc.getZ();
						} else if (tempLoc.getZ() > maxZ) {
							maxZ = tempLoc.getZ();
						}

						icicleBlockLocs.add(tempLoc);
						tempLoc.getBlock().setType(Material.AIR);
					}
				}
			}
		}

		if (icicleBlockLocs.isEmpty()) {
			return;
		}
		runIcicleRespawn(icicleBlockLocs);

		final BoundingBox icicleBox = BoundingBox.of(new Vector(minX, minY, minZ), new Vector(maxX, maxY, maxZ));
		final List<FallingBlock> ices = new ArrayList<>(icicleBlockLocs.size());
		icicleBlockLocs.forEach(blockLoc -> ices.add(mWorld.spawn(blockLoc, FallingBlock.class,
			CreatureSpawnEvent.SpawnReason.CUSTOM, (final FallingBlock ice) -> {
				ice.setBlockData(Bukkit.createBlockData(Material.ICE));
				ice.setVelocity(new Vector(0, -2, 0));
				ice.setDropItem(false);
				EntityUtils.disableBlockPlacement(ice);
			})));

		final FallingBlock icicleTip = ices.get(0);

		new BukkitRunnable() {
			int mTicks = 0;
			double mPreviousY = icicleTip.getLocation().getY();

			@Override
			public void run() {
				final double diffY = icicleTip.getLocation().getY() - mPreviousY;
				icicleBox.shift(new Vector(0, diffY, 0));

				/* If the tip of the icicle collides with the ground or it takes too long to hit the ground */
				if (icicleTip.isOnGround() || icicleTip.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR
					|| mTicks >= Constants.TICKS_PER_SECOND * 2) {
					new PartialParticle(Particle.FIREWORKS_SPARK, icicleTip.getLocation(), 20, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, icicleTip.getLocation(), 20, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.BLOCK_CRACK, icicleTip.getLocation(), 20, 0, 0, 0, 0.1,
						Bukkit.createBlockData(Material.ICE)).spawnAsEntityActive(mBoss);
					for (final FallingBlock b : ices) {
						final Location bLoc = b.getLocation();
						bLoc.add(0, -1, 0);
						if (bLoc.getBlock().getType() == FrostGiant.ICE_TYPE) {
							bLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
						}
						b.remove();
					}
					this.cancel();
				}

				/* Break the armor of any FrostGiant instance hit by the icicle */
				if (FrostGiant.testHitByIcicle(icicleBox)) {
					ices.forEach(Entity::remove);
					this.cancel();
				}

				mTicks += 2;
				mPreviousY = icicleTip.getLocation().getY();
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void runIcicleRespawn(final List<Location> icicle) {
		if (icicle.isEmpty()) {
			return;
		}

		mCurrentlyRespawning = true;

		final BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			int mCount = 0;

			@Override
			public void run() {
				/* Replace a slice of blocks every tick, so that all blocks are replaced over the duration and
				 * remove projectiles stuck in blocks when respawning completes */
				for (; mCount < (((icicle.size() - 1) * mTicks) / RESPAWN_DURATION) + 1; mCount++) {
					icicle.get(mCount).getBlock().setType(Material.ICE);
				}
				if (mCount >= icicle.size() || mTicks > RESPAWN_DURATION) {
					mCurrentlyRespawning = false;
					mProjectileDetectionBox.getHitEntitiesByClass(AbstractArrow.class).forEach(Entity::remove);
					this.cancel();
				}
				mTicks++;
			}
		};
		runnable.runTaskTimer(mPlugin, Constants.TICKS_PER_SECOND * 2, 1);
	}
}
