package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collection;
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
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellFallingIcicle extends Spell {
	private static final int RESPAWN_DURATION = 20 * 3;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final BoundingBox mBox;
	private boolean mCurrentlyRespawning = false;


	public SpellFallingIcicle(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mBox = BoundingBox.of(mBoss.getLocation(), 6.5, 1, 6.5);
		mBox.expand(0, 15, 0, 0, 25, 0);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Collection<Entity> projectiles = world.getNearbyEntities(mBox, (proj) -> proj instanceof Projectile);
		for (Entity entity : projectiles) {
			Projectile proj = (Projectile) entity;
			if (proj instanceof AbstractArrow) { // check if it is an arrow or a trident
				// tridents inherit from ThrowableProjectile and AbstractArrow
				if (((AbstractArrow) proj).getAttachedBlock() != null) {
					proj.setShooter(mBoss);
					runIcicleFall(proj);
				}
			} else if (proj instanceof ThrowableProjectile) { // otherwise check for any other throwable projectile
				if (((ThrowableProjectile) proj).getBoundingBox().overlaps(mBox)) {
					proj.setShooter(mBoss);
				}
			}
		}
	}

	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (mBox.overlaps(event.getEntity().getBoundingBox())) {
			runIcicleFall(event.getEntity());
		}
	}

	private void runIcicleFall(Projectile proj) {
		if (mCurrentlyRespawning) {
			/* No arrow checks while still respawning */
			return;
		}
		if (proj.isValid()) {
			proj.remove();
		}
		Location loc = mBoss.getLocation();
		World world = mBoss.getWorld();

		//Create the bounding box for the whole icicle off of the smallest and largest x, y, z values
		double minX = loc.getX();
		double minY = loc.getY();
		double minZ = loc.getZ();

		double maxX = minX;
		double maxY = minY;
		double maxZ = minZ;

		List<Location> icicle = new ArrayList<>();
		for (int x = -5; x <= 5; x++) {
			for (int z = -5; z <= 5; z++) {
				for (int y = 20; y >= -5; y--) {
					//Can't make location values more optimized - each one needs to be a separate Location object
					Location l = loc.clone().add(x, y, z);
					if (l.getBlock().getType() == Material.ICE) {

						if (l.getX() < minX) {
							minX = l.getX();
						} else if (l.getX() > maxX) {
							maxX = l.getX();
						}

						if (l.getY() < minY) {
							minY = l.getY();
						} else if (l.getY() > maxY) {
							maxY = l.getY();
						}

						if (l.getZ() < minZ) {
							minZ = l.getZ();
						} else if (l.getZ() > maxZ) {
							maxZ = l.getZ();
						}

						icicle.add(l);
						l.getBlock().setType(Material.AIR);
					}
				}
			}
		}

		if (icicle.size() == 0) {
			return;
		}

		BoundingBox icicleBox = BoundingBox.of(new Vector(minX, minY, minZ), new Vector(maxX, maxY, maxZ));

		runIcicleRespawn(icicle);

		List<FallingBlock> ices = new ArrayList<>(icicle.size());
		for (Location l : icicle) {
			ices.add(world.spawnFallingBlock(l, Bukkit.createBlockData(Material.ICE)));
		}
		for (FallingBlock ice : ices) {
			ice.setVelocity(new Vector(0, -2, 0));
			ice.setDropItem(false);
			EntityUtils.disableBlockPlacement(ice);
		}

		FallingBlock ice = ices.get(0);

		new BukkitRunnable() {
			int mTicks = 0;
			double mPreviousY = ice.getLocation().getY();

			@Override
			public void run() {

				double diffY = ice.getLocation().getY() - mPreviousY;
				icicleBox.shift(new Vector(0, diffY, 0));

				//Cancel automatically if it takes too long
				if (mTicks >= 40) {
					new PartialParticle(Particle.FIREWORKS_SPARK, ice.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, ice.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.BLOCK_CRACK, ice.getLocation(), 40, 0, 0, 0, 1, Bukkit.createBlockData(Material.ICE)).spawnAsEntityActive(mBoss);
					for (FallingBlock b : ices) {
						Location bLoc = b.getLocation();
						bLoc.add(0, -1, 0);
						if (bLoc.getBlock().getType() == Material.FROSTED_ICE) {
							bLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
						}
						b.remove();
					}
					this.cancel();
				}
				if (ice.isOnGround() || ice.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
					new PartialParticle(Particle.FIREWORKS_SPARK, ice.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, ice.getLocation(), 40, 0, 0, 0, 1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.BLOCK_CRACK, ice.getLocation(), 40, 0, 0, 0, 1, Bukkit.createBlockData(Material.ICE)).spawnAsEntityActive(mBoss);
					for (FallingBlock b : ices) {
						Location bLoc = b.getLocation();
						bLoc.add(0, -1, 0);
						if (bLoc.getBlock().getType() == Material.FROSTED_ICE) {
							bLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
						}
						b.remove();
					}
					this.cancel();
				}

				//Method gets Frost Giant instance, checks if hitboxes overlap, performs functions there
				if (FrostGiant.testHitByIcicle(icicleBox)) {
					for (FallingBlock b : ices) {
						b.remove();
					}
					this.cancel();
				}

				mTicks += 2;
				mPreviousY = ice.getLocation().getY();
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}


	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void runIcicleRespawn(List<Location> icicle) {
		if (icicle.size() > 0) {
			mCurrentlyRespawning = true;

			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;
				int mCount = 0;

				@Override
				public void run() {
					/* Replace a slice of blocks every tick, so that all blocks are replaced over the duration */
					for (; mCount < (((icicle.size() - 1) * mTicks) / RESPAWN_DURATION) + 1; mCount++) {
						icicle.get(mCount).getBlock().setType(Material.ICE);
					}
					if (mCount >= icicle.size()) {
						mCurrentlyRespawning = false;

						/* Remove any arrows that had already been stuck in the ice while it was respawning */
						for (AbstractArrow proj : mBoss.getWorld().getNearbyEntitiesByType(AbstractArrow.class, mBoss.getLocation(), 25)) {
							if (proj.getBoundingBox().overlaps(mBox)) {
								proj.remove();
							}
						}

						this.cancel();
						return;
					}
					mTicks++;
				}
			};
			runnable.runTaskTimer(mPlugin, 20 * 2, 1);
		}
	}
}
