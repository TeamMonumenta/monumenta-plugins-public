package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class TrabemTemporis extends Spell {
	private static final double LENGTH = 25;
	private static final double HEIGHT = 25;
	private static final int TRAVEL_TIME = 12 * 20;
	private static final double TRAVEL_DISTANCE = 60;
	private static final double DAMAGE = 80;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;
	private final TealSpirit mTealSpirit;

	public TrabemTemporis(LivingEntity boss, Location center, int cooldownTicks, TealSpirit tealSpirit) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mTealSpirit = tealSpirit;
	}

	@Override
	public void run() {
		mTealSpirit.setInterspellCooldown(TRAVEL_TIME * 2 + 2 * 20);

		PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> player.sendMessage(ChatColor.DARK_AQUA + "endless lines of time itself will seal your fate!"));

		Vector dir1 = (FastUtils.RANDOM.nextBoolean() ? new Vector(1, 0, 0) : new Vector(0, 0, 1)).multiply(FastUtils.RANDOM.nextBoolean() ? 1 : -1);
		Vector dir2 = new Vector(dir1.getZ(), 0, dir1.getX()).multiply(FastUtils.RANDOM.nextBoolean() ? 1 : -1);

		Location startingLocation = mCenter.clone().add(dir1.clone().multiply(-TRAVEL_DISTANCE / 2)).setDirection(dir1.clone());

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			Location mLocation = startingLocation;
			List<FallingBlock> mBlocks = buildWall(startingLocation);

			@Override
			public void run() {
				if (mT == TRAVEL_TIME) {
					removeWall(mBlocks);
					mLocation = mCenter.clone().add(dir2.clone().multiply(-TRAVEL_DISTANCE / 2)).setDirection(dir2.clone());
					mBlocks = buildWall(mLocation);
				} else if (mT >= TRAVEL_TIME * 2) {
					this.cancel();
					return;
				}

				// Annoying bug: the blocks only visually teleport twice a second or so despite their position changing 20 times a second

				Vector travel = mLocation.getDirection().clone().multiply(TRAVEL_DISTANCE / TRAVEL_TIME);
				mBlocks.forEach(block -> block.teleport(block.getLocation().clone().add(travel)));
				mLocation.add(travel);

				if (mT % 10 == 0) {
					for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
						BoundingBox playerBox = player.getBoundingBox();
						Location playerLoc = player.getLocation();
						Location closestLoc = null;
						for (FallingBlock block : mBlocks) {
							if (block.getBoundingBox().expand(0.5, 0, 0.5).overlaps(playerBox)) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, null, false, true, "Trabem Temporis");
								closestLoc = block.getLocation();
								break;
							}

							if (closestLoc == null || closestLoc.distanceSquared(playerLoc) > block.getLocation().distanceSquared(playerLoc)) {
								closestLoc = block.getLocation();
							}
						}

						if (closestLoc != null) {
							player.playSound(closestLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.5f);
							player.playSound(closestLoc, Sound.BLOCK_DEEPSLATE_TILES_STEP, 2.0f, 0.7f);
						}
					}
				}

				mT++;
			}

			// If this gets cancelled by any means, remove the blocks
			@Override
			public synchronized void cancel() {
				removeWall(mBlocks);
				super.cancel();
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private List<FallingBlock> buildWall(Location loc) {
		List<FallingBlock> blocks = new ArrayList<>();

		World world = loc.getWorld();

		Vector dir = loc.getDirection();
		Vector perp = new Vector(-dir.getZ(), dir.getY(), dir.getX());
		perp.multiply(LENGTH / 2);

		// Bottom back corner
		Location corner1 = loc.clone().add(perp);
		// Top front corner
		Location corner2 = loc.clone().add(0, HEIGHT, 0).subtract(perp);

		BoundingBox box = BoundingBox.of(corner1, corner2);

		for (double x = box.getMinX(); x <= box.getMaxX(); x++) {
			for (double y = box.getMinY(); y <= box.getMaxY(); y++) {
				for (double z = box.getMinZ(); z <= box.getMaxZ(); z++) {
					FallingBlock block = world.spawnFallingBlock(new Location(world, x, y, z), Material.SMOOTH_QUARTZ.createBlockData());
					block.setGravity(false);
					block.setInvulnerable(true);
					block.setDropItem(false);
					blocks.add(block);
				}
			}
		}

		playStartingSound(loc);

		return blocks;
	}

	private void removeWall(List<FallingBlock> blocks) {
		blocks.forEach(Entity::remove);
	}

	private void playStartingSound(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
			player.playSound(player.getLocation().add(LocationUtils.getDirectionTo(loc, player.getLocation())).multiply(3), Sound.BLOCK_PISTON_EXTEND, 1, 0.5f);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public boolean canRun() {
		return !mTealSpirit.isInterspellCooldown();
	}
}
