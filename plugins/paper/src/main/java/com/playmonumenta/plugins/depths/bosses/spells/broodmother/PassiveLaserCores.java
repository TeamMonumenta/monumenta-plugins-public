package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class PassiveLaserCores extends Spell {

	public static final String SPELL_NAME = "Laser Cores";
	public static final int TRIGGER_COOLDOWN = 250;
	public static final int TELEGRAPH_TIME = 40;
	public static final int ACTIVE_TIME = 50;
	public static final double CORE_MOVEMENT_SPEED = 0.3;
	public static final double CORE_MOVEMENT_SPEED_DISCHARGING = 0.1;
	public static final double MAX_X_DISTANCE_FROM_BOSS = 50;
	public static final double DAMAGE = 20;
	public static final double IFRAMES = 5;

	private final LivingEntity mBoss;
	private final Location mCoreSpawnLocation;
	private final ArrayList<LaserCore> mCores = new ArrayList<>();
	private final double mBarrierX;
	private final Map<UUID, Integer> mHitPlayers = new HashMap<>();

	private int mSpellTicks = 0;
	private boolean mDischarging = false;

	public PassiveLaserCores(LivingEntity boss) {
		mBoss = boss;
		mCoreSpawnLocation = boss.getLocation().add(-34.5, -0.2, 0);
		mBarrierX = boss.getLocation().getX() - MAX_X_DISTANCE_FROM_BOSS;

		// Spawn initial cores, in strategic locations
		spawnCore(mCoreSpawnLocation.clone().add(12, 0, 12), new Vector(1, 0, 0));
		spawnCore(mCoreSpawnLocation.clone().add(-12, 0, 12), new Vector(1, 0, 0));
		spawnCore(mCoreSpawnLocation.clone().add(12, 0, -12), new Vector(1, 0, 0));
		spawnCore(mCoreSpawnLocation.clone().add(-12, 0, -12), new Vector(1, 0, 0));
	}

	@Override
	public void run() {
		if (mCores.size() == 0) {
			return;
		}

		if (mSpellTicks < TRIGGER_COOLDOWN) {
			mSpellTicks++;
			if (mSpellTicks >= TRIGGER_COOLDOWN - TELEGRAPH_TIME) {
				telegraphDischarge();
			}
			moveCores();
			return;
		}

		discharge();
		mSpellTicks = -ACTIVE_TIME;
	}

	public void removeAllCores() {
		mCores.forEach(LaserCore::remove);
	}

	public void spawnNewCores(int amount) {
		double theta = Math.random();
		double thetaIncrease = 2 * Math.PI / amount;

		for (int i = 0; i < amount; i++) {
			Vector dir = new Vector(FastUtils.cos(theta), 0, FastUtils.sin(theta));
			theta += thetaIncrease;

			spawnCore(mCoreSpawnLocation, dir);
		}
	}

	private void spawnCore(Location loc, Vector dir) {
		Location spawnLoc = loc.clone().setDirection(dir);
		new PartialParticle(Particle.FLASH, spawnLoc, 1).spawnAsBoss();

		Entity entity = mBoss.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
		if (entity instanceof BlockDisplay blockDisplay) {
			blockDisplay.setBlock(Material.RED_GLAZED_TERRACOTTA.createBlockData());
			blockDisplay.setTransformation(DisplayEntityUtils.getTranslation(-0.5f, -0.5f, -0.5f));
			mCores.add(new LaserCore(blockDisplay, spawnLoc.getDirection()));
		}
	}

	private void moveCores() {
		for (LaserCore core : mCores) {
			core.move();
			core.doContactDamage();
			core.unstuck();
		}
	}

	private void telegraphDischarge() {
		mDischarging = true;
		for (int i = 0; i < mCores.size() - 1; i++) {
			for (int j = i + 1; j < mCores.size(); j++) {
				new PPLine(Particle.CRIT, mCores.get(i).getLocation(), mCores.get(j).getLocation())
					.countPerMeter(0.25).offset(FastUtils.randomDoubleInRange(0.5, 1.5)).extra(0.05).spawnAsBoss();
			}
		}
	}

	private boolean shouldBeHit(Player player) {
		if (!mHitPlayers.containsKey(player.getUniqueId())) {
			mHitPlayers.put(player.getUniqueId(), Bukkit.getCurrentTick());
			return true;
		}

		if (mHitPlayers.get(player.getUniqueId()) <= Bukkit.getCurrentTick() - IFRAMES) {
			mHitPlayers.put(player.getUniqueId(), Bukkit.getCurrentTick());
			return true;
		}

		return false;
	}

	private void discharge() {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				for (int i = 0; i < mCores.size() - 1; i++) {
					playSound(mCores.get(i));
					for (int j = i + 1; j < mCores.size(); j++) {
						new PPLine(Particle.ELECTRIC_SPARK, mCores.get(i).getLocation(), mCores.get(j).getLocation())
							.countPerMeter(0.25).offset(FastUtils.randomDoubleInRange(0.5, 1.5)).spawnAsBoss();
						Hitbox.approximateCylinder(mCores.get(i).getLocation(), mCores.get(j).getLocation(), 0.65, true)
							.getHitPlayers(true).forEach(hitPlayer -> {
								hitPlayer.getWorld().playSound(hitPlayer, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, SoundCategory.HOSTILE, 0.5f, 2);
								if (shouldBeHit(hitPlayer)) {
									DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, false, SPELL_NAME);
								}
							});
						playSound(mCores.get(j));
					}
				}

				mTicks++;
				if (mTicks >= ACTIVE_TIME) {
					mDischarging = false;
					cancel();
				}
			}

			public void playSound(LaserCore core) {
				if (mTicks % 5 == 0) {
					mBoss.getWorld().playSound(core.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.HOSTILE, 0.55f, 2);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

	private class LaserCore {
		private final BlockDisplay mDisplay;
		private final Vector mDir;

		private int mStuckTicks = 0;

		public LaserCore(BlockDisplay display, Vector dir) {
			mDisplay = display;
			mDir = dir;
		}

		public void move() {
			Location newLoc = mDisplay.getLocation();
			RayTraceResult rayTraceResult = mDisplay.getWorld().rayTraceBlocks(newLoc, mDir, CORE_MOVEMENT_SPEED * 1.1, FluidCollisionMode.NEVER, true);

			if (rayTraceResult == null) {
				// Nothing hit. Check if it has reached the maximum X wall
				checkAndShowBarrierCollision(newLoc);
				if (mDischarging) {
					newLoc.add(mDir.clone().multiply(CORE_MOVEMENT_SPEED_DISCHARGING));
				} else {
					newLoc.add(mDir.clone().multiply(CORE_MOVEMENT_SPEED));
				}
				mDisplay.teleport(newLoc);
			} else {
				Block block = rayTraceResult.getHitBlock();
				BlockFace blockFace = rayTraceResult.getHitBlockFace();
				if (block != null && blockFace != null) {
					Vector bounceVector = new Vector(blockFace.getModX() != 0 ? -1 : 1, 1, blockFace.getModZ() != 0 ? -1 : 1);
					mDir.multiply(bounceVector);
				}
				Vector hitPos = rayTraceResult.getHitPosition();
				newLoc.set(hitPos.getX(), hitPos.getY(), hitPos.getZ());
				checkAndShowBarrierCollision(newLoc);
				mDisplay.teleport(newLoc);
			}
		}

		public void unstuck() {
			// Prevent it from getting stuck inside the Broodmother structure
			if (mDisplay.getLocation().getBlock().isSolid()) {
				mStuckTicks++;
				if (mStuckTicks > 20) {
					respawn();
				}
			} else {
				mStuckTicks = 0;
			}
		}

		public void respawn() {
			mDisplay.teleport(mCoreSpawnLocation);
			new PartialParticle(Particle.FLASH, mCoreSpawnLocation, 1).spawnFull();
		}

		public void doContactDamage() {
			new Hitbox.SphereHitbox(mDisplay.getLocation(), 1).getHitPlayers(true).forEach(hitPlayer -> {
				hitPlayer.getWorld().playSound(hitPlayer, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, SoundCategory.HOSTILE, 0.5f, 2);
				if (shouldBeHit(hitPlayer)) {
					DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, false, SPELL_NAME);
				}
			});
		}

		private void checkAndShowBarrierCollision(Location newLoc) {
			if (newLoc.getX() < mBarrierX) {
				mDir.multiply(new Vector(-1, 1, 1));
				Location particleLoc = newLoc.clone();
				particleLoc.setX(mBarrierX);
				mDisplay.teleport(mDisplay.getLocation().add(CORE_MOVEMENT_SPEED * 1.5, 0, 0));
				new PartialParticle(Particle.WAX_OFF, particleLoc, 20).delta(0, 0, 0.5).spawnAsBoss();
			}
		}

		public void remove() {
			new PartialParticle(Particle.FLASH, mDisplay.getLocation(), 1).spawnAsBoss();
			mDisplay.remove();
		}

		public Location getLocation() {
			return mDisplay.getLocation();
		}
	}

}
