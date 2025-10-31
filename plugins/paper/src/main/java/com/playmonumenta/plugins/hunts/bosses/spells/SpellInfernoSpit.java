package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SpellInfernoSpit extends Spell implements CoreElemental.CoreElementalBase {
	// Time needed to charge/ telegraph time
	private static final int CHARGE_TIME = 30;
	// Maximum number of projectiles the boss will shoot
	private static final int NUMBER_OF_TARGETS = 3;
	// Radius of the area after the projectile lands
	private static final double AOE_RADIUS = 2.8;
	// For how long the areas will last for
	private static final int AOE_LINGER = 5 * 20;
	// Projectile damage
	private static final int DAMAGE = 60;
	// Area damage
	private static final int AOE_DAMAGE = 35;
	private static final Particle.DustOptions ORANGE_PARTICLE = new Particle.DustOptions(Color.fromRGB(252, 94, 3), 2);
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final CoreElemental mQuarry;
	private final Location mStartLoc;
	private final World mWorld;

	public SpellInfernoSpit(Plugin plugin, LivingEntity boss, CoreElemental quarry, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
		mStartLoc = startLoc;
		mWorld = mBoss.getWorld();
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, CHARGE_TIME);
		// Charging
		BukkitRunnable runnable = new BukkitRunnable() {
			double mRadius = 5;
			int mT = 0;

			@Override
			public void run() {
				// Telegraph
				new PPCircle(Particle.REDSTONE, mBoss.getLocation(), mRadius)
					.count(30)
					.delta(0.25)
					.data(ORANGE_PARTICLE)
					.spawnAsEntityActive(mBoss);
				mRadius -= (double) 5 / CHARGE_TIME;
				mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1f, 0.5f + (float) mT++ / CHARGE_TIME);
				if (mRadius <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					jump();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void jump() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 20, true);
		Collections.shuffle(players);
		if (players.isEmpty()) {
			// No players to target
			return;
		}
		mBoss.setVelocity(new Vector(0, 1.2, 0));
		// Sound
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_MAGMA_CUBE_JUMP, SoundCategory.HOSTILE, 2f, 0.5f);
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTick = 0;

			@Override
			public void run() {
				mBoss.setRotation(mTick * 15, 0);
				if (mTick++ == 20) {
					for (int i = 0; i < Math.min(NUMBER_OF_TARGETS, players.size()); i++) {
						// Effects
						mWorld.playSound(mBoss.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 2f, 0.67f);
						mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 2f, 0.5f);
						projectileBullet(mBoss.getEyeLocation(), LocationUtils.getDirectionTo(players.get(i).getLocation(), mBoss.getEyeLocation()));
					}
				}
				if (mTick > 20 && mBoss.isOnGround()) {
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void projectileBullet(Location origin, Vector direction) {
		Location location = origin.clone();
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				new PartialParticle(Particle.REDSTONE, location)
					.count(10)
					.delta(0.25)
					.data(ORANGE_PARTICLE)
					.spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.FLAME, location)
					.count(15)
					.delta(0.25)
					.extra(1)
					.spawnAsEntityActive(mBoss);

				RayTraceResult result = mWorld.rayTrace(location, direction, 0.01, FluidCollisionMode.NEVER, true, 0.5, null);
				if (result != null) {
					if (result.getHitEntity() != null && result.getHitEntity() instanceof Player player) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, true, getSpellName());
						EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 40, player, mBoss);
						spawnAOE(result.getHitPosition().toLocation(mWorld));
						this.cancel();
						mActiveRunnables.remove(this);
					} else if (result.getHitBlock() != null) {
						spawnAOE(result.getHitPosition().toLocation(mWorld));
						this.cancel();
						mActiveRunnables.remove(this);
					}
				}
				location.add(direction);
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void spawnAOE(Location location) {
		Location pendingLocation = LocationUtils.fallToGround(location.add(0, 3, 0), mStartLoc.getY() - 5);

		// Spawn Fire
		for (Block block : LocationUtils.getNearbyBlocks(location.getBlock(), 3)) {
			if (FastUtils.randomDoubleInRange(0, 1) <= 0.1 && block.isSolid()) {
				Block up = block.getRelative(BlockFace.UP);
				if (up.isEmpty()) {
					if (TemporaryBlockChangeManager.INSTANCE.changeBlock(up, Material.FIRE, 12000)) {
						mQuarry.addChangedBlock(up);
					}
				}
			}
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				// Effects
				if (mT % 3 == 0) {
					for (double r = 0; r < AOE_RADIUS; r += AOE_RADIUS / 5d) {
						double radius = r;
						new PPParametric(Particle.FLAME, pendingLocation, (t, builder) -> {
							Location finalLocation = LocationUtils.fallToGround(pendingLocation.clone().add(radius * FastUtils.cosDeg(t * 360), 15, radius * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5);
							Vector finalVector = LocationUtils.getDirectionTo(pendingLocation, finalLocation);
							builder.location(finalLocation);
							builder.offset(finalVector.getX(), 1, finalVector.getZ());
						})
							.count(20)
							.extra(FastUtils.randomDoubleInRange(0.05, 0.1))
							.directionalMode(true)
							.spawnAsEntityActive(mBoss);
					}
					mWorld.playSound(location, Sound.ENTITY_MAGMA_CUBE_SQUISH, SoundCategory.HOSTILE, 1f, 0.5f);
				}
				new PPParametric(Particle.REDSTONE, pendingLocation, (t, builder) -> {
					Location finalLocation = LocationUtils.fallToGround(pendingLocation.clone().add(AOE_RADIUS * FastUtils.cosDeg(t * 360), 15, AOE_RADIUS * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5);
					Vector finalVector = LocationUtils.getDirectionTo(pendingLocation, finalLocation);
					builder.location(finalLocation);
					builder.offset(finalVector.getX(), 0.5, finalVector.getZ());
				})
					.count(50)
					.data(ORANGE_PARTICLE)
					.directionalMode(true)
					.spawnAsEntityActive(mBoss);

				// Damage players
				for (Player player : PlayerUtils.playersInCylinder(pendingLocation, AOE_RADIUS, 2)) {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, AOE_DAMAGE, null, false, false, getSpellName());
					EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 40, player, mBoss);
				}
				if (mT++ * 5 > AOE_LINGER) {
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 7;
	}

	@Override
	public String getSpellName() {
		return "Inferno Spit";
	}

	@Override
	public String getSpellChargePrefix() {
		return "Charging";
	}

	@Override
	public int getChargeDuration() {
		return CHARGE_TIME;
	}

	@Override
	public int getSpellDuration() {
		return 20;
	}

	@Override
	public boolean canRun() {
		return !mQuarry.mIsCastingBanish;
	}
}
