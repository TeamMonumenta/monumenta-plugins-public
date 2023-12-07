package com.playmonumenta.plugins.bosses.spells;

import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellSpawnerMimic extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private @Nullable List<EntityNBT> mEntities;
	private @Nullable String mLosPool;
	private int mCooldown = 0;
	private int mSpawnCount = 1;
	private double mSpawnRadius = 3.0;

	public SpellSpawnerMimic(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	// spawnCount, spawnRadius (with check for possible spawning spaces)

	private void projectileSpawnAesthetics(Location start, Location target, @Nullable EntityNBT entityNBT) {
		new BukkitRunnable() {
			final Location mCurrent = start;
			final Location mTarget = target;
			final Location mAimLocation = mTarget.clone().add(0, 0.5, 0);
			final Vector mVelocity = new Vector(0, 0.75, 0);
			double mCurve = 0;

			@Override
			public void run() {
				double distance = mCurrent.distance(mAimLocation);
				if (distance <= 1) {
					if (entityNBT != null) {
						Entity spawnedEntity = entityNBT.spawn(mTarget);
						spawnAesthetics(mTarget, spawnedEntity);
					}
					cancel();
					return;
				}

				mCurve += 0.05;
				mVelocity.add(LocationUtils.getDirectionTo(mAimLocation, mCurrent).multiply(mCurve));

				if (mVelocity.length() > 0.5) {
					mVelocity.normalize().multiply(0.5);
				}

				mCurrent.add(mVelocity);
				mCurrent.getWorld().playSound(mCurrent, Sound.BLOCK_CHAIN_PLACE, SoundCategory.HOSTILE, 0.5f, 0.1f);
				new PartialParticle(Particle.REDSTONE, mCurrent, 3).delta(0.05)
					.data(new Particle.DustOptions(Color.fromRGB(165, 100, 100), 2))
					.spawnAsEnemy();
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void spawnAesthetics(Location spawnLocation, Entity spawnedEntity) {
		spawnLocation.getWorld().playSound(spawnLocation, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1, 0.5f);
		spawnLocation.getWorld().playSound(spawnLocation, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1, 0.5f);

		new PPCircle(Particle.FLAME, LocationUtils.getHalfHeightLocation(spawnedEntity), spawnedEntity.getWidth())
			.ringMode(true).extra(0).countPerMeter(4).spawnAsEnemy();
		new PPCircle(Particle.FLAME, LocationUtils.getHalfHeightLocation(spawnedEntity).clone().subtract(0, 0.25, 0), spawnedEntity.getWidth())
			.ringMode(true).extra(-0.05).directionalMode(true).delta(0, 1, 0).countPerMeter(4)
			.spawnAsEnemy();
		new PPCircle(Particle.FLAME, LocationUtils.getHalfHeightLocation(spawnedEntity).clone().add(0, 0.25, 0), spawnedEntity.getWidth())
			.ringMode(true).extra(0.05).directionalMode(true).delta(0, 1, 0).countPerMeter(4)
			.spawnAsEnemy();
		new PartialParticle(Particle.FLAME, spawnLocation, 50).delta(0.2, 0.05, 0.2).extra(0.05).spawnAsEnemy();
	}

	@Override
	public void run() {
		if (mEntities == null && mLosPool == null) {
			return;
		}

		for (int i = 0; i < mSpawnCount; i++) {
			EntityNBT entityNBT = null;
			if (mLosPool != null) {
				entityNBT = EntityNBT.fromEntityData(LibraryOfSoulsIntegration.getPool(mLosPool).keySet().stream().toList().get(0).getNBT());
			} else if (mEntities != null) {
				entityNBT = FastUtils.getRandomElement(mEntities);
			}

			// Try to find a random location nearby to spawn the mob.
			for (int j = 0; j < 20; j++) {
				Location spawnLoc = LocationUtils.randomLocationInCircle(mBoss.getLocation(), mSpawnRadius);
				if (spawnLoc.getBlock().isPassable() && LocationUtils.hasLineOfSight(mBoss.getEyeLocation(), spawnLoc)) {
					projectileSpawnAesthetics(mBoss.getEyeLocation(), spawnLoc, entityNBT);
					break;
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	public void setSpawnerEntities(List<EntityNBT> entities) {
		mEntities = entities;
	}

	public void setSpawnerLosPool(@Nullable String losPool) {
		mLosPool = losPool;
	}

	public void setCooldown(int cooldown) {
		mCooldown = cooldown;
	}

	public void setSpawnCount(int spawnCount) {
		mSpawnCount = spawnCount;
	}

	public void setSpawnRadius(double spawnRadius) {
		mSpawnRadius = spawnRadius;
	}
}
