package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class Haunted {

	public static final String DESCRIPTION = "Your regrets haunt you.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"A looming figure haunts you relentlessly",
			"until the dungeon has been completed,",
			"only moving when you do."
		}
	};
	public static final double MAX_SPEED = 0.1;
	public static final double DAMAGE = 0.3; //percentage
	public static final double RANGE = 50;

	private static void followPlayer(Player p, ArmorStand armorStand) {
		new BukkitRunnable() {
			Location mPLoc = p.getLocation();
			final Location mSLoc = armorStand.getLocation();

			double mRadian = 0;
			double mSpeed = MAX_SPEED;

			int mHitTimer = 0;
			int mBeatCD = 0;
			@Override
			public void run() {
				if (!p.isOnline() || !armorStand.isValid()) {
					this.cancel();

					// We "hide" the armor stand by removing its equipment and glowing, and use its location to spawn a new shade later
					armorStand.getEquipment().clear();
					armorStand.setGlowing(false);
					return;
				} else if (p.isDead()) {
					return;
				}

				if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR || mPLoc.distance(p.getLocation()) < 0.005) {
					return;
				}

				double distance = armorStand.getLocation().distance(p.getLocation());

				if (distance > RANGE) {
					armorStand.remove();
					this.cancel();
					summonBeast(p, p.getLocation().add(0, 10, 0));
					return;
				} else if (distance > RANGE / 2) {
					mSpeed = MAX_SPEED;
				} else {
					mSpeed = MAX_SPEED * 1.5;
				}
				// Change the direction of the stand and its head
				Vector direction = LocationUtils.getDirectionTo(p.getLocation(), mSLoc);
				mSLoc.setDirection(direction);
				armorStand.setHeadPose(new EulerAngle(Math.toRadians(mSLoc.getPitch()), 0, 0));
				armorStand.teleport(mSLoc.clone().add(0, FastMath.sin(mRadian) * 0.35, 0));
				mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks

				// Hit detection
				if (mHitTimer <= 0 && distance < 1) {
					p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1f, 2f);
					p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);

					Location loc = p.getLocation().add(0, 1, 0);
					BossUtils.bossDamagePercent(armorStand, p, DAMAGE);
					if (p.isDead()) {
						p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_GHAST_HURT, 1f, 0.5f);
						p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 0.65f);

						new PartialParticle(Particle.SOUL, loc, 70, 0, 0, 0, 0.15)
							.minimumMultiplier(false).spawnAsPlayerActive(p);
						new PartialParticle(Particle.SMOKE_LARGE, loc, 40, 0, 0, 0, 0.185)
							.minimumMultiplier(false).spawnAsPlayerActive(p);
					} else {
						new PartialParticle(Particle.SMOKE_LARGE, loc, 15, 0, 0, 0, 0.125)
							.minimumMultiplier(false).spawnAsPlayerActive(p);
					}
					mHitTimer = 10;
				}
				mHitTimer--;

				// Visuals
				new PartialParticle(Particle.SMOKE_LARGE, armorStand.getLocation().add(0, 1, 0), 1, 0.3, 0.4, 0.3, 0)
					.minimumMultiplier(false).spawnAsEntityActive(armorStand);
				new PartialParticle(Particle.SOUL, armorStand.getLocation().add(0, 1, 0), 1, 0.3, 0.4, 0.3, 0.025)
					.minimumMultiplier(false).spawnAsEntityActive(armorStand);

				distance = mSLoc.distance(p.getLocation());
				if (distance <= 16) {
					int beatPeriod = Math.max(7, (int) (distance * 2));
					if (mBeatCD <= 0) {
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2, 0);
						int beatRate = 5;
						if (distance <= 6) {
							beatRate = 3;
							if (distance <= 3) {
								beatRate = 2;
							}
						}
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
							() -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2, 0), beatRate);
						mBeatCD = beatPeriod;
					}
				}
				mBeatCD--;

				mSLoc.add(direction.multiply(mSpeed));
				mPLoc = p.getLocation();
			}
		}.runTaskTimer(Plugin.getInstance(), 0L, 1L);
	}

	private static void summonBeast(Player p, Location loc) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		for (Entity nearbyEntity : p.getLocation().getNearbyEntities(100, 100, 100)) {
			if (nearbyEntity instanceof ArmorStand && nearbyEntity.getScoreboardTags().contains(phantomName + p.getUniqueId())) {
				return;
			}
		}
		ArmorStand armorStand = (ArmorStand) LibraryOfSoulsIntegration.summon(loc, "LoomingConsequence");
		armorStand.addScoreboardTag(phantomName + p.getUniqueId());
		followPlayer(p, armorStand);
	}

	public static void applyModifiers(Player p) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		Location loc = p.getLocation().add(new Vector(0, 10, 0));
		ArmorStand armorStand = null;
		List<Entity> nearbyEntities = (List<Entity>) p.getWorld().getNearbyEntities(p.getLocation(), 100, 100, 100);
		for (Entity nearbyEntity : nearbyEntities) {
			if (nearbyEntity instanceof ArmorStand stand && nearbyEntity.getScoreboardTags().contains(phantomName + p.getUniqueId())) {

				// We found the old "hidden" armor stand, so now we use its location to spawn a fresh Shade, and remove the old one.
				Location standLoc = stand.getLocation();
				armorStand = (ArmorStand) LibraryOfSoulsIntegration.summon(standLoc, "LoomingConsequence");
				armorStand.addScoreboardTag(phantomName + p.getUniqueId());

				stand.remove();
			}
		}
		if (armorStand == null) {
			BukkitScheduler scheduler = Bukkit.getScheduler();
			scheduler.runTaskLater(Plugin.getInstance(), () -> {
				summonBeast(p, loc);
			}, 100L);
		} else {
			followPlayer(p, armorStand);
		}
	}

}
