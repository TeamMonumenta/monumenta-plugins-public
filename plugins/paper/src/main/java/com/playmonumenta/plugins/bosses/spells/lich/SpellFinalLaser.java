package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class SpellFinalLaser extends Spell {

	private Plugin mPlugin;
	private double mT = 20 * 4;
	private int mSoloCooldown = 20 * 15;
	private double mCooldown;
	private double mMaxFactor = 1.35;
	private Location mCenter;
	private double mRange;
	private LivingEntity mBoss;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<Player>();

	private static final double BOX_SIZE = 0.5;
	private static final double CHECK_INCREMENT = 0.5;

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
			Material.AIR,
			Material.COMMAND_BLOCK,
			Material.CHAIN_COMMAND_BLOCK,
			Material.REPEATING_COMMAND_BLOCK,
			Material.BEDROCK,
			Material.BARRIER,
			Material.SPAWNER,
			Material.WATER,
			Material.LAVA,
			Material.END_PORTAL
		);

	public SpellFinalLaser(Plugin plugin, LivingEntity boss, Location loc, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
	}

	@Override
	public void run() {
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}
		//cooldown
		double cooldownFactor = Math.min(mMaxFactor, (Math.sqrt(mPlayers.size()) / 5 + 0.8) / 4 * 3);
		mCooldown = mSoloCooldown / cooldownFactor;
		mT -= 5;
		if (mT <= 0) {
			mT += mCooldown;
			laser();
		}
	}

	private void laser() {
		List<Player> potentialTargets = Lich.playersInRange(mBoss.getLocation(), mRange, true);
		List<Player> toRemove = new ArrayList<Player>();
		for (Player target : potentialTargets) {
			if (target.getLocation().getY() > mCenter.getY() + 3) {
				launch(target);
				toRemove.add(target);
			}
		}
		//remove all targeted players above 5 blocks of the ground, and target 1/3 of the remaining players
		potentialTargets.removeAll(toRemove);
		Collections.shuffle(potentialTargets);
		for (int i = 0; i < potentialTargets.size() / 3; i++) {
			launch(potentialTargets.get(i));
		}
	}

	private void launch(Player target) {
		BukkitRunnable runA = new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				Location startLocation = mBoss.getLocation().add(0, 5, 0);
				Location targetedLocation = target.getLocation().add(0, target.getEyeHeight() / 2, 0);

				World world = mBoss.getWorld();
				BoundingBox movingLaserBox = BoundingBox.of(startLocation, BOX_SIZE, BOX_SIZE, BOX_SIZE);
				Vector vector = new Vector(
					targetedLocation.getX() - startLocation.getX(),
					targetedLocation.getY() - startLocation.getY(),
					targetedLocation.getZ() - startLocation.getZ()
				);

				LocationUtils.travelTillObstructed(
						world,
						movingLaserBox,
						startLocation.distance(targetedLocation),
						vector,
						CHECK_INCREMENT,
						(Location loc) -> {
							new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsBoss();
							new PartialParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1).spawnAsBoss();
						},
						1,
						6
				);

				if (mTicks % 8 == 0) {
					target.playSound(target.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 2) {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 4) {
					target.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 6) {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (mTicks / 100f) * 1.5f);
				}

				if (mTicks >= 100) {
					world.playSound(movingLaserBox.getCenter().toLocation(world), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);
					new PartialParticle(Particle.EXPLOSION_LARGE, movingLaserBox.getCenter().toLocation(world), 30, 0, 0, 0, 0.3).spawnAsBoss();
					breakBlocks(movingLaserBox.getCenter().toLocation(world));
					if (movingLaserBox.overlaps(target.getBoundingBox())) {
						BossUtils.bossDamage(mBoss, target, 55);
						MovementUtils.knockAway(mCenter, target, 3.2f);
						Lich.cursePlayer(mPlugin, target);
					}
					this.cancel();
				}
				if (Lich.bossDead()) {
					this.cancel();
				}
				mTicks += 2;
			}
		};
		runA.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runA);
	}

	private void breakBlocks(Location l) {
		List<Block> badBlockList = new ArrayList<Block>();
		Location testloc = l.clone();
		for (int x = -1; x <= 1; x++) {
			testloc.setX(l.getX() + x);
			for (int z = -1; z <= 1; z++) {
				testloc.setZ(l.getZ() + z);
				for (int y = -1; y <= 1; y++) {
					testloc.setY(l.getY() + y + 0.2);

					Block block = testloc.getBlock();
					if (!mIgnoredMats.contains(block.getType())) {
						badBlockList.add(block);
					}
				}
			}
		}

		/* If there are any blocks, destroy all blocking blocks */
		if (badBlockList.size() > 0) {

			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mBoss, l, badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				if (block.getState() instanceof Container) {
					block.breakNaturally();
				} else {
					block.setType(Material.AIR);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
